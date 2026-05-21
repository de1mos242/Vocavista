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

Local runtime defaults use real ElevenLabs TTS, local RustFS storage, and browser-side TalkingHead rendering. Provide the ElevenLabs key through the environment before starting the app:

```bash
export VOCAVISTA_ELEVENLABS_API_KEY=...
export VOCAVISTA_ELEVENLABS_VOICE_ID=JBFqnCBsd6RMkjVDRZzb
export VOCAVISTA_ELEVENLABS_FIRST_WORD_SPEED=0.72
export VOCAVISTA_ELEVENLABS_SECOND_WORD_SPEED=1.0
export VOCAVISTA_ELEVENLABS_PHRASE_SPEED=0.86
```

The backend stores generated audio and returns `audioUrl`; the browser preview animates a TalkingHead avatar locally. Automated tests mock provider and storage boundaries where they exercise generation behavior, so they do not call external services.

Run the browser TalkingHead preview after starting the app:

```text
http://localhost:8080/talking-head.html
```

The page loads TalkingHead/HeadAudio modules from public CDNs, calls `POST /api/v1/media/pronunciations`, polls the returned id, and plays the completed `audioUrl` through a browser-rendered avatar. The `audioUrl` is served back through the backend as `/api/v1/media/pronunciations/{id}/audio` so browser WebAudio decoding does not depend on RustFS CORS settings.

`backend/.env.example` lists the supported local storage and provider variables without real secret values. Real-provider tests should remain opt-in and skip automatically unless the required mode and credentials are present.

Run the application:

```bash
docker compose up -d postgres rustfs rustfs-create-bucket
export VOCAVISTA_ELEVENLABS_API_KEY=...
./mvnw spring-boot:run
```

For local overrides, copy the example local config and edit provider and storage settings there:

```bash
cp src/main/resources/application-local.example.yaml src/main/resources/application-local.yaml
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

`src/main/resources/application-local.yaml` is gitignored and loaded only when the `local` Spring profile is active. Keep secrets and personal overrides there, not in `src/main/resources/application.yaml`.

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
