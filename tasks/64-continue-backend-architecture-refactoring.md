# Continue Backend Architecture Refactoring

- Issue: https://github.com/de1mos242/Vocavista/issues/64
- Goal: reduce remaining backend coupling in small behavior-preserving slices after #62 / PR #63.

## Scope

- Work from latest `main` after PR #63, not the original stale task checkout baseline.
- Hide media repositories/entities from word suggestions and dictionary review flows behind a media read/query boundary.
- Read the stored noun article through a word-info boundary so media code does not parse generated API word-info DTOs directly.
- Keep API error handlers explicit unless a real boundary is needed; do not add helper classes just to hide one response-construction call.
- Remove or soften tests that only verify implementation details and make refactoring harder.

## Decisions

- Keep OpenAPI generation and generated controller DTOs.
- Keep cached `word_info_records.response_json` shape unchanged because it is persisted data.
- Keep media asset entities related to `WordInfoRecord`; changing the database relationship is too large for this slice.
- Do not move phrase image implementation packages in this task; follow-up issue #65 tracks that mechanical package cleanup.
- Keep domain-specific error handlers separate and self-contained; the attempted shared `ApiErrorResponses` helper was removed because it did not simplify the design.
- Do not pass both article and gender across the word-info/media boundary; the article is enough for pronunciation generation to derive script-specific speaker wording.

## Progress

- Follow-up issue #65 created for moving phrase image backend code out of the pronunciation package.
- Task branch fast-forwarded to include PR #63 before implementation.
- Added `MediaAssetQueryService` as a neutral read boundary for pronunciation and phrase image lookups used by word suggestions and dictionary review/manifest responses.
- Moved media query projections from manual service methods into `MediaAssetMapper` so the query service only queries and delegates mapping.
- Moved word suggestion, dictionary review/manifest, pronunciation response, and phrase-image response projections into MapStruct mappers while leaving validation, deduplication, persistence, and enrichment in services.
- Replaced `WordInfoMetadataReader`/`PronunciationMetadata` with `WordInfoArticleReader`, which returns only the stored noun article; pronunciation generation derives speaker wording from that article.
- Kept existing domain error handlers/status mappings separate and removed the low-value `ApiErrorResponses` helper so each handler constructs its own response explicitly.
- Removed the empty `VocavistaBackendApplicationTests` context smoke test and two dictionary tests that only asserted repository method selection.

## Verification

- `./mvnw -Dtest=WordInfoControllerTest,UserDictionaryServiceTest,PronunciationGenerationProcessorTest,PronunciationControllerTest test` passed.
- `./mvnw -Dtest=WordInfoControllerTest,PronunciationControllerTest test` passed after removing `ApiErrorResponses`.
- `./mvnw -Dtest=DictionaryControllerTest test` passed after removing `ApiErrorResponses`.
- `./mvnw -Dtest=PronunciationGenerationProcessorTest test` passed after replacing `PronunciationMetadata` with article-only reading.
- `./mvnw -Dtest=UserDictionaryServiceTest test` passed after restoring the `Optional` import style.
- `./mvnw -Dtest=UserDictionaryServiceTest,PronunciationControllerTest test` passed after moving media query projections into MapStruct.
- `./mvnw -Dtest=UserDictionaryServiceTest,DictionaryControllerTest,WordInfoControllerTest,PronunciationServiceTest,PhraseImageServiceTest,PronunciationControllerTest test` passed after moving additional service projections into MapStruct.
- `./mvnw test` compiled and ran non-container tests, but failed on `AuthControllerTest` because Docker/Testcontainers is unavailable for the PostgreSQL container in this environment.
