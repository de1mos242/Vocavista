# Vocavista

Vocavista is a language-learning application focused on German vocabulary practice with AI-generated word metadata and audio-backed pronunciation playback.

## Current Features

- `GET /api/v1/words/info` returns compact German word information for learners who know English and Russian.
- Generated word information is cached in PostgreSQL so repeated requests can avoid AI calls.
- `GET /api/v1/words/suggestions` searches cached word info and generated pronunciations for autocomplete.
- `POST /api/v1/media/pronunciations` queues or reuses generated pronunciation audio for a German word and phrase.
- Generated pronunciation audio is stored in S3-compatible storage and served back through a same-origin `audioUrl` for browser playback.
- `backend/src/main/resources/static/talking-head.html` is the current manual TalkingHead UI for searching words, selecting phrases, generating audio, and playing it with the avatar.

## Current Stack

- Backend: Spring Boot 4 on Java 25.
- Database: PostgreSQL with Flyway migrations.
- Storage: S3-compatible object storage, using RustFS locally.
- AI providers: OpenAI for word metadata and ElevenLabs for pronunciation audio.

## Documentation

- [`documentation/app-specification.md`](documentation/app-specification.md)
- [`documentation/technical-architecture.md`](documentation/technical-architecture.md)
