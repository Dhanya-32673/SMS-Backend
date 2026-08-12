# Multi-stage Dockerfile for Spring Boot SICMS Backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package executable jar
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
EXPOSE 8080

# Copy packaged jar from build stage
COPY --from=build /app/target/sicms-0.0.1-SNAPSHOT.jar app.jar

# Run Spring Boot application
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
