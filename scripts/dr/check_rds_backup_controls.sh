#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib_aws_evidence.sh
source "$SCRIPT_DIR/lib_aws_evidence.sh"

require_command aws
require_command jq
require_env DB_INSTANCE_ID

MIN_BACKUP_RETENTION_DAYS="${MIN_BACKUP_RETENTION_DAYS:-7}"
REQUIRE_DELETION_PROTECTION="${REQUIRE_DELETION_PROTECTION:-true}"
REQUIRE_MULTI_AZ="${REQUIRE_MULTI_AZ:-false}"

section "RDS Backup Controls"
echo "DB_INSTANCE_ID=$DB_INSTANCE_ID"
echo "MIN_BACKUP_RETENTION_DAYS=$MIN_BACKUP_RETENTION_DAYS"
echo "REQUIRE_DELETION_PROTECTION=$REQUIRE_DELETION_PROTECTION"
echo "REQUIRE_MULTI_AZ=$REQUIRE_MULTI_AZ"

instance_json="$(aws rds describe-db-instances \
  --db-instance-identifier "$DB_INSTANCE_ID" \
  --output json)"

instance="$(jq '.DBInstances[0]' <<<"$instance_json")"
if [[ "$instance" == "null" ]]; then
  fail "RDS instance was not found"
  finish_evidence
fi

backup_retention="$(jq -r '.BackupRetentionPeriod // 0' <<<"$instance")"
storage_encrypted="$(jq -r '.StorageEncrypted // false' <<<"$instance")"
deletion_protection="$(jq -r '.DeletionProtection // false' <<<"$instance")"
multi_az="$(jq -r '.MultiAZ // false' <<<"$instance")"
db_status="$(jq -r '.DBInstanceStatus // "unknown"' <<<"$instance")"
engine="$(jq -r '.Engine // "unknown"' <<<"$instance")"
kms_key_id="$(jq -r '.KmsKeyId // empty' <<<"$instance")"
latest_restorable_time="$(jq -r '.LatestRestorableTime // empty' <<<"$instance")"

echo "Engine=$engine"
echo "Status=$db_status"
echo "BackupRetentionPeriod=$backup_retention"
echo "StorageEncrypted=$storage_encrypted"
echo "KmsKeyId=${kms_key_id:-not-set}"
echo "DeletionProtection=$deletion_protection"
echo "MultiAZ=$multi_az"
echo "LatestRestorableTime=${latest_restorable_time:-not-reported}"

if (( backup_retention >= MIN_BACKUP_RETENTION_DAYS )); then
  pass "RDS backup retention is at least ${MIN_BACKUP_RETENTION_DAYS} day(s)"
else
  fail "RDS backup retention is ${backup_retention} day(s), below ${MIN_BACKUP_RETENTION_DAYS}"
fi

if [[ "$storage_encrypted" == "true" ]]; then
  pass "RDS storage encryption is enabled"
else
  fail "RDS storage encryption is not enabled"
fi

if [[ -n "$latest_restorable_time" ]]; then
  pass "RDS reports a latest restorable time"
else
  fail "RDS does not report a latest restorable time"
fi

if [[ "$REQUIRE_DELETION_PROTECTION" == "true" ]]; then
  if [[ "$deletion_protection" == "true" ]]; then
    pass "RDS deletion protection is enabled"
  else
    fail "RDS deletion protection is not enabled"
  fi
else
  warn "RDS deletion protection requirement is disabled for this check"
fi

if [[ "$REQUIRE_MULTI_AZ" == "true" ]]; then
  if [[ "$multi_az" == "true" ]]; then
    pass "RDS Multi-AZ is enabled"
  else
    fail "RDS Multi-AZ is not enabled"
  fi
else
  warn "RDS Multi-AZ requirement is disabled for this check"
fi

finish_evidence
