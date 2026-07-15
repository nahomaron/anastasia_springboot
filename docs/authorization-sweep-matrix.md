# Production Authorization Sweep Matrix

## Objective

Harden every production controller before launch so access is:

- explicit
- least-privilege
- tenant-safe
- test-covered

This matrix is based on the current Spring Security configuration, controller mappings, seeded permissions, and test coverage in `Anastasia_BackEnd`.

## Current Root Cause Summary

The inconsistent 403s come from authorization drift rather than one broken mechanism:

- Controllers mix `hasRole`, `hasAnyAuthority`, and the custom permission evaluator.
- Some business areas are guarded by broader fallback permissions from other domains, which makes the effective access model hard to reason about.
- Newer calendar authorization had no dedicated `VIEW_CALENDAR` / `MANAGE_CALENDAR` permissions, so it depended on event or appointment permissions instead of a calendar-specific contract.
- Tenant isolation is enforced separately in `TenantFilter` and in service/repository queries, which is correct but means header/tenant mismatches can surface as 403s before controller logic runs.

The stabilization approach in this pass is to keep tenant boundaries intact, add the missing calendar permissions, and make the calendar controller and occurrence service recognize them alongside the older event-based fallbacks.

## Non-Negotiables

1. Remove broad whitelist patterns from `SecurityConfig`.
   Keep only exact public routes.
   `/api/v1/tenant/**` and `/api/v1/onboarding/**` cannot stay globally public.

2. No business endpoint should rely on fallback `anyRequest().authenticated()`.
   Every non-public controller should have class-level or method-level authorization.

3. Use permissions as the main enforcement model.
   Roles should stay coarse-grained. Permissions should drive business actions.

4. Add ownership and tenant-scope checks for ID-based endpoints.
   `memberId`, `userId`, `tenantId`, `appointmentId`, `sessionId`, `staffId`, `accountId`, and similar identifiers must not be trusted on role checks alone.

5. Every controller family gets negative security tests.

## Public Endpoint Policy

These routes may remain public, but only if explicitly matched in `SecurityConfig` and covered by tests/rate limits:

| Area | Route pattern | Keep public | Notes |
|---|---|---:|---|
| Auth | `/api/v1/auth/**` | Yes | Public by design. Keep rate limiting and token validation strong. |
| OAuth | `/oauth2/**`, `/login/oauth2/**` | Yes | Framework flow. |
| Tenant signup | `/api/v1/tenant/subscription` | Yes | Public registration only. |
| Tenant phone verification | `/api/v1/tenant/verify-phone`, `/api/v1/tenant/resend-phone-otp` | Yes | Public, but rate-limited. |
| Onboarding email verification | `/api/v1/onboarding/email-verification/**` | Yes | Public, rate-limited, anti-enumeration behavior required. |
| Onboarding billing session bootstrap | Specific onboarding session routes only | Conditional | Must be session-bound and non-enumerable. Do not whitelist the whole namespace. |
| Stripe webhook | `/webhooks/stripe` | Yes | Signature verification required. |
| Priest registration | `/api/v1/priests/register` | Conditional | Keep public only if this is a real onboarding flow. Otherwise remove from whitelist. |
| Membership card verification | `/api/v1/membership-cards/verify/**` | Yes | Token-based public lookup. |
| Swagger/Actuator | current docs/actuator routes | No in production | Restrict by profile, network, or admin auth. |
| Dev email preview | `/dev/email/**` | Dev only | Must never be reachable in production profile. |

## Permission Normalization Backlog

Current `PermissionType` covers older modules well enough, but newer domains are incomplete. Add or normalize permissions before the controller sweep:

### Accounting

- `VIEW_ACCOUNTS`
- `MANAGE_ACCOUNTS`
- `VIEW_FUNDS`
- `MANAGE_FUNDS`
- `RECORD_TRANSACTIONS`
- `RECONCILE_ACCOUNTS`
- `IMPORT_FINANCIAL_DATA`
- `EXPORT_FINANCIAL_DATA`

### Staff

- `VIEW_STAFF`
- `MANAGE_STAFF`
- `RESET_STAFF_CREDENTIALS`

### Calendar
- `VIEW_CALENDAR`
- `MANAGE_CALENDAR`

### Tenant Administration

- `VIEW_TENANT_USERS`
- `INVITE_TENANT_USERS`
- `MANAGE_TENANT_USERS`
- `MANAGE_TENANT_BILLING`

### Platform / Support

