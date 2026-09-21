# Data imports

The default catalogue is bundled inside the application. It contains fictional films and cast, original descriptions and deterministic synthetic ratings. The browser's **Fictional demo** label distinguishes it from user imports.

## JSON

Use `--catalog /path/to/catalog.json`. A catalogue has a required `movies` array and an optional `ratings` array.

```json
{
  "movies": [
    {
      "id": 1,
      "title": "Example Film",
      "overview": "A short description.",
      "releaseDate": "2024-06-15",
      "runtime": 108,
      "genres": ["Drama", "Mystery"],
      "cast": [{"id": 101, "name": "Example Actor", "character": "The lead", "order": 0}],
      "crew": [{"id": 201, "name": "Example Director", "job": "Director", "department": "Directing"}]
    }
  ],
  "ratings": [
    {"userId": 1, "movieId": 1, "rating": 4.5, "timestamp": "2026-01-01T12:00:00"}
  ]
}
```

Each movie requires a unique positive `id` and a non-empty `title`. Missing overview, genres, release date, runtime or credits use empty defaults. Dates use ISO `YYYY-MM-DD`; rating timestamps use ISO local date-time. Treat JSON timestamps as UTC by convention; the format carries no offset.

Credit entries require `id` and `name`; other credit fields are optional. Cast billing defaults to its array position. Reuse a person's ID across films to connect their filmography, keeping their name consistent.

Ratings require a positive user ID, an existing movie ID, a finite value between 0 and 5, and a timestamp. Duplicate `(userId, movieId)` pairs and duplicate movie IDs are rejected. A failed import stops startup rather than exposing a partially loaded catalogue.

## CSV directory

Use `--csv-dir /path/to/directory`. Supply UTF-8 files with these names and columns:

| File | Required columns |
| --- | --- |
| `movies_metadata.csv` | `tmdb_id`, `title`, `original_title`, `overview`, `tagline`, `status`, `genres`, `release_date`, `budget`, `revenue`, `original_language`, `runtime` |
| `credits.csv` | `tmdb_id`, `cast`, `crew` |
| `ratings.csv` | `userId`, `tmdbId`, `rating`, `timestamp` |

Additional columns are ignored. `genres`, `cast` and `crew` contain JSON arrays quoted according to CSV rules. Genre objects have `id` and `name`; credit objects follow the JSON credit shape above. `release_date` accepts ISO dates or Unix seconds; rating timestamps are Unix seconds and are converted to UTC. Empty release dates remain unknown; empty numeric metadata defaults to zero.

The CSV adapter deliberately skips credits and ratings whose movie IDs are absent from the metadata file, making catalogue subsets possible. Duplicate imported ratings and credit rows are rejected. Invalid records report the filename and data-record number. The adapter imports the fields used by the discovery interface; it does not preserve unused columns, poster URLs, keywords or collection metadata.

This is a specific CSV schema, not a universal importer. Rename or transform columns for datasets with other schemas. No external dataset is downloaded or included automatically.

## Inspect without launching the server

```bash
./gradlew run --args="--catalog /path/to/catalog.json --inspect"
./gradlew run --args="--csv-dir /path/to/directory --inspect"
```

The command prints counts, genre distribution and overall average, then exits. This is useful for checking an import before opening the interface.
