# Erlang review: stop tracking WebUI intermediate shared-* bundles

**Date:** 2026-07-17  
**Branch (planned):** `chore/webui-untrack-generated-shared-bundles`  
**Base:** `origin/development` @ `56b30bd1d7` (includes already-pushed line-endings-only commit on the former tracked blobs)  
**Reviewer persona:** Erlang (strict pre-commit / pre-PR)

## Summary

Removes checked-in intermediate concatenations (`shared-common.js`, `shared-common-minuet.js`, `shared-finder.js`, and CSS siblings) from `WebUI/src/main/webapp/cm/` (and `WebUI/war/shared-finder.js`), gitignores them, and hardens `build-legacy-bundles.js` so Maven packaging gets non-empty outputs only from `target/generated-webui/cm/`. Fixes the broken source root (`webapp` → `webapp/cm`) that would otherwise have shipped near-empty bundles once the committed blobs were deleted. Adds behavioral vitest coverage for resolution and build size.

## Scope

- Uncommitted + staged vs `HEAD` / `origin/development` (same tip).
- Does **not** include the already-pushed `fix line endings` commit as new work to review for content — that history stays on `development`; this PR only deletes those files from tracking going forward.
- **Memory patterns hit:** missing behavioral tests for non-trivial logic; non-portable path joins; false-green empty outputs; CodeQL path-exclude hygiene.
- Cross-platform path review: **clean** — Node `path.join` / `path.sep` used; tests normalize path display with `split(path.sep).join("/")` for assertions only (not filesystem construction).

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No hard-gate bugs: portable path APIs, phase-1 fail-on-missing + size floor, and 6 new vitest cases that exercise real resolve/build behavior (not token-only greps).

## Issues

### suggestion

1. **`perc_common_ui*.js` still tracked and incompletely generated**  
   **Where:** `WebUI/src/main/webapp/cm/perc_common_ui.js`, `perc_common_ui_slim.js`, and `common-ui-bundle.json` `../../delivery/common/js/...` paths  
   **Why:** Same “generated intermediate” class; delivery sources still miss under current resolve roots, so this PR correctly left them alone. Follow-up should retarget delivery paths (e.g. `modules/perc-common-ui-bundle`) then untrack those blobs too.  
   **Suggestion:** Track as a separate chore; do not block this PR.

2. **Legacy root builder still targets `WebUI/war/`**  
   **Where:** `WebUI/scripts/build-legacy-bundles.js`  
   **Why:** Maven uses `src/main/frontend/scripts/…` only. Dual builders can confuse local workflows.  
   **Suggestion:** Document or deprecate the root script in a follow-up; optional.

### nit

3. **Weak packaging-contract test**  
   **Where:** `buildLegacyBundles.test.js` last case only asserts `OUTPUT_DIR` ≠ `WAR_DIR` path join for filenames  
   **Suggestion:** Optional later: assert `git check-ignore` or that webapp copies are not required by reading package overlay docs only — not a gate.

## Verification performed

- `node scripts/build-legacy-bundles.js` produces ~2.7MB `shared-common.js` under `target/generated-webui/cm/`
- `npm test -- --run src/test/js/buildLegacyBundles.test.js src/test/js/percUtils.test.js` → **20/20 pass**
- `git ls-files` no longer lists `shared-common*` / `shared-finder*` under webapp after `git rm`
- `git check-ignore` hits webapp intermediate paths

## Files in change set

|                               Path                               |                        Role                         |
|------------------------------------------------------------------|-----------------------------------------------------|
| `WebUI/src/main/frontend/scripts/build-legacy-bundles.js`        | WAR_DIR fix, fail-on-missing, assert sizes, exports |
| `WebUI/src/main/frontend/src/test/js/buildLegacyBundles.test.js` | Behavioral tests                                    |
| `.gitignore`, `WebUI/.gitignore`                                 | Ignore intermediates                                |
| `WebUI/AGENTS.md`                                                | Generated-not-committed contract                    |
| `.github/codeql/codeql-config.yml`                               | Comment refresh (paths-ignore retained)             |
| Deletions under `webapp/cm/` + `war/shared-finder.js`            | Stop tracking generated blobs                       |

