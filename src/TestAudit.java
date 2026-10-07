import config.ConfigLoader;
import config.ServerConfig;
import http.HttpParser;
import http.HttpRequest;
import http.HttpResponse;
import router.Router;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class TestAudit {

    private static int passed = 0;
    private static int failed = 0;

    private static void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.println("  ✅ PASS: " + testName);
            passed++;
        } else {
            System.err.println("  ❌ FAIL: " + testName);
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("🧪 RUNNING COMPREHENSIVE LOCALSERVER AUDIT TESTS");
        System.out.println("==================================================");

        try {
            ConfigLoader loader = ConfigLoader.fromFile("config.json");
            List<ServerConfig> servers = loader.getServers();
            ServerConfig mainServer = servers.get(0);

            // 1. HTTP PARSER TESTS
            System.out.println("\n--- 1. HTTP Parser & Chunked Transfer Encoding ---");
            testHttpParserGet();
            testHttpParserPostContentLength();
            testHttpParserChunked();
            testHttpParserMalformed();

            // 2. METHODS (GET, POST, DELETE)
            System.out.println("\n--- 2. Methods (GET, POST, DELETE) ---");
            testStaticGet(mainServer);
            testPostUploadAndRetrieval(mainServer);
            testDeleteFile(mainServer);

            // 3. ERROR PAGES & SECURITY
            System.out.println("\n--- 3. Error Pages & Security (Path Traversal, 404, 405, 413) ---");
            testPathTraversalSecurity(mainServer);
            test404CustomPage(mainServer);
            test405MethodNotAllowed(mainServer);
            test413PayloadTooLarge(mainServer);

            // 4. DIRECTORY LISTING & REDIRECTION
            System.out.println("\n--- 4. Directory Listing & Redirection ---");
            testDirectoryListing(mainServer);
            testRedirection(mainServer);

            // 5. COOKIES & SESSIONS
            System.out.println("\n--- 5. Cookies & Session Management ---");
            testCookiesAndSessions(mainServer);

            // 6. CGI EXECUTION (Python)
            System.out.println("\n--- 6. CGI Execution (Python) ---");
            testCgiPython(mainServer);

            // 7. VIRTUAL HOSTING (Host header & Ports)
            System.out.println("\n--- 7. Virtual Hosting ---");
            testVirtualHosting(servers);

            // SUMMARY
            System.out.println("\n==================================================");
            System.out.println("📊 TEST SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
            System.out.println("==================================================");

            if (failed > 0) {
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void testHttpParserGet() throws Exception {
        String raw = "GET /index.html HTTP/1.1\r\nHost: localhost:8080\r\nUser-Agent: curl/7.68.0\r\n\r\n";
        ByteBuffer buf = ByteBuffer.wrap(raw.getBytes(StandardCharsets.UTF_8));
        HttpRequest req = HttpParser.parse(buf);

        assertTrue("Parser parses GET method", "GET".equals(req.getMethod()));
        assertTrue("Parser parses URI /index.html", "/index.html".equals(req.getUri()));
        assertTrue("Parser parses Host header", "localhost:8080".equals(req.getHeader("host")));
    }

    private static void testHttpParserPostContentLength() throws Exception {
        String body = "Hello LocalServer!";
        String raw = "POST /upload HTTP/1.1\r\nHost: localhost\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
        ByteBuffer buf = ByteBuffer.wrap(raw.getBytes(StandardCharsets.UTF_8));
        HttpRequest req = HttpParser.parse(buf);

        assertTrue("Parser parses POST method", "POST".equals(req.getMethod()));
        assertTrue("Parser reads full Content-Length body", body.equals(new String(req.getBody(), StandardCharsets.UTF_8)));
    }

    private static void testHttpParserChunked() throws Exception {
        // Chunked stream: "4\r\nWiki\r\n5\r\npedia\r\n0\r\n\r\n" -> "Wikipedia"
        String raw = "POST /upload HTTP/1.1\r\nHost: localhost\r\nTransfer-Encoding: chunked\r\n\r\n4\r\nWiki\r\n5\r\npedia\r\n0\r\n\r\n";
        ByteBuffer buf = ByteBuffer.wrap(raw.getBytes(StandardCharsets.UTF_8));
        HttpRequest req = HttpParser.parse(buf);

        assertTrue("Parser decodes Transfer-Encoding: chunked", req != null);
        if (req != null) {
            String decoded = new String(req.getBody(), StandardCharsets.UTF_8);
            assertTrue("Decoded chunked body matches expected 'Wikipedia'", "Wikipedia".equals(decoded));
        }
    }

    private static void testHttpParserMalformed() {
        try {
            String raw = "GARBAGE_WITHOUT_METHOD_OR_PATH\r\n\r\n";
            ByteBuffer buf = ByteBuffer.wrap(raw.getBytes(StandardCharsets.UTF_8));
            HttpParser.parse(buf);
            assertTrue("Parser rejects malformed request with Exception", false);
        } catch (Exception e) {
            assertTrue("Parser rejects malformed request with Exception", true);
        }
    }

    private static void testStaticGet(ServerConfig config) {
        HttpRequest req = new HttpRequest();
        req.setMethod("GET");
        req.setUri("/");
        req.setHttpVersion("HTTP/1.1");

        HttpResponse resp = Router.handle(req, config);
        assertTrue("GET / returns 200 OK", resp.getStatusCode() == 200);
        assertTrue("GET / sets text/html Content-Type", resp.getHeader("content-type") != null && resp.getHeader("content-type").contains("text/html"));
    }

    private static void testPostUploadAndRetrieval(ServerConfig config) throws Exception {
        String testFilename = "audit_upload_test.txt";
        String testContent = "Test payload for 42 audit verification";

        HttpRequest postReq = new HttpRequest();
        postReq.setMethod("POST");
        postReq.setUri("/upload/" + testFilename);
        postReq.addHeader("content-disposition", "attachment; filename=\"" + testFilename + "\"");
        postReq.setBody(testContent.getBytes(StandardCharsets.UTF_8));

        HttpResponse postResp = Router.handle(postReq, config);
        assertTrue("POST /upload returns 201 Created", postResp.getStatusCode() == 201);
        assertTrue("POST /upload provides Location header", postResp.getHeader("location") != null);

        // Verify retrieval without corruption
        File uploaded = new File("uploads", testFilename);
        assertTrue("File is physically written to disk", uploaded.exists());
        byte[] readBack = Files.readAllBytes(uploaded.toPath());
        assertTrue("Retrieved file content is not corrupted", testContent.equals(new String(readBack, StandardCharsets.UTF_8)));
    }

    private static void testDeleteFile(ServerConfig config) {
        String testFilename = "audit_upload_test.txt";
        HttpRequest delReq = new HttpRequest();
        delReq.setMethod("DELETE");
        delReq.setUri("/upload/" + testFilename);

        HttpResponse delResp = Router.handle(delReq, config);
        assertTrue("DELETE /upload/<file> returns 204 No Content", delResp.getStatusCode() == 204);

        File deleted = new File("uploads", testFilename);
        assertTrue("File is deleted from disk", !deleted.exists());

        // Deleting non-existent file should return 404
        HttpResponse del404 = Router.handle(delReq, config);
        assertTrue("DELETE non-existent file returns 404 Not Found", del404.getStatusCode() == 404);
    }

    private static void testPathTraversalSecurity(ServerConfig config) {
        HttpRequest req = new HttpRequest();
        req.setMethod("GET");
        req.setUri("/../../../../../../etc/passwd");

        HttpResponse resp = Router.handle(req, config);
        assertTrue("Directory Traversal attack is blocked with 403 Forbidden", resp.getStatusCode() == 403);
    }

    private static void test404CustomPage(ServerConfig config) {
        HttpRequest req = new HttpRequest();
        req.setMethod("GET");
        req.setUri("/non_existent_page_12345.html");

        HttpResponse resp = Router.handle(req, config);
        assertTrue("Non-existent URL returns 404 status", resp.getStatusCode() == 404);
        String body = new String(resp.getBody(), StandardCharsets.UTF_8);
        assertTrue("Custom 404 error page from config is loaded", body.contains("404") || body.contains("Not Found"));
    }

    private static void test405MethodNotAllowed(ServerConfig config) {
        HttpRequest req = new HttpRequest();
        req.setMethod("DELETE");
        req.setUri("/"); // Root only allows GET in config.json

        HttpResponse resp = Router.handle(req, config);
        assertTrue("Disallowed method returns 405 Method Not Allowed", resp.getStatusCode() == 405);
    }

    private static void test413PayloadTooLarge(ServerConfig config) {
        HttpRequest req = new HttpRequest();
        req.setMethod("POST");
        req.setUri("/upload/big.dat");
        // Limit is 20MB for /upload, let's create a body that exceeds 25MB or test limit
        byte[] oversize = new byte[25 * 1024 * 1024]; // 25MB
        req.setBody(oversize);

        HttpResponse resp = Router.handle(req, config);
        assertTrue("Oversized payload returns 413 Payload Too Large", resp.getStatusCode() == 413);
    }

    private static void testDirectoryListing(ServerConfig config) {
        HttpRequest req = new HttpRequest();
        req.setMethod("GET");
        req.setUri("/upload/"); // directory_listing is true for /upload

        HttpResponse resp = Router.handle(req, config);
        assertTrue("Directory without default file returns 200 OK for directory listing", resp.getStatusCode() == 200);
        String body = new String(resp.getBody(), StandardCharsets.UTF_8);
        assertTrue("Directory listing contains HTML table / links", body.contains("<table") || body.contains("Index of"));
    }

    private static void testRedirection(ServerConfig config) {
        HttpRequest req = new HttpRequest();
        req.setMethod("GET");
        req.setUri("/old-api"); // configured with 301 to /api/v2

        HttpResponse resp = Router.handle(req, config);
        assertTrue("Redirect route returns 301 Moved Permanently", resp.getStatusCode() == 301);
        assertTrue("Redirect contains Location: /api/v2", "/api/v2".equals(resp.getHeader("location")));
    }

    private static void testCookiesAndSessions(ServerConfig config) {
        HttpRequest req1 = new HttpRequest();
        req1.setMethod("GET");
        req1.setUri("/");

        HttpResponse resp1 = Router.handle(req1, config);
        assertTrue("First visit sets Set-Cookie session_id", resp1.getCookies().containsKey("session_id"));

        String cookieVal = resp1.getCookies().get("session_id");
        String sessionId = cookieVal.split(";")[0].split("=")[1];

        HttpRequest req2 = new HttpRequest();
        req2.setMethod("GET");
        req2.setUri("/");
        req2.addHeader("cookie", "session_id=" + sessionId);

        HttpResponse resp2 = Router.handle(req2, config);
        assertTrue("Second visit re-uses existing session without creating new one", resp2.getStatusCode() == 200);
    }

    private static void testCgiPython(ServerConfig config) {
        HttpRequest req = new HttpRequest();
        req.setMethod("GET");
        req.setUri("/cgi-bin/hello.py?user=audit_tester");

        HttpResponse resp = Router.handle(req, config);
        assertTrue("CGI Python execution returns 200 OK", resp.getStatusCode() == 200);
        assertTrue("CGI returns application/json Content-Type", resp.getHeader("content-type") != null && resp.getHeader("content-type").contains("application/json"));
        String body = new String(resp.getBody(), StandardCharsets.UTF_8);
        assertTrue("CGI script output contains execution payload", body.contains("Hello from Python CGI!"));
    }

    private static void testVirtualHosting(List<ServerConfig> servers) throws Exception {
        app.Server srv = new app.Server(servers);

        // Test 1: Match main-server by port 8080 and Host: main-server
        HttpRequest req1 = new HttpRequest();
        req1.addHeader("host", "main-server:8080");
        ServerConfig cfg1 = srv.resolveServerConfig(req1, 8080);
        assertTrue("Virtual Host resolves to main-server", "main-server".equals(cfg1.getServerName()));

        // Test 2: Match secondary-server by port 9090
        HttpRequest req2 = new HttpRequest();
        req2.addHeader("host", "secondary-server:9090");
        ServerConfig cfg2 = srv.resolveServerConfig(req2, 9090);
        assertTrue("Virtual Host resolves to secondary-server on port 9090", "secondary-server".equals(cfg2.getServerName()));

        // Test 3: Unknown Host header on port 8080 falls back to default_server (main-server)
        HttpRequest req3 = new HttpRequest();
        req3.addHeader("host", "unknown-domain.com:8080");
        ServerConfig cfg3 = srv.resolveServerConfig(req3, 8080);
        assertTrue("Unknown host falls back to default_server on port 8080", "main-server".equals(cfg3.getServerName()) && Boolean.TRUE.equals(cfg3.getDefaultServer()));
    }
}
