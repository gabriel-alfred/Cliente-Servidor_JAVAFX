package client.controller;

import client.network.SocketClient;
import common.Protocol;
import common.model.Message;
import common.model.Ticket;
import common.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class TicketFormController implements Initializable {

    @FXML
    private Label titleLabel;

    @FXML
    private TextField titleField;

    @FXML
  private TextArea descField;

    @FXML
    private Button saveButton;
    
    @FXML
 private VBox statusContainer;
    
    @FXML
    private ComboBox<Ticket.Status> statusCombo;

    private User currentUser;
  private DashboardController parentController;
    private Ticket editingTicket;
    private boolean isEditMode = false;

 @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Populate status combo
        statusCombo.setItems(FXCollections.observableArrayList(Ticket.Status.values()));
    }

    public void setContext(User user, DashboardController parent, Ticket ticketToEdit) {
  this.currentUser = user;
        this.parentController = parent;
        this.editingTicket = ticketToEdit;
        
  if (ticketToEdit != null) {
        isEditMode = true;
     titleLabel.setText("Editar Ticket");
  titleField.setText(ticketToEdit.getTitle());
   descField.setText(ticketToEdit.getDescription());
   statusCombo.setValue(ticketToEdit.getStatus());
    statusContainer.setVisible(true);
         statusContainer.setManaged(true);
    saveButton.setText("Actualizar Ticket");
        } else {
            isEditMode = false;
            titleLabel.setText("Crear Nuevo Ticket");
       saveButton.setText("Guardar Ticket");
  }
    }

    @FXML
    void handleSave(ActionEvent event) {
        String title = titleField.getText();
 String desc = descField.getText();

        if (title.isEmpty() || desc.isEmpty()) {
          showAlert("Error", "Por favor completa todos los campos.");
            return;
        }

        try {
 SocketClient client = SocketClient.getInstance();
  Message response;

 if (isEditMode) {
        // Update existing ticket
     editingTicket.setTitle(title);
       editingTicket.setDescription(desc);
                editingTicket.setStatus(statusCombo.getValue());
                
            Message request = new Message(Protocol.CMD_UPDATE_TICKET, editingTicket);
     client.sendMessage(request);
            response = client.receiveMessage();
           
            } else {
     // Create new ticket
    Ticket newTicket = new Ticket(0, title, desc, currentUser.getUsername());
    Message request = new Message(Protocol.CMD_CREATE_TICKET, newTicket);
        client.sendMessage(request);
 response = client.receiveMessage();
  }

            if (response.getCommand() == Protocol.STATUS_OK) {
     parentController.refreshTickets();
           closeWindow();
          } else {
  String error = (String) response.getObject();
       showAlert("Error", "No se pudo guardar el ticket: " + error);
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
