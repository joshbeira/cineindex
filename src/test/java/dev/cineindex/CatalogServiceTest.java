package dev.cineindex;

import dev.cineindex.io.CatalogLoader;
import dev.cineindex.service.CatalogService;
import org.json.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CatalogServiceTest {
    @Test void demoCountsSearchFiltersAndPagination() throws Exception {
        CatalogService service = new CatalogService(CatalogLoader.demo(), "test");
        assertEquals(24, service.stats().getInt("movies"));
        assertEquals(813, service.stats().getInt("ratings"));
        JSONObject search = service.search("observatory", "Science Fiction", "rating", 0, 0, 12);
        assertEquals(1, search.getInt("total"));
        assertEquals("The Last Observatory", search.getJSONArray("items").getJSONObject(0).getString("title"));
        assertEquals(0, service.search("observatory", "Comedy", "rating", 0, 0, 12).getInt("total"));
        assertEquals(0, service.search("", "", "rating", 10000, 0, 12).getInt("total"));
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (int offset = 0; offset < 24; offset += 5) for (Object item : service.search("", "", "rating", 0, offset, 5).getJSONArray("items"))
            assertTrue(ids.add(((JSONObject) item).getInt("id")));
        assertEquals(24, ids.size());
        assertEquals(0, service.search("", "", "rating", 0, Integer.MAX_VALUE, 10).getJSONArray("items").length());
    }

    @Test void detailHistogramAndRecommendationsExplainTheirScores() throws Exception {
        CatalogService service = new CatalogService(CatalogLoader.demo(), "test");
        JSONObject movie = service.detail(1);
        int count = 0;
        for (Object bin : movie.getJSONArray("ratingHistogram")) count += ((Number) bin).intValue();
        assertEquals(movie.getInt("ratingCount"), count);
        for (Object item : movie.getJSONArray("recommendations")) {
            JSONObject similar = (JSONObject) item;
            assertNotEquals(1, similar.getInt("id"));
            int score = 0;
            for (Object reason : similar.getJSONArray("reasons")) score += reason.toString().startsWith("Shared genre") ? 2 : 1;
            assertEquals(score, similar.getInt("similarityScore"));
        }
        assertNull(service.detail(999)); assertNull(service.cast(999));
        assertTrue(service.cast(101).getJSONArray("films").length() > 0);
    }

    @Test void tiesHaveStableIdOrder() {
        CatalogService service = new CatalogService(CatalogLoader.fromJson("{\"movies\":[{\"id\":3,\"title\":\"C\"},{\"id\":1,\"title\":\"A\"},{\"id\":2,\"title\":\"B\"}]}"), "test");
        JSONArray result = service.search("", "", "rating", 0, 0, 10).getJSONArray("items");
        for (int i = 0; i < 3; i++) assertEquals(i + 1, result.getJSONObject(i).getInt("id"));
    }

    @Test void malformedImportsFailBeforePublishingACatalogue() {
        assertThrows(IllegalArgumentException.class, () -> CatalogLoader.fromJson("{\"movies\":[{\"id\":1,\"title\":\"A\"},{\"id\":1,\"title\":\"B\"}]}"));
        assertThrows(IllegalArgumentException.class, () -> CatalogLoader.fromJson("{\"movies\":[],\"ratings\":[{\"userId\":1,\"movieId\":7,\"rating\":4,\"timestamp\":\"2026-01-01T00:00:00\"}]}"));
    }
}
