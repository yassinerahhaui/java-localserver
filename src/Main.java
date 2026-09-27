import java.io.IOException;

import app.Server;

public class Main {
    public static void main(String[] args) {
        try {
            Server.startServer();
            
        } catch (IOException e) {
            // TODO: handle exception
        }
    }
}