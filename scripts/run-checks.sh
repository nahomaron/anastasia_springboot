#!/usr/bin/env bash
set -euo pipefail

./mvnw verify
./mvnw dependency:tree -Dscope=runtime

NVD_API_KEY="${NVD_API_KEY:-}"
if [[ -z "${NVD_API_KEY}" ]]; then
  echo "NVD_API_KEY must be set to run OWASP Dependency-Check." >&2
  exit 1
fi

DC_DATA_DIR="${DC_DATA_DIR:-${PWD}/target/dependency-check-data}"
mkdir -p "${DC_DATA_DIR}"
find "${DC_DATA_DIR}" -mindepth 1 -exec rm -rf {} +

./mvnw org.owasp:dependency-check-maven:check \
  -DskipTests \
  -DnvdApiKey="${NVD_API_KEY}" \
  -DdataDirectory="${DC_DATA_DIR}"
