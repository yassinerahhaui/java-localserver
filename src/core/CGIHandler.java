package core;

import http.HttpRequest;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;

/**
 * Executes external CGI script using ProcessBuilder.
 */
public class CGIHandler {
    /**
     * Executes the script and its standard output.
     * @param scriptFile The target CGI script file.
     * @param request The parsed HTTP request.
     * @return The script output as a byte array.
     * @throws Exception If the script fails or cannot be found.
     */
    public byte[] executeCGI(File scriptFile, HttpRequest request) throws Exception {
        // Error Handling: Ensure the file actually exists before executing
        if (scriptFile == null || !scriptFile.exists() || !scriptFile.isFile()) {
            throw new IllegalArgumentException("500 Internal Server Error: CGI script not found.");
        }

        String scriptPath = scriptFile.getAbsolutePath();
        ProcessBuilder pb;

        // 1. Determine the interpreter dynamically based on the file extension
        if (scriptPath.endsWith(".py")) {
            pb = new ProcessBuilder("python3", scriptPath);
        } else if (scriptPath.endsWith(".php")) {
            pb = new ProcessBuilder("php-cgi", scriptPath);
        } else if (scriptPath.endsWith(".sh")) {
            pb = new ProcessBuilder("bash", scriptPath);
        } else {
            // Fallback: Let the OS execute it directly 
            // (Requires the file to have execution permissions 'chmod +x' and a valid Shebang #!)
            pb = new ProcessBuilder(scriptPath);
        }

        // 2. Setup Environment Variables
        Map<String, String> env = pb.environment();
        env.put("GATEWAY_INTERFACE", "CGI/1.1");
        env.put("SERVER_PROTOCOL", request.getVersion() != null ? request.getVersion() : "HTTP/1.1");
        env.put("REQUEST_METHOD", request.getMethod().name());

        // Pass the URI as PATH_INFO so the script knows exactly what was requested
        env.put("PATH_INFO", request.getUri() != null ? request.getUri() : "");

        // Map HTTP Headers to Environment Variables (Prefix with HTTP_)
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                String envKey = "HTTP_" + header.getKey().toUpperCase().replace("-", "_");
                env.put(envKey, header.getValue());
            }
        }

        // 3. Start the process
        Process process = pb.start();

        // 4. Pass the Request Body to the Script's Standard Input (stdin)
        if (request.getBody() != null && !request.getBody().isEmpty()) {
            try (OutputStream os = process.getOutputStream()) {
                os.write(request.getBody().getBytes());
                os.flush();
            }
        } else {
            // Close the stream if there's no body so the script doesn't hang waiting
            process.getOutputStream().close();
        }

        // 5. Read the Script's Standard Output (stdout)
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream is = process.getInputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        // 6. Error Handling: Wait for the process and check the exit code
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            // Read standard error (stderr) for debugging
            try (InputStream es = process.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(es))) {
                String errorLine;
                while ((errorLine = reader.readLine()) != null) {
                    System.err.println("CGI Script Error: " + errorLine);
                }
            }
            throw new RuntimeException("500 Internal Server Error: Script failed with exit code " + exitCode);
        }

        return outputStream.toByteArray();
    }
}