# syntax=docker/dockerfile:1.7
FROM maven:3.9.11-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=builder /workspace/target/flaky-downstream-service-1.0.0.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java","-jar","/app/app.jar"]

