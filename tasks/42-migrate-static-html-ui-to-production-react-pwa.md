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

## Verification

- `npm run build` passed in `frontend/`.
- `./mvnw test` passed in `backend/`.
- `npm run build` passed again after the desktop layout fix.
- Local shell Node is `20.10.0`, so Vite prints a warning; Maven installs Node `22.13.1` for packaged builds.
