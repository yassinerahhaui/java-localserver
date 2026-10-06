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
            
            // Get the list of server configurations
            List<ServerConfig> servers = loader.getServers();
            
            // Collect all unique ports to avoid binding errors
            Set<Integer> uniquePorts = new HashSet<>();
            for (ServerConfig serverConfig : servers) {
                uniquePorts.addAll(serverConfig.getPorts());
            }

            // Pass the 'servers' list to the Server constructor
            Server server = new Server(servers);

            // Start the server on all unique ports
            server.startServer(new ArrayList<>(uniquePorts));
        } catch (Exception e) {
            System.err.println("Server encountered an error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}