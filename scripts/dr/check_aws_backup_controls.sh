#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib_aws_evidence.sh
source "$SCRIPT_DIR/lib_aws_evidence.sh"

require_command aws
require_command jq

BACKUP_VAULT_NAME="${BACKUP_VAULT_NAME:-}"
BACKUP_PLAN_ID="${BACKUP_PLAN_ID:-}"
BACKUP_RESOURCE_ARN="${BACKUP_RESOURCE_ARN:-}"
MIN_BACKUP_RULES="${MIN_BACKUP_RULES:-1}"

section "AWS Backup Controls"
echo "BACKUP_VAULT_NAME=${BACKUP_VAULT_NAME:-not-set}"
echo "BACKUP_PLAN_ID=${BACKUP_PLAN_ID:-not-set}"
echo "BACKUP_RESOURCE_ARN=${BACKUP_RESOURCE_ARN:-not-set}"
echo "MIN_BACKUP_RULES=$MIN_BACKUP_RULES"

if [[ -n "$BACKUP_VAULT_NAME" ]]; then
  vault_json="$(aws backup describe-backup-vault --backup-vault-name "$BACKUP_VAULT_NAME" --output json)"
  vault_arn="$(jq -r '.BackupVaultArn // empty' <<<"$vault_json")"
  recovery_points="$(jq -r '.NumberOfRecoveryPoints // 0' <<<"$vault_json")"
  echo "BackupVaultArn=${vault_arn:-not-found}"
  echo "NumberOfRecoveryPoints=$recovery_points"
  if [[ -n "$vault_arn" ]]; then
    pass "AWS Backup vault exists"
  else
    fail "AWS Backup vault was not found"
  fi
else
  warn "BACKUP_VAULT_NAME not set; skipping vault check"
fi

if [[ -n "$BACKUP_PLAN_ID" ]]; then
  plan_json="$(aws backup get-backup-plan --backup-plan-id "$BACKUP_PLAN_ID" --output json)"
  plan_name="$(jq -r '.BackupPlan.BackupPlanName // empty' <<<"$plan_json")"
  rule_count="$(jq '.BackupPlan.Rules | length' <<<"$plan_json")"
  lifecycle_count="$(jq '[.BackupPlan.Rules[] | select(.Lifecycle.DeleteAfterDays != null)] | length' <<<"$plan_json")"
  echo "BackupPlanName=${plan_name:-not-found}"
  echo "BackupRuleCount=$rule_count"
  echo "RulesWithDeleteAfterDays=$lifecycle_count"

  if (( rule_count >= MIN_BACKUP_RULES )); then
    pass "AWS Backup plan has at least ${MIN_BACKUP_RULES} rule(s)"
  else
    fail "AWS Backup plan has ${rule_count} rule(s), below ${MIN_BACKUP_RULES}"
  fi

  if (( lifecycle_count > 0 )); then
    pass "AWS Backup plan has retention lifecycle settings"
  else
    fail "AWS Backup plan has no retention lifecycle settings"
  fi
else
  warn "BACKUP_PLAN_ID not set; skipping backup plan check"
fi

if [[ -n "$BACKUP_RESOURCE_ARN" ]]; then
  protected_json="$(aws backup list-protected-resources --output json)"
  protected_count="$(jq --arg arn "$BACKUP_RESOURCE_ARN" '[.Results[] | select(.ResourceArn == $arn)] | length' <<<"$protected_json")"
  echo "ProtectedResourceMatches=$protected_count"
  if (( protected_count > 0 )); then
    pass "AWS Backup protects the supplied resource ARN"
  else
    fail "AWS Backup does not list the supplied resource ARN as protected"
  fi
else
  warn "BACKUP_RESOURCE_ARN not set; skipping protected-resource check"
fi

finish_evidence
