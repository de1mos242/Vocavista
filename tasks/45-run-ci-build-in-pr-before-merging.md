# Run CI Build in PR Before Merging

- Issue: https://github.com/de1mos242/Vocavista/issues/45
- Branch: `45-run-ci-build-in-pr-before-merging`

## Goal

Run the project build automatically for pull requests targeting `main` so broken changes are caught before merge.

## Scope

- Add a GitHub Actions workflow for PR build verification.
- Keep deployment behavior unchanged.
- Use the existing Maven build as the single source of truth because it already installs and builds the frontend.

## Implementation Notes

- Existing deployment workflow only runs on `main` pushes and manual dispatch.
- `backend/pom.xml` targets Java 25 and runs frontend `npm ci` plus `npm run build` during Maven resource generation.
- The PR workflow should avoid path filters so it can be used as a required branch protection check without being skipped for some PRs.

## Progress

- Added `.github/workflows/ci-build.yml`.
- The workflow runs on pull requests targeting `main` and on manual dispatch.
- The workflow sets up Temurin Java 25 with Maven caching and runs `./mvnw --batch-mode verify` from `backend`.
- Deployment workflow remains unchanged.

## Verification

- `./mvnw --batch-mode verify` passed locally from `backend`.
- Maven verification built the frontend, compiled the backend, ran 57 tests, and packaged the Spring Boot jar successfully.
- `.github/workflows/ci-build.yml` parsed successfully as YAML.
