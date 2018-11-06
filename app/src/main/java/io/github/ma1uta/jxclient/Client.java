package io.github.ma1uta.jxclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ResourceBundle;

/**
 * Jx client.
 */
public class Client extends Application {

    private static final System.Logger LOGGER = System.getLogger("CLIENT");

    private static Client app;

    private Stage rootStage;
    private Scene rootScene;

    private ResourceBundle i18nBundle;

    public static Client getInstance() {
        return app;
    }

    @Override
    public void init() throws Exception {
        app = this;

        i18nBundle = ResourceBundle.getBundle("/i18n/messages");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        rootStage = primaryStage;
    }

    private void showForm(Parent parent) {
        if (Platform.isFxApplicationThread()) {
            showFormInFxThread(parent);
        } else {
            Platform.runLater(() -> showFormInFxThread(parent));
        }
    }

    private void showFormInFxThread(Parent parent) {
        if (rootScene == null) {
            rootScene = new Scene(parent);
            rootStage.setScene(rootScene);
        } else {
            rootScene.setRoot(parent);
            parent.layout();
        }
        if (!rootStage.isShowing()) {
            rootStage.show();
        }
    }
}

