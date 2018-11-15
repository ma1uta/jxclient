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

package io.github.ma1uta.jxclient.matrix;

import static java.lang.System.Logger.Level.ERROR;

import io.github.ma1uta.jxclient.Client;
import io.github.ma1uta.jxclient.ui.AccountViewController;
import io.github.ma1uta.jxclient.ui.LoginViewController;
import io.github.ma1uta.matrix.Id;
import io.github.ma1uta.matrix.client.MatrixClient;
import io.github.ma1uta.matrix.client.model.account.WhoamiResponse;
import io.github.ma1uta.matrix.client.model.auth.LoginResponse;
import io.github.ma1uta.matrix.client.model.sync.SyncResponse;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material.Material;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Matrix account.
 */
public class MatrixAccount {

    private static final double DEFAULT_SYNC_PERIOD = 30D;

    private System.Logger logger;

    private Tab accountTab;
    private Parent loadingView;

    private AccountViewController accountViewController;
    private Parent accountView;

    private LoginViewController loginViewController;
    private Parent loginView;

    private MatrixClient client;

    private ResourceBundle i18n;

    private String userId;
    private String deviceId;

    private AccountModeService accountModeService = new AccountModeService();
    private Service<Void> initialSync;
    private ScheduledService<Void> syncLoop;

    /**
     * Initialize account.
     *
     * @param accountNode account preferences.
     * @param i18n        localized messages.
     */
    public void init(Preferences accountNode, ResourceBundle i18n) {
        init(accountNode.get("homeserver", null), accountNode.get("deviceId", null), accountNode.get("token", null), i18n);
    }

    /**
     * Initialize anonymous mode.
     *
     * @param i18n localized messages.
     */
    public void init(ResourceBundle i18n) {
        init(null, null, null, i18n);
    }

    private void init(String homeserver, String deviceId, String token, ResourceBundle i18n) {
        this.i18n = i18n;
        accountTab = new Tab();
        accountTab.setOnCloseRequest(event -> {
            if (isStub()) {
                return;
            }
            var closeAccountDialog = new Alert(Alert.AlertType.CONFIRMATION, i18n.getString("account.close.title"), ButtonType.YES,
                ButtonType.NO);
            closeAccountDialog.setGraphic(FontIcon.of(Material.WARNING));
            closeAccountDialog.showAndWait();
            if (ButtonType.NO == closeAccountDialog.getResult()) {
                event.consume();
            }
        });
        accountTab.setOnClosed(event -> Client.getInstance().removeAccount(MatrixAccount.this));
        accountTab.setContent(loadingView());
        accountView();
        if (homeserver == null || deviceId == null || token == null) {
            anonymousMode();
        } else {
            accountModeService.updateDeviceInfo(homeserver, deviceId, token);
            accountModeService.start();
        }
        initialSync = new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        MatrixAccount.this.parseInitialSync(MatrixAccount.this.client.sync().sync(null, null, true, null, 0L).join());
                        return null;
                    }
                };
            }
        };
        syncLoop = new ScheduledService<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        MatrixAccount.this.parseSync(MatrixAccount.this.client.sync().sync(
                            "",
                            "",
                            true,
                            "",
                            0L
                        ).join());
                        return null;
                    }
                };
            }
        };
        syncLoop.setPeriod(Duration.seconds(DEFAULT_SYNC_PERIOD));
    }

    public Tab getTab() {
        return accountTab;
    }

    public boolean isStub() {
        return this.deviceId == null;
    }

    private void showAccountView() {
        accountTab.setText(userId);
        accountTab.setContent(accountView);
        accountView.layout();
    }

    private void showLoginView() {
        Parent loginView = loginView();
        accountTab.setContent(loginView);
        loginView.layout();
        accountTab.setText(i18n.getString("account.login"));
    }

    private void syncPreferences(String homeserver, String deviceId, String token) {
        var root = Preferences.userRoot();
        Preferences node = root.node("jxclient/accounts/" + URLEncoder.encode(this.userId, StandardCharsets.UTF_8));
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
        try {
            this.client = new MatrixClient.Builder().requestFactory(new PlainRequestFactory(homeserver)).accessToken(token).build();
            this.client.getDefaultParams().deviceId(deviceId);
            this.client.account().whoami().thenApply(WhoamiResponse::getUserId).thenAccept(userId -> {
                this.userId = userId;
                this.deviceId = deviceId;
                this.logger = System.getLogger("ACCOUNT-" + userId);
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
        this.logger = System.getLogger("ACCOUNT-ANONYMOUS-" + new Random().nextInt());
        if (Platform.isFxApplicationThread()) {
            showLoginView();
        } else {
            Platform.runLater(this::showLoginView);
        }
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

    private void accountView() {
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
    }

    /**
     * Update account credentials.
     *
     * @param loginResponse account credentials.
     */
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

    private void parseInitialSync(SyncResponse syncResponse) {

    }

    private void parseSync(SyncResponse syncResponse) {

    }

    /**
     * MatrixAccount credentials verification service.
     */
    public class AccountModeService extends Service<Void> {

        private String homeserver;
        private String deviceId;
        private String token;

        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() throws Exception {
                    MatrixAccount.this.userMode(homeserver, deviceId, token);
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