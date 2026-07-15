# Authorization Audit Findings

This document records the current authorization assessment for the backend and the next-step task split.

## Executive Summary

The app does not have a single authorization failure. It has authorization drift:

- roles, permissions, JWT authorities, controller guards, and tenant filters are all working, but not always in the same way
- some controllers are over-reliant on broad role fallbacks
- some endpoints are under-provisioned because the intended permission does not exist yet
- tenant isolation is correctly enforced in several layers, but that makes auth failures look inconsistent when the request tenant context is wrong

The result is predictable: fixing one endpoint can expose another endpoint that was silently depending on the old fallback path.

## Source Of Truth

The current source of truth is split across these places:

- `PermissionType` for canonical permission names
- `RoleType` for built-in role permission sets
- `Role` and `Permission` entities for database-backed mappings
- `UserPrincipal` for runtime authorities
- `JwtUtil` for JWT claim serialization
- `TenantFilter` and service/repository tenant checks for tenant scope
- controller annotations and helper checks for endpoint-level access control

## Findings

### 1. Permissions are incomplete for some business domains

Some controllers rely on permissions from a neighboring domain instead of a purpose-built permission set.

Examples:

- calendar currently leans on event and appointment permissions
- some admin-style user and tenant flows are protected by general user-management permissions
- some feature areas still use only `isAuthenticated()` where a real business permission would be clearer

Impact:

- authorized users can be denied if they have the right role intent but not the borrowed permission
- unrelated fixes can break previously working endpoints

### 2. Some roles are too broad for some endpoints, and too narrow for others

The built-in roles are not consistently aligned with the access model implied by controllers.

Examples:

- `PRIMARY_ADMIN` is close to tenant-superuser behavior
- `ADMIN` is sometimes treated as a fallback for tenant admin behavior, sometimes not
- `PRIEST` is used as a broad service-role in places that should be permission-driven

Impact:

- a role may work for one endpoint but fail for another in the same module
- this creates the appearance of random 403s even when the user is valid

### 3. Tenant isolation is correct, but failure signaling is not always distinct

Tenant access is enforced in multiple layers:

- request header resolution
- JWT tenant claim fallback
- tenant context filters
- service-side tenant checks
- repository tenant-scoped queries

Impact:

- wrong or missing tenant headers can look like authorization failures
- cross-tenant denials are correct, but the failure path is not always obvious to callers

### 4. Some controller methods are guarded at the wrong level

Observed patterns:

- `isAuthenticated()` is used for some actions that are really business-authorized operations
- class-level guards are sometimes too coarse for mixed read/write controllers
- ownership checks are sometimes only enforced in service logic, which is valid but should be mirrored by clear controller intent

Impact:

- permissions are either too strong for a read-only endpoint or too weak for a write endpoint
- `/me` routes and administrative routes are not always separated cleanly

### 5. JWT and runtime authority generation are mostly consistent, but role naming must stay stable

Runtime authority construction includes:

- role names
- `ROLE_`-prefixed authorities
- permission enum names

Impact:

- if a new controller uses a permission that is not present in the seeded role set, older users can be denied until the role mapping is refreshed
- if a controller uses `hasRole(...)`, the role name and `ROLE_` prefix behavior must stay consistent

## High-Risk Controller Groups

These are the main groups that need a permission normalization pass:

- `CalendarController`
- `EventController`
- `AppointmentController`
- `GroupController`
- `UserController`
- `TenantAdminAssignmentController`
- `TenantController`
- `TenantEntitlementController`
- accounting controllers under `/api/v1/accounting/**`
- `ImageAssetController`
- `MemberController`
- `ChildController`
- `PriestController`
- platform admin and support controllers under `/api/v1/platform/**`

## Recommended Change Pattern

1. Prefer permission-based checks for business actions.
2. Use role-based checks only when the role itself is the boundary, such as platform administration.
3. Keep tenant isolation mandatory and explicit.
4. Add missing permissions for business areas instead of borrowing unrelated ones.
5. Split read and write access when the controller mixes both.
6. Treat `/me` endpoints as self-service and enforce ownership in service logic.
7. Keep seed data and JWT authority generation aligned with the permission catalog.

