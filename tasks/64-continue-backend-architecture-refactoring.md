# Continue Backend Architecture Refactoring

- Issue: https://github.com/de1mos242/Vocavista/issues/64
- Goal: reduce remaining backend coupling in small behavior-preserving slices after #62 / PR #63.

## Scope

- Work from latest `main` after PR #63, not the original stale task checkout baseline.
- Hide media repositories/entities from word suggestions and dictionary review flows behind a media read/query boundary.
- Move word-info metadata parsing out of pronunciation generation so media code does not depend on generated API word-info DTOs.
- Consolidate duplicated API error response construction without changing status codes, error codes, or messages.
- Remove or soften tests that only verify implementation details and make refactoring harder.

## Decisions

- Keep OpenAPI generation and generated controller DTOs.
- Keep cached `word_info_records.response_json` shape unchanged because it is persisted data.
- Keep media asset entities related to `WordInfoRecord`; changing the database relationship is too large for this slice.
- Do not move phrase image implementation packages in this task; follow-up issue #65 tracks that mechanical package cleanup.
- Keep domain-specific error handlers separate; only shared response construction should be extracted.

## Progress

- Follow-up issue #65 created for moving phrase image backend code out of the pronunciation package.
- Task branch fast-forwarded to include PR #63 before implementation.
- Added `MediaAssetQueryService` as a neutral read boundary for pronunciation and phrase image lookups used by word suggestions and dictionary review/manifest responses.
- Moved pronunciation metadata parsing for stored word info into `WordInfoMetadataReader`, so pronunciation generation no longer reads generated word-info API DTOs directly.
- Added shared `ApiErrorResponses` helper and kept existing domain error handlers/status mappings separate.
- Removed the empty `VocavistaBackendApplicationTests` context smoke test and two dictionary tests that only asserted repository method selection.

## Verification

- `./mvnw -Dtest=WordInfoControllerTest,UserDictionaryServiceTest,PronunciationGenerationProcessorTest,PronunciationControllerTest test` passed.
- `./mvnw test` compiled and ran non-container tests, but failed on `AuthControllerTest` because Docker/Testcontainers is unavailable for the PostgreSQL container in this environment.
