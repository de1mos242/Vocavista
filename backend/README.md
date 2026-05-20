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

For local D-ID runs, expose RustFS through a temporary Cloudflare Tunnel:

```bash
docker compose up -d rustfs rustfs-create-bucket rustfs-tunnel
docker compose logs rustfs-tunnel
```

Copy the generated `https://...trycloudflare.com` URL from the logs and use it as the public media base URL with the bucket name appended:

```bash
export VOCAVISTA_S3_PUBLIC_BASE_URL=https://your-tunnel.trycloudflare.com/vocavista-media
```

The local bucket is configured for anonymous object reads by `rustfs-create-bucket` so D-ID can fetch generated audio through the tunnel.

Media storage configuration defaults for local development:

```bash
export VOCAVISTA_S3_ENDPOINT=http://localhost:9000
export VOCAVISTA_S3_REGION=us-east-1
export VOCAVISTA_S3_BUCKET=vocavista-media
export VOCAVISTA_S3_ACCESS_KEY=rustfsadmin
export VOCAVISTA_S3_SECRET_KEY=rustfsadmin
export VOCAVISTA_S3_PATH_STYLE_ACCESS=true
```

Optional real AI provider testing should be explicitly enabled through environment variables. Keep `VOCAVISTA_MEDIA_PROVIDER_MODE=fake` for normal automated tests. ElevenLabs text-to-speech and D-ID talking-head video are implemented for real local runs:

```bash
export VOCAVISTA_MEDIA_PROVIDER_MODE=real
export VOCAVISTA_TTS_PROVIDER=elevenlabs
export VOCAVISTA_LIPSYNC_PROVIDER=did
export VOCAVISTA_ELEVENLABS_API_KEY=...
export VOCAVISTA_ELEVENLABS_VOICE_ID=JBFqnCBsd6RMkjVDRZzb
export VOCAVISTA_ELEVENLABS_FIRST_WORD_SPEED=0.72
export VOCAVISTA_ELEVENLABS_SECOND_WORD_SPEED=1.0
export VOCAVISTA_ELEVENLABS_PHRASE_SPEED=0.86
export VOCAVISTA_DID_API_KEY=
export VOCAVISTA_DID_SOURCE_URL=https://create-images-results.d-id.com/DefaultPresenters/Noelle_f/image.png
```

`VOCAVISTA_DID_API_KEY` is used as the Basic auth token expected by D-ID. You may include the `Basic ` prefix or provide just the token value.

D-ID must be able to fetch the generated audio URL. `localhost` RustFS URLs are not reachable from D-ID cloud, so real D-ID runs need a public S3-compatible bucket, a tunnel to local RustFS, or another public object URL configured through `VOCAVISTA_S3_PUBLIC_BASE_URL`.

`backend/.env.example` lists the supported local storage and provider variables without real secret values. Real-provider tests should remain opt-in and skip automatically unless the required mode and credentials are present.

Run the application:

```bash
./mvnw spring-boot:run
```

Run one pronunciation-video generation from the command line without starting the web server:

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--spring.main.web-application-type=none --vocavista.media.pronunciation-video-command.enabled=true --vocavista.media.pronunciation-video-command.word=Hausaufgabe --vocavista.media.pronunciation-video-command.phrase=Ich mache meine Hausaufgabe nach dem Abendessen. --vocavista.media.pronunciation-video-command.language=de"
```

The command uses the same provider and storage configuration as the API, waits for completion or failure, prints the asset id/status and playable URL when available, then exits.

For repeated local runs, copy the example local config and edit the word, phrase, provider, and storage settings there:

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

Native image support is configured for the future, but it is not required for normal local development yet. Normal development uses Corretto JDK 25. Switch to GraalVM 25 or newer before building a native image:

```bash
./mvnw -Pnative native:compile
```

## Health Check

When the app is running, health is available at:

```text
GET /actuator/health
```
