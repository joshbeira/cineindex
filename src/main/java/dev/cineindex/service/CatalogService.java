package dev.cineindex.service;

import dev.cineindex.store.*;
import dev.cineindex.structure.*;
import java.util.Locale;
import org.json.*;

/** Read-only queries over a catalogue fully loaded before the HTTP server starts. */
public final class CatalogService {
    private final Stores stores;
    private final String source;
    private final long loadedAt = System.currentTimeMillis();

    public CatalogService(Stores stores, String source) { this.stores = stores; this.source = source; }

    public JSONObject stats() {
        HashTable<String, Integer> genres = new HashTable<>();
        double sum = 0;
        for (int id : stores.getMovies().getAllIDs()) {
            for (Genre genre : stores.getMovies().getGenres(id)) {
                Integer count = genres.get(genre.getName());
                genres.put(genre.getName(), count == null ? 1 : count + 1);
            }
            sum += Math.max(0, stores.getRatings().getMovieAverageRating(id)) * Math.max(0, stores.getRatings().getNumRatings(id));
        }
        AvlTree<String, JSONObject> sorted = new AvlTree<>();
        for (String name : genres.keys()) sorted.put(name, new JSONObject().put("name", name).put("count", genres.get(name)));
        JSONArray distribution = new JSONArray();
        for (JSONObject genre : sorted.valuesInOrder(genres.size(), false)) distribution.put(genre);
        return new JSONObject().put("movies", stores.getMovies().size()).put("ratings", stores.getRatings().size())
            .put("cast", stores.getCredits().getUniqueCast().length).put("crew", stores.getCredits().getUniqueCrew().length)
            .put("users", stores.getRatings().userCount()).put("averageRating", stores.getRatings().size() == 0 ? 0 : sum / stores.getRatings().size())
            .put("genres", distribution).put("source", source).put("loadedAt", loadedAt);
    }

    // Descending numeric order, then ascending ID: stable pagination even when scores tie.
    private record Rank(double score, int id) implements Comparable<Rank> {
        public int compareTo(Rank other) {
            int comparison = Double.compare(score, other.score);
            return comparison != 0 ? comparison : Integer.compare(other.id, id);
        }
    }

    public JSONObject search(String query, String genre, String sort, int minRatings, int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 100 || minRatings < 0) throw new IllegalArgumentException("Invalid pagination or minimum rating count.");
        if (!sort.equals("rating") && !sort.equals("popular") && !sort.equals("newest")) throw new IllegalArgumentException("Sort must be rating, popular, or newest.");
        long started = System.nanoTime();
        AvlTree<Rank, Integer> ranked = new AvlTree<>();
        int[] ids = query.isBlank() ? stores.getMovies().getAllIDs() : stores.getMovies().findFilms(query);
        for (int id : ids) {
            if (stores.getRatings().getNumRatings(id) < minRatings) continue;
            boolean genreMatch = genre.isBlank();
            for (Genre value : stores.getMovies().getGenres(id)) if (value.getName().equalsIgnoreCase(genre)) genreMatch = true;
            if (!genreMatch) continue;
            double score = switch (sort) {
                case "popular" -> stores.getRatings().getNumRatings(id);
                case "newest" -> stores.getMovies().getRelease(id) == null ? Long.MIN_VALUE : stores.getMovies().getRelease(id).toEpochDay();
                default -> stores.getRatings().getMovieAverageRating(id);
            };
            ranked.put(new Rank(score, id), id);
        }
        JSONArray items = new JSONArray();
        int index = 0;
        int end = (int) Math.min(ranked.size(), (long) offset + limit);
        for (int id : ranked.valuesInOrder(end, true)) if (index++ >= offset) items.put(summary(id));
        return new JSONObject().put("items", items).put("total", ranked.size()).put("offset", offset)
            .put("limit", limit).put("queryMs", (System.nanoTime() - started) / 1_000_000.0);
    }

    public JSONObject summary(int id) {
        Movies movies = stores.getMovies();
        if (!movies.contains(id)) return null;
        JSONArray genres = new JSONArray();
        for (Genre genre : movies.getGenres(id)) genres.put(genre.getName());
        return new JSONObject().put("id", id).put("title", movies.getTitle(id)).put("overview", movies.getOverview(id))
            .put("releaseDate", movies.getRelease(id) == null ? JSONObject.NULL : movies.getRelease(id).toString())
            .put("runtime", movies.getRuntime(id)).put("genres", genres)
            .put("rating", stores.getRatings().getMovieAverageRating(id)).put("ratingCount", stores.getRatings().getNumRatings(id));
    }

    public JSONObject detail(int id) {
        JSONObject movie = summary(id);
        if (movie == null) return null;
        JSONArray cast = new JSONArray();
        for (CastCredit person : stores.getCredits().getFilmCast(id)) cast.put(new JSONObject()
            .put("id", person.getID()).put("name", person.getName()).put("character", person.getCharacter()));
        JSONArray crew = new JSONArray();
        for (CrewCredit person : stores.getCredits().getFilmCrew(id)) crew.put(new JSONObject()
            .put("id", person.getID()).put("name", person.getName()).put("job", person.getJob()));
        int[] histogram = new int[11];
        for (float rating : stores.getRatings().getMovieRatings(id)) histogram[Math.round(rating * 2)]++;
        return movie.put("cast", cast).put("crew", crew).put("ratingHistogram", new JSONArray(histogram))
            .put("recommendations", similar(id, 4));
    }

    /** Explainable overlap heuristic: two points per shared genre, one per shared actor. */
    public JSONArray similar(int id, int limit) {
        HashSet<String> genres = new HashSet<>();
        for (Genre genre : stores.getMovies().getGenres(id)) genres.add(genre.getName().toLowerCase(Locale.ROOT));
        HashSet<Integer> actors = new HashSet<>();
        for (CastCredit actor : stores.getCredits().getFilmCast(id)) actors.add(actor.getID());
        AvlTree<Rank, JSONObject> ranked = new AvlTree<>();
        for (int other : stores.getMovies().getAllIDs()) {
            if (other == id) continue;
            int score = 0;
            JSONArray reasons = new JSONArray();
            HashSet<String> seenGenres = new HashSet<>();
            for (Genre genre : stores.getMovies().getGenres(other)) {
                String normalized = genre.getName().toLowerCase(Locale.ROOT);
                if (genres.contains(normalized) && seenGenres.add(normalized)) { score += 2; reasons.put("Shared genre: " + genre.getName()); }
            }
            HashSet<Integer> seenActors = new HashSet<>();
            for (CastCredit actor : stores.getCredits().getFilmCast(other))
                if (actors.contains(actor.getID()) && seenActors.add(actor.getID())) { score++; reasons.put("Shared cast: " + actor.getName()); }
            if (score > 0) ranked.put(new Rank(score, other), summary(other).put("similarityScore", score).put("reasons", reasons));
        }
        JSONArray result = new JSONArray();
        for (JSONObject item : ranked.valuesInOrder(limit, true)) result.put(item);
        return result;
    }

    public JSONObject cast(int id) {
        Person person = stores.getCredits().getCast(id);
        if (person == null) return null;
        JSONArray films = new JSONArray();
        AvlTree<Integer, Integer> sorted = new AvlTree<>();
        for (int film : stores.getCredits().getCastFilms(id)) sorted.put(film, film);
        for (int film : sorted.valuesInOrder(sorted.size(), false)) films.put(summary(film));
        return new JSONObject().put("id", id).put("name", person.getName()).put("films", films);
    }
}
