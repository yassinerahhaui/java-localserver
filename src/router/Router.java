package router;



import config.RouteConfig;
import config.ServerConfig;
import http.HttpRequest;
import http.HttpResponse;

public class Router {
    public static HttpResponse handle(HttpRequest request, ServerConfig config) {
        HttpResponse response = new HttpResponse();

        RouteConfig route = findMatchingRoute(request.getPath(), config);

        return response;
    }

    private static RouteConfig findMatchingRoute(String reqPath, ServerConfig config) {
        return null;
    }
}