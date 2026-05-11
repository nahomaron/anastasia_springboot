# CI/CD Runbook

## Workflow graph

The production-ready release path is now:

- `ci.yml`
- `security.yml`
- `build-image.yml`
- `deploy-staging.yml`
- `k6-load.yml`
- `promote-production.yml`
- `rollback.yml`

Flow:

1. `push` / `pull_request` to `main` or `dev` runs `CI` and `Security`.
2. A successful `CI` run on `main` triggers `Build Image`.
3. `Build Image` waits for `Security` to pass for the same commit, then pushes exactly one immutable GHCR image tagged by commit SHA and publishes `release-manifest.json`.
4. A successful `Build Image` run triggers `Deploy Staging`.
5. `Deploy Staging` downloads the manifest, deploys the exact image digest to staging, writes release state on the target host, and runs smoke tests.
6. A successful `Deploy Staging` run triggers `K6 Load`.
7. `Promote Production` is manual only. It reuses the same manifest and exact image digest after validating that staging and K6 both passed for that commit.
8. `Rollback` is manual only and restores the previously deployed digest for staging or production.

## Artifact model

`build-image.yml` publishes `release-manifest.json` as the source of truth for a release candidate.

Manifest fields:

- `git_sha`
- `image_repo`
- `image_tag`
- `image_digest`
- `workflow_run_id`
- `built_at`

Deploy workflows never rebuild an image. They resolve the runtime image as:

- `ghcr.io/<owner>/anastasia-backend@sha256:...`

That exact digest is written into release state on the server and is the artifact promoted from staging to production.

## Remote release state

Each deployment target keeps release state under:

- staging: `/opt/anastasis-staging/releases/current.env`
- staging: `/opt/anastasis-staging/releases/previous.env`
- production: `/opt/anastasis-production/releases/current.env`
- production: `/opt/anastasis-production/releases/previous.env`

Stored keys:

- `BACKEND_IMAGE`
- `GIT_SHA`
- `DEPLOYED_AT`

Deployment logic:

1. Copy `current.env` to `previous.env` if it exists.
2. Write the new `current.env` from the manifest.
3. Run `docker compose` with the environment-specific application file plus `current.env`.

Rollback logic:

1. Copy `previous.env` back to `current.env`.
2. Redeploy with the same compose files.
3. Verify health and login smoke checks.

## Compose layout

Files:

- `compose.yaml`: shared service definitions and the `BACKEND_IMAGE` parameter
- `docker-compose.override.yml`: local developer build override
- `docker-compose.staging.yml`: staging-only runtime settings
- `docker-compose.production.yml`: production-only runtime settings
- `src/main/resources/application-staging.yml`: staging Spring profile
- `src/main/resources/application-prod.yml`: production Spring profile

The base compose file no longer hardcodes image builds inside deployment workflows. Staging and production consume `BACKEND_IMAGE` from release state instead.
Deployments use explicit compose project names so both environments can run on the same EC2 instance:

- staging: `anastasis-staging`
- production: `anastasis-production`

Host ports on the shared EC2 instance:

- staging backend: `8081`
- production backend: `8080`

## CI gate behavior

`ci.yml` is the authoritative quality gate for branch protection.

Parallel jobs:

- `compile`: `./mvnw -q -DskipTests compile`
- `unit`: `./mvnw -q test jacoco:report -Dgroups=!experimental`
- `integration`: `./mvnw -q -Ptest verify -DskipApiTests=true`
- `checkstyle`: `./mvnw -q checkstyle:check`
- `spotbugs`: `./mvnw -q -DskipTests compile spotbugs:check`
- `api`: optional, enabled only when repository variable `CI_RUN_API_TESTS=true`

Artifacts:

- Surefire reports
- Failsafe reports
- JaCoCo report
- Allure raw results

`pom.xml` is aligned so:

- Java stays on `21`
- default `verify` runs unit tests plus stable integration tests
- `-Ptest` is the integration-only CI slice
- `-Papi-tests` is the black-box API slice
- tests tagged `experimental` stay excluded by default

## Security gate behavior

`security.yml` has two roles:

Push / PR gate:

- OWASP Dependency-Check
- Gitleaks secret scanning

Post-image supply-chain scan:

- Trivy scan against the pushed image digest
- SBOM generation

Artifacts:

- dependency check reports
- gitleaks SARIF
- Trivy SARIF
- SBOM JSON

## Smoke checks

