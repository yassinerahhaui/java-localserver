package http;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HttpParser {

    private static final int MAX_HEADER_SIZE = 16384; // 16 KB safety limit

    public static HttpRequest parse(ByteBuffer buffer) throws Exception {
        if (buffer == null) return null;
        ByteBuffer dup = buffer.duplicate();
        if (dup.position() > 0 && dup.remaining() == 0) {
            dup.flip();
        }
        int remaining = dup.remaining();
        if (remaining == 0) return null;

        byte[] data = new byte[remaining];
        dup.get(data);
        return parse(data);
    }

    public static HttpRequest parse(byte[] data) throws Exception {
        if (data == null || data.length == 0) return null;

        int[] headerEndInfo = findHeaderEnd(data);
        int headerEndIndex = headerEndInfo[0];
        int headerDelimiterLen = headerEndInfo[1];

        if (headerEndIndex == -1) {
            if (data.length > MAX_HEADER_SIZE) {
                throw new Exception("400 Bad Request: Header size exceeded " + MAX_HEADER_SIZE + " bytes");
            }
            return null; // Wait for headers to finish
        }

        HttpRequest request = new HttpRequest();

        // 1. Parse headers section
        String headerSection = new String(data, 0, headerEndIndex, StandardCharsets.UTF_8);
        String[] lines = headerSection.split("\r?\n");

        if (lines.length == 0 || lines[0].trim().isEmpty()) {
            throw new Exception("400 Bad Request: Empty HTTP Request");
        }

        // Parse Request-Line (e.g. GET /index.html HTTP/1.1)
        String[] requestLine = lines[0].trim().split("\\s+");
        if (requestLine.length < 2 || requestLine.length > 3) {
            throw new Exception("400 Bad Request: Invalid Request-Line: " + lines[0]);
        }

        request.setMethod(requestLine[0]);
        request.setUri(requestLine[1]);
        request.setHttpVersion(requestLine.length == 3 ? requestLine[2] : "HTTP/1.1");

        // Parse Header Fields
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) continue;

            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String name = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                request.addHeader(name, value);
            }
        }
        request.parseCookies();

        // 2. Parse Body
        int bodyStartIndex = headerEndIndex + headerDelimiterLen;

        if (request.isChunked()) {
            byte[] chunkedBody = parseChunkedBody(data, bodyStartIndex);
            if (chunkedBody == null) {
                return null; // Entire chunked stream not yet received
            }
            request.setBody(chunkedBody);
            request.addHeader("content-length", String.valueOf(chunkedBody.length));
        } else {
            long contentLength = request.getContentLength();
            if (contentLength > 0) {
                int availableBodyBytes = data.length - bodyStartIndex;
                if (availableBodyBytes < contentLength) {
                    return null; // Waiting for remaining body bytes
                }

                byte[] bodyBytes = Arrays.copyOfRange(data, bodyStartIndex, bodyStartIndex + (int) contentLength);
                request.setBody(bodyBytes);
            } else {
                request.setBody(new byte[0]);
            }
        }

        return request;
    }

    /**
     * Parses chunked transfer encoding according to RFC 9112 / RFC 2616.
     * Returns decoded body bytes, or null if incomplete.
     */
    private static byte[] parseChunkedBody(byte[] data, int bodyStartIndex) throws Exception {
        int current = bodyStartIndex;
        ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();
        boolean completed = false;

        while (current < data.length) {
            // Find CRLF or LF after chunk-size
            int lineEnd = -1;
            int delimLen = 0;
            for (int i = current; i < data.length; i++) {
                if (data[i] == '\n') {
                    lineEnd = (i > current && data[i - 1] == '\r') ? i - 1 : i;
                    delimLen = (i > current && data[i - 1] == '\r') ? 2 : 1;
                    break;
                }
            }

            if (lineEnd == -1) {
                return null; // Incomplete chunk size line
            }

            String chunkSizeHex = new String(data, current, lineEnd - current, StandardCharsets.UTF_8).trim();
            // Handle chunk extensions if present (e.g. "1a;ext=foo")
            int semi = chunkSizeHex.indexOf(';');
            if (semi != -1) {
                chunkSizeHex = chunkSizeHex.substring(0, semi).trim();
            }

            if (chunkSizeHex.isEmpty()) {
                current = lineEnd + delimLen;
                continue;
            }

            int chunkSize;
            try {
                chunkSize = Integer.parseInt(chunkSizeHex, 16);
            } catch (NumberFormatException e) {
                throw new Exception("400 Bad Request: Invalid chunk size: " + chunkSizeHex);
            }

            if (chunkSize < 0) {
                throw new Exception("400 Bad Request: Negative chunk size");
            }

            int chunkDataStart = lineEnd + delimLen;

            // Terminal chunk: size 0
            if (chunkSize == 0) {
                // Verify terminal CRLF or optional trailers
                int remaining = data.length - chunkDataStart;
                if (remaining >= 2 && data[chunkDataStart] == '\r' && data[chunkDataStart + 1] == '\n') {
                    completed = true;
                    break;
                } else if (remaining >= 1 && data[chunkDataStart] == '\n') {
                    completed = true;
                    break;
                } else if (remaining > 2) {
                    // Check for trailer section ending in CRLF CRLF
                    int[] trailerEnd = findHeaderEnd(Arrays.copyOfRange(data, chunkDataStart, data.length));
                    if (trailerEnd[0] != -1) {
                        completed = true;
                        break;
                    }
                    return null;
                } else {
                    return null; // Wait for final CRLF
                }
            }

            int chunkDataEnd = chunkDataStart + chunkSize;
            // Verify we have chunk data + trailing CRLF
            if (data.length < chunkDataEnd + 1) {
                return null; // Not enough data yet
            }

            // Write chunk data
            bodyOut.write(data, chunkDataStart, chunkSize);

            // Skip trailing CRLF (or LF)
            int next = chunkDataEnd;
            if (next < data.length && data[next] == '\r') next++;
            if (next < data.length && data[next] == '\n') next++;

            current = next;
        }

        if (!completed) {
            return null;
        }

        return bodyOut.toByteArray();
    }

    /**
     * Finds the end of HTTP headers (\r\n\r\n or \n\n).
     * Returns [index, delimiterLength] or [-1, 0].
     */
    private static int[] findHeaderEnd(byte[] data) {
        for (int i = 0; i < data.length - 1; i++) {
            if (data[i] == '\r' && i + 3 < data.length && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n') {
                return new int[]{i, 4};
            }
            if (data[i] == '\n' && data[i + 1] == '\n') {
                return new int[]{i, 2};
            }
        }
        return new int[]{-1, 0};
    }
}