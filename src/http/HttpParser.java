package http;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

        // Parse individual headers
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) continue;

            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String name = line.substring(0, colonIndex);
                String value = line.substring(colonIndex + 1);
                request.addHeader(name, value);
            }
        }
        request.parseCookies();

        // 3. Parse Body (if Content-Length is provided)
        int bodyStartIndex = headerEndIndex + 4; // Skip \r\n\r\n
        long contentLength = request.getContentLength();

        if (contentLength > 0) {
            // Check if we have received the full body
            int availableBodyBytes = data.length - bodyStartIndex;

            if (availableBodyBytes < contentLength) return null;

            byte[] bodyBytes = Arrays.copyOfRange(data, bodyStartIndex, bodyStartIndex + (int) contentLength);
            request.setBody(bodyBytes);
        }

        return request;
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