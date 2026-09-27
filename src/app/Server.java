package app;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {
    public static void startServer() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(8080));

        serverSocket.configureBlocking(false);

        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server is running on: http://localhost:8080");

        while(true) {
            selector.select();

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();

                iter.remove();

                if (!key.isValid()) continue;

                if (key.isAcceptable()) {
                    // accept connection
                    acceptConnection(key, selector);
                }

                if (key.isReadable()) {
                    // read request
                    readRequest(key);
                }
            }
        }
    }

    public static void acceptConnection(SelectionKey key, Selector selector) throws IOException {
        // handle connection
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();

        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("New connection accepted from: " + client.getRemoteAddress());
    }

    public static void readRequest(SelectionKey key) throws IOException {
        // handle request
        SocketChannel client = (SocketChannel) key.channel();

        // allocate buffer
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = client.read(buffer);

        if (bytesRead == -1) {
            client.close();
            System.out.println("Connection closed by client.");
            return;
        }

        if (bytesRead > 0) {
            buffer.flip();

            String requestStr = new String(buffer.array(), 0, bytesRead);
            System.out.println("--- Received HTTP Request ---\n" + requestStr);

            sendBasicResponse(client);
        }
    }

    // Helper method to send a simple response
    private static void sendBasicResponse(SocketChannel client) throws IOException {
        String httpResponse = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/plain\r\n"
                + "Content-Length: 13\r\n"
                + "\r\n"
                + "Hello, World!";
        
        ByteBuffer responseBuffer = ByteBuffer.wrap(httpResponse.getBytes());
        client.write(responseBuffer);
        
        // Close the client connection after sending the response (Simple test for now)
        client.close();
    }
}
