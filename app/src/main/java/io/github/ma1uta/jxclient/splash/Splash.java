package io.github.ma1uta.jxclient.splash;

import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.controlsfx.dialog.ExceptionDialog;

/**
 * App splash screen.
 */
public class Splash extends Preloader {

    private static final System.Logger LOGGER = System.getLogger("INIT");

    private Stage stage;

    private static final double PREF_WIDTH = 400.0D;
    private static final double PREF_HEIGHT = 300.0D;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;

        var progressBar = new ProgressBar();

        var borderPane = new BorderPane(progressBar);
        borderPane.setPrefSize(PREF_WIDTH, PREF_HEIGHT);

        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        if (info.getType() == StateChangeNotification.Type.BEFORE_START) {
            stage.hide();
        }
    }

    @Override
    public boolean handleErrorNotification(ErrorNotification info) {
        LOGGER.log(System.Logger.Level.ERROR, info.getDetails(), info.getCause());
        new ExceptionDialog(info.getCause()).show();
        return true;
    }
}
