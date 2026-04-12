# Multi-stage Dockerfile for Spring Boot app built with Gradle (Groovy DSL only)

FROM eclipse-temurin:25-jdk-alpine-3.23 AS build
WORKDIR /app

# Copy Gradle wrapper
COPY gradlew .
COPY gradle ./gradle

# Copy only Groovy build files (build.gradle, settings.gradle) and optional properties
COPY build.gradle settings.gradle gradle.properties* ./

# Download dependencies for optimal layer caching
RUN ./gradlew --no-daemon dependencies --no-build-cache

# Copy source code
COPY src ./src

# Build the executable JAR (skip tests; remove -x test if you want to run them)
RUN ./gradlew --no-daemon clean bootJar -x test

# === RUNTIME STAGE – DISTROLESS (recommended) ===
FROM gcr.io/distroless/java25-debian13:nonroot

WORKDIR /opt/app

# Copy the built JAR (non-root user is already set by the base image)
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port (updated to your new port)
EXPOSE 8081

# Keep your JVM flags + run the JAR
ENTRYPOINT ["java", "-XX:+UseCompactObjectHeaders", "-jar", "app.jar"]