# Prevent Text in Generated Phrase Images

- Issue: https://github.com/de1mos242/Vocavista/issues/59
- Pull Request: https://github.com/de1mos242/Vocavista/pull/60
- Branch: `59-prevent-text-in-generated-phrase-images`

## Goal

Stop generated phrase images from containing visible text while preserving semantic relevance through a text-only scene-description pre-step.

## Scope

- Generate a visual scene description from the vocabulary word and phrase before calling Imagen.
- Remove dynamic vocabulary and phrase text from the Imagen prompt body.
- Keep word and phrase metadata for persistence, responses, and cache hashing.
- Bump prompt metadata so new generations bypass earlier prompt versions.
- Add tests that prove the Imagen prompt does not contain the literal German word or sentence.

## Implementation Notes

- The prior positive-only prompt still included `Phrase context` and `Vocabulary concept` with literal user text; Gemini/Imagen can copy that into images.
- Use OpenAI chat with the same model configuration as word info (`spring.ai.openai.chat.model`, default `gpt-5.4-mini`) to convert the word and phrase into an English visual scene description.
- Keep the final Imagen wrapper minimal so it does not add composition details that may conflict with the generated scene description.

## Decisions

- Keep German word and phrase input in the OpenAI scene-description step, not the Imagen request.
- Move phrase image prompt metadata to `prompt-v4`/`v4`.

## Progress

- Added a `PhraseImageSceneDescriber` pre-step backed by OpenAI chat.
- OpenAI receives the target word and phrase and returns a concise English visual scene description.
- Removed literal target word and phrase text from the final prompt body sent to Imagen.
- Simplified the final Imagen prompt to a minimal high-quality 16:9 image wrapper around the scene description.
- Kept word and phrase values in `PhraseImagePrompt` metadata, persistence, and cache hashing inputs.
- Bumped prompt metadata to `prompt-v4`/`v4` so new image requests bypass earlier cached prompt versions.
- Added focused test coverage for the scene-description pre-step and final Imagen prompt shape.
- Updated PR #60 after correcting the generic-prompt stopgap into the scene-description design.

## Verification

- `cd backend && ./mvnw -Dtest=PhraseImageGenerationProcessorTest,PhraseImageGeneratorTest,SpringAiPhraseImageSceneDescriberTest test` passed with 6 tests.
- `cd backend && ./mvnw test` passed with 73 tests.
- GitHub PR CI `Build` passed after the scene-description follow-up commit.

## Open Questions

- None blocking implementation or PR creation.
