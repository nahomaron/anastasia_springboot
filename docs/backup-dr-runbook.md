# Backup And Disaster Recovery Runbook

This runbook covers data recovery for Anastasia backend environments. It is separate from application rollback: rollback redeploys a previous backend image, while disaster recovery restores database and object-storage state.

## Scope

Covered:

- PostgreSQL database recovery for staging and production.
- S3 object recovery for uploaded images, templates, and generated membership-card files.
- Post-restore backend validation.

Not covered:

- Frontend deployment rollback.
- DNS, CDN, or load balancer restoration.
- Rebuilding an entire AWS account from scratch.
- Live AWS backup policy creation. Current backup policies are external operational controls until repo-owned IaC is added.

## Ownership

- Engineering owns this runbook, application validation, and repository evidence.
- Operations or the AWS account owner owns RDS, S3, KMS, AWS Backup, snapshot retention, and bucket lifecycle configuration until those resources are moved into repo-owned IaC.
- Production restores require approval from the incident commander and a second engineer before changing live traffic or replacing data.

## Recovery Targets

These are operational targets, not repository-proven guarantees:

| Environment | RPO | RTO | Notes |
|---|---:|---:|---|
| Production | <= 24 hours | <= 8 hours | Assumes RDS automated backups or AWS Backup snapshots exist. |
| Staging | <= 24 hours | <= 4 hours | Staging may be rebuilt from seed data when full fidelity is unnecessary. |
| Local development | Best effort | Best effort | Local Docker volumes are developer-owned and not part of DR. |

Tighter targets require evidence that RDS point-in-time recovery, AWS Backup plans, S3 versioning, and encryption are enabled for the relevant resources.

## Evidence Checks

Use the read-only scripts under `scripts/dr/` to collect current AWS backup/DR evidence before audits, after infrastructure changes, and after restore exercises.

Example:

```bash
DB_INSTANCE_ID=anastasis-production \
S3_BUCKET=anastasis-production-assets \
BACKUP_VAULT_NAME=production-backup-vault \
BACKUP_PLAN_ID=abcd1234-0000-1111-2222-example \
BACKUP_RESOURCE_ARN=arn:aws:rds:us-east-2:123456789012:db:anastasis-production \
scripts/dr/generate_backup_dr_evidence.sh
```

The script writes a timestamped report under `build/backup-dr-evidence/`. Store generated reports in the incident or compliance evidence system, not in the repository.

The same checks can be run from GitHub Actions through the manual `Backup DR Evidence` workflow. Use the workflow for staging or production evidence when the GitHub Environment has `BACKUP_DR_AWS_ROLE_ARN`, `BACKUP_DR_DB_INSTANCE_ID`, `BACKUP_DR_S3_BUCKET`, and `AWS_REGION` configured.

## Incident Classification

Use deployment rollback when:

- A new backend image is unhealthy.
- Smoke checks fail after deploy.
- The database and S3 data are believed to be intact.

Use disaster recovery when:

- Database data is deleted, corrupted, or migrated incorrectly.
- S3 objects are deleted, overwritten, or corrupted.
- An environment must be restored to a known earlier data state.
- The current application rollback cannot restore service because data is damaged.

## Pre-Restore Checklist

1. Declare the affected environment: `staging` or `production`.
2. Freeze deploys for the environment.
3. Record the current backend image digest from the environment release state:
   - staging: `/opt/anastasis-staging/releases/current.env`
   - production: `/opt/anastasis-production/releases/current.env`
4. Record current database host, database name, S3 bucket, and S3 prefix from the environment runtime file.
5. Capture failing request examples, timestamps, and affected tenant ids if available.
6. Decide the restore point timestamp.
7. Confirm whether the incident affects database only, S3 only, or both.

Do not delete the current database or bucket contents before validating a restored copy.

## RDS Restore Procedure

Use the AWS console, AWS CLI, or AWS Backup console according to the account's operational standard.

1. Identify the source RDS instance or cluster for the affected environment.
2. Restore to a new database instance or cluster using the selected restore point.
3. Keep the original database online but block application writes if continued writes would worsen corruption.
4. Apply the environment security group and subnet configuration required by the backend host.
5. Verify the restored database is encrypted and reachable only from approved network paths.
6. Run Flyway validation against the restored database before pointing live traffic at it.
7. Update the environment runtime file to point `DB_HOST`, `DB_PORT`, `DB_NAME`, and credentials at the restored database.
8. Redeploy the current known-good backend image using the existing deployment workflow or remote compose process.
9. Run post-restore validation.

Rollback condition:

- If validation fails, point the environment runtime file back to the previous database and redeploy the previous release state. Keep the failed restored database for investigation unless it contains sensitive exported data that must be removed under incident handling rules.

## S3 Object Restore Procedure

Use this only when the bucket has versioning, backup, replication, or another recoverability mechanism enabled outside this repository.

1. Identify the affected bucket and prefix:
   - staging usually uses `AWS_S3_PREFIX=staging`
   - production usually uses `AWS_S3_PREFIX=production`
2. Identify deleted or corrupted object keys from application logs, audit records, user reports, or S3 access logs.
3. Restore affected object versions or recover objects from backup into the same keys when safe.
4. For broad corruption, restore into a temporary prefix first.
5. Validate restored objects by issuing backend reads or presigned GET flows where applicable.
6. Move restored objects into the live prefix only after validation.
7. Keep a manifest of restored keys, versions, timestamps, and operator names.

Rollback condition:

- If restored objects are incorrect, reinstate the prior object versions or switch the application back to the prior prefix if a prefix-level restore strategy was used.

## Combined Database And S3 Restore

When both database and S3 are affected, restore them to a consistent timestamp.

1. Choose one restore point for both database and object storage.
2. Restore the database first into a new instance or cluster.
3. Restore S3 objects into a temporary prefix or verify object versions before live replacement.
4. Validate database records that reference S3 keys.
5. Point the application at the restored database and object prefix together.
6. Run post-restore validation before reopening normal traffic.

## Post-Restore Validation

Run these checks before declaring recovery complete:

1. `GET /actuator/health` returns healthy.
2. Login smoke check succeeds for the environment operational account.
3. A tenant-scoped authenticated request succeeds using the expected `X-Tenant-Id`.
4. Representative member, church, event, payment, and notification reads succeed.
5. Representative S3-backed image or membership-card retrieval succeeds.
6. Error logs show no Flyway, tenant-filter, S3, authentication, or database connection failures.
7. Background jobs do not replay destructive or duplicate work unexpectedly.

## Evidence To Capture

Store evidence in the incident record, not in the repository:

- Restore point timestamp.
- Source and restored RDS identifiers.
- S3 bucket, prefix, object versions, and restored object manifest.
- Backend image digest before and after restore.
- Validation command output or screenshots.
- Decision log with approvers and timestamps.

## Follow-Up Tasks

After any restore:

1. Document root cause and data impact.
2. Verify backup retention was sufficient for the selected restore point.
3. Verify RPO and RTO were met or update targets.
4. Add regression tests, migration safeguards, or operational checks that would have prevented the incident.
5. Update this runbook if any step was wrong, missing, or ambiguous.
