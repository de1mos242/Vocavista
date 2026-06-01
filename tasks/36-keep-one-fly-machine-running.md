# Try Fly Machine Suspend

## Issue

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/36
- Pull request: https://github.com/de1mos242/Vocavista/pull/37
- Branch: `36-keep-one-fly-machine-running`

## Goal

Try Fly.io Machine suspend mode as a lower-cost alternative to keeping one app Machine always running.

## Scope

- Update `fly.toml` to suspend the app Machine when idle instead of fully stopping it.
- Keep `min_machines_running = 0` so the app does not accrue always-on app Machine cost.
- Update Fly deployment documentation with the suspend latency/cost tradeoff and database connection caveat.
- Do not change database provider settings, secrets, or production infrastructure directly.

## Constraints

- This PR should remain separate from startup analysis and native-image experiment work.
- This change reduces Fly app Machine wake cost without guaranteeing always-warm latency.
- Existing database connections may be stale after resume; Neon database compute can still suspend unless configured separately.
- Do not deploy from this task unless explicitly requested.

## Progress

- Created issue `#36`, task checkout, and branch `36-keep-one-fly-machine-running`.
- Initially opened PR `#37` to keep one app Machine running with `min_machines_running = 1`.
- Revised the PR to try suspend mode first: `auto_stop_machines = "suspend"` and `min_machines_running = 0`.
- Updated `documentation/fly-deployment.md` to document the suspend cost/latency tradeoff and the separate Neon cold-start/database-connection caveat.
- Opened PR `#37`.

## Verification

- Passed after the suspend revision: `fly config validate`.
- Passed after the suspend revision: `git diff --check`.
