# Issue 2: Pronunciation Video Generation

## Goal

Add the first reusable media-generation feature for German word and phrase pronunciation videos.

## Scope

- Add OpenAPI contract endpoints for creating and reading pronunciation video generations.
- Persist generation metadata and cache keys in PostgreSQL.
- Normalize and validate `word`, `phrase`, and `language` inputs.
- Generate a stable cache hash for reusable assets.
- Add provider interfaces for text-to-speech, lip-sync video, and media storage.
- Provide deterministic fake providers for automated tests.
- Add S3-compatible storage wiring for local RustFS.

## Decisions

- V1 accepts only German (`de`) while still exposing `language` in the API.
- V1 limits `word` to 80 characters and `phrase` to 240 characters.
- Local runtime defaults use real ElevenLabs TTS, local RustFS storage, and browser-side TalkingHead rendering.
- Fake media providers and fake storage remain the test defaults through `src/test/resources/application.yaml` so tests do not call external services.
- D-ID MP4 generation remains opt-in through `VOCAVISTA_MEDIA_RENDER_MODE=video` and D-ID provider configuration.

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
- 2026-05-19: Added real ElevenLabs text-to-speech provider and D-ID talking-head/lip-sync provider for `provider-mode=real`, including D-ID polling and completed MP4 download into configured media storage.
- 2026-05-19: Changed the generated TTS script template to a literal spoken sequence (`word`, paused repeat with different punctuation, then phrase) so real TTS does not read prompt instructions aloud.
- 2026-05-19: Documented real-provider setup and the requirement that D-ID must be able to fetch the stored audio URL from public/tunneled object storage.
- 2026-05-19: Changed cache reuse behavior for failed pronunciation-video assets so a repeated create request clears stale failure/media metadata, requeues the same asset, and starts generation again instead of returning the cached failure.
- 2026-05-19: Changed ElevenLabs generation to use three segment-level TTS calls with separate speeds: slow first isolated word, faster second isolated word, and medium-speed phrase. Bumped the script template version so previously generated fast assets are not reused.
- 2026-05-19: Added a `cloudflared` Docker Compose service for local D-ID testing, exposing RustFS over a temporary HTTPS tunnel and applying an anonymous object-read policy to the local media bucket.
- 2026-05-20: Checked D-ID avatar/source-image options. The current Talks provider uses `source_url` and can animate any public `jpg/png`; D-ID's cataloged V3/V4 avatars are available through authenticated presenter/avatar endpoints and use separate IDs from the current Talks config.
- 2026-05-20: Checked ElevenLabs voice options for grammatical-gender-specific pronunciation videos. The native German Voice Library voices with `free_users_allowed=true` look more suitable than the default premade English-accent multilingual voices. D-ID voices remain a simpler single-provider option, but would require switching from uploaded audio to D-ID text-script TTS and would reduce the current per-segment speed control.
- 2026-05-20: Switched the default pronunciation media rendering approach from generated MP4 video to browser-side TalkingHead rendering. The backend now stores generated TTS audio, returns `audioUrl`, and skips D-ID unless `VOCAVISTA_MEDIA_RENDER_MODE=video` is explicitly set. Added `/talking-head.html` as a minimal Spring-served preview page that loads TalkingHead/HeadAudio from CDNs and animates a browser avatar from the generated audio.
- 2026-05-20: Changed local runtime defaults to real ElevenLabs TTS plus local RustFS storage, keeping only the ElevenLabs API key required for a normal local TalkingHead preview run. Added test-resource overrides so automated tests continue using fake providers/storage.
- 2026-05-20: Fixed browser TalkingHead playback for RustFS-backed audio by returning a same-origin backend audio URL in `talking-head` mode and adding `GET /api/v1/media/pronunciation-videos/{id}/audio` to stream generated audio from storage. This avoids WebAudio `fetch` failures caused by `localhost:8080` to `localhost:9000` CORS differences.
- 2026-05-20: Added a preview avatar selector using the current TalkingHead `main` avatar assets (`brunette`, `brunette-t`, `avatarsdk`, `avaturn`, `mpfb`, `vroid`) instead of only the older release-bundled `brunette.glb` asset. Kept the TalkingHead JS module pinned while using newer avatar GLBs.

## Voice Candidate Notes

Default ElevenLabs `premade` voices can be used directly by `voice-id`, but most are English-accent voices that only support German through `eleven_multilingual_v2`. Voice Library voices with `free_users_allowed=true` sound like better candidates for German pronunciation; they may need to be added to the workspace before using the `voice_id` in generation.

