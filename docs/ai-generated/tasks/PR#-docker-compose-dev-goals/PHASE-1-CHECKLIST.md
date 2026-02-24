# Phase 1 Implementation Checklist: Docker Compose Baseline (CMS-DTS + MySQL 8)

## Goal

Deliver a reproducible local runtime stack using Docker Compose with:

- CMS-DTS app container
- MySQL 8

This phase does **not** implement full integration-test orchestration yet; it establishes the runtime baseline and Maven touchpoints required for Phase 2.

## Fixed assumptions for Phase 1

- Container install root for both CMS and DTS: `/opt/Percussion`
- On container startup, update install flow runs in order:
  1. CMS update from `modules/perc-distribution-tree/target/perc-distribution-tree.jar`
  2. DTS update from `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution.jar`
- Update installers target the same install root (`/opt/Percussion`).

## Inputs confirmed from current build

- CMS installer artifact: `modules/perc-distribution-tree/target/perc-distribution-tree.jar`
- DTS installer artifact: `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution.jar`
- Existing install scripts validate these exact outputs under `.github/skills/percussioncms-dev/scripts/`.

## File-by-file checklist

### A) New runtime assets (create)

1. `docker-compose.yml`
   - Define services: `mysql`, `cms-dts`
   - Add named volume for MySQL persistence (`mysql-data`)
   - Add internal network for service-to-service communication
   - Add health checks for all services
   - Use `depends_on` with health conditions for startup order
   - Mount required persistent writable paths from `/opt/Percussion`:
     - `/opt/Percussion/ObjectStore`
     - `/opt/Percussion/var`
     - `/opt/Percussion/rxconfig`
     - `/opt/Percussion/Deployment/Server/conf`
     - `/opt/Percussion/jetty/base`
2. `.env.compose.example`
   - Non-secret defaults only (ports, hostnames, database name/user)
   - Secret placeholders only (`MYSQL_ROOT_PASSWORD`, app DB password)
   - Include comments for required variables before `compose up`
3. `docker/cms/Dockerfile`
   - Base image: JDK/JRE 21 compatible image
   - Copy CMS installer jar from build context artifact output
   - Perform unattended install/update into `/opt/Percussion`
   - Entrypoint starts both CMS and DTS processes in foreground mode
   - Add container healthcheck command aligned with CMS and DTS readiness endpoints
4. `docker/entrypoint/install-update.sh`
   - Runs at container startup
   - Executes CMS installer/update JAR first, then DTS installer/update JAR
   - Targets `/opt/Percussion` for both operations
   - Is idempotent so restarts are safe
5. `.dockerignore`
   - Exclude `.git`, `target/` noise, IDE files, and large irrelevant directories
   - Keep installer artifact paths included
6. `docs/docker/compose-dev.md`
   - Quickstart and prerequisites
   - Environment variable setup
   - Commands for up/down/logs/health verification
   - Troubleshooting for Docker daemon, ports, and health timeouts

### B) Existing docs/skill assets (update)

1. `.github/skills/percussioncms-dev/SKILL.md`
   - Add a "Maven + Compose (canonical)" section
   - Add direct pointers to planned Maven lifecycle usage
   - Keep script-based workflow as fallback/reference
   - Include MySQL 8 default statement
2. `docs/docker/compose-dev.md` (mount inventory section)
   - Explicitly document each persistent path, mount type, and purpose
   - Mark all listed paths as developer-writable
   - Distinguish named-volume default vs optional bind-mount override for IDE editing

## Maven wiring touchpoints (prepare in Phase 1, implement in Phase 2)

These are the exact files/areas to modify when wiring compose goals:

1. `pom.xml` (root)
   - Add/extend profile: `docker-compose`
   - Add properties for compose file path, project name, env file path
   - Add `exec-maven-plugin` executions for:
     - `pre-integration-test` => `docker compose up -d`
     - `post-integration-test` => `docker compose down -v` (default behavior)
     - optional `validate`/`verify` readiness wait hook
2. `pom.xml` (root, existing profile coordination)
   - Keep compatibility with existing `integration-test` profile
   - Avoid changing default build behavior outside profile activation
3. Future module POM (Phase 3)
   - `tests/integration-compose/pom.xml` with Failsafe (`*IT.java`)

## Acceptance checks (Phase 1 DoD)

### Functional

- `docker compose --env-file .env.compose up -d` starts both services.
- `mysql` reports healthy.
- `cms-dts` endpoint readiness returns success for CMS and DTS checks.
- CMS-DTS runtime can resolve and connect to MySQL using Compose service DNS.
- Restart cycle preserves data in `/opt/Percussion/ObjectStore`, `/opt/Percussion/var`, `/opt/Percussion/rxconfig`, `/opt/Percussion/Deployment/Server/conf`, and `/opt/Percussion/jetty/base`.

### Operational

- `docker compose ps` shows both services healthy.
- `docker compose logs cms-dts` show successful startup paths.
- `docker compose down -v` cleans up all containers and named volumes.

### Documentation

- `docs/docker/compose-dev.md` allows a new developer to run the stack without tribal knowledge.
- `percussioncms-dev` skill points to the canonical Compose workflow and MySQL 8 default.

## Execution order

1. Create compose and env template.
2. Create Dockerfiles and verify local image builds.
3. Verify service startup + health checks.
4. Add developer docs.
5. Update skill with pointers.
6. Sanity-run full up/down cycle twice for determinism.

## Risks to watch during implementation

- Installer jars may require interactive flags unless explicitly configured for silent mode.
- Startup times may require generous healthcheck `start_period` and retry settings.
- CMS/DTS readiness endpoints must be stable and unauthenticated for health checks.
- MySQL initialization must match CMS/DTS expected schema/bootstrap behavior.

