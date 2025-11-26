# syntax=docker/dockerfile:1

FROM gradle:8.11.1-jdk17 AS builder
WORKDIR /workspace/app

# Prepare dependencies layer
COPY gradlew ./gradlew
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

# Build application
COPY src ./src
RUN --mount=type=cache,target=/home/gradle/.gradle ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ARG JAR_FILE=/workspace/app/build/libs/*.jar
COPY --from=builder ${JAR_FILE} app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
