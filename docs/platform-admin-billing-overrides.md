# Platform Admin Billing Overrides

This feature lets platform admins attach tenant-specific commercial terms without changing the tenant's underlying subscription plan.

## Supported override types

- `FREE_ACCESS`: effective charge becomes `0` while active.
- `PERCENT_DISCOUNT`: reduces the configured plan amount by a percentage.
- `FIXED_PRICE`: replaces the effective charge with a fixed minor-unit amount.
- `TRIAL_EXTENSION`: preserves access and sets effective charge to `0` while the temporary term is active.
- `COMPED_UNTIL_DATE`: preserves access and sets effective charge to `0` until the override expires.

## Platform admin endpoints

Under `/api/v1/platform/subscriptions/{tenantId}`:

- `GET /billing`: tenant billing overview plus calculated `normalAmountMinor`, `discountAmountMinor`, `effectiveAmountMinor`, `currency`, applied override type, and override expiry.
- `GET /billing-overrides/active`: current active override, if any.
- `GET /billing-overrides`: full override history for the tenant.
- `POST /billing-overrides`: create a billing override.
- `PUT /billing-overrides/{overrideId}`: update an active billing override.
- `DELETE /billing-overrides/{overrideId}`: revoke an override.

All endpoints require the `PLATFORM_ADMIN` role.

## Validation rules

- Only one active billing override window may overlap for a tenant.
- `PERCENT_DISCOUNT` requires `discountPercent > 0 && <= 100`.
- `FIXED_PRICE` requires `fixedAmountMinor >= 0`.
- `TRIAL_EXTENSION` and `COMPED_UNTIL_DATE` require an `endsAt`.
- Revoked or expired overrides are excluded from billing calculation.

## Audit

Create, update, and revoke actions are written to `tenant_billing_override_audit` with:

- tenant id
- override id and type
- old value summary
- new value summary
- actor user id
- reason
- timestamp
