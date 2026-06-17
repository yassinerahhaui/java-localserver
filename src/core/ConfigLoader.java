import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigLoader {
    public static final class Config {
        private final String host;
        private final List<Integer> ports;

        public Config(String host, List<Integer> ports) {
            this.host = host;
            this.ports = ports;
        }

        public String getHost() {
            return host;
        }

        public List<Integer> getPorts() {
            return ports;
        }
    }

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final List<Integer> DEFAULT_PORTS = Collections.singletonList(8080);
    private static final Pattern HOST_PATTERN = Pattern.compile("\\\"host\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern PORTS_PATTERN = Pattern.compile("\\\"ports\\\"\\s*:\\s*\\[([^\\]]*)\\]");

    private ConfigLoader() {
        throw new AssertionError("ConfigLoader is a utility class");
    }

    public static Config load(String configPath) {
        try {
            Path path = Path.of(configPath);
            if (Files.notExists(path)) {
                System.err.println("Config file not found: " + configPath + ". Falling back to default configuration.");
                return defaultConfig();
            }

            String raw = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
            if (raw.isEmpty()) {
                System.err.println("Config file is empty. Falling back to default configuration.");
                return defaultConfig();
            }

            String host = parseHost(raw);
            List<Integer> ports = parsePorts(raw);
            if (ports.isEmpty()) {
                System.err.println("No valid ports found in config. Falling back to default port 8080.");
                ports = DEFAULT_PORTS;
            }

            return new Config(host, ports);
        } catch (IOException e) {
            System.err.println("Unable to read config file: " + e.getMessage() + ". Using default configuration.");
            return defaultConfig();
        } catch (RuntimeException e) {
            System.err.println("Config file is invalid: " + e.getMessage() + ". Using default configuration.");
            return defaultConfig();
        }
    }

    private static Config defaultConfig() {
        return new Config(DEFAULT_HOST, DEFAULT_PORTS);
    }

    private static String parseHost(String json) {
        Matcher matcher = HOST_PATTERN.matcher(json);
        if (matcher.find()) {
            String host = matcher.group(1).trim();
            if (!host.isEmpty()) {
                return host;
            }
        }
        return DEFAULT_HOST;
    }

    private static List<Integer> parsePorts(String json) {
        Matcher matcher = PORTS_PATTERN.matcher(json);
        if (!matcher.find()) {
            return Collections.emptyList();
        }

        String listContent = matcher.group(1);
        String[] values = listContent.split(",");
        List<Integer> result = new ArrayList<>();
        for (String value : values) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            try {
                int port = Integer.parseInt(trimmed);
                if (port >= 1 && port <= 65535) {
                    result.add(port);
                } else {
                    System.err.println("Ignoring invalid port number: " + port);
                }
            } catch (NumberFormatException ignored) {
                System.err.println("Ignoring invalid port value: " + trimmed);
            }
        }
        return result;
    }
}
