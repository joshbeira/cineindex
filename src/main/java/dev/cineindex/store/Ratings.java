package dev.cineindex.store;

import java.time.LocalDateTime;
import dev.cineindex.structure.*;

/** Bidirectional rating index with incrementally maintained sums and counts. */
public final class Ratings {
    private record Rating(float value, LocalDateTime timestamp) {}

    private static final class Bucket {
        final HashTable<Integer, Rating> entries = new HashTable<>();
        double sum;
        void put(int key, Rating rating) {
            Rating previous = entries.put(key, rating);
            sum += rating.value - (previous == null ? 0 : previous.value);
        }
        void remove(int key) {
            Rating previous = entries.remove(key);
            if (previous != null) sum -= previous.value;
        }
        float average() { return (float) (sum / entries.size()); }
        float[] values() {
            float[] values = new float[entries.size()];
            int i = 0;
            for (Rating rating : entries.values()) values[i++] = rating.value;
            return values;
        }
    }

    private final Stores stores;
    private final HashTable<Integer, Bucket> users = new HashTable<>();
    private final HashTable<Integer, Bucket> movies = new HashTable<>();
    private int size;

    public Ratings(Stores stores) { this.stores = stores; }

    private static void validate(float value, LocalDateTime timestamp) {
        if (!Float.isFinite(value) || value < 0 || value > 5)
            throw new IllegalArgumentException("Rating must be between 0 and 5.");
        if (timestamp == null) throw new IllegalArgumentException("Rating timestamp is required.");
    }

    private static Bucket bucket(HashTable<Integer, Bucket> index, int id) {
        Bucket result = index.get(id);
        if (result == null) { result = new Bucket(); index.put(id, result); }
        return result;
    }

    public boolean add(int userId, int movieId, float value, LocalDateTime timestamp) {
        validate(value, timestamp);
        Bucket user = users.get(userId);
        if (user != null && user.entries.containsKey(movieId)) return false;
        Rating rating = new Rating(value, timestamp);
        bucket(users, userId).put(movieId, rating);
        bucket(movies, movieId).put(userId, rating);
        size++;
        return true;
    }

    public boolean set(int userId, int movieId, float value, LocalDateTime timestamp) {
        validate(value, timestamp);
        Bucket user = users.get(userId);
        if (user == null || !user.entries.containsKey(movieId)) return add(userId, movieId, value, timestamp);
        Rating rating = new Rating(value, timestamp);
        user.put(movieId, rating);
        movies.get(movieId).put(userId, rating);
        return true;
    }

    public boolean remove(int userId, int movieId) {
        Bucket user = users.get(userId);
        if (user == null || !user.entries.containsKey(movieId)) return false;
        Bucket movie = movies.get(movieId);
        user.remove(movieId);
        movie.remove(userId);
        if (user.entries.isEmpty()) users.remove(userId);
        if (movie.entries.isEmpty()) movies.remove(movieId);
        size--;
        return true;
    }

    public float[] getMovieRatings(int movieId) {
        Bucket movie = movies.get(movieId);
        return movie == null ? new float[0] : movie.values();
    }
    public float[] getUserRatings(int userId) {
        Bucket user = users.get(userId);
        return user == null ? new float[0] : user.values();
    }
    public float getMovieAverageRating(int movieId) {
        Bucket movie = movies.get(movieId);
        return movie == null ? (stores.getMovies().contains(movieId) ? 0 : -1) : movie.average();
    }
    public float getUserAverageRating(int userId) {
        Bucket user = users.get(userId);
        return user == null ? -1 : user.average();
    }
    public int getNumRatings(int movieId) {
        Bucket movie = movies.get(movieId);
        return movie == null ? (stores.getMovies().contains(movieId) ? 0 : -1) : movie.entries.size();
    }
    public int size() { return size; }
    public int userCount() { return users.size(); }

    private int[] rank(HashTable<Integer, Bucket> index, int limit, boolean average) {
        AvlTree<Double, Integer> tree = new AvlTree<>();
        for (int id : index.keys()) {
            Bucket bucket = index.get(id);
            tree.put(average ? (double) bucket.average() : (double) bucket.entries.size(), id);
        }
        LinkedSequence<Integer> selected = tree.valuesInOrder(limit, true);
        int[] result = new int[selected.size()];
        int i = 0;
        for (int id : selected) result[i++] = id;
        return result;
    }
    public int[] getMostRatedMovies(int limit) { return rank(movies, limit, false); }
    public int[] getMostRatedUsers(int limit) { return rank(users, limit, false); }
    public int[] getTopAverageRatedMovies(int limit) { return rank(movies, limit, true); }
}
