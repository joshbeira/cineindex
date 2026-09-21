package dev.cineindex.store;

import dev.cineindex.structure.*;
public class Credits {
    Stores stores;

    private HashTable<Integer, CastCredit[]> filmCastMap;
    private HashTable<Integer, CrewCredit[]> filmCrewMap;

    private HashTable<Integer, Person> castPersons;
    private HashTable<Integer, Person> crewPersons;

    private HashTable<Integer, HashSet<Integer>> castFilmIDs;
    private HashTable<Integer, HashSet<Integer>> crewFilmIDs;

    private HashTable<Integer, HashSet<Integer>> castTopBillingFilmIDs;

    private HashTable<Integer, Integer> castRolesCount;

    public Credits(Stores stores) {
        this.stores = stores;
        filmCastMap = new HashTable<>();
        filmCrewMap = new HashTable<>();
        castPersons = new HashTable<>();
        crewPersons = new HashTable<>();
        castFilmIDs = new HashTable<>();
        crewFilmIDs = new HashTable<>();
        castTopBillingFilmIDs = new HashTable<>();
        castRolesCount = new HashTable<>();
    }

    public boolean add(CastCredit[] cast, CrewCredit[] crew, int id) {
        if (filmCastMap.containsKey(id) || filmCrewMap.containsKey(id)) {
            return false; // already added
        }

        // Process Cast
        if (cast != null) {
            // Sort cast by order
            AvlTree<Integer, CastCredit> castTree = new AvlTree<>();
            for (int i = 0; i < cast.length; i++) {
                CastCredit cc = cast[i];
                castTree.put(cc.getOrder(), cc);

                Person p = castPersons.get(cc.getID());
                if (p == null) {
                    p = new Person(cc.getID(), cc.getName(), cc.getProfilePath());
                    castPersons.put(p.getID(), p);
                }

                // Add to castFilmIDs
                HashSet<Integer> films = castFilmIDs.get(p.getID());
                if (films == null) {
                    films = new HashSet<>();
                    castFilmIDs.put(p.getID(), films);
                }
                films.add(id);

                // Top billing
                if (cc.getOrder() <= 3) {
                    HashSet<Integer> topFilms = castTopBillingFilmIDs.get(p.getID());
                    if (topFilms == null) {
                        topFilms = new HashSet<>();
                        castTopBillingFilmIDs.put(p.getID(), topFilms);
                    }
                    topFilms.add(id);
                }

                // Roles count
                Integer count = castRolesCount.get(p.getID());
                castRolesCount.put(p.getID(), (count == null ? 0 : count) + 1);
            }

            // Extract sorted cast
            filmCastMap.put(id, castTree.valuesInOrder(castTree.size(), false).toArray(new CastCredit[0]));
        } else {
            filmCastMap.put(id, new CastCredit[0]);
        }

        // Process Crew
        if (crew != null) {
            // Sort crew by id
            AvlTree<Integer, CrewCredit> crewTree = new AvlTree<>();
            for (int i = 0; i < crew.length; i++) {
                CrewCredit cc = crew[i];
                crewTree.put(cc.getID(), cc);

                Person p = crewPersons.get(cc.getID());
                if (p == null) {
                    p = new Person(cc.getID(), cc.getName(), cc.getProfilePath());
                    crewPersons.put(p.getID(), p);
                }

                HashSet<Integer> films = crewFilmIDs.get(p.getID());
                if (films == null) {
                    films = new HashSet<>();
                    crewFilmIDs.put(p.getID(), films);
                }
                films.add(id);
            }

            filmCrewMap.put(id, crewTree.valuesInOrder(crewTree.size(), false).toArray(new CrewCredit[0]));
        } else {
            filmCrewMap.put(id, new CrewCredit[0]);
        }

        return true;
    }

    public boolean remove(int id) {
        // Remove forward records and update reverse indexes together.
        CastCredit[] cast = filmCastMap.remove(id);
        CrewCredit[] crew = filmCrewMap.remove(id);

        if (cast == null && crew == null) return false;

        if (cast != null) {
            for (int i = 0; i < cast.length; i++) {
                CastCredit cc = cast[i];
                int pid = cc.getID();

                HashSet<Integer> films = castFilmIDs.get(pid);
                if (films != null) films.remove(id);

                HashSet<Integer> topFilms = castTopBillingFilmIDs.get(pid);
                if (topFilms != null) topFilms.remove(id);

                Integer count = castRolesCount.get(pid);
                if (count != null) {
                    if (count <= 1) {
                        castRolesCount.remove(pid);
                        castPersons.remove(pid);
                        castFilmIDs.remove(pid);
                        castTopBillingFilmIDs.remove(pid);
                    } else {
                        castRolesCount.put(pid, count - 1);
                    }
                }
            }
        }

        if (crew != null) {
            for (int i = 0; i < crew.length; i++) {
                CrewCredit cc = crew[i];
                int pid = cc.getID();

                HashSet<Integer> films = crewFilmIDs.get(pid);
                if (films != null) {
                    films.remove(id);
                    if (films.size() == 0) {
                        crewPersons.remove(pid);
                        crewFilmIDs.remove(pid);
                    }
                }
            }
        }

        return true;
    }

