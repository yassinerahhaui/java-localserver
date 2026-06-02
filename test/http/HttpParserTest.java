package test.http;

import http.HttpParser;
import http.HttpRequest;
import http.HttpMethod;

public class HttpParserTest {

    public static void main(String[] args) {
        System.out.println("🚀 Starting HttpParser Tests...\n");
        
        testValidGetRequest();
        testMissingRequestLine();
        testUnsupportedMethod();
        
        System.out.println("\n✅ All basic tests completed.");
    }

    // ==========================================
    // Test 1: Happy Path
    // ==========================================
    private static void testValidGetRequest() {
        System.out.println("Running testValidGetRequest...");
        HttpParser parser = new HttpParser();
        String raw = "GET /index.html HTTP/1.1\r\nHost: localhost\r\n\r\n";
        
        try {
            HttpRequest req = parser.parser(raw);
            // كنتأكدو بلي داكشي لي قرا البارسر هو داكشي لي كنتسناو
            if (req.getMethod() == HttpMethod.GET && req.getUri().equals("/index.html")) {
                System.out.println("  [PASS] Request parsed correctly.");
            } else {
                System.out.println("  [FAIL] Parsed data is incorrect.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("  [FAIL] Threw unexpected exception: " + e.getMessage());
        }
    }

    // ==========================================
    // Test 2: Error Handling (Empty Request)
    // ==========================================
    private static void testMissingRequestLine() {
        System.out.println("Running testMissingRequestLine...");
        HttpParser parser = new HttpParser();
        String raw = "\r\n\r\n"; // طلب خاوي
        
        try {
            parser.parser(raw);
            System.out.println("  [FAIL] Should have thrown an exception, but didn't.");
        } catch (IllegalArgumentException e) {
            System.out.println("  [PASS] Caught expected exception: " + e.getMessage());
        }
    }

    // ==========================================
    // Test 3: Error Handling (Bad Method)
    // ==========================================
    private static void testUnsupportedMethod() {
        System.out.println("Running testUnsupportedMethod...");
        HttpParser parser = new HttpParser();
        String raw = "BOMBA /api HTTP/1.1\r\nHost: localhost\r\n\r\n"; // ميتود ماكايناش
        
        try {
            parser.parser(raw);
            System.out.println("  [FAIL] Should have thrown an exception for unknown method.");
        } catch (IllegalArgumentException e) {
            System.out.println("  [PASS] Caught expected exception: " + e.getMessage());
        }
    }
}