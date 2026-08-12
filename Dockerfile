# syntax=docker/dockerfile:1

ARG MODULE=infra:eureka-server
ARG MODULE_DIR=infra/eureka-server

FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY libs ./libs
COPY infra ./infra
COPY apps ./apps
COPY config-repo ./config-repo

ARG MODULE
RUN chmod +x gradlew \
    && ./gradlew :${MODULE}:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime-base
WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S spring \
    && adduser -S spring -G spring

FROM runtime-base AS service
ARG MODULE_DIR
COPY --from=builder --chown=spring:spring /workspace/${MODULE_DIR}/build/libs/*.jar ./
RUN rm -f *-plain.jar \
    && mv ./*.jar app.jar

USER spring:spring
ENTRYPOINT ["java", "-jar", "app.jar"]

# Config Server must ship the exact configuration revision that was built with
# the image, rather than mounting a mutable repository on the runtime host.
FROM service AS config-server
USER root
COPY --from=builder --chown=spring:spring /workspace/config-repo /config-repo
USER spring:spring
