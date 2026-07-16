# Backup And Disaster Recovery Evidence

This note captures what is actually evidenced in the repository as of 2026-07-16. It is intentionally limited to what the code, checked-in configuration, and repository runbooks prove.

## Ownership model

Backup and disaster recovery controls are split into two layers:

- Application-owned evidence lives in this repository: runtime database/S3 configuration, deployment rollback documentation, read-only AWS evidence scripts under `scripts/dr/`, and the data restore runbook in `docs/backup-dr-runbook.md`.
- Cloud infrastructure controls currently live outside this repository: RDS backup schedules, AWS Backup plans, S3 versioning, lifecycle rules, bucket encryption, KMS keys, and cross-region replication.

Until those cloud controls are managed by repo-owned infrastructure-as-code or verified by repo-owned evidence reports from `scripts/dr/`, they must be treated as external operational dependencies rather than implemented repository controls.

## Implemented in repo

### Database connectivity

- Production and staging are configured to use PostgreSQL via environment-driven JDBC settings in `src/main/resources/application-prod.yml` and `src/main/resources/application-staging.yml`.
- The production example environment points at an AWS RDS hostname in `.env.prod`, which shows an RDS deployment assumption exists, but not its backup policy.
- Local development compose persists Postgres data to the `postgres_data` Docker volume in `compose.yaml`.

### Object storage usage

- Production and staging profiles configure an S3 bucket, region, optional endpoint, and key prefix in `src/main/resources/application-prod.yml` and `src/main/resources/application-staging.yml`.
- The application issues presigned upload URLs and verifies uploaded objects through S3 in `src/main/java/com/anastasia/Anastasia_BackEnd/common/aws/AwsS3Service.java`.
- Membership card binaries are also written to and read from the configured S3 bucket in `src/main/java/com/anastasia/Anastasia_BackEnd/modules/registration/service/card/MembershipCardStorageService.java`.
- LocalStack bootstrap creates a bucket and applies CORS only in `localstack/init/ready.d/01-s3-init.sh`.

### Restore procedures and targets

- `docs/backup-dr-runbook.md` defines recovery objectives, escalation rules, RDS restore flow, S3 object restore flow, and post-restore validation.
- Application rollback remains documented separately in `docs/ci-cd.md`; it restores a previous backend image digest and does not restore data.
- The runbook defines production targets of RPO <= 24 hours and RTO <= 8 hours until AWS evidence proves tighter guarantees.
- The runbook defines staging targets of RPO <= 24 hours and RTO <= 4 hours, with lower business criticality than production.

### Evidence scripts

- `scripts/dr/check_rds_backup_controls.sh` checks RDS backup retention, storage encryption, latest restorable time, deletion protection, and optional Multi-AZ.
- `scripts/dr/check_s3_backup_controls.sh` checks S3 bucket versioning, default server-side encryption, and lifecycle rules.
- `scripts/dr/check_aws_backup_controls.sh` checks AWS Backup vault, backup plan rule count, plan lifecycle retention, and optional protected resource ARN.
- `scripts/dr/generate_backup_dr_evidence.sh` runs the configured checks and writes a timestamped Markdown report under `build/backup-dr-evidence/`.
- `.github/workflows/backup-dr-evidence.yml` is a manual, environment-gated workflow that runs the evidence scripts through GitHub OIDC and uploads the generated report as an artifact.

## Configurable but not proven by repo

### Database backups

- The repo shows PostgreSQL and an RDS hostname assumption, but it does not contain RDS snapshot settings, AWS Backup configuration, backup schedules, retention rules, or restore automation.
- The repo now contains read-only scripts that can verify live RDS backup controls when supplied AWS credentials and resource ids.
- Because the repo has no infrastructure-as-code for RDS backup policy, database backup behavior appears to be configured outside this repository.

### Encryption at rest

