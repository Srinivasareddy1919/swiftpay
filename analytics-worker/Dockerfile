# Build context is the repo root
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY transaction-gateway/pom.xml transaction-gateway/pom.xml
COPY ledger-service/pom.xml      ledger-service/pom.xml
COPY analytics-worker/pom.xml    analytics-worker/pom.xml
COPY analytics-worker/src        analytics-worker/src
RUN mvn -B -ntp -pl analytics-worker -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/analytics-worker/target/analytics-worker-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
