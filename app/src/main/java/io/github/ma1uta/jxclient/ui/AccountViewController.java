/*
 * Copyright sablintolya@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ma1uta.jxclient.ui;

import io.github.ma1uta.jxclient.account.Room;
import io.github.ma1uta.jxclient.matrix.MatrixAccount;
import io.github.ma1uta.matrix.client.model.sync.SyncResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Main account controller.
 */
public class AccountViewController implements Initializable {

    private MatrixAccount account;

    @FXML
    private VBox roomList;

    @FXML
    private SplitPane accountView;

    private ResourceBundle i18n;

    private ObservableMap<String, Room> rooms;

    public void setAccount(MatrixAccount account) {
        this.account = account;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.i18n = resources;
        this.rooms = FXCollections.observableHashMap();
    }

    /**
     * Parse account data.
     *
     * @param syncResponse account data.
     * @param account      account.
     */
    public void parse(SyncResponse syncResponse, MatrixAccount account) {
        var actions = new ArrayList<Runnable>();
        var rooms = syncResponse.getRooms();
        if (rooms != null) {
            var join = rooms.getJoin();
            if (join != null) {
                join.forEach((roomId, roomData) -> {
                    var room = this.rooms.get(roomId);
                    if (room == null) {
                        room = new Room(i18n, this::selectRoom);
                        this.rooms.put(roomId, room);
                        final var newRoom = room;
                        actions.add(() -> roomList.getChildren().add(newRoom.getRoomItemView()));
                    }
                    try {
                        actions.addAll(room.parse(roomId, roomData, account));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        if (!actions.isEmpty()) {
            actions.forEach(action -> Platform.runLater(() -> {
                try {
                    action.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }
    }

    /**
     * View room in the main view.
     *
     * @param room selected room.
     */
    public void selectRoom(Room room) {
        accountView.getItems().set(1, room.getRoomView());
    }
}
