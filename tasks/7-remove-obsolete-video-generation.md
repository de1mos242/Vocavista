# 7 Remove Obsolete Video Generation

## Goal

Remove obsolete D-ID and backend video-generation code now that pronunciation playback is moving to browser-side TalkingHead rendering.

## Scope

- Remove D-ID integration code, tests, and configuration.
- Remove optional backend MP4/video rendering paths.
- Keep backend audio generation and same-origin audio serving for TalkingHead playback.
- Update docs/examples to describe audio-backed TalkingHead only.

## Constraints

- No product feature changes beyond removing obsolete video-rendering options.
- Avoid broad API/database renames in this cleanup pass unless required for correctness.
- Do not remove persisted columns only to force a destructive migration; leave schema cleanup for a dedicated migration if needed.

## Implementation Notes

- The existing API still uses `pronunciation-videos` naming. This task removes backend video generation behavior but preserves the existing route shape to avoid a larger client/API migration.

## Decisions

- 2026-05-21: Remove D-ID and lip-sync provider abstractions from backend generation. TalkingHead rendering is browser-side, so the backend only needs to generate and serve audio.
- 2026-05-22: Follow-up cleanup can rename the route and internal model now because the only exposed consumer is the local TalkingHead preview.

## Progress

- 2026-05-21: Started cleanup from issue https://github.com/de1mos242/Vocavista/issues/7.
- 2026-05-21: Removed D-ID provider implementation, fake/configured lip-sync providers, generated-video model, playable public media URL support, and D-ID/local tunnel configuration.
- 2026-05-21: Simplified pronunciation media generation to create/store audio only and return the same-origin audio endpoint for browser-side TalkingHead playback.
- 2026-05-21: Updated OpenAPI response shape, tests, local examples, README, compose setup, and app specification to remove D-ID/video-rendering options.
- 2026-05-22: Continued cleanup in `tasks/7-pronunciation-audio-cleanup.md`.

## Verification

- 2026-05-21: `./mvnw -Dtest='com.vocavista.backend.media.pronunciation.*Test' test` passed.
- 2026-05-21: Added a test-only OpenAI API key so the full Spring Boot context can start without real credentials.
- 2026-05-21: `./mvnw test` passed after Docker was started.

## Related Files

- `backend/src/main/java/com/vocavista/backend/media/pronunciation/PronunciationVideoGenerationProcessor.java`
- `backend/src/main/java/com/vocavista/backend/media/pronunciation/PronunciationVideoService.java`
- `backend/src/main/resources/application.yaml`
- `backend/README.md`
