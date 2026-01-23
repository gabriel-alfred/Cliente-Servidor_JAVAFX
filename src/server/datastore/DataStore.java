package server.datastore;

import common.model.Ticket;
import common.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DataStore {
    private static DataStore instance;

    private final List<User> users;
    private final List<Ticket> tickets;

    private DataStore() {
        // Initialize thread-safe lists
        users = Collections.synchronizedList(new ArrayList<>());
        tickets = Collections.synchronizedList(new ArrayList<>());

        // Add dummy data
        initData();
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    private void initData() {
        users.add(new User("admin", "admin", "ADMIN"));
        users.add(new User("user", "user", "USER"));

        // Add some initial tickets
        tickets.add(new Ticket(1, "Problema de red", "No puedo acceder a internet", "user"));
        tickets.add(new Ticket(2, "Error en login", "La contraseña no funciona", "user"));
    }

    // User methods
    public void addUser(User user) {
        users.add(user);
    }

    public Optional<User> findUser(String username) {
        synchronized (users) {
            return users.stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst();
        }
    }

    // Ticket methods
    public Ticket addTicket(Ticket ticket) {
        synchronized (tickets) {
            int maxId = tickets.stream().mapToInt(Ticket::getId).max().orElse(0);
            ticket.setId(maxId + 1);
            tickets.add(ticket);
            return ticket;
        }
    }

    public List<Ticket> getAllTickets() {
        // Return a copy to avoid concurrent modification exceptions during iteration
        // outside
        synchronized (tickets) {
            return new ArrayList<>(tickets);
        }
    }
}
