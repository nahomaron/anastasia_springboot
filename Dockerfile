# ========= Stage 1: Build =========
FROM eclipse-temurin:23-jdk AS build
WORKDIR /app

# Copy Maven wrapper and project files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Build the application (skip tests for faster image builds)
RUN ./mvnw clean package -DskipTests

# ========= Stage 2: Runtime =========
FROM eclipse-temurin:23-jre
WORKDIR /app

# Copy only the final jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
