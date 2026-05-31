# 32 Add Admin User Status Management And Restrict Expensive AI APIs

## Issue

- GitHub: https://github.com/de1mos242/Vocavista/issues/32

## Goal

Add persisted user statuses, configurable admin-list privileges, an admin user-management page, and server-side access control for authenticated app features.

## Scope

- Persist user account status: `pending`, `active`, `deactivated`.
- Default new non-admin users to `pending` and configured admin-list users to `active`.
- Configure admin emails with default `de1m0s242@gmail.com`.
- Backfill existing admin-list users to `active` and existing non-admin users to `pending`.
- Add admin API and static admin page for listing users and changing non-admin user statuses.
- Reject status changes for users protected by the configured admin list.
- Expose current user status/admin/AI-access flags to the UI.
- Disable expensive AI actions in the UI for pending/deactivated non-admin users.
- Enforce expensive AI access server-side for word info generation and pronunciation video generation.

## Constraints

- Admin privilege is based on configured email list, not persisted status.
- Pending and deactivated users can still sign in.
- UI disabling is advisory only; backend checks are required.
- Pending and deactivated users should keep access to `/api/v1/auth/me` so the UI can explain their account state.
- Preserve existing static-page visual style.

## Implementation Notes

- Use Spring Boot configuration properties for normalized admin emails.
- Keep authorization logic centralized in the auth package.
- Use `@RequireFunctionalAccess` instead of injecting access checks directly into controllers.
- Extend the OpenAPI spec so generated API/model interfaces stay authoritative.
- Restrict all functional API controllers with `@RequireFunctionalAccess`; leave `/api/v1/auth/me` unannotated.

## Progress

- Task worktree exists on branch `32-add-admin-user-status-management-and-restrict-expensive-ai-apis`.
- Added `UserAccountStatus` persistence with Flyway migration `V6__user_account_status.sql`.
- Added configurable `vocavista.admin.emails` with default `de1m0s242@gmail.com` and startup activation for configured admin users.
- Kept the default admin email in `application.yaml`; `AdminProperties` has no code-level default.
- Added centralized access checks for admin-list users and functional app usage.
- Replaced controller-injected access checks with `@RequireFunctionalAccess` plus a Spring MVC interceptor.
- Added admin OpenAPI endpoints, controller/service, and generated models for listing users and updating statuses.
- Added `/admin.html` and updated existing static pages to show account-status notices and disable functional actions for pending/deactivated users.
- Fixed hidden account-notice containers by adding explicit `[hidden] { display: none !important; }` styling to affected static pages.
- Added server-side checks for dictionary, word, media, and admin functional endpoints while keeping `/api/v1/auth/me` available.
- Added/updated tests for signup defaults, current user flags, admin status updates/protection, and functional API authorization.
- Posted implementation status to GitHub issue comment https://github.com/de1mos242/Vocavista/issues/32#issuecomment-4588111577.
- Posted updated functional-access and annotation decision to GitHub issue comment https://github.com/de1mos242/Vocavista/issues/32#issuecomment-4588145208.
- Posted admin-email default configuration follow-up to GitHub issue comment https://github.com/de1mos242/Vocavista/issues/32#issuecomment-4588153199.
- Posted hidden account-notice UI fix to GitHub issue comment https://github.com/de1mos242/Vocavista/issues/32#issuecomment-4588161874.

## Verification

- `./mvnw test` passed from `backend` after switching to annotation-based functional access checks.
- `./mvnw test -Dtest=UserAccountServiceTest,AuthControllerTest,AdminUserManagementServiceTest` passed after moving the admin email default out of `AdminProperties`.
