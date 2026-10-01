package http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HttpParser {
    public static HttpRequest parse(ByteBuffer buffer) throws Exception {
        buffer.flip();
        int remaining = buffer.remaining();

        if (remaining == 0) return null;

        byte[] data = new byte[remaining];
        buffer.get(data);

        int headerEndIndex = findHeaderEnd(data);
        if (headerEndIndex == -1) return null;
        
        HttpRequest request = new HttpRequest();

        // 2. parse headers
        String headerSection = new String(data, 0, headerEndIndex, StandardCharsets.UTF_8);
        String[] lines = headerSection.split("\r\n");

        // check if request is empty
        if (lines.length == 0) throw new Exception("Empty HTTP Request");

        // Parse request line
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length != 3) {
            throw new Exception("Invalid HTTP Request Line: " + lines[0]);
        }

        request.setMethod(requestLine[0]);
        request.setUri(requestLine[1]);
        request.setHttpVersion(requestLine[2]);

        return null;
    }

    private static int findHeaderEnd(byte[] data) {
        for (int i = 0; i < data.length -3; i++) {
            if (data[i] == '\r' && data[i+1] == '\n' &&
                data[i+2] == '\r' && data[i+3] == '\n') {
                return i;
            }
        }
        return -1;
    }
}