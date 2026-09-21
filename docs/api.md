# JSON API

Base URL: `http://127.0.0.1:8080`. All routes accept **GET** and return UTF-8 JSON. There is no authentication or mutation API; the application is bound to the local computer.

## Routes

| Route | Returns |
| --- | --- |
| `/api/health` | Status and application version |
| `/api/stats` | Movie/rating/person counts, user count, rating average, genre distribution, source and load timestamp |
| `/api/movies` | Filtered, ranked, paginated movie summaries |
| `/api/movies/{id}` | Summary plus cast, crew, rating histogram and explained recommendations |
| `/api/cast/{id}` | Cast member and their films, ordered by film ID |

## Search parameters

| Parameter | Default | Contract |
| --- | --- | --- |
| `q` | Empty | Case-insensitive substring in title, original title or overview |
| `genre` | Empty | Case-insensitive exact genre name |
| `sort` | `rating` | `rating`, `popular` (rating count), or `newest` (release date) |
| `minRatings` | `0` | Non-negative integer |
| `offset` | `0` | Non-negative integer |
| `limit` | `12` | Integer from 1 to 100 |

Ranks are descending, with ascending movie ID as the tie-breaker. Missing release dates sort last in `newest`. Unknown genre names and unmatched searches produce an empty result, not an error. An offset beyond the result set also produces an empty page. Duplicate query parameters are rejected; the raw query string is limited to 4,096 characters.

```bash
curl 'http://127.0.0.1:8080/api/movies?q=observatory&genre=Science%20Fiction&limit=5'
curl 'http://127.0.0.1:8080/api/movies/1'
curl 'http://127.0.0.1:8080/api/cast/103'
```

In Windows PowerShell, use `curl.exe` for these examples.

Search response shape:

```json
{
  "items": [
    {
      "id": 1,
      "title": "The Last Observatory",
      "overview": "...",
      "releaseDate": "2024-02-15",
      "runtime": 118,
      "genres": ["Science Fiction", "Drama"],
      "rating": 4.361111,
      "ratingCount": 18
    }
  ],
  "total": 1,
  "offset": 0,
  "limit": 5,
  "queryMs": 0.42
}
```

`queryMs` is an illustrative value here. At runtime it measures server-side selection, ranking and summary construction, excluding HTTP serialization, transport and rendering. It varies between calls.

## Detail fields

`ratingHistogram` has 11 bins for 0, 0.5, …, 5 stars. Arbitrary fractional ratings are rounded to the nearest half-star **for display only**; stored values and averages retain their original precision.

`recommendations` contains up to four summaries, each with `similarityScore` and `reasons`. For example, `"Shared genre: Drama"` contributes two points and `"Shared cast: Nia Hart"` contributes one. See [architecture](architecture.md#recommendations).

`/api/stats.averageRating` is weighted across all stored ratings, not an unweighted average of movie averages. `loadedAt` is the query service creation time as Unix milliseconds.

## Errors

```json
{"error":"limit must be an integer."}
```

- **400:** invalid or duplicate parameters.
- **404:** unknown resource or entity.
- **405:** method other than GET; the response includes `Allow: GET`.
- **500:** unexpected server error, without implementation details in the response.

Responses include `Cache-Control: no-store` and `X-Content-Type-Options: nosniff`. CORS is not enabled. This is a local API, not a public service deployment.
