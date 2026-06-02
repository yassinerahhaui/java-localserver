package test.http;

import http.HttpParser;
import http.HttpRequest;

public class HttpParserTest {

    public static void main(String[] args) {
        System.out.println("🚀 Starting Day 2 Body Parsing Tests...\n");
        testContentLengthBody();
        testChunkedBody();
    }

    private static void testContentLengthBody() {
        System.out.println("--- Testing Content-Length Body ---");
        HttpParser parser = new HttpParser();
        
        // JSON Body with exactly 37 characters length
        String raw = 
            """
            POST /api/login HTTP/1.1\r
            Content-Length: 37\r
            Content-Type: application/json\r
            \r
            {"user":"yassine", "password":"1234"}"""; 

        try {
            HttpRequest req = parser.parse(raw);
            System.out.println(req.toString());
            System.out.println("✅ Passed: Body parsed correctly!\n");
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Failed: " + e.getMessage());
        }
    }

    private static void testChunkedBody() {
        System.out.println("--- Testing Chunked Transfer Encoding ---");
        HttpParser parser = new HttpParser();
        
        // Chunk 1: Size 5 (Hex 5) -> "Hello"
        // Chunk 2: Size 6 (Hex 6) -> " World"
        // Chunk 3: Size 0 (Hex 0) -> End
        String raw = 
            """
            POST /api/upload HTTP/1.1\r
            Transfer-Encoding: chunked\r
            \r
            5\r
            Hello\r
            6\r
             World\r
            0\r
            \r
            """;

        try {
            HttpRequest req = parser.parse(raw);
            System.out.println(req.toString());
            if (req.getBody().equals("Hello World")) {
                System.out.println("✅ Passed: Chunks combined properly!\n");
            } else {
                System.out.println("❌ Failed: Combined body is incorrect: " + req.getBody());
            }
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Failed: " + e.getMessage());
        }
    }
}