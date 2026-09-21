package dev.cineindex.app;

import dev.cineindex.store.*;
import java.time.*;
import java.util.*;

/** Reproducible workload timings; deliberately not a substitute for JMH. */
public final class Benchmark {
    private static volatile long checksum;
    public static void main(String[] args) {
        System.out.println("# Java " + System.getProperty("java.version") + "; " + System.getProperty("os.name") + "; seed=126; warmups=3; samples=7");
        System.out.println("movies,ratings,load_median_ms,lookup_100k_median_ms,top20_median_ms,search_median_ms,checksum");
        for (int count : new int[]{1_000, 10_000, 50_000}) {
            double[][] timings = new double[4][7];
            for (int trial = -3; trial < 7; trial++) {
                long start = System.nanoTime();
                Stores stores = fixture(count);
                long loaded = System.nanoTime();
                Random random = new Random(126);
                for (int i = 0; i < 100_000; i++) checksum += stores.getMovies().getTitle(1 + random.nextInt(count)).length();
                long lookedUp = System.nanoTime();
                checksum += stores.getRatings().getTopAverageRatedMovies(20)[0];
                long ranked = System.nanoTime();
                checksum += stores.getMovies().findFilms("Orbit").length;
                long searched = System.nanoTime();
                if (trial >= 0) {
                    timings[0][trial] = (loaded - start) / 1e6;
                    timings[1][trial] = (lookedUp - loaded) / 1e6;
                    timings[2][trial] = (ranked - lookedUp) / 1e6;
                    timings[3][trial] = (searched - ranked) / 1e6;
                }
            }
            for (double[] samples : timings) Arrays.sort(samples);
            System.out.printf(Locale.ROOT, "%d,%d,%.3f,%.3f,%.3f,%.3f,%d%n", count, count * 10, timings[0][3], timings[1][3], timings[2][3], timings[3][3], checksum);
        }
    }
    private static Stores fixture(int count) {
        Stores stores = new Stores();
        LocalDateTime time = LocalDateTime.of(2026, 1, 1, 0, 0);
        for (int id = 1; id <= count; id++) {
            stores.getMovies().add(id, "Orbit " + id, "Orbit " + id, "A synthetic benchmark record.", "", "Released", new Genre[0], LocalDate.of(2020, 1, 1), 0, 0, new String[0], "en", 100, "", false, false, "");
            for (int user = 1; user <= 10; user++) stores.getRatings().add(user, id, ((id + user) % 11) / 2f, time);
        }
        return stores;
    }
}
