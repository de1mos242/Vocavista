# Persist selected vocabulary before starting media generation

- Issue: https://github.com/de1mos242/Vocavista/issues/56
- Pull Request: https://github.com/de1mos242/Vocavista/pull/58
- Branch: `56-persist-selected-vocabulary-before-starting-media-generation`

## Goal

When a user selects a generated phrase, persist the associated word and user vocabulary entry before starting image or video generation so later regeneration has a stored entity to target.

## Scope

- Ensure phrase selection stores or reuses the vocabulary entry immediately.
- Start media generation from the stored vocabulary entity rather than a transient selection.
- Keep existing regeneration flows working with persisted entities.

## Constraints

- Preserve existing UI and API patterns where possible.
- Avoid unrelated refactors.

## Progress

- Created issue and task branch.
- Updated the add page so phrase selection saves the selected word to the user's revise list when a stored `wordInfoId` is available.
- Changed the generate action to save the word before queueing image and video generation, replacing the separate save button with a combined "Save and generate assets" action.
- Added backend safety in pronunciation and phrase image creation/regeneration so media generation always ensures the current user's dictionary entry before queueing work.
- Extended affected media service tests to assert dictionary persistence is ensured before generation/reuse.
- Opened PR #58 with `Fixes #56`.

## Verification

- `npm run build` passed after installing frontend dependencies with `npm ci`.
- `./mvnw test` reached the Docker/Testcontainers-backed Spring context test and timed out after 5 minutes while initializing Docker.
- `./mvnw -Dtest=PronunciationServiceTest,PhraseImageServiceTest,UserDictionaryServiceTest test` passed.

## Open Questions

- Should phrase selection immediately start generation, or is the current explicit "Save and generate assets" step preferred after auto-saving the phrase?
