package io.github.ma1uta.jxclient.account;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import io.github.ma1uta.jxclient.matrix.PlainRequestFactory;
import io.github.ma1uta.matrix.client.MatrixClient;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class Account {

    private System.Logger LOGGER;

    private Parent accountPane;

    private Parent loginPane;

    private MatrixClient client;

    private ResourceBundle i18n;

    private String userId;

    private boolean showLoginPane = false;

    public void init(Preferences accountNode, ResourceBundle i18n) {
        init(accountNode.get("homeserver", null), accountNode.get("token", null), i18n);
        Platform.runLater(this::initForm);
    }

    public void init(ResourceBundle i18n) {
        init(null, null, i18n);
        Platform.runLater(() -> {
            initForm();
            loginPane();
        });
    }

    public Parent getPane() {
        return showLoginPane ? loginPane() : accountPane;
    }

    private void tryToLogin() {
        LOGGER.log(INFO, "Try to login");

    }

    private void init(String homeserver, String token, ResourceBundle i18n) {
        this.i18n = i18n;
        if (homeserver == null || homeserver.isBlank() || token == null || token.isBlank()) {
            anonymousMode();
        } else {
            userMode(homeserver, token);
        }
    }

    private void userMode(String homeserver, String token) {
        this.client = new MatrixClient.Builder().requestFactory(new PlainRequestFactory(homeserver)).accessToken(token).build();
        try {
            this.userId = checkCredentials();
            this.LOGGER = System.getLogger("ACCOUNT-" + userId);
            this.showLoginPane = false;
        } catch (Exception e) {
            System.getLogger("CLIENT").log(ERROR, "Credentials are wrong.", e);
            anonymousMode();
        }
    }

    private void anonymousMode() {
        this.userId = null;
        this.LOGGER = System.getLogger("ACCOUNT-ANONYMOUS-" + new Random().nextInt());
        this.showLoginPane = true;
    }

    private String checkCredentials() {
        return this.client.account().whoami().join().getUserId();
    }

    private Parent loginPane() {
        try {
            if (loginPane == null) {
                loginPane = FXMLLoader.load(getClass().getResource("/io/github/ma1uta/jxclient/ui/Login.fxml"), i18n);
            }
            return loginPane;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initForm() {
        try {
            accountPane = FXMLLoader.load(getClass().getResource("/io/github/ma1uta/jxclient/ui/Login.fxml"), i18n);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
