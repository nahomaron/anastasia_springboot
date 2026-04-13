# ========= Stage 1: Build =========
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Maven wrapper and project files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src/main src/main

# Build the application artifact only. Tests run in CI before this workflow.
RUN ./mvnw -q clean package -Dmaven.test.skip=true

# ========= Stage 2: Runtime =========
FROM eclipse-temurin:21-jre
ARG VCS_REF=unknown
ARG BUILD_DATE=unknown
ARG REPO_URL=https://github.com/unknown/unknown
WORKDIR /app

LABEL org.opencontainers.image.revision=$VCS_REF \
      org.opencontainers.image.source=$REPO_URL \
      org.opencontainers.image.created=$BUILD_DATE

# Copy only the final jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
