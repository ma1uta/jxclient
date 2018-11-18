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
import io.github.ma1uta.matrix.event.message.File;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;

/**
 * Text message.
 */
public class FileMessage extends AbstractMessage<File> implements Initializable {

    @FXML
    private Label body;

    @Override
    protected void doFillContextMenu(ContextMenu menu) {
    }

    @Override
    public void parse(RoomMessage<File> event, MatrixAccount account) {
        setEvent(event);
        body.setText(event.getContent().getFilename());
    }

    @Override
    public void doInit() {
        body.setContextMenu(contextMenu());
    }

    /**
     * Download file.
     */
    public void downloadAction() {
    }
}