- `VIEW_PLATFORM_SUBSCRIPTIONS`
- `MANAGE_PLATFORM_SUBSCRIPTIONS`

### Rules

- Stop using mixed-case raw strings in annotations.
- Use enum-backed names only.
- Keep `MANAGE_*` permissions as supersets, but still add action-specific permissions for read/write separation.

## Controller Matrix

### P0: Public Namespace And Zero-Guard Controllers

| Controller | Base path | Current posture | Target posture | Required work | Test priority |
|---|---|---|---|---|---|
| `TenantController` | `/api/v1/tenant` | Exact public routes only; tenant mutation paths guarded | Split public registration routes from authenticated tenant routes | Group C tenant ownership guard implemented for `{tenantId}` update/unsubscribe; matcher tests prevent broad `/api/v1/tenant/**` exposure | P0 |
| `TenantOnboardingBillingController` | `/api/v1/onboarding/billing` | Explicit public self-service routes; session operations require onboarding token | Public only for session-bound onboarding flow | Session access token enforcement exists in service; public session creation and auto-login are now rate-limited and covered by focused tests | P0 |
| `OnboardingEmailVerificationController` | `/api/v1/onboarding/email-verification` | Explicit public endpoints with rate limits | Explicit public endpoints with abuse controls | Keep public intentionally; send/verify rate limits and generic invalid-code response are covered by controller tests | P0 |
| `AccountController` | `/api/v1/accounting/accounts` | Permission-based with tenant resolver | Permission-based tenant admin access | Uses accounting permissions and `AccountingTenantResolver`; focused resolver tests cover missing tenant context and cross-tenant override denial | P0 |
| `FundController` | `/api/v1/accounting/funds` | Permission-based with tenant resolver | Permission-based tenant admin access | Uses `VIEW_FUNDS` / `MANAGE_FUNDS` plus tenant resolver; cross-tenant request tenant IDs are denied centrally | P0 |
| `TransactionController` | `/api/v1/accounting/transactions` | Permission-based with tenant resolver | Permission-based finance access | Uses `MANAGE_FINANCE` / `RECORD_TRANSACTIONS` / read permissions plus tenant resolver; repository reads remain tenant-scoped | P0 |
| `ReportController` | `/api/v1/accounting/reports` | Permission-based with tenant resolver | Permission-based reporting access | Uses report permissions plus tenant resolver; report aggregation queries are tenant-scoped | P0 |
| `ImportExportController` | `/api/v1/accounting/io` | Permission-based with tenant resolver | Highly restricted finance admin access | Export/import use dedicated permissions and tenant resolver; file safety remains a separate non-auth hardening area | P0 |
| `ReconciliationController` | `/api/v1/accounting/reconciliation` | Permission-based with tenant resolver | Highly restricted finance admin access | Uses `RECONCILE_ACCOUNTS` / `MANAGE_FINANCE` and tenant resolver; reconciliation account lookup is tenant-scoped | P0 |
| `WebhookController` | `/webhooks/stripe` | Public by design | Explicit public system endpoint | Keep public, verify signature, add duplicate-event protection tests | P0 |

### P1: Mixed Or Incomplete Guarding

| Controller | Base path | Current posture | Target posture | Required work | Test priority |
|---|---|---|---|---|---|
| `UserController` | `/api/v1/users` | Mixed; several endpoints have no `@PreAuthorize` | Split self-service, tenant admin, platform admin | Group C shared user-entity lookup now respects active tenant unless caller has platform-wide read authority; remaining controller guard cleanup still applies to `/info`, `/update-user-details`, `/avatar` | P1 |
| `ChurchController` | `/api/v1/churches` | Some public list/lookup endpoints, some protected | Explicit decision per lookup route | Decide whether `GET /churches` and `GET /by-number/*` are public or authenticated; if public, test enumeration risk and returned fields | P1 |
| `StaffController` | `/api/v1/staff` | Class-level guard exists, but permission string drift is present | Stable class-level guard with normalized permissions | Remove lowercase permission alias, add read/write split if needed, add reset-credentials-specific permission | P1 |
| `NotificationController` | `/api/v1/notifications` | Broad role-based class guard | Self-service authenticated access | Consider replacing broad role list with `isAuthenticated()` if service methods are self-scoped; otherwise keep permission-based admin paths separate | P1 |
| `ImageAssetController` | `/api/v1/images` | Controller is authenticated; service now enforces owner-type read/write permissions | Self-service plus ownership checks | Implemented type-specific permission checks for user/member/child/church/group/event images; keep adding tests around ownership and tenant isolation | P1 |
| `DashboardController` | `/api/v1/dashboard` | Role-based only | Role plus permission or service-scoped access | Fine for now, but confirm no data leakage across tenant/user scope | P1 |
| `MembershipCardController` | `/api/v1/membership-cards` | Mostly guarded, one public token verify endpoint | Mixed public/protected by design | Keep `/verify/{token}` public; Group C self-service `/me` paths now validate current membership against active tenant before card lookup/download | P1 |
| `TenantEntitlementController` | `/api/v1/subscriptions` | Reasonable auth; finance/subscription permissions mixed | Keep, but normalize intent | Use `OWN_SUBSCRIPTION` for owner-billing actions, `MANAGE_TENANT_BILLING` for delegated admins | P1 |
| `PlatformSubscriptionAdminController` | `/api/v1/platform/subscriptions` | Platform admin only | Keep platform-only | Add tests for platform-only access and audit sensitive changes | P1 |
| `DevEmailPreviewController` | `/dev/email` | Dev profile only, no auth | Dev-only support endpoint | Verify production profile cannot expose it; consider extra auth in shared dev/test envs | P1 |

