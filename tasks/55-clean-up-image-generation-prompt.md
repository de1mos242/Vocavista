# Clean Up Image Generation Prompt

- Issue: https://github.com/de1mos242/Vocavista/issues/55
- Branch: `55-clean-up-image-generation-prompt`

## Goal

Remove negative command wording from the phrase image generation prompt because Gemini/Imagen can treat those words as content to render, producing unwanted visible text artifacts.

## Scope

- Rewrite the phrase image prompt to describe only desired visual output.
- Remove instructions that mention labels, captions, visible text, watermarks, malformed output, or other forbidden elements.
- Bump prompt metadata so new generations are separated from prior cached prompt text.
- Add focused test coverage for the cleaned prompt.

## Implementation Notes

- Keep the existing Imagen integration and phrase image workflow unchanged.
- Keep the prompt grounded in the German phrase and target vocabulary concept while avoiding negative commands.

## Decisions

- Use positive-only visual guidance and avoid referencing text artifacts directly in the generation prompt.
- Move prompt metadata to `prompt-v2` to distinguish new output and cache keys from the old prompt.

## Progress

- Rewrote the phrase image prompt to use positive-only visual guidance.
- Removed negative command wording and references to known text-artifact terms from the generated prompt.
- Bumped prompt metadata to `prompt-v2`/`v2` so new generations are separated from previous cache keys.
- Added test coverage for the cleaned prompt language.

## Verification

- `cd backend && ./mvnw test` passed with 70 tests.

## Open Questions

- None blocking implementation or PR creation.
