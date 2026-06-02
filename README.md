# Vocavista

Vocavista is a language-learning application focused on German vocabulary practice with AI-generated word metadata and generated pronunciation media.

## Current Features

- `GET /api/v1/words/info` returns compact German word information for learners who know English and Russian.
- Generated word information is cached in PostgreSQL so repeated requests can avoid AI calls.
- `GET /api/v1/words/suggestions` searches cached word info and generated pronunciations for autocomplete.
- `POST /api/v1/media/pronunciations` queues or reuses generated pronunciation media for a German word and phrase.
- Generated pronunciation video is stored in S3-compatible storage and served back through a same-origin `videoUrl` for browser playback.
- A mobile-first React PWA provides add-word, review, and admin flows from the same Spring Boot origin.

## Current Stack

- Backend: Spring Boot 4 on Java 25.
- Frontend: React, TypeScript, Vite, generated OpenAPI client, and PWA service worker.
- Database: PostgreSQL with Flyway migrations.
- Storage: S3-compatible object storage, using RustFS locally.
- AI providers: OpenAI for word metadata, Google Veo for direct pronunciation video.

## Documentation

- [`documentation/app-specification.md`](documentation/app-specification.md)
- [`documentation/technical-architecture.md`](documentation/technical-architecture.md)
- [`documentation/fly-deployment.md`](documentation/fly-deployment.md)
