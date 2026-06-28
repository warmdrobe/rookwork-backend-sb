# Chose base image (Java, Node, Python)
FROM eclipse-temurin:21-jdk

# Folder in container
WORKDIR /app

# Copy form virtual to Container
COPY rookwork-backend.jar app.jar

# Port
EXPOSE 8080

# Cli run
ENTRYPOINT ["java", "-jar", "app.jar"]
