import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
            
            Set<Integer> uniquePorts = new HashSet<>();
            for (ServerConfig serverConfig : servers) {
                uniquePorts.addAll(serverConfig.getPorts());
            }

            Server server = new Server();
            server.startServer(new ArrayList<>(uniquePorts));
        } catch (Exception e) {
            System.err.println("Server encountered an error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}