Staging and production both run:

1. `GET /actuator/health`
2. `POST /api/v1/auth/login`
3. `GET /api/v1/users/me/profile`
4. one tenant-scoped request using `SMOKE_TENANT_PATH`

The workflows intentionally use dedicated smoke credentials rather than test-only endpoints so the release gate checks real behavior.

## Required GitHub configuration

Use GitHub Environments for `staging` and `production`.

Environment secrets required for both:

- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`
- `APP_ENV_FILE`
- `PUBLIC_BASE_URL`
- `SMOKE_EMAIL`
- `SMOKE_PASSWORD`
- `SMOKE_TENANT_PATH`
- `SMOKE_TENANT_ID` optional when the tenant endpoint requires the header

Additional `staging` environment secrets:

- `K6_OWNER_EMAIL`
- `K6_OWNER_PASSWORD`
- `K6_OWNER_PHONE` optional

Repository variables:

- `CI_RUN_API_TESTS`: set to `true` only when the CI host should run the API suite
- `CI_API_BASE_URL`: required when `CI_RUN_API_TESTS=true`

Branch protection on `main` should require:

- `CI Gate`
- `Security Gate`

`production` environment approval should remain mandatory for `promote-production.yml`.

## APP_ENV_FILE format

`APP_ENV_FILE` should contain the complete runtime environment body for the target environment, for example:

```dotenv
SPRING_PROFILES_ACTIVE=staging
COMPOSE_PROJECT_NAME=anastasis-staging
DB_HOST=postgres
DB_PORT=5432
DB_NAME=anastasia_staging
DB_USER=anastasis_staging
DB_PASSWORD=...
BACKEND_HOST_PORT=8081

APP_FRONTEND_BASE_URL=https://staging.anastasisapp.com
APP_BACKEND_BASE_URL=https://staging-api.anastasisapp.com
APP_CORS_ALLOWED_ORIGINS=https://staging.anastasisapp.com
APP_AUTH_REFRESH_COOKIE_DOMAIN=staging.anastasisapp.com
APP_AUTH_REFRESH_COOKIE_SAME_SITE=None

ANASTASIA_JWT_CURRENT_SECRET=...
ANASTASIA_JWT_PREVIOUS_SECRET=
PLATFORM_ADMIN_SECRET=...

AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_REGION=us-east-2
AWS_S3_BUCKET=anastasis-staging-assets
AWS_S3_PREFIX=staging

MAIL_HOST=email-smtp.us-east-2.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=...
MAIL_PASSWORD=...
MAIL_FROM=noreply@staging.anastasisapp.com
MAIL_PROTOCOL=smtp
MAIL_SMTP_AUTH=true
MAIL_STARTTLS_ENABLE=true
MAIL_STARTTLS_REQUIRED=true
```

`BACKEND_HOST_PORT` controls the host port exposed by Docker. The backend process inside the container remains pinned to `8080`, so deployment env files should not override `SERVER_PORT`.

The workflow writes this value to `.env.staging` or `.env.production` on the runner, uploads it to the host, and never echoes the contents in logs.

Production should mirror the same structure with:

- `SPRING_PROFILES_ACTIVE=prod`
- `COMPOSE_PROJECT_NAME=anastasis-production`
- `BACKEND_HOST_PORT=8080`
- `DB_NAME=anastasia`
- `DB_USER=anastasis_prod`
- `APP_BACKEND_BASE_URL=https://api.anastasisapp.com`
- `AWS_S3_BUCKET=anastasis-production-assets`
- `AWS_S3_PREFIX=production`

## Promotion process

1. Merge the change to `main`.
2. Wait for `CI`, `Security`, `Build Image`, `Deploy Staging`, and `K6 Load` to pass.
3. Run `Promote Production`.
4. Provide the `Build Image` run id.
5. Approve the `production` environment deployment.
6. Confirm the production smoke checks pass.

## Rollback process

1. Run `Rollback`.
2. Choose `staging` or `production`.
3. The workflow selects the correct environment root (`/opt/anastasis-staging` or `/opt/anastasis-production`), restores that environment's `previous.env`, redeploys the prior digest, and reruns health/login validation.

## Failure diagnostics

On deployment failure the workflows capture and upload:

- `docker compose ps`
- backend container logs (`--tail=200`)
- actuator health payload
- smoke-test response payloads

That keeps staging and production failures diagnosable from Actions without logging into the server first.
