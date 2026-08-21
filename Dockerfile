# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build
RUN apt-get update && apt-get install -y --no-install-recommends git && rm -rf /var/lib/apt/lists/*

ARG GITHUB_ACTOR

RUN --mount=type=secret,id=github_token \
    mkdir -p /root/.m2-config && \
    echo "<settings><servers><server><id>github</id><username>${GITHUB_ACTOR}</username><password>$(cat /run/secrets/github_token)</password></server></servers></settings>" > /root/.m2-config/settings.xml

WORKDIR /rally-common
RUN --mount=type=secret,id=github_token \
    git clone --depth 1 --branch main \
    https://${GITHUB_ACTOR}:$(cat /run/secrets/github_token)@github.com/RallyDeals/rally-common.git .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -s /root/.m2-config/settings.xml -B install -DskipTests

# 👇 TEMPORARY debug step — tells us if the jar actually landed in the shared cache
RUN --mount=type=cache,target=/root/.m2 \
    find /root/.m2/repository/com/rally/rally-common -type f 2>&1 || echo "NOTHING FOUND AT THAT PATH"

WORKDIR /rally-catalog
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -s /root/.m2-config/settings.xml -B dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -s /root/.m2-config/settings.xml -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /rally-catalog/target/rally-catalog.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
