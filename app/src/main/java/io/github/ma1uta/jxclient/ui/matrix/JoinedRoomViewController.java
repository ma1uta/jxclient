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
import io.github.ma1uta.jxclient.ui.matrix.message.AbstractMessage;
import io.github.ma1uta.jxclient.ui.matrix.message.MessageLoader;
import io.github.ma1uta.matrix.client.model.sync.JoinedRoom;
import io.github.ma1uta.matrix.client.model.sync.Timeline;
import io.github.ma1uta.matrix.event.Event;
import io.github.ma1uta.matrix.event.RoomMessage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * JoinedRoomItemViewController.
 */
public class JoinedRoomViewController implements Initializable {

    private ResourceBundle i18n;
    private MessageLoader messageLoader;
    private ObservableMap<String, AbstractMessage<?>> messages;

    @FXML
    private FlowPane timeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.i18n = resources;
        this.messageLoader = new MessageLoader(i18n);
        this.messages = FXCollections.observableHashMap();
    }

    /**
     * Parse joined room.
     *
     * @param joinedRoom the joined room.
     * @param account    the account.
     */
    public void parse(JoinedRoom joinedRoom, MatrixAccount account) {
        Timeline timeline = joinedRoom.getTimeline();
        if (timeline != null) {
            List<Event> events = timeline.getEvents();
            if (events != null) {
                events.forEach(e -> {
                    try {
                        if (e instanceof RoomMessage) {
                            RoomMessage<?> roomMessage = (RoomMessage<?>) e;
                            var pair = messageLoader.load(roomMessage, account);
                            if (pair != null) {
                                account.updateUI(() -> this.timeline.getChildren().add(pair.getValue()));
                                messages.put(roomMessage.getEventId(), pair.getKey());
                            }
                        }
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                });
            }
        }
    }
}
