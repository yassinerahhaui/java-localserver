import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Server {
    private final String host;
    private final List<Integer> ports;
    private Selector selector;

    public Server(ConfigLoader.Config config) {
        this.host = config.getHost();
        this.ports = config.getPorts();
    }

    public void start() {
        try {
            selector = Selector.open();
            for (int port : ports) {
                openListener(port);
            }

            System.out.println("Server listening on " + host + ":" + ports );
            runEventLoop();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    private void openListener(int port) throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(host, port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Bound and listening on port " + port);
    }

    private void runEventLoop() {
        while (true) {
            try {
                int ready = selector.select();
                if (ready == 0) {
                    continue;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    try {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        }
                    } catch (IOException ex) {
                        System.err.println("Connection handler error: " + ex.getMessage());
                        closeChannel(key);
                    }
                }
            } catch (IOException e) {
                System.err.println("Selector loop failed: " + e.getMessage());
                break;
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel client = serverChannel.accept();
        if (client == null) {
            return;
        }

        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = client.read(buffer);
        if (bytesRead == -1) {
            closeChannel(key);
            return;
        }

        if (bytesRead == 0) {
            return;
        }

        String response = "HTTP/1.1 501 Not Implemented\r\n"
                + "Content-Length: 0\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        client.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.US_ASCII)));
        closeChannel(key);
    }

    private void closeChannel(SelectionKey key) {
        try {
            key.channel().close();
        } catch (IOException ignored) {
        }

        key.cancel();
    }
}
