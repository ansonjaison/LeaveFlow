# ============================================================
# LeaveFlow — Dockerfile
# Multi-stage build: build the JAR, then run it in a slim image
# ============================================================

# Stage 1: Build
# Uses the official Maven + JDK 17 image to compile and package the app
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy the Maven project descriptor first.
# Docker caches this layer — dependencies are only re-downloaded
# when pom.xml changes, not on every source code change.
COPY pom.xml .

# Download all dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src ./src

# Build the JAR, skipping tests (tests run in CI, not during Docker build)
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Run
# Uses a slim JRE-only image — much smaller than the full JDK
# ============================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy only the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Port is provided by Render at runtime via the PORT env var
EXPOSE ${PORT:-8080}

# Map the PORT env var to Spring's server.port
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
