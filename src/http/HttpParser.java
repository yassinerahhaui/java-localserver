package http;

/**
 * Parses a raw HTTP string into an HttpRequest object.
 * Handles extraction of the Request Line and HTTP Headers.
 */
public class HttpParser {
    /**
     * Parses the raw HTTP request string and populates an HttpRequest object.
     *
     * @param rawRequest The raw HTTP request string received from the client.
     * @return A populated HttpRequest object containing the parsed data.
     * @throws IllegalArgumentException If the request is empty or malformed.
     */
    public HttpRequest parser(String rawRequest) throws IllegalArgumentException {
        // 1. Error Handling: Ensure the request is not empty to prevent server crashes
        if (rawRequest == null || rawRequest.isBlank()) {
            throw new IllegalArgumentException("400 Bad Request: Empty request");
        }

        HttpRequest request = new HttpRequest();

        // 2. Split the raw request into lines using the HTTP standard CRLF (\r\n) separator
        String[] lines = rawRequest.split("\r\n");

        if (lines.length == 0) {
            throw new IllegalArgumentException("400 Bad Request: Invalid request format");
        }

        // 3. Task: Extract the Request Line (always the first line at index 0)
        parseRequestLine(lines[0], request);

        // 4. Task: Read Headers line by line
        parseHeaders(lines, request);

        return request;
    }

    /**
     * Helper method to parse the Request Line (Method, URI, Version).
     *
     * @param requestLine The first line of the HTTP request.
     * @param request     The HttpRequest object to populate.
     * @throws IllegalArgumentException If the request line is malformed or the method is unknown.
     */
    private void parseRequestLine(String requestLine, HttpRequest request) {
        // The request line components are separated by spaces
        String[] parts = requestLine.split(" ");

        // Error Handling: The request line must contain exactly 3 parts
        if (parts.length != 3) {
            throw new IllegalArgumentException("400 Bad Request: Malformed request line");
        }

        // Use the HttpMethod enum to ensure Type Safety
        HttpMethod method = HttpMethod.fromString(parts[0]);
        if (method == null) {
            throw new IllegalArgumentException("501 Not Implemented: Unknown HTTP method '" + parts[0] + "'");
        }

        request.setMethod(method);
        request.setUri(parts[1]);
        request.setVersion(parts[2]);

        // Additional Validation: Ensure the protocol is HTTP/1.1
        if (!request.getVersion().equals("HTTP/1.1")) {
            throw new IllegalArgumentException("505 HTTP Version Not Supported");
        }
    }

    /**
     * Helper method to parse HTTP headers.
     * Iterates through the lines and stores them in the request object until an empty line is reached.
     *
     * @param lines   Array of string lines from the raw request.
     * @param request The HttpRequest object to populate.
     * @throws IllegalArgumentException If a header line is malformed.
     */
    private void parseHeaders(String[] lines, HttpRequest request) {
        // Start from the second line (index 1) since the first line already parsed
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];

            // Task: Stop at the empty line \r\n
            // Since we split by \r\n, the empty line becomes an empty string ""
            if (line.isEmpty()) {
                break; // End of headers reached, stop parsing
            }

            // Split the line at the first colon ":"
            // Using limit 2 ensures that colons within the header value (e.g., in a URL) are not split
            String[] headerParts = line.split(":", 2);

            if (headerParts.length == 2) {
                request.addHeader(headerParts[0], headerParts[1]);
            } else {
                throw new IllegalArgumentException("400 Bad Request: Malformed header line -> " + line);
            }
        }
    }
}