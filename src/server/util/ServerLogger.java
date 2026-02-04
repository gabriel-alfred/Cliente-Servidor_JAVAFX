package server.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized logging utility for the server.
 * Writes logs to both console and file with timestamps and log levels.
 */
public class ServerLogger {
    private static final String LOG_FILE = "server.log";
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static ServerLogger instance;
    private PrintWriter fileWriter;

    private ServerLogger() {
        try {
            // Open file in append mode
            fileWriter = new PrintWriter(new FileWriter(LOG_FILE, true), true);
            info("=== Servidor iniciado ===");
        } catch (IOException e) {
            System.err.println("Error al abrir archivo de log: " + e.getMessage());
        }
    }

    public static synchronized ServerLogger getInstance() {
        if (instance == null) {
            instance = new ServerLogger();
        }
        return instance;
    }

    /**
     * Log an INFO level message
     */
    public synchronized void info(String message) {
        log("INFO", message);
    }

    /**
     * Log a WARNING level message
     */
    public synchronized void warning(String message) {
        log("WARNING", message);
    }

    /**
     * Log an ERROR level message
     */
    public synchronized void error(String message) {
        log("ERROR", message);
    }

    /**
     * Registra un error con los detalles de la excepción
     */
    public synchronized void error(String message, Throwable throwable) {
        log("ERROR", message + " - " + throwable.getMessage());
        if (fileWriter != null) {
            throwable.printStackTrace(fileWriter);
        }
    }

    /**
     * Internal logging method
     */
    private void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);
        
        // Write to console
        System.out.println(logEntry);
        
        // Write to file
        if (fileWriter != null) {
            fileWriter.println(logEntry);
            fileWriter.flush();
        }
    }

    /**
     * Devuelve el contenido del archivo de log.
     */
    public synchronized String getLogs() {
        StringBuilder sb = new StringBuilder();
        try (java.util.Scanner scanner = new java.util.Scanner(new java.io.File(LOG_FILE))) {
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine()).append("\n");
            }
        } catch (java.io.FileNotFoundException e) {
            return "Archivo de log no encontrado.";
        }
        return sb.toString();
    }

    /**
     * Close the log file
     */
    public synchronized void close() {
        if (fileWriter != null) {
            info("=== Servidor detenido ===");
            fileWriter.close();
            fileWriter = null;
        }
    }
}
