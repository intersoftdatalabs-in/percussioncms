# Plan: Maven-Driven Docker Compose Dev Workflow (CMS + DTS + Integration Tests)

## Why this plan

Current `percussioncms-dev` guidance is script-centric. We need a Maven-goal-centric developer workflow that:

1. Builds installable artifacts.
2. Starts CMS + DTS as a reproducible Docker Compose stack.
3. Runs integration tests against the running stack.
4. Documents these as first-class goals in `percussioncms-dev` so both agents and developers can use a single entry point.

## Scope

In scope:

- Add Docker Compose assets for local dev/runtime orchestration.
- Add Maven goals/profiles to build, install, run, stop, and verify stack readiness.
- Add a dedicated integration-test module for container-backed tests.
- Update `percussioncms-dev` skill with Maven goal pointers (primary path), while preserving script references as fallback.

Out of scope (for this phase):

- Production deployment orchestration.
- Kubernetes manifests.
- Re-architecting runtime packaging beyond what is needed to run CMS + DTS in Docker.

## Decision log (agreed)

- Compose location: repo root (`docker-compose.yml`).
- Maven exposure style: profile + standard lifecycle is primary.
- One-command integration workflow default: always teardown on success and failure.
- Service dependencies for v1: keep CMS + DTS minimal baseline, but include a database service in Compose when required for reproducible local runtime.
- Default database container: MySQL 8.

## Proposed target architecture

- `docker-compose.yml` at repo root.
- Services:
  - `cms` (built from local artifacts)
  - `dts` (built from local artifacts)
  - Optional dependency services as required by runtime (DB, broker, etc.)
- Health checks for each runtime service.
- Persistent volumes for install/runtime data where needed.
- Environment variable templates (no secrets committed).

## Proposed Maven contract (developer-facing)

Primary command surface (profile + lifecycle first):

- `-Pdocker-compose package` – build required artifacts/images.
- `-Pdocker-compose pre-integration-test` – start stack and wait for readiness.
- `-Pintegration-test failsafe:integration-test failsafe:verify` – run ITs.
- `-Pdocker-compose post-integration-test` – stop/remove stack.
- `-Pdocker-compose verify` – optional all-in-one lifecycle path.

Implementation note:

- Keep profile/lifecycle commands as canonical and optionally add friendly aliases later if they improve usability.
- Avoid requiring developers to memorize raw `docker compose` commands.

## Integration test module plan

Create a dedicated module, candidate path:

- `tests/integration-compose` (preferred)

Module responsibilities:

- Owns only container-backed integration tests (`*IT.java`).
- Uses Failsafe and JUnit 5.
- Contains API/client helpers for CMS/DTS endpoint validation.
- Supports two modes:
  1. Reuse externally running stack.
  2. Optional lifecycle-coupled mode (`it:run-with-compose`).

## Phase plan

### Phase 0 — Discovery and decisions (short)

Deliverables:

- Confirm runtime dependencies needed for CMS + DTS in Docker (DB choice, ports, env vars, persistence).
- Confirm image build strategy:
  - Build from local Maven artifacts (preferred), or
  - Build from prebuilt distribution jars.
- Confirm canonical readiness endpoints for CMS and DTS.

Exit criteria:

- Decisions captured in this plan and accepted.

### Phase 1 — Compose baseline

Deliverables:

- Add initial Compose file(s) and `.env.example`.
- Add Dockerfiles for CMS and DTS image creation from local artifacts.
- Add health checks and deterministic port mappings.

Exit criteria:

- `docker compose up -d` brings up healthy CMS + DTS stack locally.

### Phase 2 — Maven goals for compose lifecycle

Deliverables:

- Add profile(s) and plugin executions for compose lifecycle goals.
- Add goal(s) to build artifacts + images in one command path.
- Add wait/readiness checks surfaced as Maven goals.

Exit criteria:

- Developers can run stack lifecycle from Maven only.

### Phase 3 — Dedicated integration-test module

Deliverables:

- Add `tests/integration-compose` module.
- Add first set of smoke ITs (auth, site list, asset list, page list, DTS endpoint).
- Wire module into parent POM with profile gating.

Exit criteria:

- `it:run` executes green against a running stack.

### Phase 4 — End-to-end workflow goal

Deliverables:

- Add `it:run-with-compose` orchestration path.
- Ensure teardown on failure paths.
- Add report location docs.

Exit criteria:

- Single command can run full integration workflow reproducibly.

### Phase 5 — Skill and docs alignment

Deliverables:

- Rewrite `percussioncms-dev` skill to be Maven-first.
- Add "Primary goals" section + fallback script section.
- Update troubleshooting for Compose and integration runs.

Exit criteria:

- Skill points developers/agents to Maven goals as canonical workflow.

## Validation strategy

- Functional:
  - Stack boots, health endpoints pass.
  - CMS API auth and key endpoints return expected responses.
  - DTS endpoint(s) reachable and healthy.
- Build:
  - `./mvnw` wrapper used for all Maven actions.
  - Module/profile combinations run without affecting default build behavior.
- Failure handling:
  - Clear errors when Docker is missing/not running.
  - Readiness timeout errors are actionable.

## Risks and mitigations

- Runtime dependency ambiguity (DB/runtime prereqs):
  - Mitigation: explicit Phase 0 decision gate before implementing Compose.
- Overloading default Maven lifecycle:
  - Mitigation: keep compose and IT orchestration profile-gated and opt-in.
- Flaky integration tests:
  - Mitigation: enforce readiness checks and deterministic test data setup.

## Proposed implementation order (lowest risk)

1. Compose baseline and health checks.
2. Maven compose lifecycle goals.
3. Dedicated integration module with smoke tests.
4. One-command E2E IT run.
5. Skill/docs update.

## Success criteria

- A new contributor can execute one Maven command sequence to:
  1. Build artifacts.
  2. Start CMS + DTS in Docker Compose.
  3. Run integration tests.
  4. Tear down cleanly.
- `percussioncms-dev` clearly documents the exact Maven goals for each step.

## Remaining clarification

None. Phase 0 decision gate is complete.
