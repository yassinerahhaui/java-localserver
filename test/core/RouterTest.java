package test.core;

import core.Router;
import http.HttpMethod;
import http.HttpRequest;
import utils.RouteResult;

public class RouterTest {

    public static void main(String[] args) {
        System.out.println("🚀 Starting Router Logic Tests...\n");
        
        // This targets the wwwroot folder in your project root
        String documentRoot = "wwwroot"; 
        Router router = new Router(documentRoot);

        testValidStaticFile(router);
        testFileNotFound(router);
        testDirectoryTraversalSecurity(router);
        testCgiRouting(router);
    }

    private static void testValidStaticFile(Router router) {
        System.out.println("--- Testing Valid Static File (/) ---");
        HttpRequest req = new HttpRequest();
        req.setMethod(HttpMethod.GET);
        req.setUri("/"); // Should default to /index.html

        RouteResult result = router.route(req);
        System.out.println(result);
        if (result.getStatusCode() == 200 && !result.isCgi()) {
            System.out.println("✅ Passed: Static file routed correctly!\n");
        } else {
            System.out.println("❌ Failed!\n");
        }
    }

    private static void testFileNotFound(Router router) {
        System.out.println("--- Testing 404 Not Found ---");
        HttpRequest req = new HttpRequest();
        req.setMethod(HttpMethod.GET);
        req.setUri("/does-not-exist.html");

        RouteResult result = router.route(req);
        System.out.println(result);
        if (result.getStatusCode() == 404) {
            System.out.println("✅ Passed: Missing file identified as 404!\n");
        } else {
            System.out.println("❌ Failed!\n");
        }
    }

    private static void testDirectoryTraversalSecurity(Router router) {
        System.out.println("--- Testing 403 Forbidden (Security) ---");
        HttpRequest req = new HttpRequest();
        req.setMethod(HttpMethod.GET);
        req.setUri("/../../windows/system32/cmd.exe"); // Hacking attempt

        RouteResult result = router.route(req);
        System.out.println(result);
        if (result.getStatusCode() == 403) {
            System.out.println("✅ Passed: Path traversal blocked with 403!\n");
        } else {
            System.out.println("❌ Failed: Security breach!\n");
        }
    }

    private static void testCgiRouting(Router router) {
        System.out.println("--- Testing CGI Routing ---");
        HttpRequest req = new HttpRequest();
        req.setMethod(HttpMethod.POST);
        req.setUri("/script.py");

        RouteResult result = router.route(req);
        System.out.println(result);
        if (result.getStatusCode() == 200 && result.isCgi()) {
            System.out.println("✅ Passed: CGI script flagged correctly!\n");
        } else {
            System.out.println("❌ Failed! Did you create script.py in wwwroot?\n");
        }
    }
}