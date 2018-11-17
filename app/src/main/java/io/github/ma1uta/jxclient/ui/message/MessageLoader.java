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

import static io.github.ma1uta.matrix.event.Event.MessageType.AUDIO;
import static io.github.ma1uta.matrix.event.Event.MessageType.EMOTE;
import static io.github.ma1uta.matrix.event.Event.MessageType.FILE;
import static io.github.ma1uta.matrix.event.Event.MessageType.IMAGE;
import static io.github.ma1uta.matrix.event.Event.MessageType.LOCATION;
import static io.github.ma1uta.matrix.event.Event.MessageType.NOTICE;
import static io.github.ma1uta.matrix.event.Event.MessageType.TEXT;
import static io.github.ma1uta.matrix.event.Event.MessageType.VIDEO;

import io.github.ma1uta.jxclient.matrix.MatrixAccount;
import io.github.ma1uta.matrix.event.RoomMessage;
import io.github.ma1uta.matrix.event.content.RoomMessageContent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.util.Pair;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Message loader.
 */
public class MessageLoader {

    private final ResourceBundle i18n;

    public MessageLoader(ResourceBundle i18n) {
        this.i18n = i18n;
    }

    /**
     * Provides a pair of the view controller and view of specified room message.
     *
     * @param roomMessage Room message.
     * @param account     The matrix account.
     * @param <C>         Type of the event content.
     * @param <M>         Type of the view controller.
     * @return The pair &lt;view controller;view&gt;
     */
    public <C extends RoomMessageContent, M extends AbstractMessage<C>> Pair<M, Node> load(RoomMessage<C> roomMessage,
                                                                                           MatrixAccount account) {
        String msgtype = roomMessage.getContent().getMsgtype();

        if (msgtype == null) {
            return null;
        }

        String viewControllerUrl;
        switch (msgtype) {
            case TEXT:
                viewControllerUrl = "/io/github/ma1uta/jxclient/ui/message/TextMessage.fxml";
                break;
            case NOTICE:
                viewControllerUrl = "/io/github/ma1uta/jxclient/ui/message/NoticeMessage.fxml";
                break;
            case IMAGE:
                viewControllerUrl = "/io/github/ma1uta/jxclient/ui/message/ImageMessage.fxml";
                break;
            case EMOTE:
                viewControllerUrl = "/io/github/ma1uta/jxclient/ui/message/EmoteMessage.fxml";
                break;
            case FILE:
                viewControllerUrl = "/io/github/ma1uta/jxclient/ui/message/FileMessage.fxml";
                break;
            case AUDIO:
                viewControllerUrl = "/io/github/ma1uta/jxclient/ui/message/AudioMessage.fxml";
                break;
            case LOCATION:
                viewControllerUrl = "/io/github/ma1uta/jxclient/ui/message/LocationMessage.fxml";
                break;
            case VIDEO:
                viewControllerUrl = "/io/github/ma1uta/jxclient/ui/message/VideoMessage.fxml";
                break;
            default:
                return null;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource(viewControllerUrl), i18n);
        try {
            Node view = loader.load();
            M viewController = loader.getController();
            viewController.parse(roomMessage, account);
            return new Pair<>(viewController, view);
        } catch (IOException e) {
            return null;
        }
    }
}
