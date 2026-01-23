package client.controller;

import client.network.SocketClient;
import common.Protocol;
import common.model.Message;
import common.model.Ticket;
import common.model.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML
    private Label userLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Button reloadButton;
    
    @FXML
    private Button addTicketButton;
    
    @FXML
    private Button logoutButton;

    @FXML
    private TableView<Ticket> ticketTable;

    @FXML
    private TableColumn<Ticket, Integer> colId;

    @FXML
    private TableColumn<Ticket, String> colTitle;

    @FXML
    private TableColumn<Ticket, String> colStatus;

    @FXML
    private TableColumn<Ticket, String> colOwner;

    @FXML
    private TableColumn<Ticket, String> colDesc;

    private User currentUser;
    private ObservableList<Ticket> ticketList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
    }

    public void initData(User user) {
        this.currentUser = user;
        userLabel.setText("Bienvenido, " + user.getUsername());
        refreshTickets();
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colOwner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        ticketTable.setItems(ticketList);
    }

    @FXML
    void handleReload(ActionEvent event) {
        refreshTickets();
    }

    @FXML
    void handleAddTicket(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ticket_form.fxml"));
            Parent root = loader.load();
            
            TicketFormController controller = loader.getController();
            controller.setContext(currentUser, this);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nuevo Ticket");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error al abrir formulario: " + e.getMessage());
        }
    }
    
    @FXML
    void handleLogout(ActionEvent event) {
        // Implement logout logic if needed, or just close
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        stage.close();
    }

    public void refreshTickets() {
        statusLabel.setText("Cargando tickets...");
        
        // This should conceptually be async to avoid freezing UI
        new Thread(() -> {
            try {
                SocketClient client = SocketClient.getInstance();
                Message request = new Message(Protocol.CMD_LIST_TICKETS, null);
                client.sendMessage(request);
                
                Message response = client.receiveMessage();
                
                if (response.getCommand() == Protocol.STATUS_OK) {
                    List<Ticket> tickets = (List<Ticket>) response.getObject();
                    
                    Platform.runLater(() -> {
                        ticketList.setAll(tickets);
                        statusLabel.setText("Tickets actualizados: " + tickets.size());
                    });
                } else {
                    Platform.runLater(() -> statusLabel.setText("Error al cargar tickets"));
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> statusLabel.setText("Error de conexión: " + e.getMessage()));
            }
        }).start();
    }
}
