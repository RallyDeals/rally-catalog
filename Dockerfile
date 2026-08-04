FROM maven:3.9.9-eclipse-temurin-21 AS build
RUN apt-get update && apt-get install -y --no-install-recommends git && rm -rf /var/lib/apt/lists/*

WORKDIR /rally-common
RUN git clone --depth 1 --branch v0.2.0 https://github.com/RallyDeals/rally-common.git .
RUN mvn -B -q install -DskipTests

WORKDIR /rally-catalog
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /rally-catalog/target/rally-catalog.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
