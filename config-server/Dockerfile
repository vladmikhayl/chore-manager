FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl

COPY --from=build /build/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]