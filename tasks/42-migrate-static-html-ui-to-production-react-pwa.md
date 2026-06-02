# Migrate static HTML UI to production React PWA

- Issue: https://github.com/de1mos242/Vocavista/issues/42
- Branch: `42-migrate-static-html-ui-to-production-react-pwa`

## Goal

Replace the current static HTML UI with a production-ready, mobile-first React PWA served by the existing Spring Boot backend.

## Scope

- Add a React + TypeScript frontend.
- Optimize the first version for phone usage with responsive desktop support.
- Use generated TypeScript API client/types from the existing OpenAPI contract.
- Add installable PWA support with a service worker.
- Cache the app shell and generated pronunciation videos where safe.
- Keep same-origin Spring Security OAuth session authentication.
- Use better application routes from the beginning instead of preserving old `.html` paths.

## Constraints

- Do not manually duplicate API paths and DTOs in the frontend.
- Keep `/api/v1/**`, `/login/google`, and `/logout` served by Spring Boot.
- Avoid caching private API data aggressively.
- Generated video caching is allowed because pronunciation media is an explicit user requirement.

## Decisions

- PWA is the first mobile solution; native mobile can be reconsidered later.
- First version is phone-first.
- Old static HTML paths do not need long-term compatibility.

## Progress

- GitHub issue and task checkout created.
- Added `frontend/` React + TypeScript + Vite PWA.
- Added OpenAPI TypeScript SDK generation from `backend/src/main/resources/openapi/vocavista-api.yaml`.
- Migrated home, add-word, review, and admin flows into React routes `/`, `/add`, `/review`, and `/admin`.
- Added PWA service worker support, including runtime cache for generated pronunciation video responses.
- Integrated the frontend production build into backend Maven packaging with `frontend-maven-plugin` and copied generated assets into Spring Boot static resources.
- Removed old static HTML pages and updated Spring Security to permit the new SPA routes/assets.
- Added `/api/v1/media/pronunciations/{id}/video` to OpenAPI so review video playback uses generated frontend and backend API contracts.
- Updated documentation and auth route tests for the new PWA routes.
- Fixed the desktop layout after review by reducing large-screen heading scale, adding safer desktop breakpoints, and preventing controls/video overflow.
- Tightened the add-word suggestions effect so pending/deactivated signed-in users do not call protected feature APIs while action buttons are disabled.
- Added `.nvmrc` with Node `22` so manual frontend commands use a Vite-compatible Node line matching Maven-packaged builds.
- Improved desktop layout so the add/review controls behave like a bounded desktop card instead of an enlarged phone view: reduced desktop heading scale, prevented child overflow, and made the controls column scroll within the viewport.
- Fixed Maven packaging hygiene by deleting `target/classes/static` before copying generated React assets, preventing stale hashed JS/CSS files from remaining in the Spring Boot jar after repeated `mvn package` runs.
- Fixed Docker packaging after the React migration by copying `frontend/` into the build image at `/workspace/frontend` before `./mvnw package`; Maven's frontend plugin expects it as `../frontend` from `/workspace/backend`.

## Verification

- `npm run build` passed in `frontend/`.
- `./mvnw test` passed in `backend/`.
- `npm run build` passed again after the desktop layout fix.
- `npm run build` passed after the restricted-account suggestions fix.
- `npm run build` passed under Node `22.22.3` after the desktop layout refinement.
- `./mvnw package -DskipTests` passed in `backend/` and showed `npm ci`, `npm run build`, `clean-frontend-assets`, and `copy-frontend-assets` running.
- Verified `backend/target/backend-0.0.1-SNAPSHOT.jar` contains the current React `index.html`, `index-_5Z_y0vP.css`, `index-Cr8envNv.js`, `sw.js`, and `manifest.webmanifest` only, without previous stale hashed assets.
- `docker build --target build -f backend/Dockerfile .` passed from the repository root and showed `npm ci` running in `/workspace/backend/../frontend` followed by the React build and backend jar packaging.
- `.nvmrc` selects Node `22` for manual frontend work, and Maven installs Node `22.13.1` for packaged builds.
