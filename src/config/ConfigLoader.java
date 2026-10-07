package config;

import java.io.IOException;
import java.util.*;


public class ConfigLoader {
    private final List<ServerConfig> servers;

    public ConfigLoader() {
        this.servers = new ArrayList<>();
    }

    public static ConfigLoader fromFile(String filePath) throws IOException {
        ConfigLoader loader = new ConfigLoader();
        loader.load(filePath);
        try {
            loader.validate();
        } catch (Exception e) {
            throw new IOException("Configuration validation failed: " + e.getMessage(), e);
        }
        return loader;
    }

    @SuppressWarnings("unchecked")
    public void load(String filePath) throws IOException {
        SimpleJsonParser parser = SimpleJsonParser.fromFile(filePath);
        Object rootObj = parser.parse();

        if (!(rootObj instanceof Map)) {
            throw new IOException("Invalid JSON: root khasso y-koun JSON Object {...}");
        }

        Map<String, Object> root = (Map<String, Object>) rootObj;
        List<Object> serversList = (List<Object>) root.get("servers");

        if (serversList != null) {
            for (Object serverObj : serversList) {
                if (serverObj instanceof Map) {
                    Map<String, Object> serverMap = (Map<String, Object>) serverObj;
                    ServerConfig server = parseServerConfig(serverMap);
                    servers.add(server);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ServerConfig parseServerConfig(Map<String, Object> serverMap) {
        String host = (String) serverMap.getOrDefault("host", "0.0.0.0");
        List<Integer> ports = parsePortList((List<Object>) serverMap.get("ports"));
        String serverName = (String) serverMap.get("server_name");
        Boolean defaultServer = (Boolean) serverMap.getOrDefault("default_server", false);

        long clientMaxBodySize = SimpleJsonParser.parseSize((String) serverMap.get("client_max_body_size"));
        Number timeoutNum = (Number) serverMap.get("request_timeout_ms");
        int requestTimeout = timeoutNum != null ? timeoutNum.intValue() : 30000;

        Map<String, String> errorPages = parseErrorPages((Map<String, Object>) serverMap.get("error_pages"));
        List<RouteConfig> routes = parseRoutes((List<Object>) serverMap.get("routes"), clientMaxBodySize);

        return new ServerConfig(host, ports, serverName, defaultServer != null && defaultServer,
                clientMaxBodySize, requestTimeout, errorPages, routes);
    }

    private List<Integer> parsePortList(List<Object> portsList) {
        List<Integer> ports = new ArrayList<>();
        if (portsList != null) {
            for (Object portObj : portsList) {
                if (portObj instanceof Number) {
                    ports.add(((Number) portObj).intValue());
                }
            }
        }
        return ports;
    }

    private Map<String, String> parseErrorPages(Map<String, Object> errorPagesMap) {
        Map<String, String> errorPages = new HashMap<>();
        if (errorPagesMap != null) {
            for (Map.Entry<String, Object> entry : errorPagesMap.entrySet()) {
                if (entry.getValue() instanceof String) {
                    errorPages.put(entry.getKey(), (String) entry.getValue());
                }
            }
        }
        return errorPages;
    }

    @SuppressWarnings("unchecked")
    private List<RouteConfig> parseRoutes(List<Object> routesList, long defaultClientMaxBodySize) {
        List<RouteConfig> routes = new ArrayList<>();
        if (routesList != null) {
            for (Object routeObj : routesList) {
                if (routeObj instanceof Map) {
                    Map<String, Object> routeMap = (Map<String, Object>) routeObj;
                    RouteConfig route = parseRouteConfig(routeMap, defaultClientMaxBodySize);
                    routes.add(route);
                }
            }
        }
        return routes;
    }

    @SuppressWarnings("unchecked")
    private RouteConfig parseRouteConfig(Map<String, Object> routeMap, long defaultClientMaxBodySize) {
        String path = (String) routeMap.get("path");
        List<String> methods = parseMethods((List<Object>) routeMap.get("methods"));
        String root = (String) routeMap.get("root");
        String defaultFile = (String) routeMap.get("default_file");
        Boolean directoryListing = (Boolean) routeMap.getOrDefault("directory_listing", false);

        Object sizeObj = routeMap.get("client_max_body_size");
        long clientMaxBodySize = defaultClientMaxBodySize;
        if (sizeObj instanceof String) {
            clientMaxBodySize = SimpleJsonParser.parseSize((String) sizeObj);
        } else if (sizeObj instanceof Number) {
            clientMaxBodySize = ((Number) sizeObj).longValue();
        }

        Map<String, String> cgi = parseCgi((Map<String, Object>) routeMap.get("cgi"));
        Map<String, Object> redirect = (Map<String, Object>) routeMap.get("redirect");

        return new RouteConfig(path, methods, root, defaultFile,
                directoryListing != null && directoryListing, clientMaxBodySize, cgi, redirect);
    }

    private List<String> parseMethods(List<Object> methodsList) {
        List<String> methods = new ArrayList<>();
        if (methodsList != null) {
            for (Object methodObj : methodsList) {
                if (methodObj instanceof String) {
                    methods.add((String) methodObj);
                }
            }
        }
        return methods;
    }

    private Map<String, String> parseCgi(Map<String, Object> cgiMap) {
        Map<String, String> cgi = new HashMap<>();
        if (cgiMap != null) {
            for (Map.Entry<String, Object> entry : cgiMap.entrySet()) {
                if (entry.getValue() instanceof String) {
                    cgi.put(entry.getKey(), (String) entry.getValue());
                }
            }
        }
        return cgi;
    }

    public List<ServerConfig> getServers() {
        return servers;
    }

   
    public void validate() throws Exception {
        if (servers.isEmpty()) {
            throw new Exception("No servers configured in config.json");
        }

        List<ServerConfig> validServers = new ArrayList<>();
        Set<String> serverKeys = new HashSet<>();

        for (ServerConfig server : servers) {
            String serverName = server.getServerName() != null ? server.getServerName() : "unnamed-server";

            if (server.getHost() == null || server.getHost().trim().isEmpty()) {
                System.err.println("⚠️ CONFIG WARNING: Server '" + serverName + "' has empty host. Disabling this server.");
                continue;
            }

            if (server.getPorts() == null || server.getPorts().isEmpty()) {
                System.err.println("⚠️ CONFIG WARNING: Server '" + serverName + "' ports list is empty. Disabling this server.");
                continue;
            }

            boolean hasPortError = false;
            Set<Integer> uniquePortsInServer = new HashSet<>();
            Iterator<Integer> portIterator = server.getPorts().iterator();

            while (portIterator.hasNext()) {
                Integer port = portIterator.next();
                if (port <= 0 || port > 65535) {
                    System.err.println("⚠️ CONFIG WARNING: Server '" + serverName + "' has invalid port number " + port + ". Disabling this server.");
                    hasPortError = true;
                    break;
                }

                // Audit Requirement: Handle duplicate port in same block bla ma y-crachi
                if (!uniquePortsInServer.add(port)) {
                    System.err.println("⚠️ CONFIG WARNING: Port " + port + " duplicated in server '" + serverName + "'. Ignoring duplicate.");
                    portIterator.remove();
                    continue;
                }

                // Audit Requirement: Handle conflicts across blocks
                String hostPortNameKey = server.getHost() + ":" + port + ":" + (server.getServerName() != null ? server.getServerName().trim() : "");
                if (!serverKeys.add(hostPortNameKey)) {
                    System.err.println("⚠️ CONFIG WARNING: Server '" + serverName + "' conflict on port " + port + ". Ignoring conflict.");
                    portIterator.remove();
                }
            }

            if (hasPortError || server.getPorts().isEmpty()) {
                continue;
            }

            boolean routeError = false;
            if (server.getRoutes() != null) {
                Set<String> routePaths = new HashSet<>();
                for (RouteConfig route : server.getRoutes()) {
                    if (route.getPath() == null || route.getPath().trim().isEmpty()) {
                        System.err.println("⚠️ CONFIG WARNING: Server '" + serverName + "' has route with empty path. Disabling this server.");
                        routeError = true;
                        break;
                    }
                    if (!routePaths.add(route.getPath())) {
                        System.err.println("⚠️ CONFIG WARNING: Server '" + serverName + "' has duplicate route path '" + route.getPath() + "'. Disabling this server.");
                        routeError = true;
                        break;
                    }
                    if (route.getMethods() == null || route.getMethods().isEmpty()) {
                        System.err.println("⚠️ CONFIG WARNING: Server '" + serverName + "' route '" + route.getPath() + "' has no methods. Disabling this server.");
                        routeError = true;
                        break;
                    }
                }
            }

            if (routeError) {
                continue;
            }

            validServers.add(server);
        }

        if (validServers.isEmpty()) {
            throw new Exception("No valid servers remaining after configuration validation.");
        }

        this.servers.clear();
        this.servers.addAll(validServers);
    }

   
}

