package client.network;

import common.Protocol;
import common.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketClient {
    private static SocketClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private SocketClient() {
    }

    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public boolean connect() {
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket("localhost", Protocol.PORT);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                System.out.println("Conectado al servidor en puerto " + Protocol.PORT);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error al conectar con el servidor: " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(Message message) throws IOException {
        if (out != null) {
            out.writeObject(message);
            out.flush();
        } else {
            throw new IOException("No hay conexión con el servidor");
        }
    }

    public Message receiveMessage() throws IOException, ClassNotFoundException {
        if (in != null) {
            return (Message) in.readObject();
        } else {
            throw new IOException("No hay conexión con el servidor");
        }
    }

    public void close() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
