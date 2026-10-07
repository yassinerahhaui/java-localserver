package cgi;

import config.RouteConfig;
import config.ServerConfig;
import error.ErrorHandler;
import http.HttpRequest;
import http.HttpResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CgiHandler {

    private static final int CGI_TIMEOUT_SECONDS = 5;

    public static boolean isCgiRequest(RouteConfig route, String requestPath) {
        if (route == null || !route.hasCgi() || requestPath == null) {
            return false;
        }

        int dot = requestPath.lastIndexOf('.');
        if (dot >= 0) {
            String ext = requestPath.substring(dot).toLowerCase();
            return route.getCgi().containsKey(ext);
        }
        return false;
    }

    public static HttpResponse executeCgi(HttpRequest request, ServerConfig serverConfig, RouteConfig route) {
        String reqPath = request.getPath();
        String routePath = route.getPath();

        String relativePath = reqPath;
        if (relativePath.startsWith(routePath)) {
            relativePath = relativePath.substring(routePath.length());
        }
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        String rootDir = route.getRoot() != null ? route.getRoot() : "./cgi-bin";
        File file = new File(rootDir, relativePath);

        try {
            // Security: Path Traversal Protection
            File canonicalRoot = new File(rootDir).getCanonicalFile();
            File canonicalScript = file.getCanonicalFile();

            if (!canonicalScript.toPath().startsWith(canonicalRoot.toPath())) {
                return ErrorHandler.handleError(serverConfig, 403, "Access Denied: Path traversal detected");
            }

            if (!canonicalScript.exists()) {
                return ErrorHandler.handleError(serverConfig, 404, "CGI Script Not Found: " + reqPath);
            }

            if (canonicalScript.isDirectory()) {
                return ErrorHandler.handleError(serverConfig, 403, "Forbidden: Target is a directory");
            }

            // Determine interpreter from extension
            int dot = canonicalScript.getName().lastIndexOf('.');
            String ext = dot >= 0 ? canonicalScript.getName().substring(dot).toLowerCase() : "";
            String interpreter = route.getCgiInterpreter(ext);

            List<String> command = new ArrayList<>();
            if (interpreter != null && !interpreter.trim().isEmpty()) {
                command.add(interpreter.trim());
            }
            command.add(canonicalScript.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            File workingDir = canonicalScript.getParentFile() != null ? canonicalScript.getParentFile() : canonicalRoot;
            pb.directory(workingDir);

            // Set CGI 1.1 Environment Variables
            Map<String, String> env = pb.environment();
            env.put("GATEWAY_INTERFACE", "CGI/1.1");
            env.put("SERVER_PROTOCOL", request.getHttpVersion() != null ? request.getHttpVersion() : "HTTP/1.1");
            env.put("REQUEST_METHOD", request.getMethod() != null ? request.getMethod().toUpperCase() : "GET");
            env.put("SCRIPT_NAME", reqPath);
            env.put("SCRIPT_FILENAME", canonicalScript.getAbsolutePath());
            env.put("PATH_INFO", reqPath);

            String uri = request.getUri();
            int qIndex = uri != null ? uri.indexOf('?') : -1;
            String queryString = (qIndex >= 0 && qIndex + 1 < uri.length()) ? uri.substring(qIndex + 1) : "";
            env.put("QUERY_STRING", queryString);

            long contentLength = request.getContentLength();
            env.put("CONTENT_LENGTH", String.valueOf(contentLength));
            env.put("CONTENT_TYPE", request.getContentType() != null ? request.getContentType() : "");

            if (serverConfig != null) {
                env.put("SERVER_NAME", serverConfig.getServerName() != null ? serverConfig.getServerName() : "localhost");
                if (serverConfig.getPorts() != null && !serverConfig.getPorts().isEmpty()) {
                    env.put("SERVER_PORT", String.valueOf(serverConfig.getPorts().get(0)));
                } else {
                    env.put("SERVER_PORT", "8080");
                }
            } else {
                env.put("SERVER_NAME", "localhost");
                env.put("SERVER_PORT", "8080");
            }

            env.put("REDIRECT_STATUS", "200");
            env.put("REMOTE_ADDR", "127.0.0.1");

            // Convert HTTP headers to HTTP_HEADER_NAME format
            if (request.getHeaders() != null) {
                for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                    String headerKey = "HTTP_" + entry.getKey().toUpperCase().replace('-', '_');
                    env.put(headerKey, entry.getValue());
                }
            }

            // Launch process
            Process process = pb.start();

         
            byte[] body = request.getBody();
            if (body != null && body.length > 0) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(body);
                    os.flush();
                }
            } else {
                try {
                    process.getOutputStream().close();
                } catch (IOException ignored) {}
            }

            // Asynchronous / Non-blocking timeout monitoring (5s)
            boolean finished = process.waitFor(CGI_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ErrorHandler.handleError(serverConfig, 504, "Gateway Timeout: CGI process exceeded " + CGI_TIMEOUT_SECONDS + "s limit");
            }

            // Read output from process stdout
            byte[] cgiOutput;
            try (InputStream is = process.getInputStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) {
                    baos.write(buf, 0, n);
                }
                cgiOutput = baos.toByteArray();
            }

            if (cgiOutput.length == 0 && process.exitValue() != 0) {
                // Read stderr if stdout is empty and process failed
                String stderrMsg = "";
                try (InputStream es = process.getErrorStream()) {
                    stderrMsg = new String(es.readAllBytes(), StandardCharsets.UTF_8);
                }
                return ErrorHandler.handleError(serverConfig, 502, "Bad Gateway: CGI script error (exit code " + process.exitValue() + "): " + stderrMsg);
            }

            return parseCgiResponse(cgiOutput, serverConfig);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ErrorHandler.handleError(serverConfig, 500, "Internal Server Error: CGI interrupted");
        } catch (IOException e) {
            return ErrorHandler.handleError(serverConfig, 500, "Internal Server Error executing CGI: " + e.getMessage());
        }
    }

    public static HttpResponse parseCgiResponse(byte[] cgiOutput, ServerConfig serverConfig) {
        if (cgiOutput == null || cgiOutput.length == 0) {
            HttpResponse emptyResp = new HttpResponse();
            emptyResp.setStatusCode(200);
            emptyResp.setBody(new byte[0]);
            return emptyResp;
        }

        int headerEnd = -1;
        int bodyStart = -1;

        // Find header-body separator: \r\n\r\n or \n\n
        for (int i = 0; i < cgiOutput.length - 1; i++) {
            if (i + 3 < cgiOutput.length &&
                    cgiOutput[i] == '\r' && cgiOutput[i + 1] == '\n' &&
                    cgiOutput[i + 2] == '\r' && cgiOutput[i + 3] == '\n') {
                headerEnd = i;
                bodyStart = i + 4;
                break;
            } else if (cgiOutput[i] == '\n' && cgiOutput[i + 1] == '\n') {
                headerEnd = i;
                bodyStart = i + 2;
                break;
            }
        }

        HttpResponse response = new HttpResponse();
        response.setStatusCode(200);

        if (headerEnd == -1) {
            // No headers found: assume entire output is body with default content-type
            response.setHeader("Content-Type", "text/html; charset=UTF-8");
            response.setBody(cgiOutput);
            return response;
        }

        // Parse headers section
        String headerSection = new String(cgiOutput, 0, headerEnd, StandardCharsets.UTF_8);
        String[] lines = headerSection.split("\r?\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int colon = line.indexOf(':');
            if (colon > 0) {
                String name = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();

                if (name.equalsIgnoreCase("Status")) {
                    //  Status: 200 OK or Status: 404
                    String[] parts = value.split("\\s+", 2);
                    try {
                        int code = Integer.parseInt(parts[0]);
                        response.setStatusCode(code);
                    } catch (NumberFormatException ignored) {}
                } else if (name.equalsIgnoreCase("Location")) {
                    response.setHeader("Location", value);
                    if (response.getStatusCode() == 200) {
                        response.setStatusCode(302);
                    }
                } else if (name.equalsIgnoreCase("Set-Cookie")) {
                    // Set-Cookie: key=val; Path=/
                    int eq = value.indexOf('=');
                    if (eq > 0) {
                        String cookieName = value.substring(0, eq).trim();
                        response.getCookies().put(cookieName, value);
                    }
                } else {
                    response.setHeader(name, value);
                }
            }
        }

        // Extract body section
        if (bodyStart >= 0 && bodyStart < cgiOutput.length) {
            byte[] bodyBytes = Arrays.copyOfRange(cgiOutput, bodyStart, cgiOutput.length);
            response.setBody(bodyBytes);
        } else {
            response.setBody(new byte[0]);
        }

        return response;
    }
}
