package router;



import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import config.RouteConfig;
import config.ServerConfig;
import http.HttpRequest;
import http.HttpResponse;

public class Router {
    public static HttpResponse handle(HttpRequest request, ServerConfig config) {
        HttpResponse response = new HttpResponse();

        // 1. find the exact or closest route for this path
        RouteConfig route = findMatchingRoute(request.getPath(), config);

        if (route == null) {
            return sendError(response, 404, "Not Found: No route configuration.");
        }

        // 2. Check if HTTP method is allowed on this route
        if (route.getMethods() != null && !route.getMethods().contains(request.getMethod())) {
            return sendError(response, 405, "Method Not Allowed!");
        }

        // 3. Check Max Body Size Limit
        if (request.getBody() != null && request.getBody().length > route.getClientMaxBodySize()) {
            return sendError(response, 413, "Payload Too Large!");
        }

        // 4. Handle Redirection (301 / 302)
        if (route.isRedirect()) {
            // not completed
            response.setStatusCode(301);
            response.setHeader("Location", String.valueOf(route.getRedirect().get("url")));
            return response;
        }

        // 5. Route to the right handler based on Method
        try {
            switch (request.getMethod()) {
                case "GET":
                    handleGet(request, response, route);
                    break;
                case "POST":
                    handlePost(request, response, route);
                case "DELETE":
                    handleDelete(request, response, route);
                default:
                    return sendError(response, 501, "Not Implemented!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return sendError(response, 500, "Internal Server Error");
        }


        return response;
    }

    private static RouteConfig findMatchingRoute(String reqPath, ServerConfig config) {
        RouteConfig bestMatch = null;
        int maxLen = -1;

        // Matches the longest prefix. Example: request for "/a/b/c"
        // Route "/a/b" beats route "/a"
        for (RouteConfig route : config.getRoutes()) {
            if (reqPath.startsWith(route.getPath())) {
                if (route.getPath().length() > maxLen) {
                    maxLen = route.getPath().length();
                    bestMatch = route;
                }
            }
        }
        return bestMatch;
    }

    private static void handleGet(HttpRequest request, HttpResponse response, RouteConfig route) throws IOException {
        // Construct the pfysical file path
        String relativePath = request.getPath().substring(route.getPath().length());
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        File requestedFile = new File(route.getRoot(), relativePath);

        // Security Audit: Prevent Directory Traversal Attack (e.g., /../../../etc/passwd)
        String canonicalRoot = new File(route.getRoot()).getCanonicalPath();
        String canonicalRequested = requestedFile.getCanonicalPath();

        if (!canonicalRequested.startsWith(canonicalRoot)) {
            sendError(response, 403, "Forbidden: Path Traversal Detected!");
            return;
        }

        if (requestedFile.exists()) {
            if (requestedFile.isDirectory()) {
                File defaultFile = new File(requestedFile, route.getDefaultFile());
                if (defaultFile.exists() && defaultFile.isFile()) {
                    serveStaticFile(defaultFile, response);
                } else if (Boolean.TRUE.equals(route.getDirectoryListing())) {
                    // TODO: Asta will build a beautiful HTML directory listing here
                    response.setStatusCode(200);
                    response.setHeader("Content-Type", "text/html");
                    response.setBody("<h1>Directory Listing for " + request.getPath() + "</h1>");
                } else {
                    sendError(response, 403, "Forbidden: Directory listing denied");
                }
                
            } else {
                // It's a normal file
                serveStaticFile(requestedFile, response);
            }
        } else {
            sendError(response, 404, "Not Found!");
        }
    }

    private static void handlePost(HttpRequest request, HttpResponse response, RouteConfig route) {
        // TODO: Handle file Upload logic here
        response.setStatusCode(201);
        response.setBody("POST request received! Ready to upload files.");
    }

    private static void handleDelete(HttpRequest request, HttpResponse response, RouteConfig route) {
        // TODO: Handle File Deletion logic here
        response.setStatusCode(204); // No Content
    }

    private static void serveStaticFile(File file, HttpResponse response) throws IOException {
        response.setStatusCode(200);
    
    // Detect MIME Type based on file extension
    String fileName = file.getName().toLowerCase();
    if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
        response.setHeader("Content-Type", "text/html");
    } else if (fileName.endsWith(".css")) {
        response.setHeader("Content-Type", "text/css");
    } else if (fileName.endsWith(".js")) {
        response.setHeader("Content-Type", "application/javascript");
    } else if (fileName.endsWith(".png")) {
        response.setHeader("Content-Type", "image/png");
    } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
        response.setHeader("Content-Type", "image/jpeg");
    } else if (fileName.endsWith(".json")) {
        response.setHeader("Content-Type", "application/json");
    } else {
        // Default fallback for unknown files
        response.setHeader("Content-Type", "application/octet-stream");
    }
    
    response.setFile(file); 
    response.setBody(Files.readAllBytes(file.toPath()));
    }

    private static HttpResponse sendError(HttpResponse response, int code, String msg) {
        response.setStatusCode(code);
        response.setHeader("Content-Type", "text/html");
        response.setBody("<h1>" + code + " " + HttpResponse.getStatusMessage(code) + "</h1><p>" + msg + "</p>");
        return response;
    }
}