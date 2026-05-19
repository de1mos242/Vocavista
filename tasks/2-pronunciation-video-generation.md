# Issue 2: Pronunciation Video Generation

## Goal

Add the first reusable media-generation feature for German word and phrase pronunciation videos.

## Scope

- Add OpenAPI contract endpoints for creating and reading pronunciation video generations.
- Persist generation metadata and cache keys in PostgreSQL.
- Normalize and validate `word`, `phrase`, and `language` inputs.
- Generate a stable cache hash for reusable assets.
- Add provider interfaces for text-to-speech, lip-sync video, and media storage.
- Provide deterministic fake providers for automated tests and local development.
- Add S3-compatible storage wiring for local RustFS when explicitly enabled.

## Decisions

- V1 accepts only German (`de`) while still exposing `language` in the API.
- V1 limits `word` to 80 characters and `phrase` to 240 characters.
- Fake media providers are the default so tests do not call external services.
- S3-compatible storage is opt-in through configuration; fake storage remains default for tests.
- Real AI provider integrations remain behind interfaces and explicit configuration.

## Progress

- 2026-05-16: Started implementation on branch `2-plan-pronunciation-video-generation-for-words-and-phrases`.
- 2026-05-16: Added OpenAPI contract for `POST /api/v1/media/pronunciation-videos` and `GET /api/v1/media/pronunciation-videos/{id}`.
- 2026-05-16: Added `pronunciation_video_assets` migration, JPA entity/repository, cache hash reuse, validation, and queued async processing.
- 2026-05-16: Added fake text-to-speech, fake lip-sync, fake storage, optional S3-compatible storage wiring, and real-provider placeholder beans that fail with controlled metadata until real integrations are implemented.
- 2026-05-16: Added controller, service, and generation processor tests.
- 2026-05-19: Added a command-line runner for pronunciation-video generation that can run with `spring.main.web-application-type=none`, wait for terminal status, print the result, and exit without starting the web API.
- 2026-05-19: Added gitignored `backend/src/main/resources/application-local.yaml` local-profile config path, plus committed `backend/src/main/resources/application-local.example.yaml` for local command-runner overrides.
- 2026-05-19: Fixed command runner bean construction by marking the application constructor for Spring autowiring.
- 2026-05-19: Fixed S3 storage bean construction by marking the application constructor for Spring autowiring.

## Verification

- `./mvnw -Djava.version=21 test` passed locally with 25 tests.
- `./mvnw test` is blocked in this environment because the active JDK is 21 and the project is configured for Java 25 (`release version 25 not supported`).
- 2026-05-19: `./mvnw test` passed with Java 25 after adding the command runner; 26 tests passed.
- 2026-05-19: `./mvnw test` passed after switching local config to standard Spring `local` profile loading; 26 tests passed.
- 2026-05-19: `./mvnw test` passed after fixing command runner constructor autowiring; 26 tests passed.
- 2026-05-19: `./mvnw test` passed after fixing S3 storage constructor autowiring; 26 tests passed.

## Links

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/2
