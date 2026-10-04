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
            SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
            clientKey.attach(new ClientConnection(client));
            System.out.println("New connection accepted from: " + client.getRemoteAddress());
        }
    }

    public void readRequest(SelectionKey key) throws IOException {
        // 1. Get the attached ClientConnection
        ClientConnection conn = (ClientConnection) key.attachment();
        SocketChannel client = conn.getChannel();

        // allocate buffer
        ByteBuffer buffer = conn.getReadBuffer();

        buffer.clear();
        int bytesRead = client.read(buffer);

        if (bytesRead == -1) {
            conn.close();
            key.cancel();
            System.out.println("Connection closed by client.");
            return;
        }

        if (bytesRead > 0) {
            buffer.flip();
            // 2. Append newly read bytes to the client's accumulator
            conn.getRequestData().write(buffer.array(), 0, bytesRead);
            // 3. Check if we received at least the full HTTP headers
            if (conn.getState() == ClientConnection.State.READING_HEADERS && conn.areHeadersComplete()) {
                conn.setState(ClientConnection.State.WRITING_RESPONSE);
                String httpResponse = "HTTP/1.1 200 OK\r\nContent-Length: 13\r\n\r\nHello, World!";
                conn.setWriteBuffer(ByteBuffer.wrap(httpResponse.getBytes()));
                key.interestOps(SelectionKey.OP_WRITE);
            }
        }
    }

    // Helper method to send a simple response
    private void writeResponse(SelectionKey key) throws IOException {
        ClientConnection conn = (ClientConnection) key.attachment();
        SocketChannel client = conn.getChannel();
        ByteBuffer buffer = conn.getWriteBuffer();

        if (buffer != null && buffer.hasRemaining()) {
            client.write(buffer);
        }
        
        // If we finished writing the whole response
        if (buffer == null || !buffer.hasRemaining()) {
            System.out.println("--- Response sent successfully ---");

            conn.close();
            key.cancel();
        }
    }
}
