# Reproducible workload measurements

Run `./gradlew benchmark` with Java 17 or 21. On Windows use `.\gradlew.bat benchmark`. The runner prints its environment, workload dimensions, timings and a checksum to standard output.

## Method

- Generate 1,000, 10,000 and 50,000 movie records, with 10 ratings per film.
- Run three warm-up trials followed by seven measured trials per size.
- Report each operation's median across the seven measured trials.
- Load directly into fresh domain stores; no CSV/JSON parsing or HTTP is involved.
- Perform 100,000 ID lookups with a fixed random seed of 126.
- Request the top 20 movies by average rating, then search for `Orbit`, which matches every generated title.
- Accumulate a volatile checksum so results are consumed.

The benchmark fixture has only ten distinct users, no credits and no genres. It exercises a particular rating-index shape, not every possible workload. The timing labelled lookup also includes random-number generation, string-length access and checksum updates.

## Reference run

Recorded on 2026-09-21 with Java 17.0.16 on Windows 11. Units are milliseconds; lower is faster.

| Movies | Ratings | Load median | 100k ID lookups | Top 20 | Full-match search |
| ---: | ---: | ---: | ---: | ---: | ---: |
| 1,000 | 10,000 | 2.837 | 2.969 | 0.449 | 0.409 |
| 10,000 | 100,000 | 13.203 | 7.920 | 1.785 | 1.648 |
| 50,000 | 500,000 | 102.832 | 16.945 | 7.367 | 10.309 |

[Raw output](benchmark-reference.csv)

## Interpretation

These results demonstrate that the supplied workload runs at the stated sizes. They are not a speed comparison against Java's built-in collections, a database, or another application. No baseline or production throughput claim is implied.

This lightweight runner shares a JVM across trials, does not use isolated forks, and does not control CPU scheduling, heap pressure or garbage collection. Object allocation, operating-system activity and JIT compilation can influence results. The checksum and warm-up reduce some common measurement mistakes but do not replace a proper microbenchmark harness.

Use the runner to reproduce functionality and compare changes on the same machine. Use JMH for precise microbenchmark claims and a separate HTTP load test for request latency or throughput.
