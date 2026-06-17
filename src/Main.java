public class Main {
    public static void main(String[] args) {
        ConfigLoader.Config config = ConfigLoader.load("config.json");
        Server server = new Server(config);
        server.start();
    }
}
