# App Specification: Vocavista

## Current Product Shape

Vocavista teaches German vocabulary through compact AI-generated word metadata and pronunciation practice.

The current backend supports:

- German word information for learners who know English and Russian.
- Pronunciation audio generation for a target word and contextual German phrase.
- Browser-side TalkingHead playback using generated audio; the backend does not generate video.
- Central media reuse by caching generated pronunciation audio metadata in PostgreSQL and storing audio in S3-compatible object storage.

## Word Information

`GET /api/v1/words/info?word=Hausaufgabe` returns:

- Normalized German word.
- English and Russian translations.
- Part of speech and frequency.
- German noun fields when relevant: gender, article, and plural.
- Compound-part meanings when relevant.
- Three German example sentences with English and Russian translations.

## Pronunciation Media

`POST /api/v1/media/pronunciations` accepts a German `word`, `phrase`, and `language` value of `de`.

Generation behavior:

- The word is spoken twice with different pacing.
- The contextual phrase is spoken after the repeated word.
- ElevenLabs generates the audio.
- S3-compatible storage keeps the generated audio object.
- The API returns `audioUrl` for same-origin browser playback when generation completes.

## Current Technical Shape

- Backend: Java 25 with Spring Boot 4.
- API contract: OpenAPI-generated Spring MVC interfaces and DTOs.
- Database: PostgreSQL with Flyway.
- Storage: S3-compatible media storage, RustFS locally.
- Tests: JUnit, Mockito, and Testcontainers for PostgreSQL-backed context testing.
