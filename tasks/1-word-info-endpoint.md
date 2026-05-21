# Issue 1: Word Info Endpoint

## Goal

Implement the first API-first backend endpoint that accepts a German word and returns compact learning-oriented metadata for German learners who know English and Russian.

## Related Issue

- GitHub: https://github.com/de1mos242/Vocavista/issues/1

## Scope

- Add OpenAPI contract for `GET /api/v1/words/info` with required `word` query parameter.
- Generate Spring MVC API interfaces and DTOs during Maven build.
- Implement controller, service boundary, provider boundary, and OpenAI-backed provider.
- Use Spring AI 2.0.0-M6 for OpenAI integration because the backend uses Spring Boot 4.x.
- Return stateless word metadata without PostgreSQL persistence.
- Add validation, controlled error mapping, and tests.

## Constraints

- Do not persist generated word information in PostgreSQL for this first version.
- Do not expose provider internals, raw prompts, or raw provider errors in API responses.
- Keep learning language fixed to German and known languages fixed to English and Russian.
- Keep implementation behind `AiWordInfoProvider` so the provider can be replaced later.
- Use generated API DTOs/interfaces from the OpenAPI spec.

## Decisions

- Use OpenAPI Generator Maven plugin `7.14.0` with Spring generator, interface-only APIs, and Jakarta validation.
- Use Spring AI BOM and `spring-ai-starter-model-openai` version `2.0.0-M6`.
- Configure OpenAI via Spring AI properties and override them with full Spring property names when needed.
- Use `__missing__` as the default API-key sentinel so the app can start locally/tests can run without secrets; endpoint calls return controlled `503` when the sentinel is present.
- Avoid default `temperature` configuration to keep GPT-5-family model selection possible.
- Use MapStruct `1.6.3` for provider-to-API DTO mapping instead of hand-written DTO construction.
- Use Lombok `1.18.38` for constructor injection boilerplate and configure `lombok-mapstruct-binding` for annotation processor ordering.

## Implementation Notes

- OpenAPI spec path: `backend/src/main/resources/openapi/vocavista-api.yaml`.
- Generated API interface: `com.vocavista.backend.api.WordsApi`.
- Generated response DTO: `com.vocavista.backend.api.model.WordInfoResponse`.
- Planned runtime flow: `WordInfoController -> WordInfoService -> AiWordInfoProvider -> SpringAiOpenAiWordInfoProvider`.

## Progress

- Added OpenAPI contract.
- Added Maven OpenAPI Generator configuration.
- Added Spring AI BOM/dependency planning in `pom.xml`.
- Generated sources successfully with `./mvnw generate-sources`.
- Added Spring AI OpenAI configuration in `application.yaml` with environment-backed API key, base URL, model, and reduced retry attempts.
- Added controller, service, provider boundary, Spring AI OpenAI provider, mapper, and error handler.
- Added unit/MVC tests for service validation/mapping, provider parsing/error mapping, and endpoint request/error behavior.
- Implementation complete.

## Verification

- `./mvnw generate-sources`: passed.
- `./mvnw test -DskipTests`: failed under the default shell JDK 21 because the project targets Java 25.
- `sdk env && ./mvnw test -DskipTests`: passed under Java 25.0.3.
- `sdk env && ./mvnw test`: passed under Java 25.0.3. Tests run: 13, failures: 0, errors: 0.

## Open Questions

- None.

## Final Technical State

- Endpoint `GET /api/v1/words/info` is implemented through the generated `WordsApi` interface.
- OpenAI integration uses Spring AI `ChatModel` and OpenAI JSON schema response format through `OpenAiChatOptions`.
- `SpringAiOpenAiWordInfoProvider` parses structured responses into `ProviderWordInfo`, then the service maps to generated OpenAPI DTOs through a Spring-managed MapStruct mapper.
- Missing OpenAI API key uses the `__missing__` sentinel and returns controlled `503` on endpoint usage without blocking application startup.
- Request validation covers missing, blank-after-trim, and too-long words.
- Provider malformed responses map to `502`; unavailable/missing configuration maps to `503`.
- Lombok provides required-args constructors for controller/service injection.

## Follow-up Changes

- Replaced static mapping logic with `@Mapper(componentModel = "spring")` while retaining provider response validation in `@BeforeMapping`.
- Added Lombok and MapStruct annotation processors to the Maven compiler configuration.
- Updated service and MVC tests to use the generated `WordInfoMapperImpl`.
- `sdk env && ./mvnw test`: passed under Java 25.0.3 after MapStruct/Lombok changes. Tests run: 13, failures: 0, errors: 0.
- Removed redundant explicit MapStruct mapping for `isCompound`; MapStruct infers this property from the source record accessor and generated DTO setter.
- `sdk env && ./mvnw test`: passed under Java 25.0.3 after removing the redundant mapping. Tests run: 13, failures: 0, errors: 0.
- Converted `WordInfoMapper` from an abstract class to an interface because it now only defines mapping methods and default type conversion helpers.
- Moved provider response validation out of MapStruct `@BeforeMapping` and into `ProviderWordInfoValidator`, called explicitly from `WordInfoService` before mapping.
- `sdk env && ./mvnw test`: passed under Java 25.0.3 after separating validation from mapping. Tests run: 13, failures: 0, errors: 0.
- Added server-side logging in `WordInfoErrorHandler`: provider failures log at `warn`, validation failures log at `debug`, while client responses remain sanitized.
- `sdk env && ./mvnw test`: passed under Java 25.0.3 after adding error handler logging. Tests run: 13, failures: 0, errors: 0.
- Updated Spring AI provider exception mapping so OpenAI SDK HTTP errors are reported as unsuccessful provider responses instead of malformed content; OpenAI IO errors map to provider unavailable.
- Fixed local PostgreSQL Compose configuration for `postgres:18-alpine` by mounting a new named volume at `/var/lib/postgresql`, which matches PostgreSQL 18 Docker image expectations and avoids deleting the old incompatible local volume.
