# Deploy Vocavista Cheaply on Fly.io

## Issue

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/22
- Pull request: https://github.com/de1mos242/Vocavista/pull/24
- Branch: `22-deploy-vocavista-cheaply-on-fly-io`

## Goal

Publish Vocavista on a low-cost Fly.io deployment path for a pet-project environment while keeping the runtime compatible with Spring Boot 4, Java 25, PostgreSQL, Google OAuth, AI providers, and S3-compatible generated media storage.

## Scope

- Add a backend Docker image suitable for Fly Machines.
- Add Fly.io app configuration with health checks and cost-conscious autostop/autostart defaults.
- Document required Fly secrets for database, OAuth, AI providers, and media storage.
- Document recommended Neon Postgres and Cloudflare R2 setup.
- Add an optional GitHub Actions workflow for Fly deploys.
- Keep generated media outside the container filesystem.

## Constraints

- Do not commit real secrets or provider credentials.
- Prefer external serverless Postgres for lower idle cost.
- Prefer Cloudflare R2 or another S3-compatible store for low idle media storage cost.
- Fly autostop/autostart is cost-effective but must be verified with the OAuth session flow.

## Decisions

- Use a Dockerfile under `backend/` and build from the repository root so Fly can deploy the existing single backend service.
- Skip tests inside Docker image builds; tests remain a separate verification step and Testcontainers should not run during Fly remote builds.
- Use `SPRING_PROFILES_ACTIVE=prod` on Fly while keeping local development defaulting to the `local` profile.
- Recommend Neon Postgres first and Fly Managed Postgres only if operational simplicity becomes more important than idle cost.
- Recommend Cloudflare R2 first; configure S3 path-style access for R2 compatibility.

## Progress

- Task planning completed and deployment-relevant backend configuration inspected.
- Added `backend/Dockerfile` for Java 25 Spring Boot image builds.
- Added `fly.toml` with Fly service, VM, HTTPS, autostop/autostart, and `/actuator/health` check settings.
- Added `.dockerignore` to keep local outputs, secrets, docs, and task notes out of the Docker build context.
- Updated runtime configuration so local development defaults to the `local` profile while Fly can set `SPRING_PROFILES_ACTIVE=prod`.
- Added forward-header configuration for Fly proxy/OAuth HTTPS URL handling.
- Added deployment documentation for Fly, Neon, Cloudflare R2, secrets, OAuth callback setup, manual verification, and optional GitHub Actions.
- Added optional `.github/workflows/fly-deploy.yml` using `flyctl deploy --remote-only`.
- Added GitHub issue status comment: https://github.com/de1mos242/Vocavista/issues/22#issuecomment-4580145746
- Opened pull request: https://github.com/de1mos242/Vocavista/pull/24
- Deployed to Fly app `vocavista`; health endpoint is `UP` at `https://vocavista.fly.dev/actuator/health`.
- Scaled down from Fly's first-deploy two-Machine HA default to one Machine for the low-cost setup.
- Updated manual deploy docs and GitHub Actions deploy command to use `--ha=false`.

## Verification

- Passed: `./mvnw test` from `backend`.
- Passed: `docker build -t vocavista-backend:fly -f backend/Dockerfile .` from the repository root.
- Passed: `git diff --check`.
- Passed: `fly deploy -a vocavista` created the first deployment.
- Passed: `curl -fsS https://vocavista.fly.dev/actuator/health` returned `UP`.
- Passed: Fly status reports one running Machine with the health check passing after scale-down.
- Passed: Fly logs show the app started with the `prod` profile and Flyway migrated/validated schema version 5.
- Pending manual verification: Google OAuth callback and media write/read.

## Manual Follow-Up

- Create or choose the Fly app name and region.
- Create Neon Postgres and Cloudflare R2 resources.
- Set Fly secrets with real credentials.
- Add the Fly public callback URL to the Google OAuth client.
- Add `FLY_API_TOKEN` to GitHub Actions if using the workflow.
