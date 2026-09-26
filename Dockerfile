FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon test installDist

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/install/cineindex/ ./
ENV HOST=0.0.0.0 JAVA_OPTS="-Xms32m -Xmx256m"
USER 10001
EXPOSE 10000
CMD ["sh", "-c", "exec bin/cineindex --port ${PORT:-10000}"]
