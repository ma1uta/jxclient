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

package io.github.ma1uta.jxclient.ui.matrix.message;

import io.github.ma1uta.jxclient.matrix.MatrixAccount;
import io.github.ma1uta.matrix.event.RoomMessage;
import io.github.ma1uta.matrix.event.message.Image;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.ImageView;

/**
 * Text message.
 */
public class ImageMessage extends AbstractMessage<Image> implements Initializable {

    @FXML
    private ImageView image;

    @Override
    protected void doFillContextMenu(ContextMenu menu) {
    }

    @Override
    public void parse(RoomMessage<Image> event, MatrixAccount account) {
        setEvent(event);
        account.getDownloader().download(event.getContent().getUrl(), url -> {
            try {
                image.setImage(new javafx.scene.image.Image(url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void doInit() {
    }
}
