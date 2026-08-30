package server.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Utility class for loading server configuration from properties files
 * and environment variables.
 */
public class ServerConfig {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties properties = new Properties();

    static {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
                System.out.println("[CONFIG] Configuración cargada desde " + CONFIG_FILE);
            } catch (IOException e) {
                System.err.println("[CONFIG] Error al cargar " + CONFIG_FILE + ": " + e.getMessage());
            }
        } else {
            System.out.println("[CONFIG] " + CONFIG_FILE + " no encontrado. Usando variables de entorno / valores por defecto.");
        }
    }

    public static String getAdminUsername() {
        String envValue = System.getenv("AETHER_ADMIN_USER");
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return properties.getProperty("admin.username", "admin");
    }

    public static String getAdminPassword() {
        String envValue = System.getenv("AETHER_ADMIN_PASS");
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return properties.getProperty("admin.password", "ChangeMe123!");
    }

    public static boolean isSeedDemoUsersEnabled() {
        String envValue = System.getenv("AETHER_SEED_DEMO_USERS");
        if (envValue != null) {
            return Boolean.parseBoolean(envValue);
        }
        return Boolean.parseBoolean(properties.getProperty("seed.demo.users", "true"));
    }

    public static int getServerPort() {
        String envValue = System.getenv("AETHER_SERVER_PORT");
        if (envValue != null) {
            try {
                return Integer.parseInt(envValue);
            } catch (NumberFormatException ignored) {}
        }
        String propValue = properties.getProperty("server.port");
        if (propValue != null) {
            try {
                return Integer.parseInt(propValue);
            } catch (NumberFormatException ignored) {}
        }
        return 9000;
    }
}
