# Add Google OAuth User Model And Authorization

## Issue

- GitHub: https://github.com/de1mos242/Vocavista/issues/16
- Branch: `16-add-google-oauth-user-model-and-authorization`

## Goal

Add the first Vocavista authorization foundation using Google OAuth. The backend should create or update an application user from the minimal Google identity profile and require authentication for existing API endpoints.

## Scope

- Add Spring Security OAuth2/OIDC login with Google as the first provider.
- Request only minimal identity scopes: OpenID, email, and profile.
- Persist application users with Google provider identity, email, and display name.
- Expose an authenticated current-user endpoint.
- Secure existing `/api/v1/**` API endpoints.
- Keep actuator health and OAuth login/callback endpoints available without prior authentication.

## Decisions

- Use Spring Security's session-based OAuth2 login for the first implementation rather than issuing custom API tokens.
- Keep word-info and pronunciation records globally reusable. This task adds users and authorization but does not change existing cache ownership semantics.
- The default OAuth start URL is `/oauth2/authorization/google`; the default callback URL is `/login/oauth2/code/google`.
- `/api/v1/**` returns `401` for unauthenticated API requests instead of redirecting API clients to Google.
- CSRF is ignored for `/api/**` so existing JSON API calls can continue to work with the authenticated session flow.

## Implementation Notes

- Added `user_accounts` with a unique `(provider, provider_subject)` key and unique email.
- Added `GoogleOidcUserService` to create or update the application user after Google OIDC user info is loaded.
- Added `GET /api/v1/auth/me` to the OpenAPI contract and generated controller implementation.
- Added Google OAuth client configuration placeholders `GOOGLE_OAUTH_CLIENT_ID` and `GOOGLE_OAUTH_CLIENT_SECRET`.
- Existing word and pronunciation API endpoints are secured by the Spring Security filter chain through the `/api/v1/**` path.
- The static Veo preview page now checks `/api/v1/auth/me`, shows either a Google sign-in link or the current user, and disables action buttons while signed out.
- The static Veo preview page provides a logout button; `/logout` returns `204 No Content` so the page can update signed-out state without a redirect.
- The static Veo preview page starts Google sign-in through `/login/google?redirect=...`; the backend stores only safe local return paths and redirects back there after successful OAuth login.

## Progress

- 2026-05-27: Created task and captured implementation scope.
- 2026-05-27: Implemented Google OAuth session login, user account persistence, current-user API, API endpoint security, documentation, and tests.
- 2026-05-27: Adopted the static Veo preview page to the auth flow with current-user display and signed-out action gating.
- 2026-05-27: Added fetch-friendly logout support to Spring Security and the static Veo preview page.
- 2026-05-27: Fixed successful Google login redirect to return users to the page where sign-in started.

## Verification

- 2026-05-27: `./mvnw test` passed in `backend` with 36 tests.
- 2026-05-27: `./mvnw test` passed again after the Veo preview page auth update.
- 2026-05-27: `./mvnw test` passed after logout support with 37 tests.
- 2026-05-27: `./mvnw test` passed after login redirect fix with 39 tests.
