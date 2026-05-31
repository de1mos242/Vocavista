# Add Spring Boot AOT Cache Image

## Issue

- GitHub issue: https://github.com/de1mos242/Vocavista/issues/26
- Status comment: https://github.com/de1mos242/Vocavista/issues/26#issuecomment-4580627284
- Fly startup fix comment: https://github.com/de1mos242/Vocavista/issues/26#issuecomment-4587942006
- Branch: `26-add-spring-boot-aot-cache-image-for-faster-fly-io-cold-starts`

## Goal

Reduce Fly.io cold-start time for the Spring Boot backend by baking a Java 25 AOT cache into the Docker image and using it at runtime.

## Scope

- Generate a Spring Boot AOT cache during `backend/Dockerfile` image builds.
- Keep Fly.io deployment compatible with the existing Dockerfile-based `fly.toml` and GitHub Actions workflow.
- Avoid requiring production secrets or external services during the Docker build-time training run.
- Document the Fly.io behavior and remaining cold-start limits.

## Constraints

- Do not commit real secrets or provider credentials.
- Keep runtime behavior on Fly using the normal `prod` profile and real Fly secrets.
- The generated cache is valid only for the same application bits and Java runtime version used by the image.
- AOT cache improves JVM/Spring startup work but does not remove Fly Machine wake latency, database connection latency, or Flyway migration time.

## Implementation Notes

- Spring Boot 4 with Java 25 supports the JVM AOT cache flow using `-XX:AOTCacheOutput` during a training run and `-XX:AOTCache` at runtime.
- The app must run from an extracted Spring Boot application layout for the cache to be effective.
- The Docker build should train with `-Dspring.context.exit=onRefresh` so the application context starts and exits without keeping the server running.
- Build-time training should disable database-dependent auto-configuration and Flyway/JPA work while preserving normal runtime behavior.

## Decisions

- Use the existing Dockerfile path rather than switching Fly to buildpacks or native images.
- Prefer AOT cache over CRaC because it integrates with a standard Docker image and does not require Fly runtime checkpoint/restore support.
- Generate the cache from the extracted Spring Boot application layout under `/app`, then copy that same layout into the runtime image so cache paths match.
- Keep the final image on `eclipse-temurin:25-jre`; the AOT cache must also be generated in a stage derived from that same JRE image so `/opt/java/openjdk/lib/modules` matches at runtime.
- Use a dedicated `aotcache` Spring profile for build-time training. It disables Flyway, disables schema validation, avoids JDBC metadata access, and disables DB health so the build does not need real infrastructure.

## Progress

- Created the task and inspected the current Docker/Fly setup.
- Updated `backend/Dockerfile` to extract the Boot jar, generate `/app/app.aot` with `-XX:AOTCacheOutput`, copy the extracted layout into the runtime image, and start with `-XX:AOTCache=/app/app.aot`.
- Added `backend/src/main/resources/application-aotcache.yaml` for the build-only training run.
- Updated Fly deployment documentation with cold-start behavior, limits, and verification guidance.
- Added GitHub issue status comment: https://github.com/de1mos242/Vocavista/issues/26#issuecomment-4580627284
- Fixed Fly startup failure caused by generating the AOT cache in the JDK build image and using it in the JRE runtime image. Java rejected the cache because `/opt/java/openjdk/lib/modules` differed between images. The Dockerfile now generates the cache in a JRE-based `aot` stage and copies that output into the final JRE runtime stage.
- Added GitHub issue fix comment: https://github.com/de1mos242/Vocavista/issues/26#issuecomment-4587942006

## Verification

- Passed: `./mvnw -B -DskipTests package` from `backend`.
- Passed: local extracted-jar AOT cache training with `-XX:AOTCacheOutput` and cached startup with `-XX:AOTCache`.
- Passed: `docker build -t vocavista-backend:aot-cache -f backend/Dockerfile .` from the repository root. Build output included `AOTCache creation is complete`.
- Passed: `docker run --rm -d -e SPRING_PROFILES_ACTIVE=aotcache -p 18080:8080 vocavista-backend:aot-cache` followed by `curl -fsS http://localhost:18080/actuator/health`, which returned `UP`.
- Passed: `./mvnw test` from `backend`.
- Passed: `git diff --check` from the repository root.
- Passed after the JRE-stage fix: `docker build --no-cache -t vocavista-backend:aot-cache-fixed -f backend/Dockerfile .`.
- Passed after the JRE-stage fix: cached Docker image started with `SPRING_PROFILES_ACTIVE=aotcache`, did not fail AOT cache processing, and `/actuator/health` returned `UP`.

## Follow-Up

- Deploy to Fly with the existing `fly deploy --ha=false` path and confirm production logs show a healthy startup with the `prod` profile.
