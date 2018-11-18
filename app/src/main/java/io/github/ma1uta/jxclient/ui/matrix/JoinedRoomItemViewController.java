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
import io.github.ma1uta.matrix.client.model.sync.JoinedRoom;
import io.github.ma1uta.matrix.event.RoomAvatar;
import io.github.ma1uta.matrix.event.RoomName;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * JoinedRoomItemViewController.
 */
public class JoinedRoomItemViewController implements Initializable {

    @FXML
    private ImageView roomAvatar;

    @FXML
    private Label roomName;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    /**
     * Parse joined room.
     *
     * @param joinedRoom the joined room.
     * @param account    the account.
     * @return UI actions.
     */
    public List<Runnable> parse(JoinedRoom joinedRoom, MatrixAccount account) {
        var actions = new ArrayList<Runnable>();
        joinedRoom.getState().getEvents().forEach(event -> {
            if (event instanceof RoomName) {
                RoomName roomName = (RoomName) event;
                actions.add(() -> this.roomName.setText(roomName.getContent().getName()));
            }
            if (event instanceof RoomAvatar) {
                RoomAvatar roomAvatar = (RoomAvatar) event;
                account.getDownloader().download(roomAvatar.getContent().getUrl(), url -> {
                    try {
                        this.roomAvatar.setImage(new Image(url));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });

        return actions;
    }
}
