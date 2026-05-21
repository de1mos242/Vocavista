# 7 Pronunciation Audio Cleanup

## Goal

Finish the pronunciation media cleanup by keeping only current audio-backed TalkingHead behavior and removing code that only exists for obsolete video generation or future options.

## Scope

- Rename the pronunciation media API, schema, and backend implementation from video assets to pronunciation audio.
- Use `/api/v1/media/pronunciations` as the exposed API path for the current TalkingHead preview.
- Remove fake text-to-speech and fake storage production beans; use mocks in tests instead.
- Remove unused command-line generation runner, placeholder TTS provider, native-image setup, and Hibernate enhancer setup.
- Recreate the current database migration as audio-only because the database can be recreated now.
- Keep MapStruct and existing gitignore rules.

## Constraints

- No new product features.
- Preserve current browser-side TalkingHead behavior.
- Existing database state does not need migration compatibility for this cleanup pass.

## Implementation Notes

- The only exposed consumer of the pronunciation media API is the repository's test HTML page, so route and DTO names can change now.
- Real runtime pronunciation audio generation uses ElevenLabs plus S3-compatible storage.
- Tests should avoid production fake implementations and mock `TextToSpeechProvider` / `MediaStorageService` where appropriate.

## Decisions

- 2026-05-22: Use `/api/v1/media/pronunciations` instead of `/api/v1/media/pronunciation-videos`.
- 2026-05-22: Keep MapStruct and gitignore sections.
- 2026-05-22: Remove fake TTS/storage production code rather than keeping test-only provider modes.
- 2026-05-22: Recreate the DB schema directly because existing local DBs can be rebuilt.

## Progress

- 2026-05-22: Started cleanup from issue https://github.com/de1mos242/Vocavista/issues/7.
- 2026-05-22: Renamed pronunciation media API from `pronunciation-videos` to `/api/v1/media/pronunciations` and renamed backend implementation classes from `PronunciationVideo*` to `Pronunciation*` / `PronunciationAsset*`.
- 2026-05-22: Recreated the Flyway schema as audio-only `pronunciation_assets`, removing obsolete video columns now that the database can be rebuilt.
- 2026-05-22: Removed fake TTS/storage production beans, the placeholder configured TTS provider, the pronunciation command-line runner, native-image plugin setup, and Hibernate enhancer setup.
- 2026-05-22: Replaced fake-provider test usage with Mockito mocks and updated the TalkingHead preview to call `/api/v1/media/pronunciations`.
- 2026-05-22: Trimmed product and architecture docs to current backend features and current runtime/test shape.
- 2026-05-22: Removed short custom environment variable aliases from backend application config, local examples, and docs. Runtime overrides now use full Spring property names, while current pronunciation defaults live in code defaults instead of the main YAML file.
- 2026-05-22: Added `ElevenLabsProperties` for the `vocavista.media.elevenlabs` block and changed `ElevenLabsTextToSpeechProvider` to consume that object instead of individual `@Value` parameters.
- 2026-05-22: Replaced manual `modelName()` concatenation with `String.formatted(...)` for the ElevenLabs model identifier.

## Verification

- 2026-05-22: `./mvnw clean test` passed.
- 2026-05-22: `./mvnw test` passed after simplifying configuration defaults and examples.
- 2026-05-22: `./mvnw test` passed after introducing ElevenLabs configuration properties.
- 2026-05-22: `./mvnw -Dtest=ElevenLabsTextToSpeechProviderTest test` passed after simplifying `modelName()` formatting.

## Related Files

- `backend/src/main/resources/openapi/vocavista-api.yaml`
- `backend/src/main/java/com/vocavista/backend/media/pronunciation/`
- `backend/src/main/resources/db/migration/`
- `backend/src/main/resources/static/talking-head.html`
