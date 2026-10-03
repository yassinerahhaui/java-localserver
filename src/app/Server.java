package app;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {
    private Selector selector;

    public Server() throws IOException {
        this.selector = Selector.open();
    }

    public void startServer(List<Integer> ports) throws IOException {
        for (int port : ports) {
            ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress(port));
    
            serverSocket.configureBlocking(false);
    
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
    
            System.out.println("Server is running on: http://localhost:" + port);

        }

        while (true) {
            selector.select();

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();

                iter.remove();

                if (!key.isValid())
                    continue;

                if (key.isAcceptable()) {
                    // accept connection
                    acceptConnection(key);
                } else if (key.isReadable()) {
                    // read request
                    readRequest(key);
                } else if (key.isWritable()) {
                    writeResponse(key);
                }
            }
        }
    }

    public void acceptConnection(SelectionKey key) throws IOException {
        // handle connection
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        if (client != null) {
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            System.out.println("New connection accepted from: " + client.getRemoteAddress());
        }
    }

    public void readRequest(SelectionKey key) throws IOException {
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

            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    // Helper method to send a simple response
    private void writeResponse(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        
        String httpResponse = """
                HTTP/1.1 200 OK\r
                Content-Type: text/plain\r
                Content-Length: 13\r
                \r
                Hello, World!""";
                
        ByteBuffer responseBuffer = ByteBuffer.wrap(httpResponse.getBytes());
        
        // Write the buffer to the channel
        client.write(responseBuffer);
        
        // In HTTP/1.1 (Keep-Alive), we shouldn't close the connection immediately.
        // But for this basic test, we will switch back to OP_READ to wait for another request.
        // If it was a "Connection: close" request, we would do: client.close();
        key.interestOps(SelectionKey.OP_READ);
        System.out.println("--- Response sent successfully ---");
    }
}
