# Issue 4: Constrain AI Word Info Enum Values

## Goal

Prevent real word info requests from failing when the AI provider returns natural-language enum values that do not match the backend API contract.

## Related Issue

- GitHub: https://github.com/de1mos242/Vocavista/issues/4

## Scope

- Inspect generated API enum values and current provider validation.
- Constrain the OpenAI structured output schema for enum-like provider fields.
- Clarify the prompt so the model uses exact backend enum values.
- Add regression coverage for enum constraints in the schema sent through `OpenAiChatOptions`.

## Constraints

- Keep the public API contract unchanged.
- Keep existing validation and sanitized provider error mapping.
- Do not loosen validation to silently accept arbitrary provider values unless there is a clear product reason.
- Do not modify unrelated worktree changes.

## Implementation Notes

- Reported runtime error: `AiProviderBadGatewayException: AI provider returned unsupported enum value for frequency` caused by generated `WordFrequency.fromValue("high")`.
- Existing provider schema normalization for localized text maps lives in `SpringAiOpenAiWordInfoProvider`.

## Decisions

- Keep validation strict; do not accept or remap arbitrary provider enum values like `high` after the fact.
- Constrain the OpenAI response schema so enum-like fields can only contain API-supported values.
- Add prompt text that lists exact enum values to reduce provider retries/failures and make the requested format explicit.

## Progress

- Created GitHub issue for tracking.
- Created task file before implementation changes.
- Confirmed accepted enum values in `backend/src/main/resources/openapi/vocavista-api.yaml`.
- Confirmed validation fails at `ProviderWordInfoValidator.requireEnum` when generated API enum parsers reject provider values.
- Updated the provider system prompt to list exact enum values.
- Extended `SpringAiOpenAiWordInfoProvider` schema normalization to constrain `language`, `partOfSpeech`, `frequency`, `gender`, and `article` with enum values.
- Allowed `null` in the generated schema for noun-only fields `gender`, `article`, and `plural` so non-noun output matches the prompt and validator expectations.
- Extended provider regression coverage to assert enum constraints in the schema passed through `OpenAiChatOptions`.

## Verification

- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw test -Dtest=SpringAiOpenAiWordInfoProviderTest`: passed under Java 25.0.3. Tests run: 6, failures: 0, errors: 0.
- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw test`: passed under Java 25.0.3. Tests run: 16, failures: 0, errors: 0.

## Open Questions

- None currently.

## Final Technical State

- The provider system prompt explicitly lists accepted enum values for language, part of speech, frequency, gender, and article.
- The OpenAI response JSON schema now constrains `language`, `partOfSpeech`, `frequency`, `gender`, and `article` to the API-supported values before it is attached to `OpenAiChatOptions`.
- `gender` and `article` allow `null` in addition to their enum values, and `plural` allows string or `null`, matching the prompt and validator behavior for non-nouns.
- Validation remains strict through generated API enum parsers; unsupported provider values still map to controlled `502` responses.
- Regression tests assert the enum and nullable-field constraints in the actual schema sent to the mocked `ChatModel`.
