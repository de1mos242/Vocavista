# Issue 20: Play Pronunciation Video After Answering Review Word

## Goal

In the dictionary review flow, reveal the correct German answer and automatically play the associated pronunciation video after a user answers correctly or presses "I do not remember".

## Scope

- Add the completed pronunciation video URL to review items when one exists for the reviewed word.
- Keep the existing client-side answer matching and review result submission behavior unchanged.
- Update the static `review.html` page to render and autoplay the video only after the result is revealed.

## Constraints

- Keep the static frontend implementation; do not introduce a frontend build pipeline.
- Do not change SRS scoring or review progression behavior.

## Implementation Notes

- Use the latest completed pronunciation asset for the review item's word info record.
- If no completed video exists, the review result should still reveal normally without playback.
- `DictionaryReviewItem` now exposes optional `pronunciationVideoUrl`.
- `UserDictionaryService` resolves that URL through `PronunciationRepository.findFirstByWordInfoRecordIdAndStatusOrderByUpdatedAtDesc`.
- `review.html` renders a hidden-until-result video element and calls `play()` after answer reveal, with a status fallback if browser autoplay is blocked.

## Progress

- Implementation complete.
- Review items include the latest completed pronunciation video URL when available.
- Answer reveal disables the existing controls as before and starts video playback without changing SRS result submission or batch progression.

## Verification

- `./mvnw test` passed from `backend`.

## Links

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/20
- Review UI: `backend/src/main/resources/static/review.html`
- Review service: `backend/src/main/java/com/vocavista/backend/dictionary/UserDictionaryService.java`
