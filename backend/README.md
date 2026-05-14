# Vocavista Backend

Spring Boot 4 backend service for Vocavista.

## Stack

- Amazon Corretto JDK 25 source target.
- Spring Boot 4.0.6.
- Spring Web MVC for REST APIs.
- Spring RestClient for outbound AI provider calls.
- Spring Data JPA with PostgreSQL.
- Flyway for database migrations.
- Bean Validation.
- Actuator health/info endpoints.
- Testcontainers for PostgreSQL-backed integration tests.
- GraalVM Native Build Tools for native image builds.

## Local Development

Use the project Java version with SDKMAN:

```bash
sdk env
```

Start PostgreSQL locally:

```bash
docker compose up -d postgres
```

The default local database is named `vocavista` and uses credentials `vocavista` / `vocavista`. Override the connection with environment variables if needed:

```bash
export VOCAVISTA_DB_URL=jdbc:postgresql://localhost:5432/vocavista
export VOCAVISTA_DB_USERNAME=vocavista
export VOCAVISTA_DB_PASSWORD=vocavista
```

Run the application:

```bash
./mvnw spring-boot:run
```

Run tests:

```bash
./mvnw test
```

Build a JVM artifact:

```bash
./mvnw package
```

Native image support is configured for the future, but it is not required for normal local development yet. Normal development uses Corretto JDK 25. Switch to GraalVM 25 or newer before building a native image:

```bash
./mvnw -Pnative native:compile
```

## Health Check

When the app is running, health is available at:

```text
GET /actuator/health
```
