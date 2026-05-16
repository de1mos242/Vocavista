# Issue 3: Fix OpenAI Word Info JSON Schema

## Goal

Fix real word info requests failing because OpenAI rejects the JSON schema response format sent by the backend.

## Related Issue

- GitHub: https://github.com/de1mos242/Vocavista/issues/3

## Scope

- Inspect the schema produced for `ProviderWordInfo` and how it is passed to Spring AI OpenAI options.
- Make the OpenAI `json_schema` payload valid for real provider requests.
- Preserve existing provider exception mapping and response parsing behavior.
- Add regression coverage for the schema included in the prompt options.

## Constraints

- Do not expose raw provider internals to API clients.
- Keep the public endpoint contract unchanged.
- Keep the change minimal and localized to the provider unless investigation shows a broader issue.

## Implementation Notes

- Reported runtime error: `400: Invalid schema for response_format 'json_schema': In context=(), object schema missing properties`.
- The failure originates in `SpringAiOpenAiWordInfoProvider.generate` when `OpenAiChatModel.call` sends the prompt.
- Suspect area: `BeanOutputConverter<ProviderWordInfo>.getJsonSchema()` used as `OpenAiChatModel.ResponseFormat.jsonSchema`.

## Decisions

- Keep `ProviderWordInfo` unchanged and normalize the generated schema at the provider boundary.
- Replace Spring AI's generated `Map(String,List(String))` definition with an explicit localized object schema containing required `en` and `ru` array properties.
- Do not switch away from JSON schema structured output; the failure is the generated map definition, not structured output itself.

## Progress

- Created GitHub issue for tracking.
- Created task file before implementation changes.
- Confirmed Java 25 verification works by sourcing SDKMAN from `$HOME/.sdkman/bin/sdkman-init.sh` and running `sdk env`.
- Inspected the generated `ProviderWordInfo` schema. Root schema has properties, but the `$defs.Map(String,List(String))` object generated for localized maps has no `properties` and `additionalProperties: false`, which OpenAI rejects.
- Updated `SpringAiOpenAiWordInfoProvider` to compute a provider-specific `responseJsonSchema` once during construction and use it in `OpenAiChatOptions`.
- Added schema normalization that replaces the generated localized map definition with an explicit object schema requiring `en` and `ru` string arrays.
- Added a provider regression test that captures the `Prompt` sent to `ChatModel` and verifies the OpenAI response schema contains explicit localized language properties.
- Updated the OpenAI HTTP error test fixture to set the SDK-required `ErrorObject.param` field.

## Verification

- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw test -Dtest=SpringAiOpenAiWordInfoProviderTest`: passed under Java 25.0.3. Tests run: 6, failures: 0, errors: 0.
- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw test`: passed under Java 25.0.3. Tests run: 16, failures: 0, errors: 0.

## Open Questions

- None currently.

## Final Technical State

- `SpringAiOpenAiWordInfoProvider` still uses `BeanOutputConverter<ProviderWordInfo>` for parsing provider JSON responses.
- The provider now stores a normalized `responseJsonSchema` and passes that schema to `OpenAiChatOptions` instead of the raw Spring AI generated schema.
- The normalized schema keeps Spring AI's generated structure but replaces `$defs.Map(String,List(String))` with a strict object containing required `en` and `ru` string-array properties.
- Existing provider error mapping is unchanged.
- Regression coverage verifies the schema included in the captured `Prompt` options sent to `ChatModel`.
