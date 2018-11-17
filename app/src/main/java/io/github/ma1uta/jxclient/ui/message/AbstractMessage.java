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

package io.github.ma1uta.jxclient.ui.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.ma1uta.jxclient.matrix.MatrixAccount;
import io.github.ma1uta.matrix.event.RoomMessage;
import io.github.ma1uta.matrix.event.content.RoomMessageContent;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Base class of all messages.
 *
 * @param <E> message type.
 */
public abstract class AbstractMessage<E extends RoomMessageContent> implements Initializable {

    private RoomMessage<E> event;
    private ResourceBundle i18n;
    private ObjectMapper mapper;

    public void setEvent(RoomMessage<E> event) {
        this.event = event;
    }

    public RoomMessage<E> getEvent() {
        return event;
    }

    public ResourceBundle getI18n() {
        return i18n;
    }

    protected ContextMenu contextMenu() {
        var menu = new ContextMenu();
        fillContextMenu(menu);
        return menu;
    }

    protected void fillContextMenu(ContextMenu menu) {
        var showSourceItem = new MenuItem(getI18n().getString("room.message.context.showSource"));
        showSourceItem.setOnAction(event -> {
            var sourceDialog = new Dialog<>();
            sourceDialog.setTitle(getI18n().getString("room.message.context.source.title"));
            try {
                sourceDialog.setContentText(mapper.writeValueAsString(getEvent()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            sourceDialog.show();
        });
        menu.getItems().add(showSourceItem);
        doFillContextMenu(menu);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.i18n = resources;
        this.mapper = new ObjectMapper();
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        doInit();
    }

    protected void doInit() {
    }

    protected abstract void doFillContextMenu(ContextMenu menu);

    /**
     * Parse the event.
     *
     * @param event   The message event.
     * @param account The matrix account.
     */
    public abstract void parse(RoomMessage<E> event, MatrixAccount account);
}
