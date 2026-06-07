# Fix Invalid Veo DurationSeconds Value

- Issue: https://github.com/de1mos242/Vocavista/issues/49
- Goal: restore a Veo `durationSeconds` value accepted by Google after production generation started failing with `INVALID_ARGUMENT`.
- Scope: pronunciation video generation config/defaults and tests.
- Constraint: preserve the merged 720p, small-video, cache, and prompt cleanup work from issue #47.

## Findings

- Merged code sends `durationSeconds: 5`.
- Google rejected that request even though the error text says the accepted range is 4 to 8 inclusive.
- The prior working value was `6`, so this fix restores `6` in runtime config, Java defaults, and tests.

## Progress

- Created follow-up issue and task checkout.
- Restored `durationSeconds` to `6` in `VeoProperties` and `application.yaml`.
- Updated generator request/model-name assertions from `5` / `5s` to `6` / `6s`.

## Verification

- Passed: `./mvnw -Dtest=PronunciationVideoGeneratorTest test` in `backend`.
