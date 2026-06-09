package core;

import java.nio.file.Path;
import java.nio.file.Paths;

import http.HttpRequest;
import java.io.File;
import utils.RouteResult;


/**
 * Maps the requested URI to local server files and determines the appropriate action.
 */
public class Router {
    // The base directory where all static files and scripts are stored
    private final String documentRoot;

    public Router(String documentRoot) {
        this.documentRoot = documentRoot;
    }

    /**
     * Routes the HTTP request to the appropriate file or error status.
     *
     * @param request The parsed HttpRequest
     * @return A RouteResult containing the target file, status code, and CGI flag
     */
    public RouteResult route(HttpRequest request) {
        String uri = request.getUri();

        // 1. Security check: Prevent Directory Traversal Attacks (e.g., ../../../etc/passwd)
        // If a hacker tries to go back in directories, return 403 Forbidden.
        if (uri.contains("..")) {
            return new RouteResult(null, 403, false);
        }

        // 2. Default document routing: If requesting root "/", serve "index.html"
        if (uri.equals("/")) {
            uri = "/index.html";
        }

        // 3. Map the requested URI to the physical file path on the server
        Path targetPath = Paths.get(documentRoot, uri);
        File targetFile = targetPath.toFile();

        // 4. Missing files: Check if the file exists and is not a directory
        if (!targetFile.exists() || targetFile.isDirectory()) {
            return new RouteResult(null, 404, false); // 404 Not Found
        }

        // 5. Permissions: Check if the server has read access to the file
        if (!targetFile.canRead()) {
            return new RouteResult(null, 403, false); // 403 Forbidden
        }

        // 6. CGI Routing: Flag the request if the extension matches our CGI scripts
        boolean isCgi = uri.endsWith(".py") || uri.endsWith(".php") || uri.endsWith(".sh") || uri.endsWith(".cgi");
        
        // 7. Success: Return 200 OK with the target file and the CGI flag
        return new RouteResult(targetFile, 200, isCgi);
    }
}
