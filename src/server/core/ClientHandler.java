package server.core;

import common.Protocol;
import common.model.Message;
import common.model.User;
import common.model.Ticket;
import server.service.AuthService;
import server.service.ReportService;
import server.datastore.DataStore;
import server.util.ServerLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService authService;
    private final ReportService reportService;
    private final ServerLogger logger;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser;
    private boolean running = true;
    private String clientAddress;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.authService = new AuthService();
        this.reportService = new ReportService();
        this.logger = ServerLogger.getInstance();
        this.clientAddress = socket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            logger.info("Cliente conectado: " + clientAddress);

            while (running) {
                try {
                    Message request = (Message) in.readObject();
                    handleRequest(request);
                } catch (ClassNotFoundException e) {
                    logger.error("Error al leer objeto desde " + clientAddress, e);
                }
            }
        } catch (IOException e) {
            logger.info("Cliente desconectado: " + clientAddress);
        } finally {
            closeConnection();
        }
    }

    private void handleRequest(Message request) throws IOException {
        Message response = new Message(Protocol.STATUS_ERROR, "Comando desconocido");

        switch (request.getCommand()) {
            case Protocol.CMD_LOGIN:
                User loginUser = (User) request.getObject();
                logger.info("Intento de login: " + loginUser.getUsername() + " desde " + clientAddress);

                User authenticatedUser = authService.login(loginUser.getUsername(), loginUser.getPassword());
                if (authenticatedUser != null) {
                    currentUser = authenticatedUser;
                    response = new Message(Protocol.STATUS_OK, authenticatedUser);
                    logger.info("Login exitoso: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
                } else {
                    response = new Message(Protocol.STATUS_UNAUTHORIZED, Protocol.ERR_LOGIN_FAILED);
                    logger.warning(
                            "Login fallido para usuario: " + loginUser.getUsername() + " desde " + clientAddress);
                }
                break;

            case Protocol.CMD_LIST_TICKETS:
                if (currentUser != null) {
                    int ticketCount = DataStore.getInstance().getAllTickets().size();
                    response = new Message(Protocol.STATUS_OK, DataStore.getInstance().getAllTickets());
                    logger.info("Usuario " + currentUser.getUsername() + " listó " + ticketCount + " tickets");
                } else {
                    response = new Message(Protocol.STATUS_UNAUTHORIZED, "Debe iniciar sesión");
                    logger.warning("Intento de listar tickets sin autenticación desde " + clientAddress);
                }
                break;

            case Protocol.CMD_CREATE_TICKET:
                if (currentUser != null) {
                    Ticket newTicket = (Ticket) request.getObject();
                    // Set owner if not set, or enforce it
                    newTicket.setOwner(currentUser.getUsername());
                    Ticket createdTicket = DataStore.getInstance().addTicket(newTicket);
                    response = new Message(Protocol.STATUS_OK, "Ticket creado correctamente");
                    logger.info("Ticket creado por " + currentUser.getUsername() +
                            ": ID=" + createdTicket.getId() + ", Título='" + createdTicket.getTitle() + "'");
                } else {
                    response = new Message(Protocol.STATUS_UNAUTHORIZED, "Debe iniciar sesión");
                    logger.warning("Intento de crear ticket sin autenticación desde " + clientAddress);
                }
                break;

            case Protocol.CMD_UPDATE_TICKET:
                if (currentUser != null) {
                    Ticket updatedTicket = (Ticket) request.getObject();
                    DataStore.getInstance().updateTicket(updatedTicket);
                    response = new Message(Protocol.STATUS_OK, "Ticket actualizado correctamente");
                    logger.info("Ticket actualizado por " + currentUser.getUsername() +
                            ": ID=" + updatedTicket.getId() + ", Estado=" + updatedTicket.getStatus());
                } else {
                    response = new Message(Protocol.STATUS_UNAUTHORIZED, "Debe iniciar sesión");
                    logger.warning("Intento de actualizar ticket sin autenticación desde " + clientAddress);
                }
                break;

            case Protocol.CMD_LOGOUT:
                if (currentUser != null) {
                    logger.info("Usuario " + currentUser.getUsername() + " cerró sesión");
                }
                running = false;
                response = new Message(Protocol.STATUS_OK, "Adios");
                break;

            case Protocol.CMD_GENERATE_REPORT:
                if (currentUser != null) {
                    try {
                        logger.info("Generando reporte para usuario: " + currentUser.getUsername());
                        byte[] pdfBytes = reportService.generateTicketReport(DataStore.getInstance().getAllTickets());
                        response = new Message(Protocol.STATUS_OK, pdfBytes);
                        logger.info("Reporte generado exitosamente (" + pdfBytes.length + " bytes)");
                    } catch (Exception e) {
                        logger.error("Error generando reporte", e);
                        response = new Message(Protocol.STATUS_ERROR, Protocol.ERR_REPORT_GENERATION);
                    }
                } else {
                    response = new Message(Protocol.STATUS_UNAUTHORIZED, "Debe iniciar sesión");
                }
                break;

            default:
                logger.warning("Comando desconocido recibido desde " + clientAddress + ": " + request.getCommand());
                break;
        }

        out.writeObject(response);
        out.flush();
    }

    private void closeConnection() {
        try {
            if (currentUser != null) {
                logger.info("Cerrando conexión para usuario: " + currentUser.getUsername());
            } else {
                logger.info("Cerrando conexión desde: " + clientAddress);
            }

            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            logger.error("Error al cerrar conexión desde " + clientAddress, e);
        }
    }
}
