# Architecture

CineIndex separates ingestion, indexing, queries and presentation. A loader constructs a fresh `Stores` instance, validates its input, and returns it only after import succeeds. The application then creates a `CatalogService` and starts the HTTP server. No HTTP route modifies the stores.

## Domain indexes

### Movies

`Movies` maps integer IDs to metadata through a custom `HashTable`. A secondary map associates collection IDs with film IDs. Reassigning a film removes its previous collection membership; deleting a film removes its collection reference.

Title and overview searches perform a linear scan with `Locale.ROOT` normalization. Release-date range queries use exclusive boundaries. There is no full-text or persistent date index.

### Credits

`Credits` stores cast and crew arrays by movie ID. At ingestion, temporary AVL trees order cast by billing order and crew by person ID. Reverse indexes map a person to a set of films; a separate role count distinguishes multiple roles in one film from unique film appearances. Billing positions 0 through 3 count as top billing.

Removal updates forward records, reverse sets and role counts. Empty person indexes are removed when the last corresponding role disappears. Returned credit arrays are defensive copies of immutable records.

### Ratings

Each `(userId, movieId)` pair has one immutable rating record shared by the user and movie indexes. A bucket stores its entries and the running sum of ratings. Add, replacement and removal update both buckets; empty buckets are discarded.

This makes movie/user averages available from `sum / count` without scanning every rating. Values must be finite and between 0 and 5 inclusive; timestamps must be present. Importers check movie references and positive user IDs. Duplicate pairs are rejected on import; the Java store also exposes an explicit replacement operation.

A known movie with no ratings has count and average zero. An unknown movie has count and average `-1`. The HTTP service returns 404 for an unknown movie and uses `ratingCount` to distinguish unrated films from a genuine zero average.

## Collection choices

| Structure | Design | Main trade-off |
| --- | --- | --- |
| `HashTable<K,V>` | Separate chaining, doubling capacity, null-key/value support | Expected O(1) access; O(n) worst-case collisions; no shrinking |
| `HashSet<E>` | Hash table backed membership | Extra table storage in exchange for expected O(1) deduplication |
| `LinkedSequence<E>` | Doubly linked nodes, O(1) tail append, linear iterator | Indexed access is O(n); bulk algorithms iterate directly |
| `AvlTree<K,V>` | Height-balanced nodes with a sequence per duplicate key | O(log d) insertion/search; temporary allocation for ranking |

`d` is the number of distinct AVL keys. Hash table key/value enumeration scans table capacity as well as live entries, so its exact cost is O(capacity + entries). Capacity can remain above the live entry count after deletion.

## Query costs

Let `M` be the number of movies, `R` the number of ratings, `P` the number of people, `C` the number of credits in one film, and `k` the requested output count. Hash costs below assume ordinary key distributions and table capacity proportional to retained data.

| Operation | Expected time | Notes |
| --- | --- | --- |
| Movie lookup | O(1) | Integer ID index |
| Rating add/update/remove | Amortized O(1) | Two hash-index updates; resize may be O(n) |
| Movie/user rating average | O(1) | Cached sum and count |
| Text search | O(total searchable text length) | Linear metadata scan |
| Date-range query | O(M) | No persistent date index |
| Credit import for one film | O(C log C) | Sorting before storing arrays |
| Filmography | O(film-set capacity + result count) | Reverse person index |
| Store rating leaderboard | O(M log M + k) upper bound | Build an AVL tree, then bounded traversal |
| API ranked search | O(scan + M log M + offset + k) upper bound | ID tie-breakers; deep offsets traverse preceding results |

AVL result extraction costs O(log d + k) on an existing tree. It does **not** make a complete ranked query O(log M): the tree still needs to be built. Domain storage is O(M + R + total credits) with capacity and object overhead; per-request ranking adds temporary O(M) space in the worst case.

## Recommendations

For candidate film `B` and selected film `A`:

```text
score(A, B) = 2 × unique shared genres + unique shared cast members
```

The selected film is excluded, zero-score candidates are omitted, and ties are resolved by ascending movie ID. Results include the exact matching genres and cast. This is a deterministic content-overlap heuristic, not personalization or collaborative filtering. Candidate evaluation scans movie genre and credit entries and ranks matching films in an AVL tree.

## HTTP and browser boundary

The JDK HTTP server binds to loopback and uses four worker threads. It serves a fixed allowlist of packaged web assets and GET-only JSON endpoints. Invalid parameters produce structured 400 responses; missing resources return 404; unsupported methods return 405.

The catalogue is safely published before the server starts. Store classes themselves are mutable and not thread-safe; applications embedding them must not perform concurrent writes. There is no transactional cascade across movie, rating and credit stores. The supported HTTP application avoids this issue by ending mutation before serving requests.

The browser builds DOM content using `textContent`, uses a same-origin content security policy, debounces search input, and discards stale search/detail responses. No external fonts, posters or frontend libraries are required. Native dialogs provide focus containment and Escape-to-close behaviour.

## Next architectural steps

- Introduce a query index if substring search becomes a measured bottleneck.
- Add a bounded top-k heap if frequent leaderboard requests justify avoiding full tree construction.
- Add persistence and a coordinated mutation layer before exposing writes.
- Use JMH and an HTTP load generator before making microbenchmark or throughput claims.
