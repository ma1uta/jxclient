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

package io.github.ma1uta.jxclient;

import io.github.ma1uta.jxclient.matrix.MatrixAccount;
import io.github.ma1uta.jxclient.splash.FinishLoadingNotification;
import io.github.ma1uta.jxclient.ui.MainViewController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 * Jx client.
 */
public class Client extends Application {

    private static final System.Logger LOGGER = System.getLogger("CLIENT");

    private static Client app;

    private Stage rootStage;
    private Parent rootForm;
    private FXMLLoader rootFormLoader;
    private List<MatrixAccount> accountList = new ArrayList<>();

    private ResourceBundle i18nBundle;

    public static Client getInstance() {
        return app;
    }

    @Override
    public void init() throws Exception {
        Platform.setImplicitExit(false);
        app = this;
        i18nBundle = ResourceBundle.getBundle("i18n/messages");
        rootFormLoader = new FXMLLoader(getClass().getResource("/io/github/ma1uta/jxclient/ui/Main.fxml"), i18nBundle);
        rootForm = rootFormLoader.load();
        loadAccounts();
        initTray();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        rootStage = primaryStage;
        var scene = new Scene(rootForm);
        rootStage.setScene(scene);
        MainViewController rootController = rootFormLoader.getController();
        for (MatrixAccount account : accountList) {
            rootController.addAccount(account);
        }
        rootStage.setTitle(i18nBundle.getString("app.title"));
        rootStage.show();
        rootController.select(0);
        notifyPreloader(new FinishLoadingNotification());
    }

    private void loadAccounts() throws BackingStoreException {
        var root = Preferences.userRoot();
        var accounts = root.node("jxclient/accounts");
        for (var accountName : accounts.childrenNames()) {
            var account = new MatrixAccount();
            account.init(accounts.node(accountName), i18nBundle);
            accountList.add(0, account);
        }
        if (accountList.isEmpty()) {
            addNewAccount(false);
        }
    }

    /**
     * Add tab to add a new account.
     *
     * @param addToController either to add new account to controller.
     */
    public void addNewAccount(boolean addToController) {
        if (!accountList.get(accountList.size() - 1).isStub()) {
            var account = new MatrixAccount();
            account.init(i18nBundle);
            accountList.add(account);
            if (addToController) {
                MainViewController controller = rootFormLoader.getController();
                controller.addAccount(account);
                controller.select(accountList.size() - 1);
            }
        }
    }

    /**
     * Remove account.
     *
     * @param matrixAccount The account to remove.
     */
    public void removeAccount(MatrixAccount matrixAccount) {
        accountList.remove(matrixAccount);
    }

    private void initTray() {
        Toolkit.getDefaultToolkit();
        if (SystemTray.isSupported()) {
            SwingUtilities.invokeLater(() -> {
                try {
                    var popupMenu = new PopupMenu();

                    var showHide = new MenuItem(i18nBundle.getString("app.tray.showHide"));
                    popupMenu.add(showHide);
                    showHide.addActionListener(e -> toggleVisibility());

                    var quit = new MenuItem(i18nBundle.getString("app.tray.quit"));
                    popupMenu.add(quit);

                    var trayIcon = new TrayIcon(ImageIO.read(getClass().getResourceAsStream("/tray.png")), "jxclient", popupMenu);
                    SystemTray tray = SystemTray.getSystemTray();

                    quit.addActionListener(e -> {
                        Platform.exit();
                        tray.remove(trayIcon);
                        System.exit(0);
                    });

                    trayIcon.addActionListener(e -> toggleVisibility());
                    tray.add(trayIcon);
                } catch (AWTException | IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void toggleVisibility() {
        Platform.runLater(() -> {
            if (rootStage.isShowing()) {
                rootStage.hide();
            } else {
                rootStage.show();
            }
        });
    }
}

