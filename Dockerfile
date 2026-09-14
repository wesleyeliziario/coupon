# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY api api
COPY domain domain
RUN chmod +x gradlew && ./gradlew :api:bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/api/build/libs/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
