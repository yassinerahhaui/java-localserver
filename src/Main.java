import java.io.IOException;

import app.Server;

public class Main {
    public static void main(String[] args) {
        try {
            Server.startServer();
        } catch (IOException e) {
            System.err.println("Server encountered an error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}