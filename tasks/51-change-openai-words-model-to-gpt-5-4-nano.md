# Issue 51: Change OpenAI Words Model to gpt-5.4-mini

- Issue: https://github.com/de1mos242/Vocavista/issues/51
- Pull request: https://github.com/de1mos242/Vocavista/pull/52
- Branch: `51-change-openai-words-model-to-gpt-5-4-nano`

## Goal

Update the OpenAI model used by words-related metadata generation to `gpt-5.4-mini`.

## Scope

- Change the default model used by `SpringAiOpenAiWordInfoProvider` when `spring.ai.openai.chat.model` is not explicitly configured.
- Keep the existing `spring.ai.openai.chat.model` override behavior.
- Update provider tests to use and assert the new words model.

## Implementation Notes

- The word-info provider sets model-specific `OpenAiChatOptions` per prompt, so the default comes from the constructor `@Value` fallback.
- Pronunciation media generation uses separate Veo/audio paths and is out of scope.

## Progress

- Created the issue/task branch.
- Updated the word-info OpenAI model fallback to `gpt-5.4-mini`.
- Updated tests to use the new model and assert it is attached to provider prompt options.
- Opened PR https://github.com/de1mos242/Vocavista/pull/52.
- Changed the selected model from `gpt-5.4-nano` to `gpt-5.4-mini` after follow-up direction.

## Verification

- `./mvnw -Dtest=SpringAiOpenAiWordInfoProviderTest test` passed.