### P2: Older Controllers That Need Consistency Review

| Controller | Base path | Current posture | Target posture | Required work | Test priority |
|---|---|---|---|---|---|
| `RoleController` | `/api/v1/admin` | Class-level `MANAGE_ROLES` plus role checks | Permission-first admin access | Keep permission requirement; confirm role checks are not redundant with desired delegated-admin behavior | P2 |
| `TenantAdminAssignmentController` | `/api/v1/tenant/admin-assignments` | Role or `MANAGE_USERS` | Permission-first tenant admin access | Consider replacing role bypass with `MANAGE_TENANT_USERS` / `MANAGE_USERS`; add audit tests | P2 |
| `MemberController` | `/api/v1/registrar/members` | Permission-first admin actions plus authenticated self-service routes | Permission-first with self-service separation | Self-registration is now authenticated-only; family relationship update/delete owner scoping has regression tests | P2 |
| `ChildController` | `/api/v1/registrar/children` | Permission-first admin actions plus authenticated self-service registration | Permission-first with self-service separation | Self-registration is now authenticated-only; child admin actions remain explicit permission checks | P2 |
| `PriestController` | `/api/v1/priests` | Mixed; public registration, authenticated church lists, permission-gated admin actions | Separate public registration, priest self-scope, and admin actions | Removed `MANAGE_USERS` fallback and blocked non-platform tenant override on assignment lookups; still revisit whether authenticated church priest lists should require `VIEW_PRIESTS` or remain member-facing discovery | P2 |
| `MemberBulkActionController` | `/api/v1/registrar/members/bulk` | Stronger than most | Permission-first bulk ops | Consider more granular bulk communication/group permissions | P2 |
| `MemberServiceRequestController` | `/api/v1/member-service-requests` | Role-based only | Self-service/member-service permission model | Group C baptism requests now require active tenant, reject church mismatch, and list current-user requests by tenant plus requester; broader reviewer/admin visibility still needs separate policy review | P2 |
| `GroupController` | `/api/v1/groups` | Permission checks plus `GroupSecuritySupport` owner/moderator checks | Permission-first with owner/moderator checks | Retracted role-name shortcuts from `GroupSecuritySupport`; moderation now requires group permissions or assigned manager ownership and is covered by annotation/delegation tests | P0 |
| `EventController` | `/api/v1/events` | Broad role fallback plus event permissions | Permission-first with event manager scope | Tighten member visibility vs manager/admin operations; validate event manager ownership | P2 |
| `CalendarController` | `/api/v1/calendar` | Role fallback plus event permissions | Dedicated calendar permission model | Add `VIEW_CALENDAR` / `MANAGE_CALENDAR`, confirm recurrence edits are tenant-safe | P2 |
| `AppointmentController` | `/api/v1/appointments` | Permission-first with separate self-service member flow | Permission-first with read/write split | Added `VIEW_APPOINTMENTS` for schedule/detail reads; mutations stay on `MANAGE_APPOINTMENT`; `/me` visibility has member-scope regression tests | P2 |
| `PaymentController` | `/api/v1/payments` | Protected | Keep, with finance-specific permissions | Confirm donation vs subscription permissions and tenant context | P2 |
| `PaymentQueryController` | `/api/v1/payments` | Protected | Keep, with read-specific finance permissions | Confirm summaries are tenant-safe and read-only | P2 |
| `SubscriptionQueryController` | `/api/v1/payments/subscriptions` | Protected | Keep | Consider `OWN_SUBSCRIPTION` vs finance-report view split | P2 |
| `NotificationController` | `/api/v1/notifications` | Self-service role list | Keep or simplify | Included above because it is likely over-specified rather than under-specified | P2 |

