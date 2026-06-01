#!/usr/bin/env bash
set -euo pipefail

RUN_API_TESTS="${RUN_API_TESTS:-false}"
RUN_GITLEAKS="${RUN_GITLEAKS:-false}"
RUN_DEPENDENCY_CHECK="${RUN_DEPENDENCY_CHECK:-true}"

run_step() {
  local name="$1"
  shift
  echo
  echo "==> ${name}"
  "$@"
}

run_maven() {
  ./mvnw --batch-mode "$@"
}

chmod +x ./mvnw

run_step "Compile" run_maven -q -DskipTests compile
run_step "Unit tests with JaCoCo report" run_maven -q test jacoco:report -Dgroups=!experimental
run_step "Integration tests" run_maven -q -Ptest verify -DskipApiTests=true
run_step "Checkstyle" run_maven -q checkstyle:check
run_step "SpotBugs" run_maven -q -DskipTests compile spotbugs:check
run_step "Runtime dependency tree" run_maven -q dependency:tree -Dscope=runtime

if [[ "${RUN_API_TESTS}" == "true" ]]; then
  : "${BASE_URL:?BASE_URL must be set when RUN_API_TESTS=true}"
  run_step "Black-box API tests" env BASE_URL="${BASE_URL}" ./mvnw --batch-mode -q -Papi-tests test
else
  echo
  echo "==> Skipping black-box API tests (set RUN_API_TESTS=true and BASE_URL to enable)"
fi

if [[ "${RUN_GITLEAKS}" == "true" ]]; then
  if ! command -v gitleaks >/dev/null 2>&1; then
    echo "gitleaks is not installed but RUN_GITLEAKS=true was requested" >&2
    exit 1
  fi
  run_step "Gitleaks" gitleaks git --verbose .
else
  echo
  echo "==> Skipping gitleaks (set RUN_GITLEAKS=true to enable)"
fi

if [[ "${RUN_DEPENDENCY_CHECK}" == "true" ]]; then
  : "${NVD_API_KEY:?NVD_API_KEY must be set to run OWASP Dependency-Check}"
  DC_DATA_DIR="${DC_DATA_DIR:-${PWD}/target/dependency-check-data}"
  DC_SUPPRESSION_FILE="${DC_SUPPRESSION_FILE:-.github/dependency-check-suppressions.xml}"
  DC_VERSION="${DC_VERSION:-12.2.1}"

  mkdir -p "${DC_DATA_DIR}" target/dependency-check-reports
  find "${DC_DATA_DIR}" -mindepth 1 -exec rm -rf {} +

  run_step "OWASP Dependency-Check" \
    ./mvnw --batch-mode -DskipTests \
      "org.owasp:dependency-check-maven:${DC_VERSION}:check" \
      -DnvdApiKey="${NVD_API_KEY}" \
      -DdataDirectory="${DC_DATA_DIR}" \
      -DsuppressionFiles="${DC_SUPPRESSION_FILE}" \
      -Dformat=ALL \
      -DoutputDirectory=target/dependency-check-reports \
      -DfailBuildOnCVSS=7
else
  echo
  echo "==> Skipping OWASP Dependency-Check (set RUN_DEPENDENCY_CHECK=true to enable)"
fi