## Task Groups For Parallel Work

### Group A: Permission Catalog And Role Seeding

Goal:

- add missing permissions
- normalize role permission sets
- update seed/bootstrap data
- identify users or tenants that need permission refresh logic

Deliverables:

- updated permission enum
- updated role definitions
- seed/migration adjustments
- notes for any data backfill needed

Implementation status:

- Added missing catalog permission `DELETE_GROUPS`.
- Added dedicated calendar permissions `VIEW_CALENDAR` and `MANAGE_CALENDAR`.
- Made `PLATFORM_ADMIN` carry the full permission catalog.
- Expanded `PRIMARY_ADMIN` into an explicit tenant-wide permission superset while excluding platform-only `MANAGE_TENANTS`, `VIEW_ALL_DATA`, and owner-only `OWN_SUBSCRIPTION`.
- Added read-facing event/calendar permissions to `PRIEST`.
- Updated permission lookup to use enum values instead of raw strings.
- Updated production and test seeders to add missing permissions to existing built-in roles instead of only creating roles when absent.
- Added Flyway backfill `V39__authorization_permission_catalog_sync.sql` for deployed databases.

### Group B: Controller Authorization Normalization

Goal:

- audit each controller endpoint
- decide whether each endpoint needs a permission add, permission retract, or ownership rule
- standardize `@PreAuthorize` usage

Deliverables:

- controller-by-controller authorization matrix
- concrete before/after permission mapping
- list of endpoints that should stop relying on broad fallbacks

Implementation status:

- Updated `TenantAdminAssignmentController` to use tenant-specific permissions instead of broad `MANAGE_USERS` fallbacks.
- Updated `UserController` tenant-access routes to use `VIEW_TENANT_USERS`, `INVITE_TENANT_USERS`, and `MANAGE_TENANT_USERS`.
- Split accounting account/fund/transaction/import-export authorization from class-level broad guards into method-level read/write permissions.
- Added `VIEW_FINANCE_REPORT` as a read/report-compatible permission for report generation and transaction reads.
- Updated `EventController` read endpoints to require event/report permissions instead of plain authentication.
- Added `DELETE_EVENTS` as an explicit event-delete permission alternative.
- Kept QR event check-in open to `VIEW_EVENTS` because the service handles it as authenticated self check-in against tenant and event scope.
- Updated `PriestController` to retract `MANAGE_USERS` from priest read/write endpoints; priest administration now uses `VIEW_PRIESTS`, `MANAGE_PRIESTS`, platform `MANAGE_TENANTS`, or read-only `VIEW_ALL_DATA` where appropriate.
- Updated `ImageAssetServiceImpl` so image reads and writes use owner-type-specific permission sets instead of one generic elevated list. Read-only authorities such as `VIEW_ALL_DATA` no longer authorize image writes.
- Updated `GroupSecuritySupport` to retract hard-coded role shortcuts (`ROLE_ADMIN`, `ROLE_PRIEST`, etc.) from group moderation. Group moderation now requires explicit group permissions or assigned group-manager ownership.
- Added `VIEW_APPOINTMENTS` for appointment schedule/detail reads and updated `AppointmentController` read endpoints to accept it without granting appointment mutation privileges.
- Cleaned up member/child self-registration annotations so self-service registration is explicitly authenticated-only, while admin registration remains permission-gated by `ADD_MEMBERS` / `MANAGE_MEMBERS`.
- Refined priest assignment lookup tenant handling so tenant-scoped users cannot override `tenantId`; cross-tenant lookup now requires `MANAGE_TENANTS` or `VIEW_ALL_DATA`.
- Added focused annotation/tenant-override regression tests for appointment read/write separation, member/child self-registration, and priest assignment tenant override.
- Added deeper Group B regression coverage for member-family owner scoping, appointment `/me` visibility, and group join-request moderation contracts.

