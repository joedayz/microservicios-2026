# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY target/order-service-spring-1.0.0.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java","-jar","/app/app.jar"]

