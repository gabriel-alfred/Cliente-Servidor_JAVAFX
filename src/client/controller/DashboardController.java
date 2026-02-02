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
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

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
    
    @FXML
    private TableColumn<Ticket, Void> colActions;

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
        
        // Setup action column with edit button
        setUpActionColumn();
 
        ticketTable.setItems(ticketList);
        
        // Enable row selection
        ticketTable.getSelectionModel().setCellSelectionEnabled(false);
    }
    
    private void setUpActionColumn() {
      Callback<TableColumn<Ticket, Void>, TableCell<Ticket, Void>> cellFactory = new Callback<TableColumn<Ticket, Void>, TableCell<Ticket, Void>>() {
        @Override
            public TableCell<Ticket, Void> call(final TableColumn<Ticket, Void> param) {
            final TableCell<Ticket, Void> cell = new TableCell<Ticket, Void>() {
          private final Button editBtn = new Button("Editar");
  {
 editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
    editBtn.setPrefWidth(80);
    }

    @Override
      public void updateItem(Void item, boolean empty) {
     super.updateItem(item, empty);
         if (empty) {
       setGraphic(null);
      } else {
             editBtn.setOnAction(event -> {
       Ticket ticket = getTableView().getItems().get(getIndex());
    handleEditTicket(ticket);
          });
           HBox pane = new HBox(editBtn);
    pane.setAlignment(Pos.CENTER);
       setGraphic(pane);
 }
   }
       };
       return cell;
            }
     };

        colActions.setCellFactory(cellFactory);
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
  controller.setContext(currentUser, this, null);
     
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
    
    void handleEditTicket(Ticket ticket) {
      try {
FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ticket_form.fxml"));
            Parent root = loader.load();
       
            TicketFormController controller = loader.getController();
    controller.setContext(currentUser, this, ticket);
  
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
       stage.setTitle("Editar Ticket");
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
