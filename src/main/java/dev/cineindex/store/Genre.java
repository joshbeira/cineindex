package dev.cineindex.store;

/** Immutable catalogue value. */
public record Genre(int id, String name) {
    public int getID() { return id; }
    public String getName() { return name; }
}