| Role | Voice ID | Name | Description |
| --- | --- | --- | --- |
| Masculine nouns / Adam | `aYjXhF7kZXskZc5G6PV2` | Philipp - Clear German Male | Clear, neutral German male voice with a calm and balanced tone. Suitable for explainers, e-learning, voice assistants; clear articulation and steady pacing. |
| Masculine fallback / Adam | `KDqku3FJfbImX6HKQdWA` | Daniel - Calm German Storyteller | Warm, trustworthy, calm German male voice with natural storytelling tone; friendly and empathetic. |
| Masculine younger option / Adam | `8aPaMtDocayOBFDFyWHp` | Markus - Deep German Soul | Calm young German male voice for social media, news, explainers, storytelling, and ads. |
| Feminine nouns / Alyssa or Amber | `E13qNLHLLuVPKQvesCoy` | Clara - Warm, clear & Calm | Warm lower female voice, calm, clear, confident; natural conversational delivery for e-learning, audiobooks, and documentary-style voiceovers. |
| Feminine fallback / Alyssa or Amber | `iFSsEDGbm0FiEd2IVH4w` | Mary K. - Clear & Captivating | Clear standard German female voice; friendly, engaging, and suitable for narratives, e-learning, podcasts, and broadcasts. |
| Feminine conversational / Alyssa or Amber | `wUrj2qIfmBZAz9x7k6yN` | Rebecca Green - Conversational and Clear | Calm, pleasant German female voice with clear articulation; good for voice agents and educational narration. |
| Neuter nouns | `pzRegoXBZ4b8y5gN3hCp` | Lukas Brandt - Corporate Narration | Neutral, professional German voice for e-learning and instructional content; clear, steady, deliberately non-performative delivery. |
| Neuter fallback | `TUKJhQmz3RPYBNAgC5A1` | German Narration - Helmut Clark | Clear and natural German narration voice; calm, neutral, trustworthy, with steady pacing and professional tone. |
| Non-nouns / general narrator | `r8MyP4qUsq5WFFSkPdfV` | Johannes - Clear and Neutral | Middle-aged German male voice with clear pronunciation and neutral intonation; suitable for narration. |
| Truly gender-neutral, but stylized | `Ewvy14akxdhONg4fmNry` | Finnegan - Children's Story Actress | German gender-neutral voice for children and teen stories with a touch of huskiness; more character-like than pronunciation-neutral. |
| Premade neutral fallback | `SAz9YHcvj6GT2YYXdXww` | River - Relaxed, Neutral, Informative | Default premade neutral voice for narrations/conversation; not explicitly verified for `de-DE` in the catalog, so pronunciation quality should be tested before use. |

## Verification

- `./mvnw -Djava.version=21 test` passed locally with 25 tests.
- `./mvnw test` is blocked in this environment because the active JDK is 21 and the project is configured for Java 25 (`release version 25 not supported`).
- 2026-05-19: `./mvnw test` passed with Java 25 after adding the command runner; 26 tests passed.
- 2026-05-19: `./mvnw test` passed after switching local config to standard Spring `local` profile loading; 26 tests passed.
- 2026-05-19: `./mvnw test` passed after fixing command runner constructor autowiring; 26 tests passed.
- 2026-05-19: `./mvnw test` passed after fixing S3 storage constructor autowiring; 26 tests passed.
- 2026-05-19: `./mvnw test` passed after adding ElevenLabs and D-ID provider integrations; 28 tests passed.
- 2026-05-19: `./mvnw test` passed after adding failed-asset retry behavior; 29 tests passed.
- 2026-05-19: `./mvnw test` passed after adding segment-level pronunciation speeds; 29 tests passed.
- 2026-05-19: `docker compose config` passed after adding the RustFS tunnel service.
- 2026-05-20: `./mvnw clean test -Dtest=PronunciationVideoGenerationProcessorTest,PronunciationVideoServiceTest,PronunciationVideoControllerTest,PronunciationVideoCommandRunnerTest` passed after changing runtime defaults; 11 focused tests passed. `./mvnw -DskipTests package` passed.
- 2026-05-20: `./mvnw clean test -Dtest=PronunciationVideoGenerationProcessorTest,PronunciationVideoServiceTest,PronunciationVideoControllerTest,PronunciationVideoCommandRunnerTest` passed after adding same-origin audio streaming; 12 focused tests passed. `./mvnw -DskipTests package` passed.
- 2026-05-20: `./mvnw -DskipTests package` passed after adding the preview avatar selector.

## Links

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/2
