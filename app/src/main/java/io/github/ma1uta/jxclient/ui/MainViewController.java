package io.github.ma1uta.jxclient.ui;

import io.github.ma1uta.jxclient.account.Account;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TabPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * MainViewController view.
 */
public class MainViewController implements Initializable {

    private ObservableList<Account> accounts = FXCollections.observableArrayList();

    @FXML
    private TabPane tabPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    /**
     * Add new account.
     *
     * @param account the new account.
     */
    public void addAccount(Account account) {
        accounts.add(account);
        tabPane.getTabs().add(account.getTab());
    }
}
