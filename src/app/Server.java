package app;

import http.HttpParser;
import http.HttpRequest;
import http.HttpResponse;
import config.ServerConfig;
import router.Router;

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
    private List<ServerConfig> configs; // The list of servers from config.json

    public Server(List<ServerConfig> configs) throws IOException {
        this.selector = Selector.open();
        this.configs = configs;
    }

    public void startServer(List<Integer> ports) throws IOException {
        // Bind all unique ports
        for (int port : ports) {
            ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false); // AUDIT: Non-blocking
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            
            System.out.println("Server is running on: http://localhost:" + port);
        }

        // The Event Loop
        while (true) {
            selector.select();

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();

                if (!key.isValid()) continue;

                if (key.isAcceptable()) {
                    acceptConnection(key);
                } else if (key.isReadable()) {
                    readRequest(key);
                } else if (key.isWritable()) {
                    writeResponse(key);
                }
            }
        }
    }

    public void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        
        if (client != null) {
            client.configureBlocking(false);
            SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
            
            // Attach the state manager to this specific client
            clientKey.attach(new ClientConnection(client));
            System.out.println("New connection accepted from: " + client.getRemoteAddress());
        }
    }

    public void readRequest(SelectionKey key) throws IOException {
        ClientConnection conn = (ClientConnection) key.attachment();
        SocketChannel client = conn.getChannel();
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
            // Append newly read bytes to the connection's data stream
            conn.getRequestData().write(buffer.array(), 0, bytesRead);
            
            // Check if we have reached \r\n\r\n
            if (conn.areHeadersComplete()) {
                byte[] fullData = conn.getRequestData().toByteArray();
                
                ByteBuffer parserBuffer = ByteBuffer.allocate(fullData.length);
                parserBuffer.put(fullData);

                try {
                    // 1. Let Asta's parser process the HTTP Request
                    HttpRequest request = HttpParser.parse(parserBuffer);
                    
                    // 2. If it's not null, it means the FULL request (including body) is ready
                    if (request != null) {
                        conn.setRequest(request);
                        conn.setState(ClientConnection.State.WRITING_RESPONSE);
                        
                        // For now, we take the first config. 
                        // Later in Day 7, we'll do Virtual Hosting matching the "Host" header.
                        ServerConfig currentConfig = configs.get(0);
                        
                        // --- Pass the request and configuration to the Router ---
                        HttpResponse response = Router.handle(request, currentConfig);
                        
                        conn.setResponse(response);
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                    // If request is null, we wait for more data in the next select() loop
                    
                } catch (Exception e) {
                    System.err.println("Bad Request Error: " + e.getMessage());
                    HttpResponse errResp = new HttpResponse();
                    errResp.setStatusCode(400);
                    errResp.setBody("400 Bad Request");
                    conn.setResponse(errResp);
                    conn.setState(ClientConnection.State.WRITING_RESPONSE);
                    key.interestOps(SelectionKey.OP_WRITE);
                }
            }
        }
    }

    private void writeResponse(SelectionKey key) throws IOException {
        ClientConnection conn = (ClientConnection) key.attachment();
        SocketChannel client = conn.getChannel();
        ByteBuffer buffer = conn.getWriteBuffer();

        if (buffer != null && buffer.hasRemaining()) {
            client.write(buffer);
        }
        
        if (buffer == null || !buffer.hasRemaining()) {
            System.out.println("--- Response sent successfully ---");
            conn.close();
            key.cancel();
        }
    }
}