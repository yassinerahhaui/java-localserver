package app;

import http.HttpParser;
import http.HttpRequest;
import http.HttpResponse;
import config.ServerConfig;
import error.ErrorHandler;
import router.Router;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Server {
    private static final long CONNECTION_TIMEOUT_MS = 30000; // 30 seconds idle timeout

    private final Selector selector;
    private final List<ServerConfig> configs;

    public Server(List<ServerConfig> configs) throws IOException {
        this.selector = Selector.open();
        this.configs = configs;
    }

    public void startServer(List<Integer> ports) throws IOException {
        int boundPortsCount = 0;

        // Bind all unique ports (AUDIT: Never crash on a single port bind failure)
        for (int port : ports) {
            try {
                ServerSocketChannel serverSocket = ServerSocketChannel.open();
                serverSocket.bind(new InetSocketAddress(port));
                serverSocket.configureBlocking(false); // AUDIT: Non-blocking
                serverSocket.register(selector, SelectionKey.OP_ACCEPT);

                System.out.println("✓ Server is running on: http://localhost:" + port);
                boundPortsCount++;
            } catch (IOException e) {
                System.err.println("⚠️ Warning: Could not bind to port " + port + ": " + e.getMessage() + ". Continuing with remaining ports.");
            }
        }

        if (boundPortsCount == 0) {
            throw new IOException("Failed to bind any of the configured ports.");
        }

        // AUDIT: Exactly ONE single-threaded event loop with ONE selector.select()
        while (true) {
            selector.select(1000); // 1-second timeout to handle stale connection cleanups

            cleanupTimedOutConnections();

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

    // AUDIT: Closes connections that have been inactive for longer than CONNECTION_TIMEOUT_MS
    private void cleanupTimedOutConnections() {
        long now = System.currentTimeMillis();
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.attachment() instanceof ClientConnection conn) {
                if (now - conn.getLastActivityTime() > CONNECTION_TIMEOUT_MS) {
                    conn.close();
                    key.cancel();
                }
            }
        }
    }

    public void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        
        if (client != null) {
            client.configureBlocking(false); // AUDIT: Non-blocking client
            int serverPort = ((InetSocketAddress) server.getLocalAddress()).getPort();

            SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
            clientKey.attach(new ClientConnection(client, serverPort));
        }
    }

    // AUDIT: Exactly ONE read per client per select iteration
    public void readRequest(SelectionKey key) throws IOException {
        ClientConnection conn = (ClientConnection) key.attachment();
        SocketChannel client = conn.getChannel();
        ByteBuffer buffer = conn.getReadBuffer();

        buffer.clear();
        int bytesRead = client.read(buffer);

        // Client disconnected or closed socket
        if (bytesRead == -1) {
            conn.close();
            key.cancel();
            return;
        }

        if (bytesRead > 0) {
            conn.updateActivity();
            buffer.flip();
            conn.getRequestData().write(buffer.array(), 0, bytesRead);

            if (conn.areHeadersComplete()) {
                byte[] fullData = conn.getRequestData().toByteArray();
                ByteBuffer parserBuffer = ByteBuffer.allocate(fullData.length);
                parserBuffer.put(fullData);

                try {
                    HttpRequest request = HttpParser.parse(parserBuffer);

                    // If request is not null, all headers and body (chunked or content-length) are complete
                    if (request != null) {
                        conn.setRequest(request);
                        conn.setState(ClientConnection.State.WRITING_RESPONSE);

                        // AUDIT: Virtual Hosting resolution based on Host header and port
                        ServerConfig currentConfig = resolveServerConfig(request, conn.getServerPort());

                        HttpResponse response = Router.handle(request, currentConfig);

                        // Attach Connection header
                        if (isKeepAlive(request)) {
                            response.setHeader("connection", "keep-alive");
                        } else {
                            response.setHeader("connection", "close");
                        }

                        conn.setResponse(response);
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                } catch (Exception e) {
                    // AUDIT: Malformed / wrong request -> 400 Bad Request, never crash
                    ServerConfig fallbackConfig = resolveServerConfig(null, conn.getServerPort());
                    HttpResponse errResp = ErrorHandler.handleError(fallbackConfig, 400, "Bad Request: " + e.getMessage());
                    errResp.setHeader("connection", "close");

                    conn.setResponse(errResp);
                    conn.setState(ClientConnection.State.WRITING_RESPONSE);
                    key.interestOps(SelectionKey.OP_WRITE);
                }
            }
        }
    }

    // AUDIT: Exactly ONE write per client per select iteration
    private void writeResponse(SelectionKey key) throws IOException {
        ClientConnection conn = (ClientConnection) key.attachment();
        SocketChannel client = conn.getChannel();
        ByteBuffer buffer = conn.getWriteBuffer();

        if (buffer != null && buffer.hasRemaining()) {
            client.write(buffer);
            conn.updateActivity();
        }

        if (buffer == null || !buffer.hasRemaining()) {
            boolean keepAlive = isKeepAlive(conn.getRequest());

            if (keepAlive) {
                // Reset connection state to accept the next request on the same socket (Keep-Alive)
                conn.resetForNextRequest();
                key.interestOps(SelectionKey.OP_READ);
            } else {
                // Close connection if not Keep-Alive
                conn.close();
                key.cancel();
            }
        }
    }

    // AUDIT: Virtual Hosting implementation
    // Matches request "Host" header with server_name for the specific listening port
    public ServerConfig resolveServerConfig(HttpRequest request, int serverPort) {
        List<ServerConfig> candidates = new ArrayList<>();
        for (ServerConfig cfg : configs) {
            if (cfg.getPorts() != null && cfg.getPorts().contains(serverPort)) {
                candidates.add(cfg);
            }
        }

        if (candidates.isEmpty()) {
            return configs.isEmpty() ? null : configs.get(0);
        }

        if (request != null) {
            String hostHeader = request.getHeader("host");
            if (hostHeader != null) {
                int colon = hostHeader.indexOf(':');
                String hostname = (colon != -1 ? hostHeader.substring(0, colon) : hostHeader).trim();

                for (ServerConfig cfg : candidates) {
                    if (cfg.getServerName() != null && cfg.getServerName().equalsIgnoreCase(hostname)) {
                        return cfg;
                    }
                }
            }
        }

        // Fallback to default_server on this port if designated
        for (ServerConfig cfg : candidates) {
            if (Boolean.TRUE.equals(cfg.getDefaultServer())) {
                return cfg;
            }
        }

        // Final fallback: first candidate for this port
        return candidates.get(0);
    }

    private boolean isKeepAlive(HttpRequest request) {
        if (request == null) return false;
        String connHeader = request.getHeader("connection");
        if ("close".equalsIgnoreCase(connHeader)) {
            return false;
        }
        if ("keep-alive".equalsIgnoreCase(connHeader)) {
            return true;
        }
        // HTTP/1.1 defaults to keep-alive; HTTP/1.0 defaults to close
        return "HTTP/1.1".equalsIgnoreCase(request.getHttpVersion());
    }
}