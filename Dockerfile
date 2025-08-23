# Stage 1: build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/exalyze-0.0.1-SNAPSHOT.jar app.jar

# Expose Render's port
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app
