package http;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpResponse {
    private int statusCode;
    private String statusMessage;
    private final Map<String, String> headers;
    private final Map<String, String> cookies;
    private byte[] body;
    private File file = null;

    public HttpResponse() {
        this.statusCode = 200;
        this.statusMessage = "OK";
        this.headers = new LinkedHashMap<>();
        this.cookies = new LinkedHashMap<>();
        this.body = new byte[0];

        // Headers par défaut
        headers.put("server", "CustomServer/1.0");
    }

    // Status Code & Message
    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int code) {
        this.statusCode = code;
        this.statusMessage = getStatusMessage(code);
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    //  Headers
    public void setHeader(String name, String value) {
        if (name != null && value != null) {
            headers.put(name.toLowerCase().trim(), value.trim());
        }
    }

    public String getHeader(String name) {
        if (name == null) return null;
        return headers.get(name.toLowerCase().trim());
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    // Cookies 
    public void addCookie(String name, String value) {
        cookies.put(name, name + "=" + value + "; Path=/");
    }

    public void addCookie(String name, String value, long maxAge) {
        cookies.put(name, name + "=" + value + "; Path=/; Max-Age=" + maxAge);
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    // Body & File
    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body != null ? body : new byte[0];
        setHeader("content-length", String.valueOf(this.body.length));
    }

    public void setBody(String text) {
        byte[] bytes = text != null ? text.getBytes(StandardCharsets.UTF_8) : new byte[0];
        setBody(bytes);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        if (file != null) {
            setHeader("content-length", String.valueOf(file.length()));
        }
    }

    public byte[] toBytes() {
        // Ila ma kanx content-length mdefini, 7sbo mn body wla file
        if (!headers.containsKey("content-length") && !headers.containsKey("transfer-encoding")) {
            long len = (file != null) ? file.length() : body.length;
            headers.put("content-length", String.valueOf(len));
        }

        StringBuilder headerBuilder = new StringBuilder();
        //  Status-Line: HTTP/1.1 200 OK\r\n
        headerBuilder.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append("\r\n");

        // Headers: Name: Value\r\n
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headerBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }

        // Set-Cookie Headers
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            headerBuilder.append("Set-Cookie: ").append(entry.getValue()).append("\r\n");
        }

        // Satr khawi kay-ferreq les headers 3la l-body (\r\n)
        headerBuilder.append("\r\n");

        byte[] headerBytes = headerBuilder.toString().getBytes(StandardCharsets.UTF_8);

        // Ila kan fichier static, toBytes() kat-rdd ghir les headers (body kaytsift b zero-copy)
        if (file != null && body.length == 0) {
            return headerBytes;
        }

        // Jme3 headers + body f byte[] wa7ed
        byte[] result = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(body, 0, result, headerBytes.length, body.length);

        return result;
    }

    public static String getStatusMessage(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 413 -> "Payload Too Large";
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "Unknown";
        };
    }

    @Override
    public String toString() {
        return "HTTP/1.1 " + statusCode + " " + statusMessage;
    }


}

