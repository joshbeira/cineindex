package dev.cineindex.store;

import java.time.LocalDate;

import dev.cineindex.structure.*;

public class Movies {
    Stores stores;

    private static class MovieData {
        int id;
        String title;
        String originalTitle;
        String overview;
        String tagline;
        String status;
        Genre[] genres;
        LocalDate release;
        long budget;
        long revenue;
        String[] languages;
        String originalLanguage;
        double runtime;
        String homepage;
        boolean adult;
        boolean video;
        String poster;

        double voteAverage = -2.0d;
        int voteCount = -2;
        int collectionID = -2;
        String collectionName;
        String collectionPosterPath;
        String collectionBackdropPath;
        String imdbID;
        double popularity = 0.0d;

        LinkedSequence<Company> productionCompanies = new LinkedSequence<>();
        LinkedSequence<String> productionCountries = new LinkedSequence<>();

        MovieData(int id, String title, String originalTitle, String overview, String tagline, String status, Genre[] genres, LocalDate release, long budget, long revenue, String[] languages, String originalLanguage, double runtime, String homepage, boolean adult, boolean video, String poster) {
            this.id = id;
            this.title = title;
            this.originalTitle = originalTitle;
            this.overview = overview;
            this.tagline = tagline;
            this.status = status;
            this.genres = genres;
            this.release = release;
            this.budget = budget;
            this.revenue = revenue;
            this.languages = languages;
            this.originalLanguage = originalLanguage;
            this.runtime = runtime;
            this.homepage = homepage;
            this.adult = adult;
            this.video = video;
            this.poster = poster;
        }
    }

    private HashTable<Integer, MovieData> moviesMap;
    private HashTable<Integer, LinkedSequence<Integer>> collectionFilms;

    /**
     * The constructor for the Movies data store.
     */
    public Movies(Stores stores) {
        this.stores = stores;
        moviesMap = new HashTable<>();
        collectionFilms = new HashTable<>();
    }

    public boolean add(int id, String title, String originalTitle, String overview, String tagline, String status, Genre[] genres, LocalDate release, long budget, long revenue, String[] languages, String originalLanguage, double runtime, String homepage, boolean adult, boolean video, String poster) {
        if (moviesMap.containsKey(id)) {
            return false; // already exists
        }
        MovieData md = new MovieData(id, title, originalTitle, overview, tagline, status, genres, release, budget, revenue, languages, originalLanguage, runtime, homepage, adult, video, poster);
        moviesMap.put(id, md);
        return true;
    }

    public boolean remove(int id) {
        MovieData md = moviesMap.remove(id);
        if (md != null) {
            if (md.collectionID != -2) {
                LinkedSequence<Integer> films = collectionFilms.get(md.collectionID);
                if (films != null) {
                    films.remove(id);
                }
            }
            return true;
        }
        return false;
    }

    public int[] getAllIDs() {
        LinkedSequence<Integer> keys = moviesMap.keys();
        Integer[] keyArr = new Integer[keys.size()];
        keys.toArray(keyArr);
        int[] result = new int[keyArr.length];
        for (int i = 0; i < keyArr.length; i++) {
            result[i] = keyArr[i];
        }
        return result;
    }

    public int[] getAllIDsReleasedInRange(LocalDate start, LocalDate end) {
        LinkedSequence<Integer> matching = new LinkedSequence<>();
        LinkedSequence<MovieData> values = moviesMap.values();
        for (MovieData md : values) {
            if (md.release != null) {
                if (md.release.isAfter(start) && md.release.isBefore(end)) {
                    matching.add(md.id);
                }
            }
        }
        int[] result = new int[matching.size()];
        int index = 0;
        for (int id : matching) result[index++] = id;
        return result;
    }

    public String getTitle(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.title : null;
    }

    public String getOriginalTitle(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.originalTitle : null;
    }

    public String getOverview(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.overview : null;
    }

    public String getTagline(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.tagline : null;
    }

    public String getStatus(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.status : null;
    }

    public Genre[] getGenres(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.genres : null;
    }

    public LocalDate getRelease(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.release : null;
    }

    public long getBudget(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.budget : -1;
    }

    public long getRevenue(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.revenue : -1;
    }

    public String[] getLanguages(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.languages : null;
    }

    public String getOriginalLanguage(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.originalLanguage : null;
    }

    public double getRuntime(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.runtime : -1.0d;
    }

    public String getHomepage(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.homepage : null;
    }

    public boolean getAdult(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.adult : false;
    }

    public boolean getVideo(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.video : false;
    }

    public String getPoster(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.poster : null;
    }

    public boolean setVote(int id, double voteAverage, int voteCount) {
        MovieData md = moviesMap.get(id);
        if (md != null) {
            md.voteAverage = voteAverage;
            md.voteCount = voteCount;
            return true;
        }
        return false;
    }

    public double getVoteAverage(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.voteAverage : -1.0d;
    }

