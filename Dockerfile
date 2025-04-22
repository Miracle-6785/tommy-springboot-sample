# Use an official Maven image that includes Temurin JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build

# Set the working directory
WORKDIR /workspace

# Copy Maven wrapper files (if you use mvnw) - Optional but good practice
# COPY .mvn/ .mvn
# COPY mvnw pom.xml ./

# Copy pom.xml first to leverage Docker cache for dependencies
COPY pom.xml .

# Download dependencies (if pom.xml hasn't changed, this layer is cached)
# Consider using mvnw if you copied it: RUN ./mvnw dependency:go-offline
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src src

# Build the application JAR
# Consider using mvnw if you copied it: RUN ./mvnw package -DskipTests
RUN mvn package -DskipTests

# --- Runtime Stage ---
# Use a slim JRE image for the final stage
FROM eclipse-temurin:21-jre

# Create a dedicated user and group
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

# Set the working directory
WORKDIR /app

# Copy *only* the built JAR from the build stage
# Adjust the path if your JAR name pattern is different
COPY --from=build --chown=appuser:appgroup /workspace/target/*.jar app.jar

# Switch to the non-root user
USER appuser

# Expose the port
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]