# Multi-stage Dockerfile for Spring Boot app built with Gradle (Groovy DSL only)
# No Kotlin DSL support needed

FROM eclipse-temurin:25-jdk-alpine AS build
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

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /opt/app

# Copy the built JAR
COPY --from=build /app/build/libs/*.jar app.jar

# Fix ownership
RUN chown appuser:appgroup app.jar

# Expose port
EXPOSE 8080

# Run as non-root
USER appuser

ENTRYPOINT ["java", "-XX:+UseCompactObjectHeaders", "-jar", "app.jar"]