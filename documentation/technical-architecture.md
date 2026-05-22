# Technical Architecture

## Current Backend

The current application is a Spring Boot 4 backend on Java 25.

Implemented API areas:

- `GET /api/v1/words/info` for AI-generated German vocabulary metadata.
- `GET /api/v1/words/suggestions` for simple autocomplete over cached word info and pronunciation assets.
- `POST /api/v1/media/pronunciations` for reusable generated pronunciation audio.
- `GET /api/v1/media/pronunciations/{id}` for generation status.
- `GET /api/v1/media/pronunciations/{id}/audio` for same-origin generated audio playback.

## API Contract

The backend keeps the public REST contract in `backend/src/main/resources/openapi/vocavista-api.yaml`.

The Maven build generates Spring MVC interfaces and API DTOs from that contract. Controllers implement generated interfaces instead of duplicating request and response shapes by hand.

## Database

PostgreSQL stores structured application state.

Current persisted data:

- Pronunciation generation inputs.
- Cached generated word-info responses.
- Normalized pronunciation cache keys.
- Generation status and errors.
- Generated audio object keys.
- Provider/model metadata for generated audio.

Generated audio bytes are not stored in PostgreSQL. They are stored in S3-compatible object storage.

## Media Storage

The backend uses S3-compatible storage through `S3MediaStorageService`.

Local development uses RustFS from `backend/compose.yaml` with the `vocavista-media` bucket.

## AI Providers

OpenAI is used for word information generation through Spring AI.

ElevenLabs is used for pronunciation audio generation through Spring `RestClient`.

Provider failures are mapped to controlled backend errors rather than exposing raw provider responses.

Word-info responses are cached after successful provider generation and reused by normalized query to reduce repeat AI calls.

## Testing

Unit and MVC tests mock provider and storage boundaries.

The Spring context test uses Testcontainers with PostgreSQL so Flyway and JPA validation run against a real database engine.
