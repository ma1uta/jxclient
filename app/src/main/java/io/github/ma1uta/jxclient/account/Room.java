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

package io.github.ma1uta.jxclient.account;

import io.github.ma1uta.jxclient.matrix.MatrixAccount;
import io.github.ma1uta.jxclient.ui.JoinedRoomItemViewController;
import io.github.ma1uta.jxclient.ui.JoinedRoomViewController;
import io.github.ma1uta.matrix.client.model.sync.JoinedRoom;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * The room.
 */
public class Room {

    private String roomId;
    private final ResourceBundle i18n;
    private final Consumer<Room> selectAction;

    private JoinedRoomItemViewController roomItemViewController;
    private Node roomItemView;

    private JoinedRoomViewController roomViewController;
    private Node roomView;

    public Room(ResourceBundle i18n, Consumer<Room> selectAction) {
        this.i18n = i18n;
        this.selectAction = selectAction;
    }

    /**
     * Parse joined room.
     *
     * @param roomId     the room id.
     * @param joinedRoom the room data.
     * @param account    the account.
     * @return UI actions.
     */
    public List<Runnable> parse(String roomId, JoinedRoom joinedRoom, MatrixAccount account) {
        if (getRoomId() == null) {
            return initRoom(roomId, joinedRoom, account);
        } else {
            return updateRoom(joinedRoom, account);
        }
    }

    protected List<Runnable> initRoom(String roomId, JoinedRoom joinedRoom, MatrixAccount account) {
        setRoomId(roomId);

        try {
            FXMLLoader roomItemLoader = new FXMLLoader(Room.class.getResource("/io/github/ma1uta/jxclient/ui/JoinedRoomItem.fxml"), i18n);
            roomItemView = roomItemLoader.load();
            roomItemViewController = roomItemLoader.getController();
            roomItemView.setOnMouseClicked(e -> selectAction.accept(this));

            FXMLLoader roomLoader = new FXMLLoader(Room.class.getResource("/io/github/ma1uta/jxclient/ui/JoinedRoom.fxml"), i18n);
            roomView = roomLoader.load();
            roomViewController = roomLoader.getController();

            return updateRoom(joinedRoom, account);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    protected List<Runnable> updateRoom(JoinedRoom joinedRoom, MatrixAccount account) {
        var actions = new ArrayList<Runnable>();
        actions.addAll(roomItemViewController.parse(joinedRoom, account));
        actions.addAll(roomViewController.parse(joinedRoom, account));
        return actions;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public JoinedRoomItemViewController getRoomItemViewController() {
        return roomItemViewController;
    }

    public void setRoomItemViewController(JoinedRoomItemViewController roomItemViewController) {
        this.roomItemViewController = roomItemViewController;
    }

    public Node getRoomItemView() {
        return roomItemView;
    }

    public void setRoomItemView(Node roomItemView) {
        this.roomItemView = roomItemView;
    }

    public JoinedRoomViewController getRoomViewController() {
        return roomViewController;
    }

    public void setRoomViewController(JoinedRoomViewController roomViewController) {
        this.roomViewController = roomViewController;
    }

    public Node getRoomView() {
        return roomView;
    }

    public void setRoomView(Node roomView) {
        this.roomView = roomView;
    }
}
