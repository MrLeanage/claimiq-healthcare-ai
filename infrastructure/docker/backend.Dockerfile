FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml ./pom.xml
RUN mvn -B dependency:go-offline
COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 claimiq \
    && useradd --uid 10001 --gid claimiq --no-create-home claimiq
WORKDIR /app
COPY --from=build --chown=claimiq:claimiq /build/target/backend-0.0.1-SNAPSHOT.jar app.jar
USER claimiq
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