- The repo uses HTTPS/TLS-oriented application settings and AWS-managed services, but static checked-in config does not prove RDS encryption-at-rest, S3 default encryption, KMS usage, or encrypted backup storage.
- The new evidence scripts can prove some live encryption settings when run against AWS, but no current evidence artifact is committed.

### Retention

- The codebase contains tenant workspace deletion-retention logic for application data lifecycle, not infrastructure backup retention, in `TenantWorkspaceLifecycleService`.
- The new evidence scripts can report RDS retention and AWS Backup plan lifecycle retention when run against AWS.
- No current evidence artifact is committed proving retention periods for database snapshots, S3 object versions, or backup archives.

## Absent or undocumented in repo

### S3 object recoverability and versioning

- No checked-in infrastructure file enables S3 bucket versioning.
- No checked-in infrastructure file enables lifecycle rules, object lock, cross-region replication, or bucket-level restore behavior.
- The new evidence scripts can verify live bucket versioning, default encryption, and lifecycle rules when run against AWS.
- LocalStack setup does not configure versioning or server-side encryption; it only creates the bucket and sets CORS.

### Restore procedures

- Data restore procedures, RTO, and RPO are documented in `docs/backup-dr-runbook.md`.
- The repository contains manual cloud evidence collection scripts, but it still does not contain automated restore tooling.
- The existing rollback documentation in `docs/ci-cd.md` covers application deployment rollback, not data restoration.

## Running evidence checks

Prerequisites:

- AWS CLI authenticated to the target account.
- `jq` installed locally.
- Read-only permissions for RDS, S3, and AWS Backup APIs used by the scripts.

Example:

```bash
DB_INSTANCE_ID=anastasis-production \
S3_BUCKET=anastasis-production-assets \
BACKUP_VAULT_NAME=production-backup-vault \
BACKUP_PLAN_ID=abcd1234-0000-1111-2222-example \
BACKUP_RESOURCE_ARN=arn:aws:rds:us-east-2:123456789012:db:anastasis-production \
scripts/dr/generate_backup_dr_evidence.sh
```

The generated report is written under `build/backup-dr-evidence/`. Review it before sharing externally because it can contain AWS resource identifiers.

### GitHub workflow

Run the `Backup DR Evidence` workflow manually against `staging` or `production`.

Each GitHub Environment must define:

- `AWS_REGION`
- `BACKUP_DR_AWS_ROLE_ARN`
- `BACKUP_DR_DB_INSTANCE_ID`
- `BACKUP_DR_S3_BUCKET`

Optional variables:

- `BACKUP_DR_BACKUP_VAULT_NAME`
- `BACKUP_DR_BACKUP_PLAN_ID`
- `BACKUP_DR_BACKUP_RESOURCE_ARN`

The AWS role should allow only read/list/describe access needed by the scripts for RDS, S3 bucket configuration, and AWS Backup.

## Operational conclusion

- Database backups: not evidenced in repo.
- Object storage backup/versioning: S3 usage is implemented, but backup/versioning is not evidenced in repo.
- Retention: tenant data retention logic exists in application code; infrastructure backup retention is not evidenced in repo.
- Encryption at rest: not evidenced in repo.
- Evidence collection: read-only scripts and a manual GitHub evidence workflow exist, but no generated evidence report is committed.
- Restore process: documented in repo, but not automated.
- RTO/RPO: documented as operational targets, but not yet backed by automated evidence.

## Gaps against legal/security wording

- Prior legal/security docs described backup retention and backup control details more concretely than this repository can support.
- Those statements have been narrowed to avoid implying that backup schedules, validation, encryption, or retention are implemented by this codebase when the controlling evidence would live in cloud infrastructure outside the repo.

## Next evidence work

1. Run the evidence workflow against staging and production and archive the generated reports in the operational evidence store.
2. Add scheduled evidence collection if compliance needs recurring proof rather than manual runs.
3. Move cloud controls into repo-owned infrastructure-as-code if the project decides this backend repository should own AWS resource configuration.
