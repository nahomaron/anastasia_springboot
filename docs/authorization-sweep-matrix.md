# Production Authorization Sweep Matrix

## Objective

Harden every production controller before launch so access is:

- explicit
- least-privilege
- tenant-safe
- test-covered

This matrix is based on the current Spring Security configuration, controller mappings, seeded permissions, and test coverage in `Anastasia_BackEnd`.

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
| `TenantController` | `/api/v1/tenant` | Namespace is globally public; only some methods have `@PreAuthorize` | Split public registration routes from authenticated tenant routes | Remove namespace whitelist; explicitly mark only subscribe/verify/resend public; require auth for current-status/update/unsubscribe; verify tenant ownership on `{tenantId}` operations | P0 |
| `TenantOnboardingBillingController` | `/api/v1/onboarding/billing` | Entire namespace public; no method security | Public only for session-bound onboarding flow | Replace broad whitelist with explicit routes; require signed or opaque session ownership for `checkout`, `finalize`, `getSession`, `auto-login`; rate-limit `sessions` and `auto-login` | P0 |
| `OnboardingEmailVerificationController` | `/api/v1/onboarding/email-verification` | Public; no method security | Explicit public endpoints with abuse controls | Keep public intentionally, add rate limits, anti-enumeration, replay protection, attempt throttling | P0 |
| `AccountController` | `/api/v1/accounting/accounts` | Authenticated fallback only | Permission-based tenant admin access | Add class-level guard using `MANAGE_FINANCE` plus new account permissions; replace request `tenantId` trust with tenant-context validation | P0 |
| `FundController` | `/api/v1/accounting/funds` | Authenticated fallback only | Permission-based tenant admin access | Add read/write split with `VIEW_FUNDS` and `MANAGE_FUNDS`; enforce tenant context | P0 |
| `TransactionController` | `/api/v1/accounting/transactions` | Authenticated fallback only | Permission-based finance access | Guard income/expense/transfer with `MANAGE_FINANCE` or `RECORD_TRANSACTIONS`; verify tenant scope server-side | P0 |
| `ReportController` | `/api/v1/accounting/reports` | Authenticated fallback only | Permission-based reporting access | Require `VIEW_FINANCE_REPORT` or `GENERATE_FINANCE_REPORT`; verify report scope and tenant | P0 |
| `ImportExportController` | `/api/v1/accounting/io` | Authenticated fallback only | Highly restricted finance admin access | Export needs `EXPORT_FINANCIAL_DATA`; import needs `IMPORT_FINANCIAL_DATA`; validate tenant and file safety | P0 |
| `ReconciliationController` | `/api/v1/accounting/reconciliation` | Authenticated fallback only | Highly restricted finance admin access | Require `MANAGE_FINANCE` or `RECONCILE_ACCOUNTS`; do not trust body `tenantId`; bind to tenant context | P0 |
| `WebhookController` | `/webhooks/stripe` | Public by design | Explicit public system endpoint | Keep public, verify signature, add duplicate-event protection tests | P0 |

### P1: Mixed Or Incomplete Guarding

