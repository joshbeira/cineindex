package dev.cineindex;

import dev.cineindex.store.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class StoreInvariantTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 1, 1, 0, 0);
    static void movie(Stores stores, int id) {
        stores.getMovies().add(id, "Film " + id, "Film " + id, "An island story", "", "Released", new Genre[0], LocalDate.of(2020, 1, 1), 0, 0, new String[0], "en", 100, "", false, false, "");
    }

    @Test void replacementAndDeletionKeepBothRatingIndexesAndAggregatesConsistent() {
        Stores stores = new Stores(); movie(stores, 1); movie(stores, 2);
        Ratings ratings = stores.getRatings();
        ratings.add(1, 1, 2, TIME); ratings.add(2, 1, 4, TIME); ratings.add(1, 2, 1, TIME);
        assertFalse(ratings.add(1, 1, 5, TIME));
        ratings.set(1, 1, 5, TIME);
        assertEquals(4.5, ratings.getMovieAverageRating(1)); assertEquals(3, ratings.getUserAverageRating(1));
        assertEquals(3, ratings.size());
        assertTrue(ratings.remove(1, 1)); assertTrue(ratings.remove(2, 1));
        assertEquals(0, ratings.getNumRatings(1)); assertEquals(0, ratings.getMovieAverageRating(1));
        assertArrayEquals(new int[]{2}, ratings.getMostRatedMovies(10)); assertArrayEquals(new int[]{1}, ratings.getMostRatedUsers(10));
        assertFalse(ratings.remove(2, 1)); assertEquals(1, ratings.userCount());
    }

    @Test void randomizedRatingMutationsMatchIndependentReference() {
        Stores stores = new Stores();
        Map<Integer, Map<Integer, Float>> expected = new HashMap<>();
        Random random = new Random(126);
        for (int movie = 1; movie <= 30; movie++) movie(stores, movie);
        for (int i = 0; i < 5000; i++) {
            int user = random.nextInt(40), movie = 1 + random.nextInt(30);
            float rating = random.nextInt(11) / 2f;
            Map<Integer, Float> row = expected.computeIfAbsent(user, ignored -> new HashMap<>());
            if (random.nextBoolean()) { row.put(movie, rating); stores.getRatings().set(user, movie, rating, TIME); }
            else { assertEquals(row.remove(movie) != null, stores.getRatings().remove(user, movie)); }
        }
        int count = expected.values().stream().mapToInt(Map::size).sum();
        assertEquals(count, stores.getRatings().size());
        for (int movie = 1; movie <= 30; movie++) {
            double sum = 0; int n = 0;
            for (Map<Integer, Float> row : expected.values()) if (row.containsKey(movie)) { sum += row.get(movie); n++; }
            assertEquals(n, stores.getRatings().getNumRatings(movie));
            assertEquals(n == 0 ? 0 : sum / n, stores.getRatings().getMovieAverageRating(movie), 0.00001);
        }
    }

    @ParameterizedTest @ValueSource(floats = {-1, 6, Float.NaN, Float.POSITIVE_INFINITY})
    void invalidRatingsDoNotMutateStores(float rating) {
        Ratings ratings = new Stores().getRatings();
        assertThrows(IllegalArgumentException.class, () -> ratings.add(1, 1, rating, TIME));
        assertEquals(0, ratings.size());
    }

    @Test void movingFilmBetweenCollectionsRemovesOldMembership() {
        Stores stores = new Stores(); movie(stores, 1);
        stores.getMovies().addToCollection(1, 10, "First", "", "");
        stores.getMovies().addToCollection(1, 20, "Second", "", "");
        assertArrayEquals(new int[0], stores.getMovies().getFilmsInCollection(10));
        assertArrayEquals(new int[]{1}, stores.getMovies().getFilmsInCollection(20));
        stores.getMovies().remove(1);
        assertArrayEquals(new int[0], stores.getMovies().getFilmsInCollection(20));
    }

    @Test void removingDuplicateRolesPreservesOtherFilmCredits() {
        Stores stores = new Stores();
        CastCredit first = new CastCredit(1, "A", "a", 7, "Mara", 0, "");
        CastCredit second = new CastCredit(2, "B", "b", 7, "Mara", 1, "");
        stores.getCredits().add(new CastCredit[]{second, first}, new CrewCredit[0], 1);
        stores.getCredits().add(new CastCredit[]{first}, new CrewCredit[0], 2);
        assertEquals(3, stores.getCredits().getNumCastCredits(7));
        assertEquals(0, stores.getCredits().getFilmCast(1)[0].getOrder());
        stores.getCredits().remove(1);
        assertEquals(1, stores.getCredits().getNumCastCredits(7));
        assertArrayEquals(new int[]{2}, stores.getCredits().getCastFilms(7));
        stores.getCredits().remove(2);
        assertEquals(0, stores.getCredits().getUniqueCast().length);
        assertArrayEquals(new int[0], stores.getCredits().getCastStarsInFilms(7));
    }

    @Test void searchIsIndependentOfDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Stores stores = new Stores(); movie(stores, 1);
            assertArrayEquals(new int[]{1}, stores.getMovies().findFilms("ISLAND"));
        } finally { Locale.setDefault(previous); }
    }
}
