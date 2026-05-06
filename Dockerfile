FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradle gradle/
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
COPY models models

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
