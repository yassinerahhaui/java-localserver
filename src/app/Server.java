package app;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
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
                }
            }
        }
    }

    public static void acceptConnection(SelectionKey key, Selector selector) {
        // handle connection
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
    }

    public static void readRequest() {
        // handle request
    }

    public static void sendResponse() {}
}
