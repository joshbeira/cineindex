# Contributing

Use Java 17 or 21 and the checked-in Gradle wrapper. Run `./gradlew test` before opening a pull request; on Windows use `.\gradlew.bat test`.

Keep domain indexing in `store` and `structure`, import handling in `io`, and transport concerns in `app`. The custom collection implementations are intentional: changes to them should include a regression test for the affected invariant. Standard Java collections are appropriate at transport, benchmark and test boundaries.

For an algorithm change, explain its complexity and memory trade-offs. Add benchmark evidence when making a performance claim. The lightweight workload is useful for comparisons on one machine; use JMH for precise microbenchmark claims.

Use minimal synthetic fixtures in bug reports. Avoid uploading personal data, credentials, or datasets whose redistribution terms are unclear.

For dashboard changes, check desktop and narrow layouts, keyboard focus, empty results, and request failures. No frontend build step is required; restart the application after changing packaged resources.
