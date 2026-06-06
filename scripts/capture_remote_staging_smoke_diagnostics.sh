#!/usr/bin/env bash
set -euo pipefail

cd "${REMOTE_ROOT}"
mkdir -p diagnostics

docker compose -p "${COMPOSE_PROJECT_NAME}" --env-file releases/current.env --env-file .env.staging \
  -f compose.yaml -f docker-compose.staging.yml ps > diagnostics/smoke-compose-ps.txt || true
docker compose -p "${COMPOSE_PROJECT_NAME}" --env-file releases/current.env --env-file .env.staging \
  -f compose.yaml -f docker-compose.staging.yml logs --tail=200 backend > diagnostics/smoke-backend.log || true

curl -sS -D diagnostics/smoke-local-health.headers -o diagnostics/smoke-local-health.json -w "%{http_code}" \
  http://localhost:8081/actuator/health > diagnostics/smoke-local-health.status || true

if [[ -n "${ACCESS_TOKEN:-}" ]]; then
  curl -sS -D diagnostics/smoke-local-profile.headers -o diagnostics/smoke-local-profile.json -w "%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    http://localhost:8081/api/v1/users/me/profile > diagnostics/smoke-local-profile.status || true

  curl -sS -D diagnostics/smoke-local-tenant.headers -o diagnostics/smoke-local-tenant.json -w "%{http_code}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    "http://localhost:8081${TENANT_PATH}" > diagnostics/smoke-local-tenant.status || true

  if [[ "$(cat diagnostics/smoke-local-tenant.status 2>/dev/null || true)" != "200" && -n "${TENANT_ID:-}" ]]; then
    curl -sS -D diagnostics/smoke-local-tenant.headers -o diagnostics/smoke-local-tenant.json -w "%{http_code}" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H "X-Tenant-ID: ${TENANT_ID}" \
      "http://localhost:8081${TENANT_PATH}" > diagnostics/smoke-local-tenant.status || true
  fi
fi

grep -E -i "flyway|migration|sqlstate|caused by:|exception|relation .* does not exist|user_profiles|user_two_factor_backup_codes" \
  diagnostics/smoke-backend.log | tail -n 120 > diagnostics/smoke-backend-hints.txt || true
