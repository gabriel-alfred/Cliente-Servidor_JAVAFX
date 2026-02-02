package client.controller;

import client.network.SocketClient;
import common.Protocol;
import common.model.Message;
import common.model.User;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    private SocketClient socketClient;

    @FXML
    public void initialize() {
        socketClient = SocketClient.getInstance();
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();


        boolean isValid = true;

        if (username.isEmpty()) {
            usernameField.getStyleClass().add("error-field");
            isValid = false;
        }
        if (password.isEmpty()) {
            passwordField.getStyleClass().add("error-field");
            isValid = false;
        }

        if (!isValid) {
            showError("Por favor llene todos los campos.");
            // Add listeners to remove error style on typing
            usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
                usernameField.getStyleClass().remove("error-field");
            });
            passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
                passwordField.getStyleClass().remove("error-field");
            });
            return;
        }


        loginButton.setDisable(true);
        errorLabel.setVisible(false);

        Task<User> loginTask = new Task<User>() {
            @Override
            protected User call() throws Exception {
                if (!socketClient.connect()) {
                    throw new Exception("No se pudo conectar al servidor.");
                }

                User credentials = new User(username, password, null);
                Message loginRequest = new Message(Protocol.CMD_LOGIN, credentials);
                socketClient.sendMessage(loginRequest);

                Message response = socketClient.receiveMessage();

                if (response.getCommand() == Protocol.STATUS_OK) {
                    return (User) response.getObject();
                } else {
                    throw new Exception((String) response.getObject());
                }
            }
        };

        loginTask.setOnSucceeded(e -> {
            User loggedUser = loginTask.getValue();
            loginButton.setDisable(false);
            System.out.println("Login exitoso: " + loggedUser.getUsername());
            loadDashboard(loggedUser);
        });

        loginTask.setOnFailed(e -> {
            loginButton.setDisable(false);
            Throwable ex = loginTask.getException();
            showError(ex.getMessage());
            ex.printStackTrace();
        });

        new Thread(loginTask).start();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void loadDashboard(User user) {
        try {
            // Assumes dashboard.fxml exists in view folder
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/dashboard.fxml"));
            Parent root = loader.load();

            // You might want to pass the user to the dashboard controller here
            DashboardController controller = loader.getController();
            controller.initData(user);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("Dashboard");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error al cargar el dashboard.");
        }
    }
}