    public CastCredit[] getFilmCast(int filmID) {
        CastCredit[] cast = filmCastMap.get(filmID);
        return cast != null ? cast.clone() : new CastCredit[0];
    }

    public CrewCredit[] getFilmCrew(int filmID) {
        CrewCredit[] crew = filmCrewMap.get(filmID);
        return crew != null ? crew.clone() : new CrewCredit[0];
    }

    public int sizeOfCast(int filmID) {
        CastCredit[] cast = filmCastMap.get(filmID);
        return cast != null ? cast.length : -1;
    }

    public int sizeOfCrew(int filmID) {
        CrewCredit[] crew = filmCrewMap.get(filmID);
        return crew != null ? crew.length : -1;
    }

    public Person[] getUniqueCast() {
        return castPersons.values().toArray(new Person[0]);
    }

    public Person[] getUniqueCrew() {
        return crewPersons.values().toArray(new Person[0]);
    }

    public Person[] findCast(String searchTerm) {
        if (searchTerm == null) return new Person[0];
        String lower = searchTerm.toLowerCase(java.util.Locale.ROOT);
        LinkedSequence<Person> matching = new LinkedSequence<>();
        LinkedSequence<Person> vals = castPersons.values();
        for (Person p : vals) {
            if (p.getName() != null && p.getName().toLowerCase(java.util.Locale.ROOT).contains(lower)) {
                matching.add(p);
            }
        }
        Person[] arr = new Person[matching.size()];
        matching.toArray(arr);
        return arr;
    }

    public Person[] findCrew(String searchTerm) {
        if (searchTerm == null) return new Person[0];
        String lower = searchTerm.toLowerCase(java.util.Locale.ROOT);
        LinkedSequence<Person> matching = new LinkedSequence<>();
        LinkedSequence<Person> vals = crewPersons.values();
        for (Person p : vals) {
            if (p.getName() != null && p.getName().toLowerCase(java.util.Locale.ROOT).contains(lower)) {
                matching.add(p);
            }
        }
        Person[] arr = new Person[matching.size()];
        matching.toArray(arr);
        return arr;
    }

    public Person getCast(int castID) {
        return castPersons.get(castID);
    }

    public Person getCrew(int crewID) {
        return crewPersons.get(crewID);
    }

    public int[] getCastFilms(int castID) {
        HashSet<Integer> films = castFilmIDs.get(castID);
        if (films != null) {
            LinkedSequence<Integer> list = films.toList();
            Integer[] listArr = new Integer[list.size()];
            list.toArray(listArr);
            int[] arr = new int[listArr.length];
            for(int i=0; i<listArr.length; i++) arr[i] = listArr[i];
            return arr;
        }
        return new int[0];
    }

    public int[] getCrewFilms(int crewID) {
        HashSet<Integer> films = crewFilmIDs.get(crewID);
        if (films != null) {
            LinkedSequence<Integer> list = films.toList();
            Integer[] listArr = new Integer[list.size()];
            list.toArray(listArr);
            int[] arr = new int[listArr.length];
            for(int i=0; i<listArr.length; i++) arr[i] = listArr[i];
            return arr;
        }
        return new int[0];
    }

    public int[] getCastStarsInFilms(int castID) {
        HashSet<Integer> films = castTopBillingFilmIDs.get(castID);
        if (films != null) {
            LinkedSequence<Integer> list = films.toList();
            Integer[] listArr = new Integer[list.size()];
            list.toArray(listArr);
            int[] arr = new int[listArr.length];
            for(int i=0; i<listArr.length; i++) arr[i] = listArr[i];
            return arr;
        }
        return new int[0];
    }

    public Person[] getMostCastCredits(int numResults) {
        AvlTree<Integer, Person> countTree = new AvlTree<>();
        LinkedSequence<Integer> keys = castRolesCount.keys();
        for (Integer pid : keys) {
            Integer count = castRolesCount.get(pid);
            Person p = castPersons.get(pid);
            countTree.put(count, p);
        }

        return countTree.valuesInOrder(numResults, true).toArray(new Person[0]);
    }

    public int getNumCastCredits(int castID) {
        Integer count = castRolesCount.get(castID);
        return count != null ? count : -1;
    }

    public int size() {
        // Count films, including entries whose cast and crew are empty.
        return filmCastMap.size();
    }
}
