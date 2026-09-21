# Third-party components and data

CineIndex depends on JSON-java, Apache Commons CSV and its transitive dependencies. Tests use JUnit; Gradle and JaCoCo provide build and coverage tooling. These components retain their respective licences. Dependency JARs carry their upstream notices and licence files; see `./gradlew dependencies` for the resolved dependency graph.

The Gradle wrapper scripts and wrapper JAR are distributed under the Apache License 2.0. Their original notices are retained.

The bundled catalogue consists of original fictional film descriptions and synthetic audience ratings. Its names, dates, credits and ratings do not describe a real film dataset. The interface artwork is drawn in CSS and SVG; no external posters, fonts or image services are used.

Imported datasets remain the property of their respective owners and are not included in this repository. Import support does not grant redistribution rights over third-party data.
