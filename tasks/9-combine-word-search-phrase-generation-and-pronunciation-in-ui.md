# Issue 9: Combine word search, phrase generation, and pronunciation in UI

## Goal

Create a simple end-to-end UI flow that lets a user search for a German word, view generated word information, choose an example phrase, generate pronunciation audio, and play it through the TalkingHead preview.

## Scope

- Persist generated word-info responses so repeated requests can reuse cached data instead of calling the AI provider again.
- Add a focused `WordSuggestionService` with a simple contains search for autocomplete.
- Search suggestions across cached word-info records and existing pronunciation assets.
- Extend the existing static `talking-head.html` preview rather than adding a separate frontend app.
- Link every generated pronunciation asset to the word-info record it was generated from.

## Constraints

- Keep search intentionally simple: SQL `LIKE %query%` semantics are enough.
- Do not overbuild ranking, fuzzy matching, or autocomplete infrastructure.
- 400-status tests are not required for the new suggestions endpoint.

## Implementation Notes

- Add an OpenAPI `GET /api/v1/words/suggestions` endpoint.
- Store word-info records with the normalized query and serialized API response.
- Return the persisted word-info ID from word-info responses and require it when creating pronunciation audio.
- Store `pronunciation_assets.word_info_record_id` as a non-null foreign key to `word_info_records(id)`.
- Return pronunciation suggestions with phrase and reusable audio metadata when available.
- Return cached word-info suggestions without phrase so selecting one can load the cached word-info response.

## Progress

- 2026-05-22: Planned simple implementation shape with cached word-info records and a focused suggestion service.
- 2026-05-22: Added OpenAPI contract changes, word-info persistence, simple autocomplete suggestions, and updated the TalkingHead preview flow.
- 2026-05-22: Added focused positive MVC coverage for the suggestions endpoint and updated existing word-info tests for caching.
- 2026-05-22: Simplified malformed AI response diagnostics to dump the parsed provider response on validation failures.
- 2026-05-22: Moved malformed AI response dump from the exception message into the service log entry.
- 2026-05-22: Moved malformed AI response dump into the existing `WordInfoErrorHandler` invalid-response log by attaching the parsed provider response to the bad-gateway exception.
- 2026-05-22: Added normalized word variants under the search button and changed example phrase selection to use the word-info normalized word for pronunciation audio.
- 2026-05-22: Linked generated pronunciation assets to persisted word-info records through required `wordInfoId` API fields and a non-null database foreign key.

## Verification

- 2026-05-22: `./mvnw test` passed with 29 tests.
- 2026-05-22: Manual browser smoke test was not run because the local backend, AI provider keys, and media services were not started in this session.
- 2026-05-22: `./mvnw test` passed again after response-dump diagnostics change with 29 tests.
- 2026-05-22: `./mvnw test` passed after moving response dump to logging with 29 tests.
- 2026-05-22: `./mvnw test` passed after moving response dump to the error-handler log with 29 tests.
- 2026-05-22: `./mvnw test` passed after normalized-word UI change with 29 tests.
- 2026-05-22: `./mvnw test` passed after requiring word-info linkage for pronunciation audio with 29 tests.

## Links

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/9
