package io.github.ma1uta.jxclient.ui;

import io.github.ma1uta.jxclient.account.Account;
import io.github.ma1uta.jxclient.matrix.PlainRequestFactory;
import io.github.ma1uta.matrix.client.MatrixClient;
import io.github.ma1uta.matrix.client.model.auth.LoginRequest;
import io.github.ma1uta.matrix.client.model.auth.LoginResponse;
import io.github.ma1uta.matrix.client.model.auth.UserIdentifier;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * LoginViewController view.
 */
public class LoginViewController implements Initializable {

    private static final System.Logger LOGGER = System.getLogger("LOGIN");

    @FXML
    private TextField localpartField;

    @FXML
    private TextField serverField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ProgressBar loginProgress;

    private Account account;

    private Service<LoginResponse> loginResponseService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ValidationSupport validationSupport = new ValidationSupport();
        validationSupport
            .registerValidator(localpartField,
                Validator.createEmptyValidator(resources.getString("login.validation.localpart.empty"), Severity.ERROR));
        validationSupport
            .registerValidator(serverField,
                Validator.createEmptyValidator(resources.getString("login.validation.server.empty"), Severity.ERROR));
        validationSupport
            .registerValidator(passwordField,
                Validator.createEmptyValidator(resources.getString("login.validation.password.empty"), Severity.ERROR));

        loginResponseService = new Service<>() {

            @Override
            protected Task<LoginResponse> createTask() {
                return new Task<>() {
                    @Override
                    protected LoginResponse call() throws Exception {
                        var request = new LoginRequest();
                        UserIdentifier userIdentifier = new UserIdentifier();
                        userIdentifier.setUser(localpartField.getText());
                        request.setIdentifier(userIdentifier);
                        request.setPassword(passwordField.getText().toCharArray());
                        request.setInitialDeviceDisplayName("jxclient");
                        var loginClient = new MatrixClient.Builder().requestFactory(new PlainRequestFactory(serverField.getText())).build();
                        return loginClient.auth().login(localpartField.getText(), passwordField.getText().toCharArray()).join();
                    }
                };
            }
        };
        loginResponseService.setOnSucceeded(event -> account.updateToken((LoginResponse) event.getSource().getValue()));
        loginResponseService.setOnFailed(event -> {
            Platform.runLater(() -> editable(true));
            Throwable exception = event.getSource().getException();
            LOGGER.log(System.Logger.Level.ERROR, "Failed to login.", exception);
            new ExceptionDialog(exception).show();
        });
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    private void editable(boolean editable) {
        loginProgress.setVisible(!editable);
        localpartField.setEditable(editable);
        serverField.setEditable(editable);
        passwordField.setEditable(editable);
    }

    /**
     * LoginViewController action.
     */
    public void login() {
        editable(false);
        if (!loginResponseService.isRunning()) {
            if (loginResponseService.getState() != Worker.State.READY) {
                loginResponseService.reset();
            }
            loginResponseService.start();
        }
    }

    /**
     * Register action.
     */
    public void register() {
        System.out.println("Register");
    }
}
