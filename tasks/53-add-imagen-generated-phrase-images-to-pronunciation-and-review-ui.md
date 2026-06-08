# Add Imagen-Generated Phrase Images to Pronunciation and Review UI

- Issue: https://github.com/de1mos242/Vocavista/issues/53
- Branch: `53-add-imagen-generated-phrase-images-to-pronunciation-and-review-ui`

## Goal

Generate a high-quality contextual image for each selected phrase and target word, cache it, show it near pronunciation media, and show it before answers during review.

## Scope

- Use Google Imagen 4 by default, not a cheaper or fast model.
- Use cinematic 16:9 images with a compact UI display around mobile-friendly dimensions.
- Cache by phrase, target word, model, and prompt/style version.
- Allow regeneration for bad images without reusing rejected results.
- Keep pronunciation and review usable if image generation fails.

## Implementation Notes

- Reuse the existing generated media storage pattern.
- Keep image generation independent from pronunciation video generation so failures do not block video or review flows.
- Expose image status and binary streaming through the OpenAPI contract so the frontend generated client remains the integration point.

## Progress

- Added persistent phrase image assets with cached active entries and rejected regenerated entries.
- Added Google Imagen 4 generation through the existing Google AI API key, using cinematic `16:9` output and `1K` sample image size.
- Added OpenAPI-backed endpoints to create/reuse, poll, stream, and regenerate phrase images.
- Added review item phrase/image metadata so cached images can be shown before review answers.
- Added frontend image generation near pronunciation video generation and review-time image display before the answer field.
- Added regeneration controls in both add-word and review UI.
- Image failures stay isolated from pronunciation and review flows.
- Changed add-word UX to a two-step asset workflow: choose phrase, generate both image and video together, regenerate either preview individually, then explicitly save to the revise list.
- Media generation no longer adds dictionary entries automatically; saving uses a dedicated dictionary entry endpoint.
- Pronunciation videos now support rejection/regeneration like phrase images, with rejected videos excluded from cache reuse.
- Fixed word-info provider robustness for nouns where the model returns `gender` but `article: null`; the prompt now explicitly says noun article must not be null, and the backend derives the nominative article from gender before validation.
- Fixed phrase image and pronunciation regeneration startup so async generation starts only after the queued replacement asset transaction commits. This prevents the processor from failing to find a freshly queued regenerated asset.

## Verification

- `cd frontend && npm run build` passed.
- `cd backend && ./mvnw test` passed with 66 tests after the workflow refactor.
- `cd backend && ./mvnw test` passed with 67 tests after the missing noun article fix.
- `cd backend && ./mvnw -Dtest=PhraseImageServiceTest,PronunciationServiceTest test` passed with 10 tests after the regeneration transaction fix.
- `cd backend && ./mvnw test` passed with 69 tests after the regeneration transaction fix.
