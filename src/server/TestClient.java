package server;

import common.Protocol;
import common.model.Message;
import common.model.User;
import common.model.Ticket;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class TestClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", Protocol.PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            System.out.println("Conectado al servidor.");

            // 1. Try Login
            System.out.println("Enviando login...");
            User loginUser = new User("admin", "admin", null);
            out.writeObject(new Message(Protocol.CMD_LOGIN, loginUser));
            
            Message response = (Message) in.readObject();
            System.out.println("Respuesta Login: " + response.getCommand());
            if (response.getObject() instanceof User) {
                System.out.println("Usuario logueado: " + ((User)response.getObject()).getUsername());
            } else {
                System.out.println("Error: " + response.getObject());
            }

            // 2. List Tickets
            System.out.println("Pidiendo tickets...");
            out.writeObject(new Message(Protocol.CMD_LIST_TICKETS, null));
            
            response = (Message) in.readObject();
            System.out.println("Respuesta Tickets: " + response.getCommand());
            if (response.getObject() instanceof List) {
                List<Ticket> tickets = (List<Ticket>) response.getObject();
                System.out.println("Tickets recibidos: " + tickets.size());
                tickets.forEach(System.out::println);
            }

            // 3. Logout
            out.writeObject(new Message(Protocol.CMD_LOGOUT, null));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
