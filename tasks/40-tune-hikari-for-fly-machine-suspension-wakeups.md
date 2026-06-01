# Tune Hikari for Fly Machine Suspension Wakeups

## Issue

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/40
- Branch: `40-tune-hikari-for-fly-machine-suspension-wakeups`

## Goal

Make database access recover quickly after a Fly Machine resumes from `auto_stop_machines = "suspend"` and existing PostgreSQL connections are stale.

## Scope

- Tune HikariCP runtime settings for stale idle connections after suspend/resume.
- Keep the Fly app on suspend mode; do not change production infrastructure or secrets.
- Document the operational behavior for Fly suspend and Neon cold database wakeups.

## Constraints

- Do not deploy or run production commands from this task.
- Hikari settings must remain compatible with Spring Boot 4 and HikariCP 7.

## Implementation Notes

- HikariCP 7 defaults to a fixed-size pool because `minimumIdle` defaults to `maximumPoolSize`, so `idleTimeout` has no effect unless `minimum-idle` is lowered.
- Hikari validates idle connections on checkout after the alive bypass window, but short validation and connection timeouts limit how long a stale socket can block a request.
- Spring Boot 4 exposes Hikari timeout properties as `Long` millisecond values, so use numeric milliseconds rather than duration strings.

## Decisions

- Use `minimum-idle: 0` and a short `idle-timeout` so idle pools can drain before suspension and do not keep unnecessary stale sockets around.
- Use shorter `max-lifetime`, explicit `keepalive-time`, and shorter validation/connection timeouts to replace stale connections quickly after wakeup.

## Progress

- Created issue `#40`, task checkout, and branch `40-tune-hikari-for-fly-machine-suspension-wakeups`.
- Verified HikariCP 7 timeout constraints and defaults from the local Maven source artifact.
- Configured runtime Hikari settings in `backend/src/main/resources/application.yaml` to use a drainable idle pool, shorter connection lifetime, explicit keepalive, and shorter validation/connection timeouts.
- Updated `documentation/fly-deployment.md` with the suspend/resume stale-connection behavior and remaining Neon reconnect/cold-wake latency caveat.

## Verification

- Passed: `./mvnw test` from `backend`.
- Passed: `git diff --check` from the repository root.
