# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace/backend

COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x ./mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY backend/src src
RUN ./mvnw -B -DskipTests package

RUN mkdir /app \
    && java -Djarmode=tools -jar target/backend-0.0.1-SNAPSHOT.jar extract --destination /app \
    && mv /app/backend-0.0.1-SNAPSHOT.jar /app/app.jar

WORKDIR /app
RUN java \
    -XX:AOTCacheOutput=/app/app.aot \
    -Dspring.context.exit=onRefresh \
    -Dspring.profiles.active=aotcache \
    -jar /app/app.jar

FROM eclipse-temurin:25-jre

WORKDIR /app
RUN addgroup --system vocavista && adduser --system --ingroup vocavista vocavista

COPY --from=build /app/ ./

USER vocavista
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:AOTCache=/app/app.aot", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
