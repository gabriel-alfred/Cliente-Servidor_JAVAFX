package client;

import client.network.SocketClient;
import common.Protocol;
import common.model.Message;
import common.model.User;

public class VerificationMain {
    public static void main(String[] args) {
        System.out.println("--- Starting Verification ---");

        SocketClient client = SocketClient.getInstance();

        // 1. Test Connection
        System.out.println("Testing Connection...");
        if (!client.connect()) {
            System.err.println("FAILED: Could not connect to server.");
            System.exit(1);
        }
        System.out.println("SUCCESS: Connected to server.");

        try {
            // 2. Test Login (Success)
            System.out.println("Testing Valid Login...");
            User validUser = new User("user", "user", null);
            client.sendMessage(new Message(Protocol.CMD_LOGIN, validUser));

            Message response = client.receiveMessage();
            if (response.getCommand() == Protocol.STATUS_OK) {
                User loggedUser = (User) response.getObject();
                System.out.println("SUCCESS: Logged in as " + loggedUser.getUsername());
            } else {
                System.err.println("FAILED: Login expected SUCCESS but got " + response.getObject());
            }

            // 3. Test Login (Failure)
            System.out.println("Testing Invalid Login...");
            // Re-connect for a clean state if needed (or just send next header if protocol
            // supports it)
            // Implementation of ServerListener shows it keeps loop `while (running)`.

            User invalidUser = new User("bad", "guy", null);
            client.sendMessage(new Message(Protocol.CMD_LOGIN, invalidUser));

            response = client.receiveMessage();
            if (response.getCommand() == Protocol.STATUS_UNAUTHORIZED) {
                System.out.println("SUCCESS: Invalid login correctly rejected with " + response.getObject());
            } else {
                System.err.println("FAILED: Invalid login expected UNAUTHORIZED but got " + response.getCommand());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("FAILED: Exception during verification");
        } finally {
            client.close();
            System.out.println("--- Verification Finished ---");
        }
    }
}
