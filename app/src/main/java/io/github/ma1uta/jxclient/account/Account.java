package io.github.ma1uta.jxclient.account;

import static java.lang.System.Logger.Level.ERROR;

import io.github.ma1uta.jxclient.matrix.PlainRequestFactory;
import io.github.ma1uta.jxclient.ui.AccountViewController;
import io.github.ma1uta.jxclient.ui.LoginViewController;
import io.github.ma1uta.matrix.Id;
import io.github.ma1uta.matrix.client.MatrixClient;
import io.github.ma1uta.matrix.client.model.account.WhoamiResponse;
import io.github.ma1uta.matrix.client.model.auth.LoginResponse;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Account {

    private System.Logger LOGGER;

    private Tab accountTab;
    private StackPane accountLayer;
    private Parent loadingView;

    private AccountViewController accountViewController;
    private Parent accountView;

    private LoginViewController loginViewController;
    private Parent loginView;

    private MatrixClient client;

    private ResourceBundle i18n;

    private String userId;
    private String deviceId;

    private AccountModeService accountModeService;

    public void init(Preferences accountNode, ResourceBundle i18n) {
        init(accountNode.get("homeserver", null), accountNode.get("deviceId", null), accountNode.get("token", null), i18n);
    }

    public void init(ResourceBundle i18n) {
        init(null, null, null, i18n);
    }

    public Tab getTab() {
        return accountTab;
    }

    private void showAccountView() {
        loadingView.setVisible(false);
        if (loadingView != null) {
            loadingView.setVisible(false);
        }
        accountView.setVisible(true);
        accountTab.setText(userId);
        accountLayer.layout();
    }

    private void showLoginView() {
        loadingView.setVisible(false);
        accountView.setVisible(false);
        Parent loginView = loginView();
        accountLayer.getChildren().add(loginView);
        loginView.setVisible(true);
        accountTab.setText(i18n.getString("account.login"));
        accountLayer.layout();
    }

    private void init(String homeserver, String deviceId, String token, ResourceBundle i18n) {
        this.i18n = i18n;
        accountTab = new Tab();
        accountTab.setClosable(false);
        accountLayer = new StackPane();
        accountLayer.getChildren().addAll(loadingView(), accountView());
        accountTab.setContent(accountLayer);
        accountView.setVisible(false);
        accountModeService = new AccountModeService(this);
        accountModeService.updateDeviceInfo(homeserver, deviceId, token);
        accountModeService.start();
    }

    private void syncPreferences(String homeserver, String deviceId, String token) {
        var root = Preferences.userRoot();
        Preferences node = root.node("jxclient/accounts/" + this.userId);
        node.put("homeserver", homeserver);
        node.put("token", token);
        node.put("deviceId", deviceId);
        try {
            node.sync();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private void userMode(String homeserver, String deviceId, String token) {
        this.client = new MatrixClient.Builder().requestFactory(new PlainRequestFactory(homeserver)).accessToken(token).build();
        this.client.getDefaultParams().deviceId(deviceId);
        try {
            this.client.account().whoami().thenApply(WhoamiResponse::getUserId).thenAccept(userId -> {
                this.userId = userId;
                this.deviceId = deviceId;
                this.LOGGER = System.getLogger("ACCOUNT-" + userId);
                syncPreferences(homeserver, deviceId, token);
                Platform.runLater(this::showAccountView);
            });
        } catch (Exception e) {
            System.getLogger("CLIENT").log(ERROR, "Credentials are wrong.", e);
            anonymousMode();
        }
    }

    private void anonymousMode() {
        this.userId = null;
        this.LOGGER = System.getLogger("ACCOUNT-ANONYMOUS-" + new Random().nextInt());
        Platform.runLater(this::showLoginView);
    }

    private Parent loadingView() {
        try {
            if (loadingView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/ma1uta/jxclient/ui/AccountLoading.fxml"), i18n);
                loadingView = loader.load();
            }
            return loadingView;
        } catch (IOException e) {
            return null;
        }
    }

    private Parent loginView() {
        try {
            if (loginView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/ma1uta/jxclient/ui/Login.fxml"), i18n);
                loginView = loader.load();
                loginViewController = loader.getController();
                loginViewController.setAccount(this);
            }
            return loginView;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Parent accountView() {
        if (accountView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/ma1uta/jxclient/ui/Account.fxml"), i18n);
                accountView = loader.load();
                accountViewController = loader.getController();
                accountViewController.setAccount(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return accountView;
    }

    public void updateToken(LoginResponse loginResponse) {
        if (!accountModeService.isRunning()) {
            accountModeService.reset();
            accountModeService.updateDeviceInfo(
                Id.getInstance().domain(loginResponse.getUserId()),
                loginResponse.getDeviceId(),
                loginResponse.getAccessToken());
            accountModeService.start();
        }
    }

    public class AccountModeService extends Service<Void> {

        private final Account account;
        private String homeserver;
        private String deviceId;
        private String token;

        AccountModeService(Account account) {
            this.account = account;
        }

        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() throws Exception {
                    if (homeserver == null || homeserver.isBlank() || token == null || token.isBlank()) {
                        account.anonymousMode();
                    } else {
                        account.userMode(homeserver, deviceId, token);
                    }

                    return null;
                }
            };
        }

        void updateDeviceInfo(String homeserver, String deviceId, String token) {
            this.homeserver = homeserver;
            this.deviceId = deviceId;
            this.token = token;
        }
    }
}
