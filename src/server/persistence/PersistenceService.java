package server.persistence;

import common.model.Ticket;
import common.model.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for persisting and loading server data using Java serialization.
 * Handles saving/loading users and tickets to/from a binary file.
 */
public class PersistenceService {
    private static final String DATA_FILE = "server_data.dat";
    private static PersistenceService instance;

    private PersistenceService() {
    }

    public static synchronized PersistenceService getInstance() {
        if (instance == null) {
            instance = new PersistenceService();
        }
        return instance;
    }

    /**
     * Container class to hold all server data for serialization
     */
    public static class ServerData implements Serializable {
        private static final long serialVersionUID = 1L;
        List<User> users;
        List<Ticket> tickets;

        ServerData(List<User> users, List<Ticket> tickets) {
            this.users = users;
            this.tickets = tickets;
        }
    }

    /**
     * Save users and tickets to file
     */
    public synchronized void saveData(List<User> users, List<Ticket> tickets) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(DATA_FILE))) {

            ServerData data = new ServerData(
                    new ArrayList<>(users),
                    new ArrayList<>(tickets));

            oos.writeObject(data);
            System.out.println("[PERSISTENCE] Datos guardados correctamente en " + DATA_FILE);

        } catch (IOException e) {
            System.err.println("[PERSISTENCE] Error al guardar datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load users and tickets from file
     * Returns null if file doesn't exist or error occurs
     */
    public synchronized ServerData loadData() {
        File file = new File(DATA_FILE);

        if (!file.exists()) {
            System.out.println("[PERSISTENCE] Archivo de datos no encontrado. Se usarán datos iniciales.");
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(DATA_FILE))) {

            ServerData data = (ServerData) ois.readObject();
            System.out.println("[PERSISTENCE] Datos cargados correctamente desde " + DATA_FILE);
            System.out.println("[PERSISTENCE] - Usuarios: " + data.users.size());
            System.out.println("[PERSISTENCE] - Tickets: " + data.tickets.size());
            return data;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[PERSISTENCE] Error al cargar datos: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get users from loaded data
     */
    public List<User> getUsers(ServerData data) {
        return data != null ? data.users : new ArrayList<>();
    }

    /**
     * Get tickets from loaded data
     */
    public List<Ticket> getTickets(ServerData data) {
        return data != null ? data.tickets : new ArrayList<>();
    }
}