#### Controller-by-controller action list

Legend:

- `add permission` means the controller should gain a missing or more precise permission.
- `retract permission` means the controller should drop an over-broad fallback or role shortcut.
- `keep as-is` means the current shape is acceptable for now, assuming tenant/ownership checks remain in place.

##### Core auth and admin

| Controller | Decision | Notes |
|---|---|---|
| `RoleController` | keep as-is | `MANAGE_ROLES` is the right boundary for role administration. Keep role naming checks only where necessary. |
| `UserController` | add permission | Keep self-service routes as authenticated, but tighten admin-style routes with explicit permissions such as `VIEW_TENANT_USERS`, `INVITE_TENANT_USERS`, and `MANAGE_TENANT_USERS`. |
| `PlatformAdminRegistrationController` | keep as-is | Public bootstrap is intentionally separate and rate-limited. |

##### Tenant and subscription

| Controller | Decision | Notes |
|---|---|---|
| `TenantController` | add permission | Public subscription and verification routes are fine, but authenticated tenant-management routes should be split more clearly and tied to tenant-scoped permissions. |
| `TenantAdminAssignmentController` | add permission | `MANAGE_USERS` is too broad for delegated tenant admin work; prefer `MANAGE_TENANT_USERS` / `VIEW_TENANT_USERS` and keep billing contact changes separate. |
| `TenantSettingsController` | keep as-is | Current tenant-level gating is acceptable if the tenant context is enforced. |
| `TenantEntitlementController` | keep as-is | The `OWN_SUBSCRIPTION` / billing permission mix is reasonable, but should stay tenant-scoped. |
| `TenantOnboardingBillingController` | add permission | Public onboarding bootstrap is too broad for every operation; session-bound routes should be explicitly protected by session ownership or a dedicated onboarding permission. |
| `PlatformSubscriptionAdminController` | keep as-is | Platform-only boundary is correct. |
| `ChurchController` | keep as-is | Mostly correct, but the public lookup endpoints should be reviewed separately for data leakage risk. |

##### Registration and people management

| Controller | Decision | Notes |
|---|---|---|
| `MemberController` | add permission | Self-service registration is now explicitly authenticated-only; admin search, edit, approval, delete, and advanced search remain explicit permission checks. Family relationship owner-scope is covered by service regression tests. |
| `ChildController` | add permission | Self-service registration is now explicitly authenticated-only; child list/edit/delete/admin registration remain child/member permission-gated. |
| `MemberBulkActionController` | keep as-is | `MANAGE_MEMBERS` is acceptable for bulk operations, but it is a strong permission and should stay limited. |
| `MembershipCardController` | keep as-is | Public verify endpoint is acceptable; the rest should remain permission-gated with ownership validation. |
| `ImageAssetController` | add permission | Implemented in the service boundary: image access is now split by owner type and read/write intent while keeping self-user image ownership. |
| `PriestController` | retract permission | Removed `MANAGE_USERS` fallback from priest endpoints and blocked tenant-id override in assignment lookups unless the caller has platform authority. Keep authenticated church-priest lists for member-facing discovery unless a separate public/read permission is introduced. |

##### Events, calendar, appointments, groups

| Controller | Decision | Notes |
|---|---|---|
| `CalendarController` | add permission | Add and keep dedicated calendar permissions; do not rely only on event or appointment fallback. |
| `EventController` | add permission | Introduce clearer read/write separation where calendar/event operations diverge, especially for reporting and attendance management. |
| `AppointmentController` | add permission | Added `VIEW_APPOINTMENTS` for admin/staff schedule reads. Mutation endpoints remain on `MANAGE_APPOINTMENT`, while `/me` endpoints remain self-service with member-visible appointment tests. |
| `GroupController` | retract permission | Join-request administration remains behind `GroupSecuritySupport`; helper now requires explicit group permissions or assigned manager ownership, not role name shortcuts. Controller annotation and delegation tests cover moderation routes. |
| `MemberServiceRequestController` | add permission | Self-service request creation can stay authenticated, but reviewer/admin visibility and action paths should be more explicit if expanded. |

