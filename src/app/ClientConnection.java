package app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientConnection {

    public enum State {
        READING_HEADERS,
        READING_BODY,
        WRITING_RESPONSE,
        CLOSED
    }

    private final SocketChannel channel;
    private State state;

    private final ByteBuffer readBuffer;

    private final ByteArrayOutputStream requestData;

    private ByteBuffer writeBuffer;

    public ClientConnection(SocketChannel channel) {
        this.channel = channel;
        this.state = State.READING_HEADERS;
        this.readBuffer = ByteBuffer.allocate(8192);
        this.requestData = new ByteArrayOutputStream();
    }


    // Getters and Setters
    public SocketChannel getChannel() { return channel;}
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

    public ByteBuffer getReadBuffer() { return readBuffer; }
    public ByteArrayOutputStream getRequestData() { return requestData; }

    public ByteBuffer getWriteBuffer() { return writeBuffer; }
    public void setWriteBuffer(ByteBuffer writeBuffer) { this.writeBuffer = writeBuffer; }


    // Helper: Checks if the end of HTTP headers (\r\n\r\n) has been reached
    public boolean areHeadersComplete() {
        byte[] data = requestData.toByteArray();
        for (int i = 0; i < data.length - 3; i++) {
            if (data[i] == '\r' && data[i+1] == '\n' && 
                data[i+2] == '\r' && data[i+3] == '\n') {
                return true;
            }
        }
        return false;
    }

    // Helper: Closes the connection safely
    public void close() {
        try {
            this.state = State.CLOSED;
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch(IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}