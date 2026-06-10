# Prevent Text in Generated Phrase Images

- Issue: https://github.com/de1mos242/Vocavista/issues/59
- Pull Request: https://github.com/de1mos242/Vocavista/pull/60
- Branch: `59-prevent-text-in-generated-phrase-images`

## Goal

Stop generated phrase images from containing visible text by ensuring the prompt sent to Imagen does not include the literal target word or phrase.

## Scope

- Remove dynamic vocabulary and phrase text from the image-provider prompt body.
- Keep word and phrase metadata for persistence, responses, and cache hashing.
- Bump prompt metadata so new generations bypass earlier prompt versions.
- Add tests that prove the provider prompt does not contain the literal German word or sentence.

## Implementation Notes

- The prior positive-only prompt still included `Phrase context` and `Vocabulary concept` with literal user text; Gemini/Imagen can copy that into images.
- This change trades semantic specificity for preventing rendered text. A later improvement can add a separate visual-scene-description step if stronger semantic matching is needed without exposing raw words to Imagen.

## Decisions

- Keep the Imagen request prompt static and visual-only.
- Move phrase image prompt metadata to `prompt-v3`/`v3`.

## Progress

- Removed literal target word and phrase text from the prompt body sent to Imagen.
- Kept word and phrase values in `PhraseImagePrompt` metadata, persistence, and cache hashing inputs.
- Bumped prompt metadata to `prompt-v3`/`v3` so new image requests bypass earlier cached prompt versions.
- Added focused test coverage proving `prompt.text()` and the Imagen request body omit the literal German word and phrase.
- Opened PR #60 and updated issue #59 with final implementation status.

## Verification

- `cd backend && ./mvnw test` passed with 70 tests.

## Open Questions

- None blocking implementation or PR creation.
