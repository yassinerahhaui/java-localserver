package http;

/**
 * Represents the standard HTTP methods as defined by the HTTP/1.1 protocol.
 * This enum provides a type-safe way to handle HTTP methods within the server's request lifecycle.
 */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    HEAD,
    OPTIONS,
    TRACE;

    /**
     * Safely converts a string representation of an HTTP method into its corresponding {@code HttpMethod} enum constant.
     * <p>
     * This method handles null, empty, and incorrectly cased inputs gracefully without throwing exceptions.
     * It is particularly useful for parsing raw HTTP request lines.
     * </p>
     *
     * @param method the raw string representation of the HTTP method parsed from the request (e.g., "GET", "post").
     * @return the matching {@code HttpMethod} constant, or {@code null} if the provided string is null, empty, or does not match any supported method.
     */
    public static HttpMethod fromString(String method) {
        if (method == null || method.trim().isEmpty()) {
            return null;
        }
        try {
            // Notice: Changed toUpperCase() because enum constants are declared in uppercase
            return HttpMethod.valueOf(method.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}