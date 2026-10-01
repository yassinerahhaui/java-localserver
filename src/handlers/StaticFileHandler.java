package handlers;

import config.RouteConfig;
import config.ServerConfig;
import error.ErrorHandler;
import http.HttpRequest;
import http.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class StaticFileHandler {

    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        // Text & Documents
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm", "text/html; charset=UTF-8");
        MIME_TYPES.put("css", "text/css; charset=UTF-8");
        MIME_TYPES.put("js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("txt", "text/plain; charset=UTF-8");
        MIME_TYPES.put("xml", "application/xml; charset=UTF-8");
        MIME_TYPES.put("pdf", "application/pdf");

        // Images
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("webp", "image/webp");

        // Media & Binaries
        MIME_TYPES.put("mp3", "audio/mpeg");
        MIME_TYPES.put("mp4", "video/mp4");
        MIME_TYPES.put("zip", "application/zip");
        MIME_TYPES.put("tar", "application/x-tar");
        MIME_TYPES.put("gz", "application/gzip");
    }

    
    //   Resolves the MIME type from a filename or path.
    
    public static String getMimeType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            String ext = filename.substring(dot + 1).toLowerCase();
            return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }

    
    //  Main handler entrypoint 
    
    public static HttpResponse handle(HttpRequest request, ServerConfig serverConfig, RouteConfig route) {
        if (route == null) {
            return ErrorHandler.handleError(serverConfig, 404, "No matching route found for: " + request.getPath());
        }

        //  Check HTTP Method allowed on this route
        if (route.getMethods() != null && !route.getMethods().isEmpty()) {
            if (!route.getMethods().contains(request.getMethod())) {
                return ErrorHandler.handleError(serverConfig, 405, "Method " + request.getMethod() + " not allowed on this route");
            }
        }

        //  Handle HTTP Redirections (e.g. 301 Moved Permanently)
        if (route.isRedirect()) {
            Map<String, Object> redir = route.getRedirect();
            int code = redir.containsKey("code") ? ((Number) redir.get("code")).intValue() : 301;
            String url = redir.containsKey("url") ? (String) redir.get("url") : "/";
            HttpResponse resp = new HttpResponse();
            resp.setStatusCode(code);
            resp.setHeader("Location", url);
            resp.setHeader("Content-Type", "text/plain; charset=UTF-8");
            resp.setBody("Redirecting to " + url);
            return resp;
        }

        //  Resolve relative path within route root
        String reqPath = request.getPath();
        String routePath = route.getPath();

        String relativePath = reqPath;
        if (relativePath.startsWith(routePath)) {
            relativePath = relativePath.substring(routePath.length());
        }
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        String rootDir = route.getRoot() != null ? route.getRoot() : ".";
        File file = new File(rootDir, relativePath);

        try {
            //  Security Check: Path Traversal Protection
            File canonicalRoot = new File(rootDir).getCanonicalFile();
            File canonicalFile = file.getCanonicalFile();

            if (!canonicalFile.getPath().startsWith(canonicalRoot.getPath())) {
                return ErrorHandler.handleError(serverConfig, 403, "Access Denied: Path traversal detected");
            }

            //  Check existence
            if (!canonicalFile.exists()) {
                return ErrorHandler.handleError(serverConfig, 404, "File Not Found: " + reqPath);
            }

            // Handle Directory vs File
            if (canonicalFile.isDirectory()) {
                // Ensure directory URL ends with trailing slash
                if (!reqPath.endsWith("/")) {
                    HttpResponse redirect = new HttpResponse();
                    redirect.setStatusCode(301);
                    redirect.setHeader("Location", reqPath + "/");
                    return redirect;
                }

                // Check for default_file (e.g. index.html)
                String defaultFileName = route.getDefaultFile() != null ? route.getDefaultFile() : "index.html";
                File defaultFile = new File(canonicalFile, defaultFileName);
                if (defaultFile.exists() && defaultFile.isFile()) {
                    return serveFile(defaultFile, serverConfig);
                }

                // If no default file, check directory_listing flag
                if (Boolean.TRUE.equals(route.getDirectoryListing())) {
                    return generateDirectoryListing(canonicalFile, reqPath);
                } else {
                    return ErrorHandler.handleError(serverConfig, 403, "Directory listing is forbidden on this path");
                }
            } else {
                return serveFile(canonicalFile, serverConfig);
            }

        } catch (IOException e) {
            return ErrorHandler.handleError(serverConfig, 500, "Internal Server Error reading file: " + e.getMessage());
        }
    }

 
    public static HttpResponse serveFile(File file, ServerConfig serverConfig) {
        if (!file.canRead()) {
            return ErrorHandler.handleError(serverConfig, 403, "Permission Denied: Cannot read file " + file.getName());
        }

        try {
            byte[] data = Files.readAllBytes(file.toPath());
            HttpResponse response = new HttpResponse();
            response.setStatusCode(200);
            response.setHeader("Content-Type", getMimeType(file.getName()));
            response.setHeader("Content-Length", String.valueOf(data.length));
            response.setBody(data);
            return response;
        } catch (IOException e) {
            return ErrorHandler.handleError(serverConfig, 500, "Error reading file: " + e.getMessage());
        }
    }

    //   Generates HTML directory listing page for directory requests.
    public static HttpResponse generateDirectoryListing(File dir, String uriPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"en\">\n");
        sb.append("<head>\n");
        sb.append("    <meta charset=\"UTF-8\">\n");
        sb.append("    <title>Index of ").append(escapeHtml(uriPath)).append("</title>\n");
        sb.append("    <style>\n");
        sb.append("        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0f172a; color: #f8fafc; padding: 40px; margin: 0; }\n");
        sb.append("        .container { max-width: 900px; margin: 0 auto; background: #1e293b; padding: 30px; border-radius: 12px; border: 1px solid #334155; box-shadow: 0 10px 25px rgba(0,0,0,0.3); }\n");
        sb.append("        h1 { margin-top: 0; color: #60a5fa; font-size: 1.8rem; border-bottom: 1px solid #334155; padding-bottom: 15px; }\n");
        sb.append("        table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n");
        sb.append("        th, td { text-align: left; padding: 12px; border-bottom: 1px solid #334155; }\n");
        sb.append("        th { color: #94a3b8; font-weight: 600; font-size: 0.9rem; text-transform: uppercase; }\n");
        sb.append("        tr:hover { background: #273549; }\n");
        sb.append("        a { color: #38bdf8; text-decoration: none; font-weight: 500; }\n");
        sb.append("        a:hover { text-decoration: underline; }\n");
        sb.append("        .size, .date { color: #94a3b8; font-size: 0.9rem; }\n");
        sb.append("        .footer { margin-top: 25px; font-size: 0.85rem; color: #64748b; text-align: right; }\n");
        sb.append("    </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("    <div class=\"container\">\n");
        sb.append("        <h1>📁 Index of ").append(escapeHtml(uriPath)).append("</h1>\n");
        sb.append("        <table>\n");
        sb.append("            <thead><tr><th>Name</th><th>Size</th><th>Last Modified</th></tr></thead>\n");
        sb.append("            <tbody>\n");

        // Parent Directory link if not root
        if (!"/".equals(uriPath)) {
            String parentUri = uriPath.endsWith("/") ? uriPath.substring(0, uriPath.length() - 1) : uriPath;
            int lastSlash = parentUri.lastIndexOf('/');
            parentUri = lastSlash >= 0 ? parentUri.substring(0, lastSlash + 1) : "/";
            if (parentUri.isEmpty()) parentUri = "/";

            sb.append("                <tr>");
            sb.append("<td><a href=\"").append(parentUri).append("\">⤴️ ../ (Parent Directory)</a></td>");
            sb.append("<td class=\"size\">-</td><td class=\"date\">-</td></tr>\n");
        }

        File[] files = dir.listFiles();
        if (files != null) {
            // Sort: directories first, then alphabetically
            Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (File f : files) {
                if (f.isHidden() || f.getName().startsWith(".")) continue;

                String name = f.getName() + (f.isDirectory() ? "/" : "");
                String link = uriPath.endsWith("/") ? uriPath + name : uriPath + "/" + name;
                String icon = f.isDirectory() ? "📁 " : "📄 ";
                String sizeStr = f.isDirectory() ? "-" : formatFileSize(f.length());
                String dateStr = sdf.format(new Date(f.lastModified()));

                sb.append("                <tr>\n");
                sb.append("                    <td><a href=\"").append(escapeHtml(link)).append("\">")
                        .append(icon).append(escapeHtml(name)).append("</a></td>\n");
                sb.append("                    <td class=\"size\">").append(sizeStr).append("</td>\n");
                sb.append("                    <td class=\"date\">").append(dateStr).append("</td>\n");
                sb.append("                </tr>\n");
            }
        }

        sb.append("            </tbody>\n");
        sb.append("        </table>\n");
        sb.append("        <div class=\"footer\">Localhost Web Server / 42 Project</div>\n");
        sb.append("    </div>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");

        HttpResponse response = new HttpResponse();
        response.setStatusCode(200);
        response.setHeader("Content-Type", "text/html; charset=UTF-8");
        response.setBody(sb.toString());
        return response;
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

}