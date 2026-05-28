# Issue 18: Add User Dictionary Review Flow

## Goal

Add a per-user dictionary that is populated when users generate or reuse pronunciation media, then provide a simple Anki-style review flow for German vocabulary.

## Scope

- Store one dictionary entry per user and normalized German word.
- Link entries to shared `word_info_records` instead of duplicating word metadata.
- Return due review items by default, maximum 10.
- Support practice-more through `includeUpcoming=true`, returning the top 10 entries by `dueAt`.
- Use client-side answer matching and submit only pass/fail review results.
- Add a static `review.html` page without introducing a frontend build pipeline.

## Decisions

- Keep the implementation simple and do not add a separate review-attempt history table.
- Persist current review state on `user_dictionary_entries` only.
- Review API may return the German `expectedAnswer`; the UI must hide it until submit or forgotten.
- Submit API accepts only `{ "correct": true|false }`.
- Scheduling uses UTC with a 04:00 day boundary.
- Incorrect and forgotten answers are both submitted as `correct=false`.
- Maximum SRS interval is 30 days.

## Implementation Notes

- Add a Flyway migration for `user_dictionary_entries`.
- Expose current authenticated `UserAccount` to dictionary and pronunciation services.
- Ensure `PronunciationService.create` upserts a dictionary entry even when reusing an existing media asset.
- Add OpenAPI schemas and generated controller implementation for dictionary review endpoints.
- Add client-side answer normalization for case, whitespace, punctuation, German umlaut alternatives, and `ß/ss`.

## Verification

- `./mvnw test` passed from `backend`.

## Follow-up UI Updates

- Added `index.html` as a simple root homepage with links to add a new word and review words.
- Added duplicated navigation bars to the homepage, add-word page, and review page.
- Changed review answer handling so correct typed answers are detected immediately on input; the submit button was removed.
- `./mvnw test` passed from `backend` after these updates.

## Follow-up Navigation Updates

- Moved sign-in/user info/logout controls into the top navigation bar on all static pages.
- Unified the homepage and review page styling with the dark Veo page visual treatment.
- Kept duplicated navbar/auth code in each page for now.
- `./mvnw test` passed from `backend` after these updates.
