#!/usr/bin/env bash
set -euo pipefail

EVIDENCE_FAILURES=0

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "ERROR: required command not found: $command_name" >&2
    exit 2
  fi
}

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "ERROR: required environment variable is not set: $name" >&2
    exit 2
  fi
}

section() {
  printf '\n## %s\n' "$1"
}

pass() {
  printf 'PASS: %s\n' "$1"
}

warn() {
  printf 'WARN: %s\n' "$1"
}

fail() {
  EVIDENCE_FAILURES=$((EVIDENCE_FAILURES + 1))
  printf 'FAIL: %s\n' "$1"
}

json_value() {
  local query="$1"
  jq -r "$query // empty"
}

finish_evidence() {
  if [[ "$EVIDENCE_FAILURES" -gt 0 ]]; then
    printf '\nEvidence check completed with %s failure(s).\n' "$EVIDENCE_FAILURES" >&2
    exit 1
  fi
  printf '\nEvidence check completed successfully.\n'
}
