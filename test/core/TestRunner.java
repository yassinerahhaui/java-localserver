public class TestRunner {
    public static void main(String[] args) throws Exception {
        ConfigLoaderTest.main(args);
        ServerIntegrationTest.main(args);
        System.out.println("All tests passed.");
    }
}
  