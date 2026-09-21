package dev.cineindex;

import dev.cineindex.io.CatalogLoader;
import java.nio.file.*;
import java.io.*;
import org.apache.commons.csv.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CsvImportTest {
    @TempDir Path directory;

    private void fixture() throws Exception {
        try (var writer = Files.newBufferedWriter(directory.resolve("movies_metadata.csv"));
             var csv = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            csv.printRecord("tmdb_id", "title", "original_title", "overview", "tagline", "status", "genres", "release_date", "budget", "revenue", "original_language", "runtime");
            csv.printRecord(1, "A, B & C", "A, B & C", "A story\nwith a second line", "", "Released", "[{\"id\":1,\"name\":\"Drama\"}]", "2024-01-01", 0, 0, "en", 95);
        }
        try (var writer = Files.newBufferedWriter(directory.resolve("credits.csv"));
             var csv = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            csv.printRecord("tmdb_id", "cast", "crew");
            csv.printRecord(1, "[{\"id\":7,\"name\":\"Actor\",\"character\":\"Lead\"}]", "[]");
        }
        Files.writeString(directory.resolve("ratings.csv"), "userId,tmdbId,rating,timestamp\n1,1,4.5,1704067200\n2,999,3,1704067200\n");
    }

    @Test void quotedFieldsEmbeddedJsonAndForeignRatingsAreHandled() throws Exception {
        fixture();
        var stores = CatalogLoader.csvDirectory(directory);
        assertEquals("A, B & C", stores.getMovies().getTitle(1));
        assertTrue(stores.getMovies().getOverview(1).contains("\n"));
        assertEquals(1, stores.getRatings().size());
        assertEquals(4.5f, stores.getRatings().getMovieAverageRating(1));
        assertEquals("Actor", stores.getCredits().getCast(7).getName());
    }

    @Test void malformedRowsReportFileAndRecord() throws Exception {
        fixture();
        Files.writeString(directory.resolve("ratings.csv"), "userId,tmdbId,rating,timestamp\n1,1,9,1704067200\n");
        IOException exception = assertThrows(IOException.class, () -> CatalogLoader.csvDirectory(directory));
        assertTrue(exception.getMessage().contains("ratings.csv record 1"));
    }

    @Test void duplicateRatingsAreRejected() throws Exception {
        fixture();
        Files.writeString(directory.resolve("ratings.csv"), "userId,tmdbId,rating,timestamp\n1,1,4,1704067200\n1,1,3,1704067200\n");
        assertThrows(IOException.class, () -> CatalogLoader.csvDirectory(directory));
    }
}
