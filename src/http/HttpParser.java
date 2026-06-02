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

    }
}