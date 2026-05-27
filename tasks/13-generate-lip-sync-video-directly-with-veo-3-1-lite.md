# Issue 13: Generate Lip Sync Video Directly with Veo 3.1 Lite

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/13
- Branch: `13-generate-lip-sync-video-directly-with-veo-3-1-lite`

## Goal

Generate pronunciation lip-sync video directly with Veo 3.1 Lite instead of making OpenAI TTS audio first and animating it in browser-side TalkingHead.

## Scope

- Make direct Veo video generation the default pronunciation media path.
- Remove the old OpenAI TTS plus TalkingHead path once direct Veo video is in place.
- Store and serve generated MP4 video through the existing media storage service.
- Preserve the current pronunciation API path and asynchronous asset lifecycle.

## Implementation Notes

- Keep one pronunciation generation path: direct Veo video.
- Map the requested Veo 3.1 Lite path to the configurable Gemini API model default `veo-3.1-lite-generate-preview`.

## Progress

- 2026-05-26: Created issue and task worktree. Inspected current pronunciation flow: backend generates OpenAI TTS audio, stores it, and `talking-head.html` animates a browser avatar from the returned `audioUrl`.
- 2026-05-26: Added direct Veo video generation. The backend starts and polls Gemini/Veo long-running video operations, downloads the generated MP4, stores it through `MediaStorageService`, and returns a same-origin `videoUrl`.
- 2026-05-26: Added `GOOGLE_AI_API_KEY` configuration, updated docs, and added Flyway columns for video metadata.
- 2026-05-27: Added a dedicated `/veo-video.html` page for the new flow. It searches word info, lets the user choose an example phrase, queues pronunciation video generation, polls for the longer video generation window, and plays the returned `videoUrl`.
- 2026-05-27: Removed the default `personGeneration: allow_adult` setting because it is not currently supported by the selected Veo API/model. `personGeneration` is now omitted by default and only sent when explicitly configured.
- 2026-05-27: Changed Veo output to vertical `9:16`. Pronunciation scripts now read linked word-info metadata so noun second repetitions include the article, for example `Zugabe ... die Zugabe ...`, and the Veo prompt asks for male/female/gender-neutral speaker presentation based on noun gender.
- 2026-05-27: Tightened the Veo prompt to require 0.5 seconds of silence with a closed mouth before speech and 0.5 seconds of silence with a closed mouth after the final word. Also explicitly forbids any spoken words before or after the quoted German script.
- 2026-05-27: Cleaned up the implementation to remove the old OpenAI TTS plus TalkingHead path. The API no longer accepts `renderMode`, responses and suggestions no longer expose `audioUrl` or `renderMode`, `/talking-head.html` and `/audio` serving were removed, and the database migration now drops obsolete audio metadata columns while adding video metadata.

## Verification

- 2026-05-26: `./mvnw -Dtest=PronunciationVideoGeneratorTest,PronunciationGenerationProcessorTest,PronunciationServiceTest,PronunciationControllerTest test` passed; 16 focused tests.
- 2026-05-26: `./mvnw test` ran 37 tests with 36 passing and 1 environment failure: `VocavistaBackendApplicationTests.contextLoads` could not start Testcontainers because Docker is unavailable.
- 2026-05-27: `git diff --check` passed after adding `/veo-video.html`.
- 2026-05-27: `./mvnw -DskipTests compile` passed after adding `/veo-video.html`; Maven copied the new static resource and compiled backend sources.
- 2026-05-27: `./mvnw -Dtest=PronunciationVideoGeneratorTest test` passed after making `personGeneration` opt-in.
- 2026-05-27: `./mvnw -Dtest=PronunciationVideoGeneratorTest,PronunciationGenerationProcessorTest,PronunciationAudioGeneratorTest,PronunciationServiceTest test` passed after adding vertical output, noun article repetition, and gender-aware speaker prompting.
- 2026-05-27: `./mvnw -Dtest=PronunciationVideoGeneratorTest test` passed after adding closed-mouth start/end silence instructions.
- 2026-05-27: `git diff --check` passed before commit.
- 2026-05-27: `./mvnw -Dtest=PronunciationVideoGeneratorTest,PronunciationGenerationProcessorTest,PronunciationAudioGeneratorTest,PronunciationServiceTest,PronunciationControllerTest test` passed before commit; 19 focused tests.
- 2026-05-27: `git diff --check` passed after removing the TalkingHead/OpenAI TTS pronunciation path.
- 2026-05-27: `./mvnw -Dtest=PronunciationVideoGeneratorTest,PronunciationGenerationProcessorTest,PronunciationServiceTest,PronunciationControllerTest test` passed after cleanup; 13 focused tests.
