# Quickstart Validation: JCR 2.0 API Migration

**Feature**: `987-jcr-2-0-api-migration`  
**Date**: 2026-07-16  

Use this guide to prove Phase 1 (compile) and feature-complete (behavior) without re-deriving design from `plan.md` / `research.md`.

---

## Prerequisites

- JDK 21 via project wrapper (`./mvn-env.sh`)
- Git branch based on current `development` (so parent BOM has `javax.jcr:jcr:2.0`)
- Local Maven cache can resolve `javax.jcr:jcr:2.0` and Jackrabbit commons

---

## Phase 0 — Confirm dependency pin

```bash
# From repo root
rg -n 'javax\.jcr' -A3 pom.xml | head -20
# Expect jcr artifact version 2.0 in dependencyManagement
```

**Expected**: `javax.jcr:jcr` version **2.0** (not 1.0).

If still 1.0: merge/rebase `origin/development` (includes #531) before compile work.

---

## Phase 1 — Compile-clean validation

### 1. Inventory compile failures (after pin is 2.0)

```bash
./mvn-env.sh -pl modules/utils,system -am compile -DskipTests 2>&1 | tee tmp/jcr-compile-phase1.log
```

Expand module list as errors appear (toolkit, sitemanage, extensions, deployer).

**Expected**: Log shows missing methods on JCR implementors (see `research.md` R2) until fixed.

### 2. After implementor stubs/methods land

```bash
./mvn-env.sh -DskipTests compile 2>&1 | tee tmp/jcr-compile-full.log
```

**Expected**: BUILD SUCCESS (or only pre-existing unrelated failures — none from `javax.jcr` implementors).

### 3. Focused unit tests (touched helpers)

```bash
./mvn-env.sh -pl modules/utils -Dtest=PSValuesTest,PSPathTest test
# Plus any new tests added for Binary / getIdentifier / Query bind-limit
```

**Expected**: Tests pass.

### 4. Phase 1 PR gate

- PR title/body scopes **compile-clean only**
- CI compile green for the change set
- Deprecation cleanup **not** required in this PR (FR-014, SC-008)

---

## Phase 2 — Deprecation cleanup spot-check

```bash
# JCR Node getUUID call sites (review carefully — many false positives on IPSGuid)
rg -n '\.getUUID\(\)' --type java -g '!**/target/**' system/services modules/utils projects/sitemanage | head -50
```

**Expected after Phase 2**: Critical editor/publish JCR node paths use `getIdentifier()` or documented helpers; any remaining exceptions listed per FR-013.

---

## Feature-complete — Automated + scripted smoke

### Automated

Run designated repository-backed module tests (adjust list during implementation if suites move):

```bash
./mvn-env.sh -pl modules/utils,system,projects/sitemanage -am test
```

**Expected**: Green (or documented intentional expectation changes).

### Scripted smoke (record results on final PR)

On a running CMS build from the feature-complete branch:

| Step | Action | Expected |
|------|--------|----------|
| 1 | Log in as editor | Success |
| 2 | Create and save a page or asset | Persists; reopen shows same fields |
| 3 | Open existing content item | Loads without repository errors |
| 4 | Preview | Renders without new JCR errors |
| 5 | Publish one site/edition (or minimal publish path) | Completes; no new repository API failures |

Record: date, build id/commit, pass/fail per step (PR comment or checklist attachment).

---

## Dependency / security spot-check

```bash
./mvn-env.sh -pl system -am dependency:tree -Dincludes=javax.jcr:jcr
```

**Expected**: Only version **2.0**; no 1.0 leaves on shipping modules.

---

## References

- Implementor obligations: [contracts/jcr-2.0-implementor-surface.md](./contracts/jcr-2.0-implementor-surface.md)
- Integrator rebuild: [contracts/integrator-rebuild.md](./contracts/integrator-rebuild.md)
- Logical model: [data-model.md](./data-model.md)
- Research decisions: [research.md](./research.md)
