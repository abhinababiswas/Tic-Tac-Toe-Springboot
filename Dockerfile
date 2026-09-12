# ---------------------------------------------------
# Stage 1: Build the Spring Boot application with Maven
# ---------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy Maven POM and backend source files
COPY backend/pom.xml .
COPY backend/src ./src

# Package production executable JAR (tests run in CI/local verify)
RUN mvn clean package -DskipTests

# ---------------------------------------------------
# Stage 2: Minimal, secure JRE runtime image
# ---------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create a non-root system user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copy built executable JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Render injects $PORT at runtime; default to 8080
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
