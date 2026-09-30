package error;

import http.HttpResponse;
import config.ServerConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;



public class ErrorHandler {

    public static HttpResponse handleError(ServerConfig serverConfig, int statusCode, String defaultMessage) {
        HttpResponse response = new HttpResponse();
        response.setStatusCode(statusCode);
        response.setHeader("content-type", "text/html; charset=UTF-8");

        // 1. Qleb wach kayna custom error page f config.json
        if (serverConfig != null && serverConfig.getErrorPages() != null) {
            String errorPagePath = serverConfig.getErrorPages().get(String.valueOf(statusCode));
            if (errorPagePath != null) {
                try {
                    String resolvedPath = errorPagePath;
                    if (resolvedPath.startsWith("/")) {
                        resolvedPath = "." + resolvedPath;
                    }
                    if (Files.exists(Paths.get(resolvedPath))) {
                        byte[] content = Files.readAllBytes(Paths.get(resolvedPath));
                        response.setBody(content);
                        return response;
                    }
                } catch (IOException e) {
                    // Fallback l default error page
                }
            }
        }

        // 2. Default Error Page HTML ila ma lqanach custom page
        String htmlContent = generateDefaultErrorPage(statusCode, defaultMessage);
        response.setBody(htmlContent);
        return response;
    }

    private static String generateDefaultErrorPage(int statusCode, String message) {
        String statusText = HttpResponse.getStatusMessage(statusCode);
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><meta charset=\"UTF-8\"><title>" + statusCode + " " + statusText + "</title>\n" +
                "<style>body { font-family: sans-serif; background: #0f172a; color: white; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; }\n" +
                ".box { background: #1e293b; padding: 40px; border-radius: 10px; border: 1px solid #334155; }\n" +
                "h1 { font-size: 3rem; margin: 0; color: #ef4444; } p { color: #94a3b8; }</style></head>\n" +
                "<body><div class=\"box\"><h1>" + statusCode + " " + statusText + "</h1><p>" + message + "</p></div></body>\n" +
                "</html>";
    }

  
}
