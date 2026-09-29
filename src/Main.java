import java.io.IOException;

import app.Server;
import config.ConfigLoader;

public class Main {
    public static void main(String[] args) {
        try {
            String configPath = args.length > 0 ? args[0] : "config.json";
            ConfigLoader configLoader = ConfigLoader.fromFile(configPath);
            System.out.println("✓ Configuration loaded successfully! (" + configLoader.getServers().size() + " servers configured)");

            Server.startServer();
        } catch (Exception e) {
            System.err.println("Server encountered an error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}