##### Accounting and finance

| Controller | Decision | Notes |
|---|---|---|
| `AccountController` | add permission | Needs explicit account permissions; `MANAGE_FINANCE` alone is too coarse for all account actions. |
| `FundController` | add permission | Split view/manage semantics for funds and keep tenant resolution explicit. |
| `TransactionController` | add permission | Finance write paths need explicit transaction permissions, not only generic finance management. |
| `ReportController` | add permission | Reporting should be gated by read/report-specific permissions, not general finance access. |
| `ImportExportController` | add permission | Import and export should be separated into distinct permissions. |
| `ReconciliationController` | add permission | Reconciliation should stay restricted with a dedicated permission or a very narrow finance-admin boundary. |

##### Staff, dashboard, notifications, mobile

| Controller | Decision | Notes |
|---|---|---|
| `StaffController` | keep as-is | Current permission model is acceptable, but confirm the read/write split matches the real product flow. |
| `DashboardController` | keep as-is | Broad dashboard visibility is acceptable if response data is tenant-safe and does not leak cross-role data. |
| `NotificationController` | keep as-is | Authentication is acceptable because the service appears self-scoped; if admin notification management grows, it should split out. |
| `MobileController` | add permission | This area should be checked carefully for stale role fallbacks and data-scope assumptions; it is likely to need permission refinement. |

##### Service and marriage domain

| Controller | Decision | Notes |
|---|---|---|
| `MarriageCaseController` | keep as-is | Broad authenticated access is acceptable only because service logic appears to enforce deeper case ownership and workflow checks. |
| `MarriageCaseCollaborationController` | retract permission | Notes/history endpoints look overly open for general authenticated access and should be narrowed to participants or reviewers where applicable. |
| `MarriageOperationsController` | keep as-is | Current service-level permission is strong enough for now, but this should be revisited if the workflow splits further. |
| `MarriageReviewController` | keep as-is | Same reasoning as operations; permission is broad but intentional for the current workflow. |
| `MarriageCertificateController` | keep as-is | Service-admin style access is acceptable if tenant and registry checks remain intact. |
| `MarriageLookupController` | keep as-is | Read-only lookup access is acceptable, assuming the lookup data itself is not tenant-leaking. |

##### Platform, support, system

| Controller | Decision | Notes |
|---|---|---|
| `PlatformAdminController` | keep as-is | Platform admin boundary should stay role-based. |
| `PlatformSubscriptionAdminController` | keep as-is | Already platform-only and should remain separate from tenant roles. |
| `SesSnsWebhookController` | keep as-is | Public webhook access is fine only with signature verification. |
| `NotificationController` | keep as-is | Included above; no extra platform split needed unless admin notification management is added. |
| `DevEmailPreviewController` | keep as-is | Dev-only support surface. |

##### Test and internal-only controllers

| Controller | Decision | Notes |
|---|---|---|
| `TestLookupController` | keep as-is | Test-only surface; not part of the production authorization contract. |
| `TestCleanupController` | keep as-is | Test-only surface. |
| `TestAuthController` | keep as-is | Test-only surface. |
| `TestSubscriptionController` | keep as-is | Test-only surface. |

#### Priority order

1. `CalendarController`
2. `EventController`
3. `GroupController`
4. `UserController`
5. `TenantAdminAssignmentController`
6. accounting controllers
7. `ImageAssetController`
8. `PriestController`
9. `AppointmentController`
10. `MarriageCaseCollaborationController`

#### Notes on the decisions

- `add permission` is the default action when a controller is using an unrelated fallback or missing a dedicated business permission.
- `retract permission` is appropriate when `isAuthenticated()` or a role fallback is giving too much trust to a moderation or ownership-sensitive route.
- `keep as-is` is only acceptable when deeper service-layer checks already carry the real boundary and the controller guard is not the weak point.

