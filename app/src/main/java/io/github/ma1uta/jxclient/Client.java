package io.github.ma1uta.jxclient;

import io.github.ma1uta.jxclient.account.Account;
import io.github.ma1uta.jxclient.ui.MainViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Jx client.
 */
public class Client extends Application {

    private static final System.Logger LOGGER = System.getLogger("CLIENT");

    private static Client app;

    private Stage rootStage;
    private Parent rootForm;
    private FXMLLoader rootFormLoader;
    private List<Account> accountList = new ArrayList<>();

    private ResourceBundle i18nBundle;

    public static Client getInstance() {
        return app;
    }

    @Override
    public void init() throws Exception {
        app = this;
        i18nBundle = ResourceBundle.getBundle("/i18n/messages");
        rootFormLoader = new FXMLLoader(getClass().getResource("/io/github/ma1uta/jxclient/ui/Main.fxml"), i18nBundle);
        rootForm = rootFormLoader.load();
        loadAccounts();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        rootStage = primaryStage;
        var scene = new Scene(rootForm);
        rootStage.setScene(scene);
        MainViewController rootController = rootFormLoader.getController();
        for (Account account : accountList) {
            rootController.addAccount(account);
        }
        rootStage.setTitle(i18nBundle.getString("app.title"));
        rootStage.show();
        if (accountList.size() == 1) {
            accountList.get(0).getTab().getContent().requestFocus();
        } else {
            accountList.get(accountList.size() - 1).getTab().getContent().requestFocus();
        }
    }

    private void loadAccounts() throws BackingStoreException {
        var root = Preferences.userRoot();
        var accounts = root.node("jxclient/accounts");
        for (var accountName : accounts.childrenNames()) {
            var account = new Account();
            account.init(accounts.node(accountName), i18nBundle);
            accountList.add(0, account);
        }
        if (accountList.isEmpty()) {
            addStubAccount();
        }
    }

    private final AtomicBoolean barrier = new AtomicBoolean(false);

    public void addStubAccount() {
        if (!accountList.get(accountList.size() - 1).isStub()) {
            synchronized (barrier) {
                if (barrier.get()) {
                    return;
                }
                barrier.set(true);
            }
            try {
                var account = new Account();
                account.init(i18nBundle);
                accountList.add(account);
                MainViewController controller = rootFormLoader.getController();
                controller.addAccount(account);
            } finally {
                barrier.set(false);
            }
        }
    }
}

