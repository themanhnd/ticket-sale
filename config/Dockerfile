FROM eclipse-temurin:21-jre

WORKDIR /app

COPY config/target/config-1.0-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]