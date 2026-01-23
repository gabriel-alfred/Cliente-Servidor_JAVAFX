package client.controller;

import client.network.SocketClient;
import common.Protocol;
import common.model.Message;
import common.model.Ticket;
import common.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class TicketFormController {

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descField;

    @FXML
    private Button saveButton;

    private User currentUser;
    private DashboardController parentController;

    public void setContext(User user, DashboardController parent) {
        this.currentUser = user;
        this.parentController = parent;
    }

    @FXML
    void handleSave(ActionEvent event) {
        String title = titleField.getText();
        String desc = descField.getText();

        if (title.isEmpty() || desc.isEmpty()) {
            showAlert("Error", "Por favor completa todos los campos.");
            return;
        }

        Ticket newTicket = new Ticket(0, title, desc, currentUser.getUsername());
        
        // Option A: Send directly from here
        // Option B: Return to DashboardController to send
        // We will send from here for simplicity and then tell parent to refresh
        
        try {
            SocketClient client = SocketClient.getInstance();
            Message request = new Message(Protocol.CMD_CREATE_TICKET, newTicket);
            client.sendMessage(request);
            
            Message response = client.receiveMessage();
            if (response.getCommand() == Protocol.STATUS_OK) {
                // Success
                parentController.refreshTickets();
                closeWindow();
            } else {
                String error = (String) response.getObject();
                showAlert("Error", "No se pudo crear el ticket: " + error);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            showAlert("Error de Conexión", e.getMessage());
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
