package test.core;

import core.CGIHandler;
import http.HttpMethod;
import http.HttpRequest;

import java.io.File;

public class CGIHandlerTest {

    public static void main(String[] args) {
        System.out.println("🚀 Starting CGIHandler Tests...\n");

        CGIHandler cgiHandler = new CGIHandler();
        
        // Ensure you have script.py inside the wwwroot folder
        File scriptFile = new File("wwwroot/script.py");

        testValidCgiExecution(cgiHandler, scriptFile);
        testCgiErrorHandling(cgiHandler);
    }

    private static void testValidCgiExecution(CGIHandler cgiHandler, File scriptFile) {
        System.out.println("--- 1. Testing Valid CGI Execution ---");
        try {
            // Mocking an HTTP Request
            HttpRequest req = new HttpRequest();
            req.setMethod(HttpMethod.POST);
            req.setUri("/script.py");
            req.setVersion("HTTP/1.1");
            req.addHeader("User-Agent", "Java-Test-Client/1.0");
            req.setBody("name=Yassine&role=Developer");

            // Execute
            byte[] resultBytes = cgiHandler.executeCGI(scriptFile, req);
            String output = new String(resultBytes);

            System.out.println("✅ Passed: Output received from Python script:\n");
            System.out.println("-------------------------------------------------");
            System.out.println(output);
            System.out.println("-------------------------------------------------\n");

        } catch (Exception e) {
            System.out.println("❌ Failed: " + e.getMessage());
        }
    }

    private static void testCgiErrorHandling(CGIHandler cgiHandler) {
        System.out.println("--- 2. Testing Invalid Input (Error Handling) ---");
        try {
            HttpRequest req = new HttpRequest();
            req.setMethod(HttpMethod.GET);
            req.setUri("/fake-script.py");

            // Passing a non-existent file to test crash prevention
            cgiHandler.executeCGI(new File("wwwroot/fake-script.py"), req);
            
            System.out.println("❌ Failed: Should have thrown an exception.");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Passed: Handled missing file gracefully -> " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Failed with unexpected error: " + e.getMessage());
        }
    }
}