package http;

import java.util.*;


public class HttpRequest {
    private String method;
    private String uri;
    private String httpVersion;
    private final Map<String, String> headers;
    private final Map<String, String> cookies;
    private final Map<String, String> queryParams;
    private byte[] body;

    public HttpRequest() {
        this.headers = new HashMap<>();
        this.cookies = new HashMap<>();
        this.queryParams = new HashMap<>();
        this.body = new byte[0];
    }

    // Getters & Setters 
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method != null ? method.toUpperCase() : null;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
        parseQueryString();
    }


    public String getPath() {
        if (uri == null) return "/";
        int q = uri.indexOf('?');
        return q >= 0 ? uri.substring(0, q) : uri;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    // Headers
    public void addHeader(String name, String value) {
        if (name != null) {
            headers.put(name.toLowerCase().trim(), value != null ? value.trim() : "");
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
    public Map<String, String> getCookies() {
        if (cookies.isEmpty() && getHeader("cookie") != null) {
            parseCookies();
        }
        return cookies;
    }

    public String getCookie(String name) {
        if (name == null) return null;
        if (cookies.isEmpty() && getHeader("cookie") != null) {
            parseCookies();
        }
        return cookies.get(name);
    }

    public void parseCookies() {
        cookies.clear();
        String cookieHeader = getHeader("cookie");
        if (cookieHeader != null) {
            String[] pairs = cookieHeader.split(";");
            for (String pair : pairs) {
                String[] kv = pair.trim().split("=", 2);
                if (kv.length == 2) {
                    cookies.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
    }

    // Query Params (?key=val&key2=val2)
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getQueryParam(String name) {
        return queryParams.get(name);
    }

    private void parseQueryString() {
        queryParams.clear();
        if (uri == null) return;

        int q = uri.indexOf('?');
        if (q >= 0 && q + 1 < uri.length()) {
            String query = uri.substring(q + 1);
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    queryParams.put(urlDecode(kv[0]), urlDecode(kv[1]));
                } else if (kv.length == 1) {
                    queryParams.put(urlDecode(kv[0]), "");
                }
            }
        }
    }

    //  Body 
    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body != null ? body : new byte[0];
    }

    //  Helpers 
    public long getContentLength() {
        String cl = getHeader("content-length");
        if (cl != null) {
            try {
                return Long.parseLong(cl);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return body != null ? body.length : 0;
    }

    public String getContentType() {
        return getHeader("content-type");
    }

    public boolean isChunked() {
        String te = getHeader("transfer-encoding");
        return te != null && te.toLowerCase().contains("chunked");
    }

    public static String urlDecode(String s) {
        if (s == null) return "";
        try {
            return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }

    @Override
    public String toString() {
        return method + " " + uri + " " + httpVersion;
    }
}