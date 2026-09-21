package dev.cineindex.app;

import dev.cineindex.io.CatalogLoader;
import dev.cineindex.service.CatalogService;
import dev.cineindex.store.Stores;
import java.nio.file.Path;

public final class CineIndex {
    private CineIndex() {}
    public static void main(String[] args) {
        try {
            int port = 8080;
            Path json = null;
            Path csv = null;
            boolean inspect = false;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--help", "-h" -> {
                        System.out.println("CineIndex 1.0.0\nUsage: cineindex [--port 8080] [--catalog file.json | --csv-dir directory] [--inspect]\nWithout a data option, loads the bundled fictional demo. --inspect prints statistics and exits.");
                        return;
                    }
                    case "--port" -> port = Integer.parseInt(value(args, ++i));
                    case "--catalog" -> json = Path.of(value(args, ++i));
                    case "--csv-dir" -> csv = Path.of(value(args, ++i));
                    case "--inspect" -> inspect = true;
                    default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
                }
            }
            if (port < 1 || port > 65535) throw new IllegalArgumentException("Port must be between 1 and 65535.");
            if (json != null && csv != null) throw new IllegalArgumentException("Choose --catalog or --csv-dir, not both.");
            long start = System.nanoTime();
            Stores stores = json != null ? CatalogLoader.json(json) : csv != null ? CatalogLoader.csvDirectory(csv) : CatalogLoader.demo();
            CatalogService catalogue = new CatalogService(stores, json != null ? "JSON import" : csv != null ? "CSV import" : "Fictional demo");
            if (inspect) { System.out.println(catalogue.stats().toString(2)); return; }
            ApiServer server = new ApiServer(catalogue, port);
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            server.start();
            System.out.printf("CineIndex | %,d movies | %,d ratings | loaded in %.1f ms%nhttp://127.0.0.1:%d%n", stores.getMovies().size(), stores.getRatings().size(), (System.nanoTime() - start) / 1_000_000.0, server.port());
        } catch (Exception exception) {
            System.err.println("CineIndex: " + exception.getMessage());
            System.exit(1);
        }
    }
    private static String value(String[] args, int i) {
        if (i >= args.length) throw new IllegalArgumentException("Missing value for " + args[i - 1]);
        return args[i];
    }
}