### Group C: Tenant Isolation And Ownership Checks

Goal:

- verify tenant context is required where appropriate
- verify cross-tenant access is denied in service and repository layers
- audit `/me` endpoints for self-only semantics

Deliverables:

- list of endpoints with tenant risk
- list of service methods needing ownership checks
- list of repository methods that should stay tenant-scoped

Implementation status:

- `TenantController` now performs a path-tenant ownership check before tenant update/unsubscribe operations, while preserving platform-authority override for platform-scoped access.
- `BaptismRequestServiceImpl` now requires active tenant context for create/list-mine, rejects church submissions outside the active tenant, and lists requester records by tenant plus user.
- `UserServiceImpl.findEntity` now follows tenant-scoped lookup rules unless the caller has platform-wide read authority.
- `MembershipCardService` now validates `/me` membership ownership against the active tenant before reading or downloading the current user's card.
- Focused tests were added for tenant path checks, baptism request tenant scoping, user entity lookup scoping, and membership-card self-service tenant isolation.

High-risk follow-up status:

- Tenant namespace exposure is guarded by exact public matcher tests so `/api/v1/tenant/**` is not broadly whitelisted.
- Onboarding billing remains intentionally public for self-service signup, but session replay/access is token-bound and public session creation plus auto-login are rate-limited.
- Accounting tenant override risk is handled by `AccountingTenantResolver`, which requires active tenant context and rejects mismatched request tenant IDs.
- Focused tests now cover onboarding access-token delegation, onboarding billing rate limits, broad matcher regression prevention, accounting tenant mismatch denial, and tenant path ownership denial.

### Group D: Regression Tests

Goal:

- add positive and negative security tests for each major controller group
- add cross-tenant denial tests
- add platform-admin-only denial tests
- add self-service ownership tests

Deliverables:

- integration tests for the major controller groups
- unit tests for permission and authority construction
- focused regression coverage for any endpoint recently failing with 403

Implementation status:

- Added controller annotation regression coverage for accounting, payments, staff, platform admin, appointment, group moderation, member, and child security boundaries.
- Added direct payment controller tenant-context tests so payment create/query/subscription endpoints cannot run without an active tenant.
- Reused focused Group A/B/C regression tests for tenant ownership, onboarding public access, accounting tenant resolver, membership-card `/me`, baptism request tenant scoping, image permissions, appointment member scope, group moderation, member self-service ownership, and user lookup tenant scoping.
- Current focused Group D validation command covers 118 selected tests without running the full suite.

### Group E: Frontend Auth/Header Audit

Goal:

- verify protected requests send the expected auth and tenant headers
- remove stale frontend permission assumptions
- confirm the UI does not hide valid actions for users who already have the right permission

Deliverables:

- list of frontend request paths needing auth/tenant header verification
- list of permission checks that should be refreshed in the UI

Implementation status:

- Frontend tenant interceptor now sends `X-Tenant-Id` for authenticated API requests when a tenant is active, while preserving explicit tenant headers and skipping public onboarding endpoints.
- Frontend auth-token interceptor has regression coverage proving bearer tokens are sent to protected API routes and omitted for public onboarding/non-API requests.
- Tenant-admin navigation now recognizes backend read permissions for staff, calendar, and appointments so valid read-only users are not hidden from allowed areas.
- Frontend permission catalog now includes permissions added or normalized in the backend authorization sweep: tenant billing, staff read/reset, delete groups, appointment read, calendar read/manage, and accounting read/write/import/export/reconcile permissions.
- Focused frontend tests cover auth headers, tenant headers, permission catalog sync, and tenant-admin nav permission gates.

## Suggested Execution Order

1. Group A
2. Group B
3. Group C
4. Group D
5. Group E

## Notes

- This document now tracks implemented authorization changes as well as remaining follow-up areas.
- Any future controller change should be reviewed against the tenant filter, service-level ownership rules, frontend tenant headers, and frontend permission gates before merge.
