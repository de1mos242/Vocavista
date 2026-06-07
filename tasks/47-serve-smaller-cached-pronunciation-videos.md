# Serve Smaller Cached Pronunciation Videos

- Issue: https://github.com/de1mos242/Vocavista/issues/47
- Goal: keep Veo generation on the cheaper 720p tier, create smaller compressed pronunciation video derivatives, serve them by default, and proactively cache the user's dictionary videos in the web app.
- Scope: backend media generation/storage/API, dictionary video manifest, PWA/browser cache sync, review playback URL handling.
- Constraints: keep the full video URL available; current app is web/PWA only; avoid breaking existing completed assets that only have the original video object.

## Implementation Notes

- Existing Veo config already uses `veo-3.1-lite-generate-preview` and vertical `9:16`, so generation stays on the Lite/720p path.
- Added explicit `resolution: 720p` to the Veo request parameters and model cache key.
- Smaller videos are stored beside the original as `pronunciations/{assetId}/video-small.mp4` when FFmpeg produces a smaller MP4.
- Existing completed assets without `video-small.mp4` are compressed and stored on the first small-video request, so cache preload can backfill older videos without a separate migration job.
- Generation prompt was simplified, speaker descriptions now request German speakers, and Veo duration is aligned to 5 seconds in defaults and runtime config.
- FFmpeg settings keep the same source dimensions while lowering H.264 quality/bitrate (`crf 30`, `maxrate 650k`, `64k` AAC audio, `faststart`).
- Compression failure or a larger compressed output does not fail generation; the small-video endpoint falls back to the original object for existing or uncompressed assets.
- Normal playback uses `/api/v1/media/pronunciations/{id}/video/small`; full playback remains available through `/api/v1/media/pronunciations/{id}/video`.
- Added `/api/v1/dictionary/videos` manifest with pronunciation asset ID, word info ID, small URL, full URL, and `updatedAt`.
- Browser cache sync runs after a signed-in functional user loads the app: it downloads missing/stale small videos into the `pronunciation-videos` cache using a versioned refresh URL, stores them under the clean playback URL, and deletes small videos no longer present in the server manifest.
- Review playback now uses direct small-video URLs instead of fetching blobs, allowing the Cache API/Workbox cache to serve playback.
- Backend Docker runtime now installs FFmpeg.
- Add Word page starts with empty word and phrase fields and supports pressing Enter to search word info.

## Progress

- Created task checkout and issue.
- Implemented backend small-video storage/API and dictionary manifest.
- Implemented frontend manifest sync and review playback changes.
- Regenerated frontend OpenAPI SDK/types.
- Added Add Word input usability tweak: empty initial word/phrase and Enter-to-search.
- Updated prompt-related tests for simplified prompt text, German speaker descriptions, and 5-second Veo duration.

## Verification

- Passed: `npm run build` in `frontend`.
- Passed: `./mvnw -Dtest=PronunciationControllerTest,PronunciationServiceTest,PronunciationVideoGeneratorTest,PronunciationGenerationProcessorTest,DictionaryControllerTest,UserDictionaryServiceTest test` in `backend`.
- Full `./mvnw test` was attempted but blocked by missing Docker/Testcontainers in this environment (`Could not find a valid Docker environment`).
- Passed after prompt changes: `./mvnw -Dtest=PronunciationGenerationProcessorTest,PronunciationVideoGeneratorTest test` in `backend`.
