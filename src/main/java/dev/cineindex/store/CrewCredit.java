package dev.cineindex.store;

/** Immutable catalogue value. */
public record CrewCredit(String elementID, String department, int id, String job, String name, String profilePath) {
    public String getElementID() { return elementID; }
    public String getDepartment() { return department; }
    public int getID() { return id; }
    public String getJob() { return job; }
    public String getName() { return name; }
    public String getProfilePath() { return profilePath; }
}
