package dev.cineindex.store;

/** Immutable catalogue value. */
public record CastCredit(int elementID, String character, String creditID, int id, String name, int order, String profilePath) {
    public int getElementID() { return elementID; }
    public String getCharacter() { return character; }
    public String getCreditID() { return creditID; }
    public int getID() { return id; }
    public String getName() { return name; }
    public int getOrder() { return order; }
    public String getProfilePath() { return profilePath; }
}
