# syntax=docker/dockerfile:1

ARG MODULE=infra:eureka-server
ARG MODULE_DIR=infra/eureka-server

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY libs ./libs
COPY infra ./infra
COPY apps ./apps

ARG MODULE
RUN chmod +x gradlew \
    && ./gradlew :${MODULE}:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

ARG MODULE_DIR
COPY --from=builder /workspace/${MODULE_DIR}/build/libs/*.jar ./
RUN rm -f *-plain.jar \
    && mv ./*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
