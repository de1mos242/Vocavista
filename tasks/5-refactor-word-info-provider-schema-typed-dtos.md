# Issue 5: Refactor Word Info Provider Schema To Typed DTOs

## Goal

Replace manual JSON schema mutation in the OpenAI word info provider with typed provider DTO fields that let Spring AI and the underlying schema generator produce the correct schema naturally.

## Related Issue

- GitHub: https://github.com/de1mos242/Vocavista/issues/5

## Scope

- Inspect generated API enum DTOs and current provider mapping/validation.
- Replace localized `Map<String, List<String>>` fields with a typed localized-text record.
- Replace provider enum-like string fields with enum types where feasible.
- Remove custom schema mutation helpers from `SpringAiOpenAiWordInfoProvider`.
- Update tests to assert the generated schema remains valid for localized text and enums.

## Constraints

- Keep the public API contract unchanged.
- Preserve strict validation and sanitized provider error mapping.
- Do not modify unrelated worktree changes.
- Prefer the minimal refactor that removes manual schema rewriting.

## Implementation Notes

- Current manual helpers were added to fix OpenAI schema rejection for maps and unsupported enum values like `frequency: high`.
- A better model is to encode these constraints in `ProviderWordInfo` itself so the schema library can generate them.

## Decisions

- Use typed provider DTOs rather than schema post-processing.
- Use a `LocalizedText` provider record for `en` and `ru` arrays instead of `Map<String, List<String>>`.
- Use provider-local enums with lowercase constants so Spring AI's schema generator emits the exact JSON enum values expected by the API contract.
- Use `Optional` for noun-only nullable fields because Spring AI's schema generator emits nullable schemas for `Optional<T>` and `BeanOutputConverter` parses JSON `null` into `Optional.empty()`.

## Progress

- Created GitHub issue for tracking.
- Created task file before implementation changes.
- Verified with `BeanOutputConverter` in JShell that records generate explicit nested object schemas and lowercase enum constants generate matching JSON enum values.
- Verified with `BeanOutputConverter` in JShell that `Optional<T>` fields generate nullable schemas and parse JSON `null` correctly.
- Refactored `ProviderWordInfo` to use `LocalizedText`, provider enum types, and `Optional` noun-only fields.
- Removed manual JSON schema mutation from `SpringAiOpenAiWordInfoProvider`; it now passes `outputConverter.getJsonSchema()` directly to `OpenAiChatOptions`.
- Updated provider validation and MapStruct mapping helpers around typed provider fields.
- Updated tests and fixtures for the typed provider model.

## Verification

- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw test -Dtest=SpringAiOpenAiWordInfoProviderTest,WordInfoServiceTest`: passed under Java 25.0.3. Tests run: 9, failures: 0, errors: 0.
- `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw test`: passed under Java 25.0.3. Tests run: 16, failures: 0, errors: 0.

## Open Questions

- None currently.

## Final Technical State

- `ProviderWordInfo` encodes AI output constraints directly with `LocalizedText`, lowercase provider enums, and `Optional` noun-only fields.
- `SpringAiOpenAiWordInfoProvider` no longer mutates generated JSON schema; it passes `outputConverter.getJsonSchema()` directly to `OpenAiChatOptions`.
- Spring AI now generates the localized-text object schema, enum values, and nullable noun-only fields from the provider DTO shape.
- `ProviderWordInfoValidator` validates typed provider fields and localized text without parsing string enums.
- `WordInfoMapper` maps provider-local types into generated OpenAPI DTO enums and language maps.
- Provider regression tests assert the generated schema shape sent through `OpenAiChatOptions`.
