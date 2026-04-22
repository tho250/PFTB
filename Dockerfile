# Stage 1: Build the application using Maven and JDK 17
# We use a specific Maven version that comes with JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project definition file
COPY pom.xml .

# Copy the Maven wrapper files (good practice, though we use mvn directly)
COPY .mvn/ .mvn
COPY mvnw .

# Copy the rest of your application's source code
COPY src ./src

# Run the Maven build to create the executable JAR file
# -DskipTests skips running tests, which is common for production builds
RUN mvn package -DskipTests

# Stage 2: Create the final, lightweight runtime image
# We use a JRE (Java Runtime Environment) image, which is smaller than a full JDK
FROM eclipse-temurin:17-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the executable JAR file that was built in the 'build' stage
# Make sure the artifactId and version in pom.xml match this path
COPY --from=build /app/target/pftb-db-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that the Spring Boot application runs on (default is 8080)
EXPOSE 8080

# The command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
