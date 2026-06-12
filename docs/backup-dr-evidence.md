# Backup and Disaster Recovery Evidence

This note captures what is actually evidenced in the repository as of 2026-06-11. It is intentionally limited to what the code and checked-in configuration prove.

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

## Configurable but not proven by repo

### Database backups

- The repo shows PostgreSQL and an RDS hostname assumption, but it does not contain RDS snapshot settings, AWS Backup configuration, backup schedules, retention rules, or restore automation.
- Because the repo has no infrastructure-as-code for RDS backup policy, database backup behavior appears to be controlled outside this repository.

### Encryption at rest

- The repo uses HTTPS/TLS-oriented application settings and AWS-managed services, but it does not prove RDS encryption-at-rest, S3 default encryption, KMS usage, or encrypted backup storage.
- Those controls may exist in the live cloud environment, but they are not evidenced here.

### Retention

- The codebase contains tenant workspace deletion-retention logic for application data lifecycle, not infrastructure backup retention, in `TenantWorkspaceLifecycleService`.
- No repository file proves retention periods for database snapshots, S3 object versions, or backup archives.

## Absent or undocumented in repo

### S3 object recoverability and versioning

- No checked-in file enables or documents S3 bucket versioning.
- No checked-in file enables lifecycle rules, object lock, cross-region replication, or bucket-level restore behavior.
- LocalStack setup does not configure versioning or server-side encryption; it only creates the bucket and sets CORS.

### Restore procedures

- No database restore runbook, S3 restore runbook, recovery time objective, or recovery point objective is documented in the repo.
- The existing rollback documentation in `docs/ci-cd.md` covers application deployment rollback, not data restoration.

## Operational conclusion

- Database backups: not evidenced in repo.
- Object storage backup/versioning: S3 usage is implemented, but backup/versioning is not evidenced in repo.
- Retention: tenant data retention logic exists in application code; infrastructure backup retention is not evidenced in repo.
- Encryption at rest: not evidenced in repo.
- Restore process: not documented in repo.

## Gaps against legal/security wording

- Prior legal/security docs described backup retention and backup control details more concretely than this repository can support.
- Those statements have been narrowed to avoid implying that backup schedules, validation, encryption, or retention are implemented by this codebase when the controlling evidence would live in cloud infrastructure outside the repo.
