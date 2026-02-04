package client.controller;

import client.network.SocketClient;
import common.Protocol;
import common.model.Message;
import common.model.Ticket;
import common.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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

import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class DashboardController implements Initializable {
    
  private Timeline autoRefreshTimeline;

  @FXML
  private Label userLabel;

  @FXML
  private Label statusLabel;

  @FXML
  private Button reloadButton;

  @FXML
  private Button reportButton;

  @FXML
  private Button logsButton;

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
    
    // Si es admin, mostrar botón de logs
    if ("ADMIN".equals(user.getRole())) {
        logsButton.setVisible(true);
        logsButton.setManaged(true);
    }
    
    refreshTickets(false);
    startAutoRefresh();
  }

  private void startAutoRefresh() {
      // Auto-refresh every 3 seconds
      autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> refreshTickets(true)));
      autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
      autoRefreshTimeline.play();
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
    
    // Dynamic styling based on status
    ticketTable.setRowFactory(tv -> new javafx.scene.control.TableRow<Ticket>() {
        @Override
        protected void updateItem(Ticket item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll("status-open", "status-progress", "status-closed");
            if (item == null || empty) {
                setStyle(""); // Reset style
            } else {
                // Apply classes based on status string (from enum or string)
                if (item.getStatus() != null) {
                    switch (item.getStatus()) {
                        case OPEN:
                            getStyleClass().add("status-open");
                            break;
                        case IN_PROGRESS:
                            getStyleClass().add("status-progress");
                            break;
                        case CLOSED:
                            getStyleClass().add("status-closed");
                            break;
                    }
                }
            }
        }
    });
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
    refreshTickets(false);
  }

  @FXML
  void handleGenerateReport(ActionEvent event) {
    reportButton.setDisable(true);
    statusLabel.setText("Generando reporte...");

    Task<byte[]> reportTask = new Task<byte[]>() {
      @Override
      protected byte[] call() throws Exception {
        SocketClient client = SocketClient.getInstance();
        Message request = new Message(Protocol.CMD_GENERATE_REPORT, null);
        client.sendMessage(request);

        Message response = client.receiveMessage();
        if (response.getCommand() == Protocol.STATUS_OK) {
          return (byte[]) response.getObject();
        } else {
          throw new Exception((String) response.getObject());
        }
      }
    };

    reportTask.setOnSucceeded(e -> {
      byte[] pdfBytes = reportTask.getValue();
      statusLabel.setText("Reporte generado. Guardando...");

      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Guardar Reporte");
      fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
      fileChooser.setInitialFileName("tickets_report.pdf");

      File file = fileChooser.showSaveDialog(reportButton.getScene().getWindow());

      if (file != null) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
          fos.write(pdfBytes);
          statusLabel.setText("Reporte guardado en: " + file.getName());

          Alert alert = new Alert(Alert.AlertType.INFORMATION);
          alert.setTitle("Éxito");
          alert.setHeaderText(null);
          alert.setContentText("El reporte se ha guardado correctamente.");
          alert.showAndWait();
        } catch (IOException ex) {
          statusLabel.setText("Error al guardar archivo");
          ex.printStackTrace();
        }
      } else {
        statusLabel.setText("Guardado cancelado");
      }
      reportButton.setDisable(false);
    });

    reportTask.setOnFailed(e -> {
      statusLabel.setText("Error al generar reporte: " + reportTask.getException().getMessage());
      reportButton.setDisable(false);
      reportTask.getException().printStackTrace();
    });

    new Thread(reportTask).start();
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
  void handleViewLogs(ActionEvent event) {
    statusLabel.setText("Obteniendo logs del servidor...");
    logsButton.setDisable(true);

    Task<String> logTask = new Task<String>() {
      @Override
      protected String call() throws Exception {
        SocketClient client = SocketClient.getInstance();
        Message request = new Message(Protocol.CMD_GET_LOGS, null);
        client.sendMessage(request);

        Message response = client.receiveMessage();
        if (response.getCommand() == Protocol.STATUS_OK) {
            return (String) response.getObject();
        } else {
            throw new Exception((String) response.getObject());
        }
      }
    };

    logTask.setOnSucceeded(e -> {
      String logs = logTask.getValue();
      logsButton.setDisable(false);
      statusLabel.setText("Logs cargados.");
      
      // Mostrar logs en un diálogo simple
      showLogsDialog(logs);
    });

    logTask.setOnFailed(e -> {
      logsButton.setDisable(false);
      statusLabel.setText("Error al cargar logs.");
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle("Error");
      alert.setContentText("No se pudieron cargar los logs: " + logTask.getException().getMessage());
      alert.showAndWait();
    });

    new Thread(logTask).start();
  }

  private void showLogsDialog(String logsContent) {
    Stage stage = new Stage();
    stage.setTitle("Logs del Servidor");
    stage.initModality(Modality.APPLICATION_MODAL);
    
    javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(logsContent);
    textArea.setEditable(false);
    textArea.setFont(new javafx.scene.text.Font("Monospaced", 12));
    
    Scene scene = new Scene(textArea, 600, 400);
    stage.setScene(scene);
    stage.show();
  }

  @FXML
  void handleLogout(ActionEvent event) {
    // Implement logout logic if needed, or just close
    if (autoRefreshTimeline != null) {
        autoRefreshTimeline.stop();
    }
    Stage stage = (Stage) logoutButton.getScene().getWindow();
    stage.close();
  }

  public void refreshTickets() {
      refreshTickets(false);
  }

  public void refreshTickets(boolean silent) {
    if (!silent) {
        statusLabel.setText("Cargando tickets...");
        reloadButton.setDisable(true);
    }

    Task<List<Ticket>> fetchTask = new Task<List<Ticket>>() {
      @Override
      protected List<Ticket> call() throws Exception {
        SocketClient client = SocketClient.getInstance();
        Message request = new Message(Protocol.CMD_LIST_TICKETS, null);
        client.sendMessage(request);

        Message response = client.receiveMessage();

        if (response.getCommand() == Protocol.STATUS_OK) {
          return (List<Ticket>) response.getObject();
        } else {
          throw new Exception("Error del servidor: " + response.getObject());
        }
      }
    };

    fetchTask.setOnSucceeded(e -> {
      List<Ticket> tickets = fetchTask.getValue();
      ticketList.setAll(tickets);
      if (!silent) {
          statusLabel.setText("Tickets actualizados: " + tickets.size());
          reloadButton.setDisable(false);
      }
    });

    fetchTask.setOnFailed(e -> {
      if (!silent) {
          Throwable ex = fetchTask.getException();
          ex.printStackTrace();
          statusLabel.setText("Error: " + ex.getMessage());
          reloadButton.setDisable(false);
      }
    });

    new Thread(fetchTask).start();
  }
}
