package dev.cineindex.io;

import dev.cineindex.store.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import org.json.*;
import org.apache.commons.csv.*;

/** Startup-only import boundary. A failed import never publishes a partial catalogue. */
public final class CatalogLoader {
    private CatalogLoader() {}

    public static Stores demo() throws IOException {
        try (InputStream stream = CatalogLoader.class.getResourceAsStream("/demo/catalog.json")) {
            if (stream == null) throw new IOException("Bundled demo catalogue is missing.");
            return fromJson(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public static Stores json(Path path) throws IOException {
        return fromJson(Files.readString(path, StandardCharsets.UTF_8));
    }

    public static Stores fromJson(String source) {
        JSONObject root = new JSONObject(source);
        Stores stores = new Stores();
        JSONArray movies = root.getJSONArray("movies");
        for (int i = 0; i < movies.length(); i++) {
            JSONObject movie = movies.getJSONObject(i);
            int id = movie.getInt("id");
            String title = movie.getString("title").trim();
            if (id <= 0 || title.isEmpty()) throw new IllegalArgumentException("Movie IDs must be positive and titles non-empty.");
            JSONArray tags = movie.optJSONArray("genres");
            Genre[] genres = new Genre[tags == null ? 0 : tags.length()];
            for (int j = 0; j < genres.length; j++) {
                String genre = tags.getString(j);
                genres[j] = new Genre(genre.hashCode(), genre);
            }
            String date = movie.optString("releaseDate", "");
            boolean added = stores.getMovies().add(id, title, title, movie.optString("overview", ""),
                "", "Released", genres, date.isBlank() ? null : LocalDate.parse(date), 0, 0,
                new String[]{"en"}, "en", movie.optDouble("runtime", 0), "", false, false, "");
            if (!added) throw new IllegalArgumentException("Duplicate movie ID: " + id);
            addCredits(stores, id, movie.optJSONArray("cast"), movie.optJSONArray("crew"));
        }
        JSONArray ratings = root.optJSONArray("ratings");
        if (ratings != null) for (int i = 0; i < ratings.length(); i++) {
            JSONObject rating = ratings.getJSONObject(i);
            int movieId = rating.getInt("movieId");
            int userId = rating.getInt("userId");
            if (!stores.getMovies().contains(movieId)) throw new IllegalArgumentException("Rating references unknown movie: " + movieId);
            if (userId <= 0) throw new IllegalArgumentException("User IDs must be positive.");
            if (!stores.getRatings().add(userId, movieId, rating.getFloat("rating"),
                    LocalDateTime.parse(rating.getString("timestamp"))))
                throw new IllegalArgumentException("Duplicate rating for user " + userId + " and movie " + movieId);
        }
        return stores;
    }

    private static void addCredits(Stores stores, int movieId, JSONArray cast, JSONArray crew) {
        CastCredit[] actors = new CastCredit[cast == null ? 0 : cast.length()];
        for (int i = 0; i < actors.length; i++) {
            JSONObject person = cast.getJSONObject(i);
            actors[i] = new CastCredit(i, person.optString("character", ""), "", person.getInt("id"),
                person.getString("name"), person.optInt("order", i), "");
        }
        CrewCredit[] team = new CrewCredit[crew == null ? 0 : crew.length()];
        for (int i = 0; i < team.length; i++) {
            JSONObject person = crew.getJSONObject(i);
            team[i] = new CrewCredit("", person.optString("department", ""), person.getInt("id"),
                person.optString("job", ""), person.getString("name"), "");
        }
        stores.getCredits().add(actors, team, movieId);
    }

    private static CSVParser csv(Path file) throws IOException {
        return CSVParser.parse(file, StandardCharsets.UTF_8,
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get());
    }

    /** Import TMDB-style movie/credit CSVs and a ratings CSV containing tmdbId. */
    public static Stores csvDirectory(Path directory) throws IOException {
        Stores stores = new Stores();
        try (CSVParser parser = csv(directory.resolve("movies_metadata.csv"))) {
            for (CSVRecord row : parser) {
                try {
                    int id = Integer.parseInt(row.get("tmdb_id"));
                    JSONArray tags = new JSONArray(row.get("genres"));
                    Genre[] genres = new Genre[tags.length()];
                    for (int i = 0; i < genres.length; i++) {
                        JSONObject tag = tags.getJSONObject(i);
                        genres[i] = new Genre(tag.getInt("id"), tag.getString("name"));
                    }
                    String rawDate = row.get("release_date");
                    LocalDate date = rawDate.isBlank() ? null : parseDate(rawDate);
                    boolean added = stores.getMovies().add(id, row.get("title"), row.get("original_title"),
                        row.get("overview"), row.get("tagline"), row.get("status"), genres, date,
                        number(row.get("budget")), number(row.get("revenue")), new String[0],
                        row.get("original_language"), decimal(row.get("runtime")), "", false, false, "");
                    if (!added) throw new IllegalArgumentException("Duplicate movie ID " + id);
                } catch (RuntimeException error) { throw invalidRow("movies_metadata.csv", row, error); }
            }
        }
        try (CSVParser parser = csv(directory.resolve("credits.csv"))) {
            for (CSVRecord row : parser) {
                try {
                    int id = Integer.parseInt(row.get("tmdb_id"));
                    if (stores.getMovies().contains(id)) {
                        if (stores.getCredits().sizeOfCast(id) >= 0) throw new IllegalArgumentException("Duplicate credits for " + id);
                        addCredits(stores, id, new JSONArray(row.get("cast")), new JSONArray(row.get("crew")));
                    }
                } catch (RuntimeException error) { throw invalidRow("credits.csv", row, error); }
            }
        }
        try (CSVParser parser = csv(directory.resolve("ratings.csv"))) {
            for (CSVRecord row : parser) {
                try {
                    int id = Integer.parseInt(row.get("tmdbId"));
                    if (stores.getMovies().contains(id)) {
                        int user = Integer.parseInt(row.get("userId"));
                        if (user <= 0) throw new IllegalArgumentException("User IDs must be positive.");
                        if (!stores.getRatings().add(user, id, Float.parseFloat(row.get("rating")),
                            LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(row.get("timestamp"))), ZoneOffset.UTC)))
                            throw new IllegalArgumentException("Duplicate rating for user " + user + " and movie " + id);
                    }
                } catch (RuntimeException error) { throw invalidRow("ratings.csv", row, error); }
            }
        }
        return stores;
    }

    private static IOException invalidRow(String file, CSVRecord row, RuntimeException cause) {
        return new IOException(file + " record " + row.getRecordNumber() + ": " + cause.getMessage(), cause);
    }
    private static long number(String value) { return value.isBlank() ? 0 : Long.parseLong(value); }
    private static double decimal(String value) { return value.isBlank() ? 0 : Double.parseDouble(value); }
    private static LocalDate parseDate(String value) {
        if (value.matches("-?\\d+")) return Instant.ofEpochSecond(Long.parseLong(value)).atZone(ZoneOffset.UTC).toLocalDate();
        return LocalDate.parse(value);
    }
}
