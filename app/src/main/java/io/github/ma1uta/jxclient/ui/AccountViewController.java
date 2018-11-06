package io.github.ma1uta.jxclient.ui;

import io.github.ma1uta.jxclient.account.Account;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class AccountViewController implements Initializable {

    private Account account;

    public void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
