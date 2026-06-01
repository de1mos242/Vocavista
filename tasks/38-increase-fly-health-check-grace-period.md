# Increase Fly Health Check Grace Period

## Issue

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/38
- Branch: `38-increase-fly-health-check-grace-period`

## Goal

Increase the Fly.io HTTP health check grace period to the maximum effective value so slow cold starts are not marked unhealthy too early.

## Scope

- Change the Fly HTTP health check grace period from `45s` to `1m`.
- Update Fly deployment documentation to explain what the grace period does.
- Do not change suspend/always-on behavior, database settings, or secrets.

## Constraints

- This change does not make startup faster. It only prevents premature health-check enforcement during startup.
- Fly lowers service-check grace periods above `1m` to `1m`, so `5m` is not an effective setting for this check.
- Keep this PR separate from the suspend-mode and native-image experiments.
- Do not deploy from this task unless explicitly requested.

## Progress

- Created issue `#38`, task checkout, and branch `38-increase-fly-health-check-grace-period`.
- Tried `grace_period = "5m"`, but `fly config validate` warned it would be lowered to `1m`.
- Set `grace_period = "1m"`, the maximum effective value for this Fly service check.

## Verification

- Passed: `fly config validate` with `grace_period = "1m"`.
- Passed: `git diff --check`.
