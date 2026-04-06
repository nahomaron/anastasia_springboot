# CI/CD Overview

## Phase 1 – CI gate hardening

- `api-tests.yml` now runs Checkstyle and SpotBugs before the integration and black-box suites, so style or static-analysis regressions fail fast.
- A dedicated `security-scan.yml` workflow executes OWASP Dependency-Check on every push/PR to `main` or `dev`, blocking merges when CVSS ≥ 7 vulnerabilities appear and surfacing the report at `target/dependency-check-reports`.

## Phase 2 – Staging release automation

- `deploy-staging.yml` now fires on pushes to `main`/`dev` (in addition to manual dispatch) and blocks until the matching `⚡ K6 Load & Performance Dashboard` workflow finishes successfully for the same commit. Failure of that workflow or its thresholds halts the deployment.
- The deployment downloads the resulting K6 summaries so the artifacts travel with the release, then proceeds with the existing image build/push, remote compose deployment, health check, and rollback guardrails.

## Phase 3 – Production release orchestration

- `deploy-production.yml` now runs on annotated tags and manual dispatch, requires the `production` GitHub Environment (which enforces explicit approvers), and waits for the matching `⚡ K6 Load & Performance Dashboard` workflow to succeed for the commit before continuing.  
- The production job builds/pushes a `:prod` Docker image, writes `.env.production` from the `PROD_*` secrets (`PROD_HOST`, `PROD_SSH_KEY`, `PROD_DB_*`, `PROD_JWT_SECRET`, `PROD_MAIL_API_KEY`, etc.), uploads the compose bundle, and deploys the stack the same way staging does.  
- It reuses the health check and rollback guardrails from staging but pointed at the production host (health check runs for 200 HTTP responses); the job will fail or rollback if the service never becomes healthy.
- The doc also now captures that release candidates must pass the CI gates, security scan, and performance load job before `deploy-production.yml` can fire.

## Performance profile

- The `api` Spring profile is reserved for performance/load runs; it loads `application-api.yml`, which opt‑outs the limiter by setting `rate-limiter.enabled=false` so throttling does not block the load generators while other profiles retain the default bucket behaviour.
- Run `./mvnw -Papi -Drate-limiter.enabled=false verify` (the same command is wired into the `K6 Load & Performance Dashboard` workflow) whenever you need to reproduce the performance suite without the rate limiter kicking in.

Further phases (production deployment, documentation, etc.) are tracked under `docs/to-do list`.
