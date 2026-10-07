package router;

import config.RouteConfig;
import config.ServerConfig;
import error.ErrorHandler;
import handlers.StaticFileHandler;
import http.HttpRequest;
import http.HttpResponse;

import java.util.List;

public class Router {

    /**
     * Finds the best matching route for a request path using longest prefix matching.
     */
    public static RouteConfig matchRoute(ServerConfig serverConfig, String requestPath) {
        if (serverConfig == null || serverConfig.getRoutes() == null || requestPath == null) {
            return null;
        }

        RouteConfig bestMatch = null;
        int longestMatchLength = -1;

        for (RouteConfig route : serverConfig.getRoutes()) {
            String routePath = route.getPath();
            if (routePath == null) continue;

            boolean matches = false;
            if (requestPath.equals(routePath)) {
                matches = true;
            } else if (routePath.equals("/")) {
                matches = true;
            } else if (requestPath.startsWith(routePath.endsWith("/") ? routePath : routePath + "/")) {
                matches = true;
            }

            if (matches && routePath.length() > longestMatchLength) {
                longestMatchLength = routePath.length();
                bestMatch = route;
            }
        }

        return bestMatch;
    }

    /**
     * Alias for matchRoute to maintain compatibility across branches.
     */
    public static RouteConfig findMatchingRoute(String requestPath, ServerConfig serverConfig) {
        return matchRoute(serverConfig, requestPath);
    }

    /**
     * Alias for route to support Server.java calling Router.handle(request, config).
     */
    public static HttpResponse handle(HttpRequest request, ServerConfig serverConfig) {
        return route(request, serverConfig);
    }

    /**
     * Main dispatch method to route any HttpRequest to the appropriate handler.
     */
    public static HttpResponse route(HttpRequest request, ServerConfig serverConfig) {
        if (request == null) {
            return ErrorHandler.handleError(serverConfig, 400, "Bad Request: Request is null");
        }

        // Security: Path Traversal Protection
        String rawPath = request.getPath();
        String decodedPath = HttpRequest.urlDecode(rawPath);
        if (rawPath.contains("..") || decodedPath.contains("..")) {
            return ErrorHandler.handleError(serverConfig, 403, "Forbidden: Path Traversal Detected");
        }

        RouteConfig matchingRoute = matchRoute(serverConfig, request.getPath());
        if (matchingRoute == null) {
            return ErrorHandler.handleError(serverConfig, 404, "Not Found: No matching route for " + request.getPath());
        }

        // Validate allowed HTTP methods for this route
        List<String> allowedMethods = matchingRoute.getMethods();
        String method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
        if (allowedMethods != null && !allowedMethods.isEmpty()) {
            if (!allowedMethods.contains(method)) {
                return ErrorHandler.handleError(serverConfig, 405, "Method " + method + " not allowed on route " + matchingRoute.getPath());
            }
        }

        // Validate client_max_body_size limit
        long maxBodySize = matchingRoute.getClientMaxBodySize() > 0 
                ? matchingRoute.getClientMaxBodySize() 
                : (serverConfig != null ? serverConfig.getClientMaxBodySize() : 0);
        if (request.getBody() != null && maxBodySize > 0 && request.getBody().length > maxBodySize) {
            return ErrorHandler.handleError(serverConfig, 413, "Payload Too Large: Body size (" + request.getBody().length + " bytes) exceeds maximum allowed (" + maxBodySize + " bytes)");
        }

        // Handle Redirection (301 / 302)
        if (matchingRoute.isRedirect()) {
            return StaticFileHandler.handleRedirect(matchingRoute);
        }

        // Delegate to handler (CGI or Static Files)
        HttpResponse response;
        if (matchingRoute.hasCgi() && cgi.CgiHandler.isCgiRequest(matchingRoute, request.getPath())) {
            response = cgi.CgiHandler.executeCgi(request, serverConfig, matchingRoute);
        } else {
            response = StaticFileHandler.handle(request, serverConfig, matchingRoute);
        }

        // Session & Cookie Tracking (Day 5)
        utils.SessionManager.getInstance().handleRequestSession(request, response);

        return response;
    }

    /**
     * Handles POST file uploads.
     */
    public static HttpResponse handlePost(HttpRequest request, ServerConfig serverConfig, RouteConfig route) {
        return StaticFileHandler.handlePost(request, serverConfig, route);
    }

    /**
     * Handles DELETE file deletions.
     */
    public static HttpResponse handleDelete(HttpRequest request, ServerConfig serverConfig, RouteConfig route) {
        return StaticFileHandler.handleDelete(request, serverConfig, route);
    }
}