### P3: Marriage Service Domain

Current marriage controllers are mostly guarded, but rely heavily on broad role access and `MANAGE_SERVICES`.

| Controller | Base path | Current posture | Target posture | Required work | Test priority |
|---|---|---|---|---|---|
| `MarriageLookupController` | `/api/v1/marriage-lookups` | Protected | Keep | Confirm read-only lookup scope | P3 |
| `MarriageCaseController` | `/api/v1/marriage-cases` | Protected, but broad member/user/admin access | Fine-grained self-service plus reviewer/admin permissions | Separate applicant actions from church staff actions; ensure `caseId` access is participant-bound | P2 |
| `MarriageCaseCollaborationController` | `/api/v1/marriage-cases` | Protected, but note/history access is broad | Participant/reviewer scoped | Notes/history should be restricted to case participants and assigned reviewers, not all authenticated roles listed | P2 |
| `MarriageOperationsController` | `/api/v1/marriage-cases` | Protected | Keep, but refine | Split payment, witness, priest assignment, and scheduling permissions if domain complexity grows | P3 |
| `MarriageReviewController` | `/api/v1/marriage-cases` | Protected | Keep, but refine | Secretary/admin/reviewer flows should be separated cleanly | P3 |
| `MarriageCertificateController` | `/api/v1` | Protected | Keep | Confirm registry and certificate retrieval are limited by role and tenant | P3 |

## Test Sweep Matrix

Add or expand API security tests for these controller families:

### Must add

- accounting
  - accounts
  - funds
  - transactions
  - reports
  - import/export
  - reconciliation
- onboarding
  - billing session flow
  - email verification
- payments
  - payment create/query
  - subscription query
  - webhook verification
- staff
- dashboard
- notifications
- groups join-request moderation
- user self-service and tenant-access endpoints

### Existing test families to extend

- auth
- tenant
- church
- priest
- member
- child
- group
- user
- event
- avatar

## Implementation Order

### Phase 1: Stop Public Exposure

- Replace broad whitelist entries with exact public routes.
- Add tests that unauthenticated access is rejected outside the approved public list.
- Lock down accounting controllers immediately.
- Lock down onboarding billing flow immediately.

### Phase 2: Normalize Permissions

- Expand `PermissionType`.
- Update seeding and role grants.
- Remove mixed-case raw permission strings.
- Introduce class-level controller guards where obvious.

### Phase 3: Tenant And Ownership Hardening

- Remove trust in request/body `tenantId` where tenant context already exists.
- Add ownership checks for self-service endpoints.
- Add assigned-manager or participant checks where role-only checks are too broad.

### Phase 4: Regression Tests

Status:

- Implemented fast annotation regression tests for permission drift across accounting, payment, staff, platform-only, appointment, group, member, and child controllers.
- Added payment tenant-context controller tests for missing tenant context and disabled stewardship entitlement.
- Reused existing focused tests for tenant path ownership, onboarding token/rate-limit behavior, accounting tenant resolver mismatch denial, membership-card self-service tenant checks, baptism tenant scoping, group moderation, appointment member scope, image permissions, member self-service ownership, and user lookup tenant scoping.

### Phase 5: Frontend Auth/Header Alignment

Status:

- Updated the Angular tenant interceptor so protected authenticated API calls include `X-Tenant-Id` when a tenant is active, preventing tenant-context 403s caused by missing headers.
- Added auth-token and tenant-interceptor regression tests for protected API calls, public onboarding calls, explicit tenant headers, and non-API requests.
- Synced frontend permission catalog with backend permissions added during the authorization sweep.
- Updated tenant-admin navigation gates to include read permissions for staff, calendar, and appointments.

- Add negative and positive security tests for every controller family.
- Add cross-tenant denial tests.
- Add public-endpoint abuse tests for onboarding/auth verification flows.

## Definition Of Done

- Only explicitly approved public routes are public.
- No production controller depends on fallback authentication alone.
- Every controller action maps to a permission or documented self-service rule.
- Every ID-based action is tenant-safe and ownership-safe.
- Security tests cover allow and deny cases for all controller families.
