package server.datastore;

import common.model.Ticket;
import common.model.User;
import server.persistence.PersistenceService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DataStore {
    private static DataStore instance;

    private final List<User> users;
    private final List<Ticket> tickets;
    private final PersistenceService persistenceService;

    private DataStore() {
        // Initialize thread-safe lists
        users = Collections.synchronizedList(new ArrayList<>());
        tickets = Collections.synchronizedList(new ArrayList<>());
        persistenceService = PersistenceService.getInstance();

        // Intentar cargar datos, si no están disponibles usar datos por defecto
        loadData();
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    /**
     * Load data from persistence or initialize with default data
     */
    private void loadData() {
        PersistenceService.ServerData data = persistenceService.loadData();
        
        if (data != null) {
            // Load from file
            users.addAll(persistenceService.getUsers(data));
            tickets.addAll(persistenceService.getTickets(data));
        } else {
            // Initialize with default data
            initData();
        }
        
        // Ensure validation of default users even if data was loaded
        ensureDefaultUsers();
    }

    /**
     * Initialize default data (initial run)
     */
    private void initData() {
        // Tickets logic remains here
        tickets.add(new Ticket(1, "Problema de red", "No puedo acceder a internet", "usuario1"));
        tickets.add(new Ticket(2, "Error en login", "La contraseña no funciona", "usuario2"));
    }
    
    /**
     * Ensure default users always exist based on configuration
     */
    private void ensureDefaultUsers() {
        String adminUser = server.config.ServerConfig.getAdminUsername();
        String adminPass = server.config.ServerConfig.getAdminPassword();
        addIfMissing(new User(adminUser, adminPass, "ADMIN"));

        if (server.config.ServerConfig.isSeedDemoUsersEnabled()) {
            addIfMissing(new User("usuario1", "usuario1", "USER"));
            addIfMissing(new User("usuario2", "usuario2", "USER"));
            addIfMissing(new User("usuario3", "usuario3", "USER"));
        }
        saveData();
    }
    
    private void addIfMissing(User user) {
        synchronized(users) {
            boolean exists = users.stream().anyMatch(u -> u.getUsername().equals(user.getUsername()));
            if (!exists) {
                users.add(user);
            }
        }
    }

    /**
     * Save current data to persistence
     */
    public void saveData() {
        persistenceService.saveData(users, tickets);
    }

    // User methods
    public void addUser(User user) {
        users.add(user);
        saveData(); // Auto-save
    }

    public Optional<User> findUser(String username) {
        synchronized (users) {
            return users.stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst();
        }
    }

    // Ticket methods
    public synchronized Ticket addTicket(Ticket ticket) {
        synchronized (tickets) {
            int maxId = tickets.stream().mapToInt(Ticket::getId).max().orElse(0);
            ticket.setId(maxId + 1);
            tickets.add(ticket);
            saveData(); // Auto-save
            return ticket;
        }
    }

    public synchronized void updateTicket(Ticket updatedTicket) {
        synchronized (tickets) {
            for (int i = 0; i < tickets.size(); i++) {
                if (tickets.get(i).getId() == updatedTicket.getId()) {
                    tickets.set(i, updatedTicket);
                    saveData(); // Auto-save
                    return;
                }
            }
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

