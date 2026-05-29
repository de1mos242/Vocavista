# Add Favicon And Google OAuth App Branding

- Issue: https://github.com/de1mos242/Vocavista/issues/25
- Branch: `25-add-favicon-and-google-oauth-app-branding`

## Goal

Give Vocavista consistent browser and sign-in branding by adding a favicon/app icon, making static pages advertise the app name, and documenting the Google OAuth consent screen branding that must be configured in Google Cloud Console.

## Scope

- Add static icon assets served by the backend.
- Reference those assets from browser pages.
- Use a clearer app name in page metadata and Spring OAuth client metadata.
- Document Google OAuth app name and logo setup for local and deployed environments.

## Constraints

- Google OAuth consent screen name and logo are controlled in Google Cloud Console, not by runtime Spring configuration.
- Keep the implementation static and dependency-free.

## Implementation Notes

- Added `backend/src/main/resources/static/favicon.svg` as the shared Vocavista browser and app icon source.
- Added `backend/src/main/resources/static/site.webmanifest` with the public app name, short name, theme color, and icon reference.
- Added `backend/src/main/resources/static/google-g.svg` and updated all Google sign-in buttons to show the Google icon.
- Updated `index.html`, `veo-video.html`, and `review.html` with clearer page titles, `application-name`, mobile app title, theme color, favicon, and manifest links.
- Set the Spring Google OAuth client display name to `Vocavista` in default and local example configuration.
- Updated README and Fly deployment docs to clarify that Google's consent screen app name/logo must be configured in Google Cloud Console, using the app icon artwork as the source.

## Progress

- Implementation complete.
- Browser pages now advertise `Vocavista` consistently and display the favicon metadata.
- Google sign-in buttons now include a Google icon.
- Google OAuth Console branding steps are documented for deployed environments.

## Verification

- `./mvnw test` passed from `backend` with 50 tests, 0 failures, 0 errors.
