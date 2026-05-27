# Issue 13: Generate Lip Sync Video Directly with Veo 3.1 Lite

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/13
- Branch: `13-generate-lip-sync-video-directly-with-veo-3-1-lite`

## Goal

Generate pronunciation lip-sync video directly with Veo 3.1 Lite instead of making OpenAI TTS audio first and animating it in browser-side TalkingHead.

## Scope

- Make direct Veo video generation the default pronunciation media path.
- Keep the existing OpenAI TTS plus TalkingHead flow available as an explicit nearby option.
- Store and serve generated MP4 video through the existing media storage service.
- Preserve the current pronunciation API path and asynchronous asset lifecycle.

## Implementation Notes

- Use `veo-video` and `talking-head` render modes on pronunciation requests and responses.
- Map the requested Veo 3.1 Lite path to the configurable Gemini API model default `veo-3.1-fast-generate-preview`.
- Keep OpenAI TTS generation in `PronunciationAudioGenerator` for `talking-head` render mode only.

## Progress

- 2026-05-26: Created issue and task worktree. Inspected current pronunciation flow: backend generates OpenAI TTS audio, stores it, and `talking-head.html` animates a browser avatar from the returned `audioUrl`.
- 2026-05-26: Added direct Veo video generation as the default `veo-video` render mode. The backend now starts and polls Gemini/Veo long-running video operations, downloads the generated MP4, stores it through `MediaStorageService`, and returns a same-origin `videoUrl`.
- 2026-05-26: Kept the OpenAI TTS plus TalkingHead flow as `renderMode: "talking-head"`. The pronunciation API accepts optional `renderMode`, cache keys include render mode/provider/model, and the existing audio endpoint remains available.
- 2026-05-26: Updated the manual preview page with a render-mode selector and video playback, added `GOOGLE_AI_API_KEY` configuration, updated docs, and added Flyway columns for video metadata.
- 2026-05-27: Added a dedicated `/veo-video.html` page for the new flow. It searches word info, lets the user choose an example phrase, queues `renderMode: "veo-video"`, polls for the longer video generation window, and plays the returned `videoUrl`. The TalkingHead page now links to this new page.
- 2026-05-27: Removed the default `personGeneration: allow_adult` setting because it is not currently supported by the selected Veo API/model. `personGeneration` is now omitted by default and only sent when explicitly configured.
- 2026-05-27: Changed Veo output to vertical `9:16`. Pronunciation scripts now read linked word-info metadata so noun second repetitions include the article, for example `Zugabe ... die Zugabe ...`, and the Veo prompt asks for male/female/gender-neutral speaker presentation based on noun gender.
- 2026-05-27: Tightened the Veo prompt to require 0.5 seconds of silence with a closed mouth before speech and 0.5 seconds of silence with a closed mouth after the final word. Also explicitly forbids any spoken words before or after the quoted German script.

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
