package http;

/**
 * Parses a raw HTTP string into an HttpRequest object.
 * Capable of handling standard bodies and chunked transfer encoding.
 */
public class HttpParser {

    public HttpRequest parse(String rawRequest) throws IllegalArgumentException {
        if (rawRequest == null || rawRequest.isBlank()) {
            throw new IllegalArgumentException("400 Bad Request: Empty request");
        }

        HttpRequest request = new HttpRequest();

        // 🆕 1. Find the boundary between Headers and Body
        int boundaryIndex = rawRequest.indexOf("\r\n\r\n");
        if (boundaryIndex == -1) {
            throw new IllegalArgumentException("400 Bad Request: Missing headers-body boundary");
        }

        // 🆕 2. Split into two raw parts
        String headerSection = rawRequest.substring(0, boundaryIndex);
        // boundaryIndex + 4 to skip the "\r\n\r\n" itself
        String rawBodySection = rawRequest.substring(boundaryIndex + 4);

        // 3. Parse headers section (Same logic as Day 1)
        String[] headerLines = headerSection.split("\r\n");
        parseRequestLine(headerLines[0], request);
        parseHeaders(headerLines, request);

        // 🆕 4. Decide how to parse the body based on Headers
        String transferEncoding = request.getHeader("transfer-encoding");
        String contentLengthStr = request.getHeader("content-length");

        try {
            if ("chunked".equalsIgnoreCase(transferEncoding)) {
                // Task 3: Parse Chunked Encoding
                parseChunkedBody(rawBodySection, request);
            } else if (contentLengthStr != null) {
                // Task 2: Parse Content-Length
                parseContentLengthBody(rawBodySection, contentLengthStr, request);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("400 Bad Request: Invalid body size format", e);
        }

        return request;
    }

    private void parseRequestLine(String requestLine, HttpRequest request) {
        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            throw new IllegalArgumentException("400 Bad Request: Malformed request line");
        }

        HttpMethod method = HttpMethod.fromString(parts[0]);
        if (method == null) {
            throw new IllegalArgumentException("501 Not Implemented: Unknown HTTP method '" + parts[0] + "'");
        }
        request.setMethod(method);
        request.setUri(parts[1]);
        request.setVersion(parts[2]);
    }

    private void parseHeaders(String[] lines, HttpRequest request) {
        for (int i = 1; i < lines.length; i++) {
            String[] headerParts = lines[i].split(":", 2);
            if (headerParts.length == 2) {
                request.addHeader(headerParts[0], headerParts[1]);
            } else {
                throw new IllegalArgumentException("400 Bad Request: Malformed header");
            }
        }
    }

    /**
     * Parses the body based on the Content-Length header.
     */
    private void parseContentLengthBody(String rawBody, String contentLengthStr, HttpRequest request) {
        int expectedLength = Integer.parseInt(contentLengthStr.trim());

        // Error Handling: Make sure we actually received enough data
        if (rawBody.length() < expectedLength) {
            throw new IllegalArgumentException("400 Bad Request: Incomplete body data");
        }

        // Extract exactly the amount of bytes/chars specified
        request.setBody(rawBody.substring(0, expectedLength));
    }

    /**
     * Parses the body based on Transfer-Encoding: chunked.
     * Reads hex sizes and exact chunks until a size 0 chunk is found.
     */
    private void parseChunkedBody(String rawBody, HttpRequest request) {
        StringBuilder finalBody = new StringBuilder();
        int currentIndex = 0;

        while (currentIndex < rawBody.length()) {
            // Find the end of the line containing the hex size
            int crlfIndex = rawBody.indexOf("\r\n", currentIndex);
            if (crlfIndex == -1) {
                throw new IllegalArgumentException("400 Bad Request: Malformed chunked data");
            }

            // Extract the hex size (ignore chunk extensions separated by ';')
            String hexLine = rawBody.substring(currentIndex, crlfIndex).split(";")[0].trim();
            int chunkSize = Integer.parseInt(hexLine, 16); // Convert Hexadecimal to Decimal

            if (chunkSize == 0) {
                break; // A chunk of size 0 means we are DONE
            }

            int chunkDataStart = crlfIndex + 2; // Skip \r\n
            int chunkDataEnd = chunkDataStart + chunkSize;

            if (chunkDataEnd > rawBody.length()) {
                throw new IllegalArgumentException("400 Bad Request: Chunk size exceeds remaining data");
            }

            // Append the actual data chunk
            finalBody.append(rawBody, chunkDataStart, chunkDataEnd);

            // Move pointer to the next chunk: Skip data + the \r\n following the data
            currentIndex = chunkDataEnd + 2;
        }

        request.setBody(finalBody.toString());
    }
}