| Controller | Base path | Current posture | Target posture | Required work | Test priority |
|---|---|---|---|---|---|
| `UserController` | `/api/v1/users` | Mixed; several endpoints have no `@PreAuthorize` | Split self-service, tenant admin, platform admin | Add guards to `/info`, `/update-user-details`, `/avatar`; fix inconsistent admin/user access; validate self-only semantics | P1 |
| `ChurchController` | `/api/v1/churches` | Some public list/lookup endpoints, some protected | Explicit decision per lookup route | Decide whether `GET /churches` and `GET /by-number/*` are public or authenticated; if public, test enumeration risk and returned fields | P1 |
| `StaffController` | `/api/v1/staff` | Class-level guard exists, but permission string drift is present | Stable class-level guard with normalized permissions | Remove lowercase permission alias, add read/write split if needed, add reset-credentials-specific permission | P1 |
| `NotificationController` | `/api/v1/notifications` | Broad role-based class guard | Self-service authenticated access | Consider replacing broad role list with `isAuthenticated()` if service methods are self-scoped; otherwise keep permission-based admin paths separate | P1 |
| `ImageAssetController` | `/api/v1/images` | `isAuthenticated()` only | Self-service plus ownership checks | Ensure upload/read is bound to owner object access rules, not just authentication | P1 |
| `DashboardController` | `/api/v1/dashboard` | Role-based only | Role plus permission or service-scoped access | Fine for now, but confirm no data leakage across tenant/user scope | P1 |
| `MembershipCardController` | `/api/v1/membership-cards` | Mostly guarded, one public token verify endpoint | Mixed public/protected by design | Keep `/verify/{token}` public; verify all member/admin actions enforce ownership or membership visibility | P1 |
| `TenantEntitlementController` | `/api/v1/subscriptions` | Reasonable auth; finance/subscription permissions mixed | Keep, but normalize intent | Use `OWN_SUBSCRIPTION` for owner-billing actions, `MANAGE_TENANT_BILLING` for delegated admins | P1 |
| `PlatformSubscriptionAdminController` | `/api/v1/platform/subscriptions` | Platform admin only | Keep platform-only | Add tests for platform-only access and audit sensitive changes | P1 |
| `DevEmailPreviewController` | `/dev/email` | Dev profile only, no auth | Dev-only support endpoint | Verify production profile cannot expose it; consider extra auth in shared dev/test envs | P1 |

### P2: Older Controllers That Need Consistency Review

| Controller | Base path | Current posture | Target posture | Required work | Test priority |
|---|---|---|---|---|---|
| `RoleController` | `/api/v1/admin` | Class-level `MANAGE_ROLES` plus role checks | Permission-first admin access | Keep permission requirement; confirm role checks are not redundant with desired delegated-admin behavior | P2 |
| `TenantAdminAssignmentController` | `/api/v1/tenant/admin-assignments` | Role or `MANAGE_USERS` | Permission-first tenant admin access | Consider replacing role bypass with `MANAGE_TENANT_USERS` / `MANAGE_USERS`; add audit tests | P2 |
| `MemberController` | `/api/v1/registrar/members` | Heavy role fallback with some permission use | Permission-first with self-service separation | Review `MEMBER` and `USER` access on family endpoints; ensure read/search/admin actions are not over-broad | P2 |
| `ChildController` | `/api/v1/registrar/children` | Similar to member controller | Permission-first with self-service separation | Remove broad role reliance where specific child/member permissions exist | P2 |
| `PriestController` | `/api/v1/priests` | Mixed; public registration and broad role access | Separate public registration, priest self-scope, and admin actions | Revisit whether `USER` should list priests by church; ensure priest-specific data is tenant-safe | P2 |
| `MemberBulkActionController` | `/api/v1/registrar/members/bulk` | Stronger than most | Permission-first bulk ops | Consider more granular bulk communication/group permissions | P2 |
| `MemberServiceRequestController` | `/api/v1/member-service-requests` | Role-based only | Self-service/member-service permission model | Confirm requests are only visible to requester or reviewers | P2 |
| `GroupController` | `/api/v1/groups` | Mixed role checks and plain `isAuthenticated()` on request management | Permission-first with owner/moderator checks | Tighten join-request listing/approval/rejection so ordinary authenticated users cannot administer groups | P0 |
| `EventController` | `/api/v1/events` | Broad role fallback plus event permissions | Permission-first with event manager scope | Tighten member visibility vs manager/admin operations; validate event manager ownership | P2 |
| `CalendarController` | `/api/v1/calendar` | Role fallback plus event permissions | Dedicated calendar permission model | Add `VIEW_CALENDAR` / `MANAGE_CALENDAR`, confirm recurrence edits are tenant-safe | P2 |
| `AppointmentController` | `/api/v1/appointments` | Good base, but mostly role fallback | Permission-first with self-service member flow | Confirm `/me` endpoints cannot access other users' appointments; verify assignee/participant admin ops | P2 |
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

- Add negative and positive security tests for every controller family.
- Add cross-tenant denial tests.
- Add public-endpoint abuse tests for onboarding/auth verification flows.

## Definition Of Done

- Only explicitly approved public routes are public.
- No production controller depends on fallback authentication alone.
- Every controller action maps to a permission or documented self-service rule.
- Every ID-based action is tenant-safe and ownership-safe.
- Security tests cover allow and deny cases for all controller families.
