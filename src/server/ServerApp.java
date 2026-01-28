package server;

import common.Protocol;
import server.core.ClientHandler;
import server.datastore.DataStore;
import server.util.ServerLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ServerApp {
    private static final ServerLogger logger = ServerLogger.getInstance();
    private static ExecutorService pool;
    
    public static void main(String[] args) {
        logger.info("Iniciando servidor en puerto " + Protocol.PORT + "...");
        
        // Initialize DataStore (loads persisted data)
        DataStore.getInstance();
        logger.info("DataStore inicializado");
        
        pool = Executors.newCachedThreadPool();

        // Add shutdown hook to save data on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Señal de apagado recibida. Guardando datos...");
            DataStore.getInstance().saveData();
            shutdownPool();
            logger.close();
        }));

        try (ServerSocket serverSocket = new ServerSocket(Protocol.PORT)) {
            logger.info("Servidor escuchando en puerto " + Protocol.PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Nueva conexión desde: " + clientSocket.getInetAddress().getHostAddress());
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            logger.error("Error en el servidor", e);
        } finally {
            shutdownPool();
            logger.close();
        }
    }

    private static void shutdownPool() {
        if (pool != null && !pool.isShutdown()) {
            logger.info("Cerrando pool de hilos...");
            pool.shutdown();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
            }
        }
    }
}
