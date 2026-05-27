# Vocavista Backend

Spring Boot 4 backend service for Vocavista.

## Stack

- Amazon Corretto JDK 25 source target.
- Spring Boot 4.0.6.
- Spring Web MVC for REST APIs.
- Spring AI for outbound OpenAI word-info provider calls.
- Spring Data JPA with PostgreSQL.
- Flyway for database migrations.
- RustFS local S3-compatible object storage via Docker Compose.
- Bean Validation.
- Actuator health/info endpoints.
- Testcontainers for PostgreSQL-backed integration tests.

## Local Development

Use the project Java version with SDKMAN:

```bash
sdk env
```

Start PostgreSQL and local S3-compatible object storage:

```bash
docker compose up -d postgres rustfs rustfs-create-bucket
```

The default local database is named `vocavista` and uses credentials `vocavista` / `vocavista`. Override the connection with standard Spring property names if needed:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/vocavista
export SPRING_DATASOURCE_USERNAME=vocavista
export SPRING_DATASOURCE_PASSWORD=vocavista
```

The local object store uses RustFS:

```text
S3 API: http://localhost:9000
Console: http://localhost:9001
Credentials: rustfsadmin / rustfsadmin
Bucket: vocavista-media
```

The `rustfs-create-bucket` compose service creates the `vocavista-media` bucket if it does not already exist.

Local runtime defaults use direct Veo video generation, local RustFS storage, and the Gemini API key from `GOOGLE_AI_API_KEY`:

```bash
export GOOGLE_AI_API_KEY=...
```

OpenAI remains required for word-info generation:

```bash
export SPRING_AI_OPENAI_API_KEY=...
```

The backend stores generated video and returns `videoUrl`. Automated tests mock provider and storage boundaries where they exercise generation behavior, so they do not call external services.

Run the browser Veo video preview after starting the app:

```text
http://localhost:8080/veo-video.html
```

The Veo page calls `POST /api/v1/media/pronunciations`, polls the returned id, and plays the completed `videoUrl`. Veo requests default to vertical `9:16` output. The prompt asks for a male speaker for masculine nouns, a female speaker for feminine nouns, and a young adult woman for neuter nouns and non-nouns.

`backend/.env.example` lists optional local secret variables without real values. Other overrides should use full Spring property names, for example `VOCAVISTA_MEDIA_S3_ENDPOINT`.

Run the application:

```bash
docker compose up -d postgres rustfs rustfs-create-bucket
export GOOGLE_AI_API_KEY=...
export SPRING_AI_OPENAI_API_KEY=...
./mvnw spring-boot:run
```

For local overrides, copy the example local config and edit provider and storage settings there:

```bash
cp src/main/resources/application-local.example.yaml src/main/resources/application-local.yaml
./mvnw spring-boot:run
```

`src/main/resources/application-local.yaml` is gitignored and loaded by the default active `local` Spring profile. Keep secrets and personal overrides there, not in `src/main/resources/application.yaml`.

Run tests:

```bash
./mvnw test
```

Build a JVM artifact:

```bash
./mvnw package
```

## Health Check

When the app is running, health is available at:

```text
GET /actuator/health
```
