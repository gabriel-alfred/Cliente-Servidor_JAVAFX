package server.core;

import common.Protocol;
import common.model.Message;
import common.model.User;
import common.model.Ticket;
import server.service.AuthService;
import server.datastore.DataStore;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final AuthService authService;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser;
    private boolean running = true;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.authService = new AuthService();
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Cliente conectado: " + socket.getInetAddress());

            while (running) {
                try {
                    Message request = (Message) in.readObject();
                    handleRequest(request);
                } catch (ClassNotFoundException e) {
                    System.err.println("Error al leer objeto: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + socket.getInetAddress());
        } finally {
            closeConnection();
        }
    }

    private void handleRequest(Message request) throws IOException {
        Message response = new Message(Protocol.STATUS_ERROR, "Comando desconocido");

        switch (request.getCommand()) {
            case Protocol.CMD_LOGIN:
                User loginUser = (User) request.getObject();
                User authenticatedUser = authService.login(loginUser.getUsername(), loginUser.getPassword());
                if (authenticatedUser != null) {
                    currentUser = authenticatedUser;
                    response = new Message(Protocol.STATUS_OK, authenticatedUser);
                    System.out.println("Usuario logueado: " + currentUser.getUsername());
                } else {
                    response = new Message(Protocol.STATUS_UNAUTHORIZED, Protocol.ERR_LOGIN_FAILED);
                }
                break;

            case Protocol.CMD_LIST_TICKETS:
                if (currentUser != null) {
                    response = new Message(Protocol.STATUS_OK, DataStore.getInstance().getAllTickets());
                } else {
                    response = new Message(Protocol.STATUS_UNAUTHORIZED, "Debe iniciar sesión");
                }
                break;

            case Protocol.CMD_CREATE_TICKET:
                if (currentUser != null) {
                    Ticket newTicket = (Ticket) request.getObject();
                    // Set owner if not set, or enforce it
                    newTicket.setOwner(currentUser.getUsername());
                    DataStore.getInstance().addTicket(newTicket);
                    response = new Message(Protocol.STATUS_OK, "Ticket creado correctamente");
                } else {
                    response = new Message(Protocol.STATUS_UNAUTHORIZED, "Debe iniciar sesión");
                }
                break;

            case Protocol.CMD_LOGOUT:
                running = false;
                response = new Message(Protocol.STATUS_OK, "Adios");
                break;
        }

        out.writeObject(response);
        out.flush();
    }

    private void closeConnection() {
        try {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
