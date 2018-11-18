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

import io.github.ma1uta.jxclient.Account;
import io.github.ma1uta.jxclient.AccountManager;
import io.github.ma1uta.jxclient.ui.matrix.AccountViewController;
import io.github.ma1uta.jxclient.ui.matrix.LoginViewController;
import io.github.ma1uta.matrix.Id;
import io.github.ma1uta.matrix.client.MatrixClient;
import io.github.ma1uta.matrix.client.model.account.WhoamiResponse;
import io.github.ma1uta.matrix.client.model.auth.LoginResponse;
import io.github.ma1uta.matrix.client.model.sync.SyncResponse;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

/**
 * Matrix account.
 */
public class MatrixAccount implements Account {

    private static final double DEFAULT_SYNC_PERIOD = 0.3D;
    private static final long DEFAULT_UI_SYNC_DELAY = 500L;
    private static final int DEFAULT_UI_ACTIONS_QUEUE = 10;

    private System.Logger logger;

    private Tab accountTab;
    private Parent loadingView;

    private AccountManager accountManager;

    private AccountViewController accountViewController;
    private Parent accountView;

    private LoginViewController loginViewController;
    private Parent loginView;

    private MatrixClient client;

    private ResourceBundle i18n;

    private String userId;
    private String deviceId;

    private AccountModeService accountModeService = new AccountModeService();
    private ScheduledService<Void> syncLoop;

    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private MediaDownloader downloader;

    private final Queue<Runnable> uiQueue = new ConcurrentLinkedQueue<>();

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

    @Override
    public AccountManager.Type getType() {
        return AccountManager.Type.MATRIX;
    }

    /**
     * Initialize account.
     *
     * @param accountNode account preferences.
     * @param i18n        localized messages.
     */
    @Override
    public void init(AccountManager accountManager, Preferences accountNode, ResourceBundle i18n) {
        this.accountManager = accountManager;
        if (accountNode == null) {
            init(null, null, null, i18n);
        } else {
            init(accountNode.get("homeserver", null), accountNode.get("deviceId", null), accountNode.get("token", null), i18n);
        }
    }

    private void init(String homeserver, String deviceId, String token, ResourceBundle i18n) {
        this.downloader = new MediaDownloader(this);
        this.i18n = i18n;
        accountTab = new Tab();
        accountTab.setOnCloseRequest(event -> {
            if (isLoginView()) {
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
        accountTab.setOnClosed(event -> accountManager.removeAccount(this));
        accountTab.setContent(loadingView());
        accountView();
        if (homeserver == null || deviceId == null || token == null) {
            anonymousMode();
        } else {
            accountModeService.setExecutor(executorService);
            accountModeService.updateDeviceInfo(homeserver, deviceId, token);
            accountModeService.start();
        }
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
        syncLoop.setExecutor(executorService);
        syncLoop.setPeriod(Duration.seconds(DEFAULT_SYNC_PERIOD));
        executorService.scheduleAtFixedRate(() -> {
            int count = 0;
            Runnable action;
            var actionsToRun = new ArrayList<Runnable>(DEFAULT_UI_ACTIONS_QUEUE);
            do {
                action = uiQueue.poll();
                if (action != null) {
                    actionsToRun.add(action);
                    count++;
                }
            }
            while (count < DEFAULT_UI_ACTIONS_QUEUE && action != null);
            if (!actionsToRun.isEmpty()) {
                Platform.runLater(() -> actionsToRun.forEach(Runnable::run));
            }
        }, 0, DEFAULT_UI_SYNC_DELAY, TimeUnit.MILLISECONDS);
    }

    @Override
    public Tab getTab() {
        return accountTab;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public MatrixClient getClient() {
        return client;
    }

    @Override
    public boolean isLoginView() {
        return this.deviceId == null;
    }

    @Override
    public void updateUI(Runnable action) {
        uiQueue.offer(action);
    }

    public MediaDownloader getDownloader() {
        return downloader;
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
        var prefs = new HashMap<String, String>();
        prefs.put("homeserver", homeserver);
        prefs.put("token", token);
        prefs.put("deviceId", deviceId);
        accountManager.sync(this, prefs);
    }

    private void userMode(String homeserver, String deviceId, String token) {
        try {
            this.client = new MatrixClient.Builder().requestFactory(new PlainRequestFactory(homeserver, executorService)).accessToken(token)
                .build();
            this.client.getDefaultParams().deviceId(deviceId);
            this.client.account().whoami().thenApply(WhoamiResponse::getUserId).thenAccept(userId -> {
                this.userId = userId;
                this.deviceId = deviceId;
                this.logger = System.getLogger("ACCOUNT-" + userId);
                syncPreferences(homeserver, deviceId, token);
                Platform.runLater(this::showAccountView);
                this.client.sync().sync(null, null, true, null, 0L).thenAccept(this::parseInitialSync);
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/ma1uta/jxclient/ui/matrix/Login.fxml"), i18n);
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
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/ma1uta/jxclient/ui/matrix/Account.fxml"), i18n);
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
        setLoading(true);
        try {
            accountViewController.parse(syncResponse);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            setLoading(false);
        }
    }

    private void parseSync(SyncResponse syncResponse) {
        if (!isLoading()) {
            setLoading(true);
            try {
                accountViewController.parse(syncResponse);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                setLoading(false);
            }
        }
    }

    public boolean isLoading() {
        return loading.get();
    }

    /**
     * Setter of the loading property.
     *
     * @param loading new value.
     */
    public void setLoading(boolean loading) {
        this.loading.setValue(loading);
    }

    /**
     * Loading property.
     *
     * @return loading property.
     */
    public BooleanProperty loadingProperty() {
        return loading;
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
