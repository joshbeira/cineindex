package dev.cineindex;

import dev.cineindex.app.ApiServer;
import dev.cineindex.io.CatalogLoader;
import dev.cineindex.service.CatalogService;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.json.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ApiServerTest {
    private ApiServer server;
    private final HttpClient client = HttpClient.newHttpClient();
    @BeforeEach void start() throws Exception { server = new ApiServer(new CatalogService(CatalogLoader.demo(), "test"), 0); server.start(); }
    @AfterEach void stop() { server.close(); }
    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    @Test void servesPackagedDashboardAndJsonRoutes() throws Exception {
        for (String path : new String[]{"/", "/app.js", "/styles.css", "/favicon.svg", "/api/health", "/api/stats", "/api/movies", "/api/movies/1", "/api/cast/101"}) assertEquals(200, get(path).statusCode(), path);
        assertTrue(get("/").body().contains("CineIndex"));
        HttpResponse<String> response = get("/api/movies?q=observatory");
        assertEquals(1, new JSONObject(response.body()).getInt("total"));
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().contains("application/json"));
        assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElseThrow());
    }
    @ParameterizedTest @ValueSource(strings = {"?limit=0", "?limit=101", "?offset=-1", "?minRatings=-1", "?limit=no", "?sort=unknown", "?q=a&q=b"})
    void invalidQueriesReturnStructured400(String query) throws Exception {
        HttpResponse<String> response = get("/api/movies" + query);
        assertEquals(400, response.statusCode()); assertTrue(new JSONObject(response.body()).has("error"));
    }
    @Test void unknownRoutesAndMethodsHaveCorrectStatus() throws Exception {
        for (String path : new String[]{"/api/movies/99999", "/api/nope", "/missing", "/../build.gradle"}) assertEquals(404, get(path).statusCode());
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + "/api/movies")).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode()); assertEquals("GET", response.headers().firstValue("Allow").orElseThrow());
    }
    @Test void parallelReadsAreConsistent() throws Exception {
        var requests = new java.util.ArrayList<CompletableFuture<HttpResponse<String>>>();
        URI uri = URI.create("http://127.0.0.1:" + server.port() + "/api/movies?sort=popular");
        for (int i = 0; i < 24; i++) requests.add(client.sendAsync(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString()));
        for (var request : requests) {
            var response = request.get(10, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode()); assertEquals(24, new JSONObject(response.body()).getInt("total"));
        }
    }
}
