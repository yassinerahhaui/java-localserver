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
     * Main dispatch method to route any HttpRequest to the appropriate handler.
     */
    public static HttpResponse route(HttpRequest request, ServerConfig serverConfig) {
        if (request == null) {
            return ErrorHandler.handleError(serverConfig, 400, "Bad Request: Request is null");
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

        // Delegate to handler
        HttpResponse response = StaticFileHandler.handle(request, serverConfig, matchingRoute);

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
