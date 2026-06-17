import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
  
public class ConfigLoaderTest {
    public static void main(String[] args) throws IOException {
        testValidConfigLoadsCorrectly();
        testMissingConfigFallsBackToDefault();
        testInvalidPortsAreIgnored();
        testEmptyConfigFallsBackToDefault();
        System.out.println("ConfigLoader tests passed.");
    }

    private static void testValidConfigLoadsCorrectly() throws IOException {
        Path tempFile = Files.createTempFile("config-test", ".json");
        try {
            Files.writeString(tempFile, "{\n  \"host\": \"127.0.0.1\",\n  \"ports\": [8080, 9090]\n}\n", StandardCharsets.UTF_8);

            ConfigLoader.Config config = ConfigLoader.load(tempFile.toString());
            assert "127.0.0.1".equals(config.getHost()) : "Expected host 127.0.0.1";
            List<Integer> ports = config.getPorts();
            assert ports.size() == 2 : "Expected 2 ports";
            assert ports.contains(8080) : "Expected port 8080";
            assert ports.contains(9090) : "Expected port 9090";
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void testMissingConfigFallsBackToDefault() {
        ConfigLoader.Config config = ConfigLoader.load("no-such-file.json");
        assert "127.0.0.1".equals(config.getHost()) : "Expected default host when config is missing";
        List<Integer> ports = config.getPorts();
        assert ports.size() == 1 && ports.get(0) == 8080 : "Expected default port 8080 when config is missing";
    }

    private static void testInvalidPortsAreIgnored() throws IOException {
        Path tempFile = Files.createTempFile("config-test", ".json");
        try {
            Files.writeString(tempFile, "{\n  \"host\": \"localhost\",\n  \"ports\": [8080, -1, 99999, \"abc\"]\n}\n", StandardCharsets.UTF_8);

            ConfigLoader.Config config = ConfigLoader.load(tempFile.toString());
            assert "localhost".equals(config.getHost()) : "Expected host localhost";
            List<Integer> ports = config.getPorts();
            assert ports.size() == 1 : "Expected only one valid port";
            assert ports.get(0) == 8080 : "Expected valid port 8080";
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void testEmptyConfigFallsBackToDefault() throws IOException {
        Path tempFile = Files.createTempFile("config-test", ".json");
        try {
            Files.writeString(tempFile, "\n", StandardCharsets.UTF_8);

            ConfigLoader.Config config = ConfigLoader.load(tempFile.toString());
            assert "127.0.0.1".equals(config.getHost()) : "Expected default host for empty config";
            List<Integer> ports = config.getPorts();
            assert ports.size() == 1 && ports.get(0) == 8080 : "Expected default port for empty config";
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
