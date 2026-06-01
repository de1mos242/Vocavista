# Keep One Fly Machine Running

## Issue

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/36
- Pull request: https://github.com/de1mos242/Vocavista/pull/37
- Branch: `36-keep-one-fly-machine-running`

## Goal

Keep one Fly.io app Machine running so Vocavista avoids Fly app Machine cold-start latency.

## Scope

- Update `fly.toml` to keep one app Machine running.
- Update Fly deployment documentation with the cost and latency tradeoff.
- Do not change database provider settings, secrets, or production infrastructure directly.

## Constraints

- This PR should remain separate from startup analysis and native-image experiment work.
- This change removes Fly app Machine cold starts only; Neon database compute can still suspend unless configured separately.
- Do not deploy from this task unless explicitly requested.

## Progress

- Created issue `#36`, task checkout, and branch `36-keep-one-fly-machine-running`.
- Updated `fly.toml` to keep one app Machine running with `min_machines_running = 1`.
- Updated `documentation/fly-deployment.md` to document the warm-machine cost/latency tradeoff and the separate Neon cold-start caveat.
- Opened PR `#37`.

## Verification

- Passed: `fly config validate`.
- Passed: `git diff --check`.
