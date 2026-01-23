package client.controller;

import client.network.SocketClient;
import common.Protocol;
import common.model.Message;
import common.model.User;
import javafx.application.Platform;
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

        if (username.isEmpty() || password.isEmpty()) {
            showError("Por favor llene todos los campos.");
            return;
        }

        // Connect if not already connected
        if (!socketClient.connect()) {
            showError("No se pudo conectar al servidor.");
            return;
        }

        try {
            User credentials = new User(username, password, null);
            Message loginRequest = new Message(Protocol.CMD_LOGIN, credentials);
            
            socketClient.sendMessage(loginRequest);
            Message response = socketClient.receiveMessage();

            if (response.getCommand() == Protocol.STATUS_OK) {
                User loggedUser = (User) response.getObject();
                System.out.println("Login exitoso: " + loggedUser.getUsername());
                loadDashboard();
            } else {
                String errorMsg = (String) response.getObject();
                showError(errorMsg);
            }

        } catch (IOException | ClassNotFoundException e) {
            showError("Error de comunicación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void loadDashboard() {
        try {
            // Assumes dashboard.fxml exists in view folder
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/dashboard.fxml"));
            Parent root = loader.load();
            
            // You might want to pass the user to the dashboard controller here
            DashboardController controller = loader.getController();
            controller.initData(loggedUser);

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
