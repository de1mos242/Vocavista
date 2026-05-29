# Fly.io Deployment

This guide deploys the Vocavista Spring Boot backend to Fly.io with low idle cost. The recommended pet-project setup is one small Fly Machine for the app, Neon Postgres for the database, and Cloudflare R2 for generated media storage.

## Architecture

- Fly.io runs the backend Docker image built from `backend/Dockerfile`.
- Fly proxy terminates public HTTPS and forwards requests to the app on port `8080`.
- Spring Boot serves the static pages and API from the same origin.
- PostgreSQL stays outside the container and is migrated by Flyway on application startup.
- Generated media bytes stay in S3-compatible object storage, not in the container filesystem.
- Secrets are stored with Fly secrets and are not committed to the repository.

## Cost Defaults

- `fly.toml` starts with one shared-CPU 1 GB Machine.
- `auto_stop_machines = "stop"`, `auto_start_machines = true`, and `min_machines_running = 0` reduce idle compute cost.
- Use `fly deploy --ha=false` for this pet-project setup so Fly does not create an extra spare Machine on the first deploy.
- If Google OAuth redirects fail after idle periods, or wake-up latency is not acceptable, set `min_machines_running = 1` and redeploy.
- Neon is the recommended first database choice for low idle cost. Use Fly Managed Postgres only if simpler Fly-native operations are worth the extra cost.
- Cloudflare R2 is the recommended first media store for low idle cost. AWS S3 also works with equivalent S3 settings.

## Cold Starts

- The Docker image builds a Java 25 AOT cache for the Spring Boot application and starts the JVM with `-XX:AOTCache=/app/app.aot`.
- The cache is generated during the Docker build with the `aotcache` Spring profile, which disables Flyway schema work and avoids opening a real database connection.
- Runtime on Fly still uses the normal `prod` profile from `fly.toml`; the build-only `aotcache` profile is not a replacement for production configuration.
- This reduces JVM and Spring startup work after a Fly Machine starts. It does not remove Fly Machine wake-up latency, Neon connection latency, or Flyway migration time.
- If near-instant responses after idle periods are required, keep at least one Machine running by setting `min_machines_running = 1`.

## Files

- `fly.toml`: Fly app, VM, service, and health-check configuration.
- `backend/Dockerfile`: multi-stage Java 25 image build.
- `backend/src/main/resources/application-aotcache.yaml`: build-time profile used only for generating the AOT cache without production services.
- `.dockerignore`: keeps local build outputs, secrets, docs, and task notes out of the Docker build context.
- `.github/workflows/fly-deploy.yml`: optional GitHub Actions deployment workflow.

## One-Time Manual Setup

### 1. Install and Log In

```bash
brew install flyctl
fly auth login
```

### 2. Choose App Name and Region

The committed default is:

```toml
app = "vocavista"
primary_region = "ams"
```

Change these in `fly.toml` before creating the app if the name is unavailable or another region is closer to you.

Useful region command:

```bash
fly platform regions
```

Create the app without deploying yet:

```bash
fly apps create vocavista
```

If you changed `app` in `fly.toml`, use that app name in all Fly commands.

### 3. Create Postgres

Recommended: create a Neon project and database.

Use a direct database URL for Flyway migrations, not a transaction-pooling URL.

Set the JDBC URL in this shape:

```text
jdbc:postgresql://<neon-host>/<database>?sslmode=require
```

### 4. Create Media Bucket

Recommended: create a Cloudflare R2 bucket, for example `vocavista-media`.

Create an R2 API token with object read/write access for that bucket. Use the S3-compatible endpoint:

```text
https://<cloudflare-account-id>.r2.cloudflarestorage.com
```

Use path-style access with R2:

```text
VOCAVISTA_MEDIA_S3_PATH_STYLE_ACCESS=true
```

### 5. Configure Google OAuth

In Google Cloud Console, add this authorized redirect URI to the OAuth client:

```text
https://vocavista.fly.dev/login/oauth2/code/google
```

If you changed the Fly app name, replace `vocavista` with your app name.

Add the custom-domain callback later if you attach a custom domain.

## Fly Secrets

Set all runtime secrets before first deploy:

```bash
fly secrets set \
  SPRING_DATASOURCE_URL='jdbc:postgresql://<host>/<database>?sslmode=require' \
  SPRING_DATASOURCE_USERNAME='<database-user>' \
  SPRING_DATASOURCE_PASSWORD='<database-password>' \
  SPRING_AI_OPENAI_API_KEY='<openai-api-key>' \
  GOOGLE_AI_API_KEY='<google-ai-api-key>' \
  GOOGLE_OAUTH_CLIENT_ID='<google-oauth-client-id>' \
  GOOGLE_OAUTH_CLIENT_SECRET='<google-oauth-client-secret>' \
  VOCAVISTA_MEDIA_S3_ENDPOINT='https://<cloudflare-account-id>.r2.cloudflarestorage.com' \
  VOCAVISTA_MEDIA_S3_REGION='auto' \
  VOCAVISTA_MEDIA_S3_BUCKET='vocavista-media' \
  VOCAVISTA_MEDIA_S3_ACCESS_KEY='<r2-access-key-id>' \
  VOCAVISTA_MEDIA_S3_SECRET_KEY='<r2-secret-access-key>' \
  VOCAVISTA_MEDIA_S3_PATH_STYLE_ACCESS='true'
```

Do not put real values in `fly.toml`, `.env.example`, or documentation.

## Deploy Manually First

Run this from the repository root:

```bash
fly deploy --ha=false
```

Watch logs during startup:

```bash
fly logs
```

Flyway migrations should run during application startup. The app should then pass the Fly health check at `/actuator/health`.

The Docker build output should show `AOTCache creation is complete`, confirming the warmed cache was baked into the deployed image.

## Verify Deployment

Health endpoint:

```bash
curl -fsS https://vocavista.fly.dev/actuator/health
```

Browser checks:

- Open `https://vocavista.fly.dev/`.
- Start Google sign-in and confirm the callback returns to the app.
- Search or generate word data after sign-in.
- Generate pronunciation media and confirm video playback works.

Database checks:

- Confirm the Neon database has Flyway schema history and application tables.
- Confirm app logs do not show Flyway or JPA validation failures.

Storage checks:

- Confirm generated media objects appear in the R2 bucket.
- Confirm playback uses same-origin URLs under `/api/v1/media/pronunciations/{id}/video`.

## Optional GitHub Actions Deploy

The workflow in `.github/workflows/fly-deploy.yml` can deploy after manual setup is verified. It uses `flyctl deploy --remote-only --ha=false` to keep the app on the single-Machine low-cost path.

Manual setup for Actions:

```bash
fly tokens create deploy -x 999999h
```

Add the token as a GitHub repository secret named `FLY_API_TOKEN`.

The workflow supports manual runs with `workflow_dispatch`. It also deploys on pushes to `main` when the workflow exists on `main`.

## Operational Notes

- Use `fly secrets list` to confirm secret names, not values.
- Use `fly ssh console` only for runtime inspection; the container filesystem is not durable storage.
- Keep media in R2/S3 and state in Postgres.
- If the app is slow after idle time, either accept the cold start for cost savings or set `min_machines_running = 1`.
- If OAuth callback URLs are generated with `http` instead of `https`, confirm `SERVER_FORWARD_HEADERS_STRATEGY=framework` is set in Fly env or secrets and redeploy.
