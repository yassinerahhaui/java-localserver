package utils;

import java.io.File;

/**
 * DTO (Data Transfer Object) to hold the result of the routing process.
 */
public class RouteResult {
    private final File file;
    private final int statusCode;
    private final boolean isCgi;

    public RouteResult(File file, int statusCode, boolean isCgi) {
        this.file = file;
        this.statusCode = statusCode;
        this.isCgi = isCgi;
    }

    public File getFile() {
        return file;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isCgi() {
        return isCgi;
    }

    @Override
    public String toString() {
        return """
                --- Route Result ---
                Status Code: %d
                Target File: %s
                Is CGI: %b
                --------------------
                """.formatted(statusCode, (file != null ? file.getAbsoluteFile() : "None"),
                isCgi);
    }
}
