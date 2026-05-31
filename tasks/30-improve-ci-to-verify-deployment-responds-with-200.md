# Improve CI to Verify Deployment Responds With 200

## Issue

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/30
- Status comment: https://github.com/de1mos242/Vocavista/issues/30#issuecomment-4588002994
- Branch: `30-improve-ci-to-verify-deployment-responds-with-200`

## Goal

Make the Fly.io deployment workflow prove that the newly deployed app is serving the root page successfully, not only that `flyctl deploy` completed.

## Scope

- Add a post-deployment GitHub Actions check that waits for the deployed root page to return HTTP 200.
- Keep the check simple and tied to the Fly app configured in `fly.toml`.
- Document the CI deployment verification behavior.

## Constraints

- Do not run deployment commands locally or against production from this task.
- Do not require extra GitHub Actions dependencies for parsing the app name.
- Keep enough retry time for Fly Machine startup and application cold starts.

## Implementation Notes

- The workflow can derive the Fly app name from the top-level `app = "..."` setting in `fly.toml` and check `https://<app>.fly.dev/`.
- The Spring Security configuration permits `/`, and static `index.html` backs the root page, so HTTP 200 is the expected success condition.
- Use `curl` with retries in the workflow so transient startup failures do not fail the deploy immediately.
- Enforce a 5-minute verification deadline while keeping each request bounded with connection and total request timeouts.

## Progress

- Created the task file for issue 30.
- Inspected Fly deployment workflow, `fly.toml`, deployment docs, and root-page security/static-file handling.
- Added a post-deploy GitHub Actions step that derives `https://<app>.fly.dev/` from `fly.toml`, waits up to 5 minutes, and fails unless `/` returns HTTP 200.
- Updated Fly deployment documentation with the new CI verification behavior.
- Added GitHub issue implementation status comment.

## Verification

- Passed: `git diff --check`.
- Passed: workflow YAML parse check with Ruby `YAML.load_file`.
- Not run: actual Fly deployment, because this task should not execute production deployment commands locally.

## Open Questions

- None.
