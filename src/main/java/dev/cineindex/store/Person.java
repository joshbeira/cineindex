package dev.cineindex.store;

/** Immutable catalogue value. */
public record Person(int id, String name, String profilePath) {
    public int getID() { return id; }
    public String getName() { return name; }
    public String getProfilePath() { return profilePath; }
}
