# Refactor Backend

- Issue: https://github.com/de1mos242/Vocavista/issues/62
- Goal: simplify generated-looking backend code without removing active product features.

## Scope

- Start with `backend/src/main/java/com/vocavista/backend/media/pronunciation` because it is the hardest backend area to read.
- Preserve API-first OpenAPI generation and generated controller DTOs.
- Preserve phrase images, image regeneration, video regeneration, and small pronunciation videos.
- Simplify regeneration to one current video and one current image per word info phrase.

## Decisions

- Do not keep media generation history.
- Regeneration requeues the existing asset row and replaces object keys only after new media is generated successfully.
- Existing media URLs remain available while an asset is queued, processing, or failed after a regeneration attempt.
- Removed `REJECTED`/`rejectedAt` flow from runtime code and replaced partial unique-index semantics with one asset row per word-info phrase.
- Avoided package moves in the first implementation slice so behavior changes remain reviewable.

## Progress

- Pronunciation and phrase image services now look up current assets by `wordInfoRecordId + normalizedPhrase`.
- Explicit regenerate endpoints requeue the same row instead of marking it rejected and creating a replacement row.
- Failed assets requeued through create/regenerate keep existing media object keys and clear errors.
- Added Flyway migration `V10__single_media_asset_per_phrase.sql` to delete obsolete rejected/history rows, drop rejected columns/indexes, and add unique phrase indexes.
- Updated media service tests for in-place regeneration semantics.
- Added generation failure coverage that verifies failed video regeneration keeps previous video keys.
- Collapsed one-line media records into nearby implementation files and removed the single-implementation phrase image scene-describer interface.
- Removed standalone files for `GeneratedVideo`, `GeneratedImage`, `StoredMedia`, `PronunciationScript`, `PhraseImagePrompt`, and `PhraseImageSceneDescriber`.
- Removed obsolete media `contentHash` runtime flow now that media is keyed by word-info phrase instead of provider/model cache keys.
- Dropped provider-client dependencies from `PronunciationService` and `PhraseImageService`; provider/model details are now only used by generation processors.
- Added Flyway migration `V11__drop_media_content_hashes.sql` to remove obsolete media hash columns.

## Verification

- `./mvnw -Dtest=PronunciationServiceTest,PhraseImageServiceTest,PronunciationGenerationProcessorTest,PhraseImageGenerationProcessorTest test` passed after the final media test updates.
- `./mvnw -Dtest=PronunciationServiceTest,PhraseImageServiceTest,PronunciationGenerationProcessorTest,PhraseImageGenerationProcessorTest,PronunciationVideoGeneratorTest,PhraseImageGeneratorTest,SpringAiPhraseImageSceneDescriberTest,PronunciationControllerTest test` passed after collapsing media support types.
- `./mvnw -Dtest=PronunciationServiceTest,PhraseImageServiceTest,PronunciationGenerationProcessorTest,PhraseImageGenerationProcessorTest,PronunciationVideoGeneratorTest,PhraseImageGeneratorTest,SpringAiPhraseImageSceneDescriberTest,PronunciationControllerTest,UserDictionaryServiceTest test` passed after removing media content hashes.
- `./mvnw test` compiled and ran non-container tests, but failed on `VocavistaBackendApplicationTests` and `AuthControllerTest` because Docker/Testcontainers is unavailable in this environment.
