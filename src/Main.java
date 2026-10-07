import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.Server;
import config.ConfigLoader;
import config.ServerConfig;

public class Main {
    public static void main(String[] args) {
        try {
            String configPath = args.length > 0 ? args[0] : "config.json";
            ConfigLoader loader = ConfigLoader.fromFile(configPath);
            System.out.println("✓ Configuration loaded successfully! (" + loader.getServers().size() + " servers configured)");

            List<ServerConfig> servers = loader.getServers();

            // AUDIT: Check for duplicate ports within same server and detect shared ports (Virtual Hosting)
            Map<Integer, List<String>> portToServersMap = new HashMap<>();
            Set<Integer> uniquePorts = new HashSet<>();

            for (ServerConfig serverConfig : servers) {
                Set<Integer> seenInThisServer = new HashSet<>();
                if (serverConfig.getPorts() != null) {
                    for (int port : serverConfig.getPorts()) {
                        if (!seenInThisServer.add(port)) {
                            System.err.println("⚠️ Configuration Warning: Duplicate port " + port + " configured within server '" + serverConfig.getServerName() + "'.");
                        }
                        portToServersMap.computeIfAbsent(port, k -> new ArrayList<>()).add(serverConfig.getServerName());
                        uniquePorts.add(port);
                    }
                }
            }

            for (Map.Entry<Integer, List<String>> entry : portToServersMap.entrySet()) {
                if (entry.getValue().size() > 1) {
                    System.out.println("ℹ️ Port " + entry.getKey() + " is shared across servers " + entry.getValue() + " (Virtual Hosting enabled).");
                }
            }

            if (uniquePorts.isEmpty()) {
                System.err.println("❌ Error: No ports defined in configuration file.");
                System.exit(1);
            }

            Server server = new Server(servers);
            server.startServer(new ArrayList<>(uniquePorts));

        } catch (Exception e) {
            System.err.println("❌ Server encountered an error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}