# Technical Architecture

## Current Backend

The current application is a Spring Boot 4 backend on Java 25.

Implemented API areas:

- `GET /api/v1/auth/me` for the authenticated application user.
- `GET /api/v1/words/info` for AI-generated German vocabulary metadata.
- `GET /api/v1/words/suggestions` for simple autocomplete over cached word info and pronunciation assets.
- `POST /api/v1/media/pronunciations` for reusable generated pronunciation media.
- `GET /api/v1/media/pronunciations/{id}` for generation status.
- `GET /api/v1/media/pronunciations/{id}/video` for same-origin generated video playback.

## Authorization

The backend uses Spring Security with Google OAuth2/OpenID Connect login. The login entrypoint is `/oauth2/authorization/google` and the default callback is `/login/oauth2/code/google`.

Google is requested with the minimal identity scopes `openid`, `email`, and `profile`. On successful login the backend creates or updates a `user_accounts` row keyed by provider and provider subject, storing the user's email and display name.

Existing `/api/v1/**` endpoints require an authenticated session. Actuator health/info, OAuth login/callback paths, `/`, and the static Veo preview page remain publicly reachable.

The static Veo preview page calls `/api/v1/auth/me` on load to decide whether to show a Google sign-in link or the current application user. API action buttons stay disabled until the current user request succeeds. Signed-in users can call `/logout` from the page; the logout handler returns `204 No Content` so the page can render signed-out state without following a redirect.

## API Contract

The backend keeps the public REST contract in `backend/src/main/resources/openapi/vocavista-api.yaml`.

The Maven build generates Spring MVC interfaces and API DTOs from that contract. Controllers implement generated interfaces instead of duplicating request and response shapes by hand.

## Database

PostgreSQL stores structured application state.

Current persisted data:

- Application users authenticated through Google OAuth.
- Pronunciation generation inputs.
- Cached generated word-info responses.
- Normalized pronunciation cache keys.
- Generation status and errors.
- Generated video object keys.
- Provider/model metadata for generated media.

Generated media bytes are not stored in PostgreSQL. They are stored in S3-compatible object storage.

## Media Storage

The backend uses S3-compatible storage through `S3MediaStorageService`.

Local development uses RustFS from `backend/compose.yaml` with the `vocavista-media` bucket.

The static manual preview page is served by Spring MVC from `backend/src/main/resources/static/veo-video.html`.

## AI Providers

OpenAI is used for word information generation through Spring AI.

Google Veo is used for default direct pronunciation video generation. The configurable default model is `veo-3.1-lite-generate-preview`. Veo requests default to `9:16` vertical output.

Pronunciation generation reads the linked word-info JSON for noun metadata. If the word is a noun and has an article, the second spoken repetition includes the article. If noun gender is available, the Veo prompt asks for a corresponding male or female speaker; neuter nouns and non-nouns use a young adult woman.

Provider failures are mapped to controlled backend errors rather than exposing raw provider responses.

Word-info responses are cached after successful provider generation and reused by normalized query to reduce repeat AI calls.

## Testing

Unit and MVC tests mock provider and storage boundaries.

The Spring context test uses Testcontainers with PostgreSQL so Flyway and JPA validation run against a real database engine.
