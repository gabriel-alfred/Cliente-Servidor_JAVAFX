package server.service;

import common.model.User;
import server.datastore.DataStore;

import java.util.Optional;

public class AuthService {
    private final DataStore dataStore;

    public AuthService() {
        this.dataStore = DataStore.getInstance();
    }

    public User login(String username, String password) {
        Optional<User> userOpt = dataStore.findUser(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }
}
