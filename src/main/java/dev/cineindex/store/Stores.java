package dev.cineindex.store;

/** Composition root for the catalogue indexes. Instances are not thread-safe. */
public final class Stores {
    private final Movies movies = new Movies(this);
    private final Credits credits = new Credits(this);
    private final Ratings ratings = new Ratings(this);

    public Movies getMovies() { return movies; }
    public Credits getCredits() { return credits; }
    public Ratings getRatings() { return ratings; }
}
