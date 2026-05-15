# Vocavista Backend

Spring Boot 4 backend service for Vocavista.

## Stack

- Amazon Corretto JDK 25 source target.
- Spring Boot 4.0.6.
- Spring Web MVC for REST APIs.
- Spring RestClient for outbound AI provider calls.
- Spring Data JPA with PostgreSQL.
- Flyway for database migrations.
- RustFS local S3-compatible object storage via Docker Compose.
- Bean Validation.
- Actuator health/info endpoints.
- Testcontainers for PostgreSQL-backed integration tests.
- GraalVM Native Build Tools for native image builds.

## Local Development

Use the project Java version with SDKMAN:

```bash
sdk env
```

Start PostgreSQL and local S3-compatible object storage:

```bash
docker compose up -d postgres rustfs rustfs-create-bucket
```

The default local database is named `vocavista` and uses credentials `vocavista` / `vocavista`. Override the connection with environment variables if needed:

```bash
export VOCAVISTA_DB_URL=jdbc:postgresql://localhost:5432/vocavista
export VOCAVISTA_DB_USERNAME=vocavista
export VOCAVISTA_DB_PASSWORD=vocavista
```

The local object store uses RustFS:

```text
S3 API: http://localhost:9000
Console: http://localhost:9001
Credentials: rustfsadmin / rustfsadmin
Bucket: vocavista-media
```

The `rustfs-create-bucket` compose service creates the `vocavista-media` bucket if it does not already exist.

Media storage configuration defaults for local development:

```bash
export VOCAVISTA_S3_ENDPOINT=http://localhost:9000
export VOCAVISTA_S3_REGION=us-east-1
export VOCAVISTA_S3_BUCKET=vocavista-media
export VOCAVISTA_S3_ACCESS_KEY=rustfsadmin
export VOCAVISTA_S3_SECRET_KEY=rustfsadmin
export VOCAVISTA_S3_PATH_STYLE_ACCESS=true
```

Optional real AI provider testing should be explicitly enabled through environment variables. Keep `VOCAVISTA_MEDIA_PROVIDER_MODE=fake` for normal automated tests. Set `VOCAVISTA_MEDIA_PROVIDER_MODE=real` and provide the relevant API keys when you intentionally want integration tests or local runs to call real providers:

```bash
export VOCAVISTA_MEDIA_PROVIDER_MODE=real
export VOCAVISTA_TTS_PROVIDER=openai
export VOCAVISTA_LIPSYNC_PROVIDER=did
export VOCAVISTA_OPENAI_API_KEY=
export VOCAVISTA_ELEVENLABS_API_KEY=
export VOCAVISTA_DID_API_KEY=
export VOCAVISTA_HEYGEN_API_KEY=
```

`backend/.env.example` lists the supported local storage and provider variables without real secret values. Real-provider tests should remain opt-in and skip automatically unless the required mode and credentials are present.

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
