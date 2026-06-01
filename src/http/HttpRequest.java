package http;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a parsed HTTP request.
 * Contains the Request Line details (Method, URI, Version) and the Headers.
 */
public class HttpRequest {
    private HttpMethod method;
    private String uri;
    private String version;
    private final Map<String,String> headers;

    public HttpRequest() {
        this.headers = new HashMap<>();
    }

    // ================== Getters && Setters ==================

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    // =============== Helper Methods ===============

    /**
     * Adds a header to the request.
     * Stores keys in lowercase to ensure case-insensitive retrieval later.
     */
    public void addHeader(String key, String value) {
        if (key != null && value != null) {
            this.headers.put(key.trim().toLowerCase(), value.trim());
        }
    }

    /**
     * Retrieves a header value by its key (Case-Insensitive).
     */
    public String getHeader(String key) {
        if (key == null) return null;
        return this.headers.get(key.toLowerCase());
    }

    /**
     * Prints the request details nicely for debugging and Mock Testing.
     */
    @Override
    public String toString() {
        return """
               === HTTP Request ===
               Method: %s
               URI: %s
               Version: %s
               Headers: %s
               ====================
               """.formatted(method, uri, version, headers);
    }
}