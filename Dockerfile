# Use Java 21 runtime
FROM eclipse-temurin:21-jdk

# App directory inside container
WORKDIR /app

# Copy built jar
COPY target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]