
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Set working directory inside container
WORKDIR /app

# Copy only pom.xml first to leverage Docker cache
COPY pom.xml .

# Copy source code
COPY src ./src

# Build the Spring Boot JAR (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Stage 2: Run the application using JRE
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port 8080 (Spring Boot default)
EXPOSE 8080

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]

