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

import io.github.ma1uta.jxclient.Client;
import io.github.ma1uta.jxclient.matrix.MatrixAccount;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * MainViewController view.
 */
public class MainViewController implements Initializable {

    private ObservableList<MatrixAccount> accounts = FXCollections.observableArrayList();

    @FXML
    private TabPane tabPane;

    @FXML
    private Button addAccountButton;

    @FXML
    private Button appSettingsButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addAccountButton.setOnAction(event -> Client.getInstance().addNewAccount(true));
    }

    /**
     * Add new account.
     *
     * @param account the new account.
     */
    public void addAccount(MatrixAccount account) {
        accounts.add(account);
        tabPane.getTabs().add(account.getTab());
    }

    /**
     * Select a tab.
     *
     * @param tabIndex index of the selected tab.
     */
    public void select(int tabIndex) {
        tabPane.getSelectionModel().select(tabIndex);
    }
}