    public int getVoteCount(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.voteCount : -1;
    }

    public boolean addToCollection(int filmID, int collectionID, String collectionName, String collectionPosterPath, String collectionBackdropPath) {
        MovieData md = moviesMap.get(filmID);
        if (md != null) {
            if (md.collectionID != -2 && md.collectionID != collectionID) {
                LinkedSequence<Integer> previous = collectionFilms.get(md.collectionID);
                if (previous != null) previous.remove(filmID);
            }
            md.collectionID = collectionID;
            md.collectionName = collectionName;
            md.collectionPosterPath = collectionPosterPath;
            md.collectionBackdropPath = collectionBackdropPath;

            LinkedSequence<Integer> films = collectionFilms.get(collectionID);
            if (films == null) {
                films = new LinkedSequence<>();
                collectionFilms.put(collectionID, films);
            }
            if (!films.contains(filmID)) {
                films.add(filmID);
            }
            return true;
        }
        return false;
    }

    public int[] getFilmsInCollection(int collectionID) {
        LinkedSequence<Integer> films = collectionFilms.get(collectionID);
        if (films != null) {
            int[] result = new int[films.size()];
            int index = 0;
            for (int id : films) result[index++] = id;
            return result;
        }
        return new int[0];
    }

    public String getCollectionName(int collectionID) {
        LinkedSequence<Integer> films = collectionFilms.get(collectionID);
        if (films != null && films.size() > 0) {
            MovieData md = moviesMap.get(films.get(0));
            if (md != null) return md.collectionName;
        }
        return null;
    }

    public String getCollectionPoster(int collectionID) {
        LinkedSequence<Integer> films = collectionFilms.get(collectionID);
        if (films != null && films.size() > 0) {
            MovieData md = moviesMap.get(films.get(0));
            if (md != null) return md.collectionPosterPath;
        }
        return null;
    }

    public String getCollectionBackdrop(int collectionID) {
        LinkedSequence<Integer> films = collectionFilms.get(collectionID);
        if (films != null && films.size() > 0) {
            MovieData md = moviesMap.get(films.get(0));
            if (md != null) return md.collectionBackdropPath;
        }
        return null;
    }

    public int getCollectionID(int filmID) {
        MovieData md = moviesMap.get(filmID);
        return md != null && md.collectionID != -2 ? md.collectionID : -1;
    }

    public boolean setIMDB(int filmID, String imdbID) {
        MovieData md = moviesMap.get(filmID);
        if (md != null) {
            md.imdbID = imdbID;
            return true;
        }
        return false;
    }

    public String getIMDB(int filmID) {
        MovieData md = moviesMap.get(filmID);
        return md != null ? md.imdbID : null;
    }

    public boolean setPopularity(int id, double popularity) {
        MovieData md = moviesMap.get(id);
        if (md != null) {
            md.popularity = popularity;
            return true;
        }
        return false;
    }

    public double getPopularity(int id) {
        MovieData md = moviesMap.get(id);
        return md != null ? md.popularity : -1.0d;
    }

    public boolean addProductionCompany(int id, Company company) {
        MovieData md = moviesMap.get(id);
        if (md != null) {
            md.productionCompanies.add(company);
            return true;
        }
        return false;
    }

    public boolean addProductionCountry(int id, String country) {
        MovieData md = moviesMap.get(id);
        if (md != null) {
            md.productionCountries.add(country);
            return true;
        }
        return false;
    }

    public Company[] getProductionCompanies(int id) {
        MovieData md = moviesMap.get(id);
        if (md != null) {
            Company[] arr = new Company[md.productionCompanies.size()];
            md.productionCompanies.toArray(arr);
            return arr;
        }
        return null;
    }

    public String[] getProductionCountries(int id) {
        MovieData md = moviesMap.get(id);
        if (md != null) {
            String[] arr = new String[md.productionCountries.size()];
            md.productionCountries.toArray(arr);
            return arr;
        }
        return null;
    }

    public boolean contains(int id) { return moviesMap.containsKey(id); }

    public int size() {
        return moviesMap.size();
    }

    public int[] findFilms(String searchTerm) {
        if (searchTerm == null) return new int[0];
        String lowerTerm = searchTerm.toLowerCase(java.util.Locale.ROOT);
        LinkedSequence<Integer> matching = new LinkedSequence<>();
        LinkedSequence<MovieData> values = moviesMap.values();
        for (MovieData md : values) {
            if ((md.title != null && md.title.toLowerCase(java.util.Locale.ROOT).contains(lowerTerm)) ||
                (md.originalTitle != null && md.originalTitle.toLowerCase(java.util.Locale.ROOT).contains(lowerTerm)) ||
                (md.overview != null && md.overview.toLowerCase(java.util.Locale.ROOT).contains(lowerTerm))) {
                matching.add(md.id);
            }
        }
        int[] result = new int[matching.size()];
        int index = 0;
        for (int id : matching) result[index++] = id;
        return result;
    }
}
