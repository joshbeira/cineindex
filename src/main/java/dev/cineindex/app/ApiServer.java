package dev.cineindex.app;

import com.sun.net.httpserver.*;
import dev.cineindex.service.CatalogService;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Local, read-only HTTP adapter. Store mutation ends before start(). */
public final class ApiServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final CatalogService catalogue;

    public ApiServer(CatalogService catalogue, int port) throws IOException {
        this.catalogue = catalogue;
        server = HttpServer.create(new InetSocketAddress(System.getenv().getOrDefault("HOST", "127.0.0.1"), port), 32);
        server.setExecutor(executor);
        server.createContext("/", this::handle);
    }

    public void start() { server.start(); }
    public int port() { return server.getAddress().getPort(); }
    public void close() { server.stop(0); executor.shutdownNow(); }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'");
            exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.getResponseHeaders().set("Allow", "GET");
                json(exchange, 405, error("Only GET requests are supported."));
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
                JSONObject result;
                if (path.equals("/api/health")) result = new JSONObject().put("status", "ok").put("version", "1.0.0");
                else if (path.equals("/api/stats")) result = catalogue.stats();
                else if (path.equals("/api/movies")) result = catalogue.search(query.getOrDefault("q", ""), query.getOrDefault("genre", ""),
                    query.getOrDefault("sort", "rating"), integer(query, "minRatings", 0), integer(query, "offset", 0), integer(query, "limit", 12));
                else if (path.matches("/api/movies/[0-9]+")) result = catalogue.detail(Integer.parseInt(path.substring(12)));
                else if (path.matches("/api/cast/[0-9]+")) result = catalogue.cast(Integer.parseInt(path.substring(10)));
                else result = null;
                json(exchange, result == null ? 404 : 200, result == null ? error("Resource not found.") : result);
                return;
            }
            String resource = switch (path) {
                case "/", "/index.html" -> "index.html";
                case "/app.js" -> "app.js";
                case "/styles.css" -> "styles.css";
                case "/favicon.svg" -> "favicon.svg";
                default -> null;
            };
            if (resource == null) { json(exchange, 404, error("Resource not found.")); return; }
            try (InputStream stream = ApiServer.class.getResourceAsStream("/web/" + resource)) {
                if (stream == null) throw new IOException("Missing web resource: " + resource);
                String type = resource.endsWith(".css") ? "text/css" : resource.endsWith(".js") ? "text/javascript" : resource.endsWith(".svg") ? "image/svg+xml" : "text/html";
                send(exchange, 200, type + "; charset=utf-8", stream.readAllBytes());
            }
        } catch (IllegalArgumentException exception) {
            json(exchange, 400, error(exception.getMessage()));
        } catch (Exception exception) {
            System.err.println("Request failed: " + exception.getClass().getSimpleName());
            json(exchange, 500, error("Unable to complete this request."));
        } finally { exchange.close(); }
    }

    private static JSONObject error(String message) { return new JSONObject().put("error", message); }
    private static int integer(Map<String, String> query, String key, int fallback) {
        try { return query.containsKey(key) ? Integer.parseInt(query.get(key)) : fallback; }
        catch (NumberFormatException exception) { throw new IllegalArgumentException(key + " must be an integer."); }
    }
    private static Map<String, String> query(String raw) {
        Map<String, String> values = new HashMap<>();
        if (raw == null || raw.isEmpty()) return values;
        if (raw.length() > 4096) throw new IllegalArgumentException("Query is too long.");
        for (String part : raw.split("&")) {
            String[] pair = part.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            if (values.putIfAbsent(key, value) != null) throw new IllegalArgumentException("Duplicate query parameter: " + key);
        }
        return values;
    }
    private static void json(HttpExchange exchange, int status, JSONObject body) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", body.toString().getBytes(StandardCharsets.UTF_8));
    }
    private static void send(HttpExchange exchange, int status, String type, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
