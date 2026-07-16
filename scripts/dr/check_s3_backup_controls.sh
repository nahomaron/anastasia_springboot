#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib_aws_evidence.sh
source "$SCRIPT_DIR/lib_aws_evidence.sh"

require_command aws
require_command jq
require_env S3_BUCKET

REQUIRE_LIFECYCLE="${REQUIRE_LIFECYCLE:-true}"

section "S3 Backup Controls"
echo "S3_BUCKET=$S3_BUCKET"
echo "REQUIRE_LIFECYCLE=$REQUIRE_LIFECYCLE"

versioning_json="$(aws s3api get-bucket-versioning --bucket "$S3_BUCKET" --output json)"
versioning_status="$(jq -r '.Status // empty' <<<"$versioning_json")"
mfa_delete="$(jq -r '.MFADelete // empty' <<<"$versioning_json")"

echo "VersioningStatus=${versioning_status:-not-enabled}"
echo "MFADelete=${mfa_delete:-not-configured}"

if [[ "$versioning_status" == "Enabled" ]]; then
  pass "S3 bucket versioning is enabled"
else
  fail "S3 bucket versioning is not enabled"
fi

if encryption_json="$(aws s3api get-bucket-encryption --bucket "$S3_BUCKET" --output json 2>/tmp/s3-encryption-error.txt)"; then
  rules_count="$(jq '.ServerSideEncryptionConfiguration.Rules | length' <<<"$encryption_json")"
  algorithm="$(jq -r '.ServerSideEncryptionConfiguration.Rules[0].ApplyServerSideEncryptionByDefault.SSEAlgorithm // empty' <<<"$encryption_json")"
  kms_key_id="$(jq -r '.ServerSideEncryptionConfiguration.Rules[0].ApplyServerSideEncryptionByDefault.KMSMasterKeyID // empty' <<<"$encryption_json")"
  echo "EncryptionRules=$rules_count"
  echo "SSEAlgorithm=${algorithm:-not-set}"
  echo "KMSMasterKeyID=${kms_key_id:-not-set}"
  if (( rules_count > 0 )); then
    pass "S3 default server-side encryption is configured"
  else
    fail "S3 default server-side encryption has no rules"
  fi
else
  fail "S3 default server-side encryption is not configured: $(cat /tmp/s3-encryption-error.txt)"
fi

if lifecycle_json="$(aws s3api get-bucket-lifecycle-configuration --bucket "$S3_BUCKET" --output json 2>/tmp/s3-lifecycle-error.txt)"; then
  lifecycle_rules="$(jq '.Rules | length' <<<"$lifecycle_json")"
  enabled_rules="$(jq '[.Rules[] | select(.Status == "Enabled")] | length' <<<"$lifecycle_json")"
  echo "LifecycleRules=$lifecycle_rules"
  echo "EnabledLifecycleRules=$enabled_rules"
  if (( enabled_rules > 0 )); then
    pass "S3 lifecycle configuration has enabled rules"
  else
    fail "S3 lifecycle configuration has no enabled rules"
  fi
else
  if [[ "$REQUIRE_LIFECYCLE" == "true" ]]; then
    fail "S3 lifecycle configuration is not present: $(cat /tmp/s3-lifecycle-error.txt)"
  else
    warn "S3 lifecycle configuration is not present"
  fi
fi

finish_evidence
