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

package io.github.ma1uta.jxclient.ui.matrix;

import io.github.ma1uta.jxclient.matrix.MatrixAccount;
import io.github.ma1uta.jxclient.matrix.Room;
import io.github.ma1uta.matrix.client.model.sync.SyncResponse;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main account controller.
 */
public class AccountViewController implements Initializable {

    private static final double ROTATION = -360D;

    private MatrixAccount account;

    @FXML
    private VBox roomList;

    @FXML
    private SplitPane accountView;

    @FXML
    private Label sync;
    private RotateTransition syncRotation;

    private ResourceBundle i18n;

    private ObservableMap<String, Room> rooms;

    public void setAccount(MatrixAccount account) {
        this.account = account;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.i18n = resources;
        this.rooms = FXCollections.observableHashMap();
        syncRotation = new RotateTransition(Duration.seconds(2), sync);
        syncRotation.setByAngle(ROTATION);
        syncRotation.setCycleCount(Animation.INDEFINITE);
        syncRotation.setInterpolator(Interpolator.LINEAR);
        syncRotation.play();
    }

    /**
     * Parse account data.
     *
     * @param syncResponse account data.
     */
    public void parse(SyncResponse syncResponse) {
        var rooms = syncResponse.getRooms();
        try {
            if (rooms != null) {
                var join = rooms.getJoin();
                if (join != null) {
                    join.forEach((roomId, roomData) -> {
                        var room = this.rooms.get(roomId);
                        if (room == null) {
                            room = new Room(i18n, this::selectRoom);
                            this.rooms.put(roomId, room);
                            final var newRoom = room;
                            account.updateUI(() -> roomList.getChildren().add(newRoom.getRoomItemView()));
                        }
                        try {
                            room.parse(roomId, roomData, account);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        } finally {
            if (Animation.Status.RUNNING == syncRotation.getStatus()) {
                account.updateUI(() -> {
                    syncRotation.stop();
                    sync.setVisible(false);
                });
            }
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
