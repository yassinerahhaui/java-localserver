import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
  
public class ServerIntegrationTest {
    public static void main(String[] args) throws Exception {
        testServerAcceptsConnectionsOnConfiguredPorts();
        System.out.println("Server integration tests passed.");
    }

    private static void testServerAcceptsConnectionsOnConfiguredPorts() throws Exception {
        Path configFile = Files.createTempFile("config-server-test", ".json");
        String host = "127.0.0.1";
        int[] ports = {8081, 9081};

        try {
            Files.writeString(configFile, String.format("{\n  \"host\": \"%s\",\n  \"ports\": [%d, %d]\n}\n", host, ports[0], ports[1]), StandardCharsets.UTF_8);
            ConfigLoader.Config config = ConfigLoader.load(configFile.toString());
            Server server = new Server(config);

            Thread serverThread = new Thread(server::start);
            serverThread.setDaemon(true);
            serverThread.start();

            waitForBind(host, ports, 5000);
            for (int port : ports) {
                testSocketConnects(host, port);
            }
        } finally {
            Files.deleteIfExists(configFile);
        }
    }

    private static void waitForBind(String host, int[] ports, int timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            boolean allOpen = true;
            for (int port : ports) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), 200);
                } catch (IOException e) {
                    allOpen = false;
                    break;
                }
            }
            if (allOpen) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Server did not bind configured ports within timeout");
    }

    private static void testSocketConnects(String host, int port) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            OutputStream output = socket.getOutputStream();
            output.write("GET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            output.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            String statusLine = reader.readLine();
            assert statusLine != null && statusLine.contains("501") : "Expected 501 response from server on port " + port + " but got: " + statusLine;
        }
    }
}
