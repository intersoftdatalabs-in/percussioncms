# Erlang review — remove commons-httpclient (Dependabot #81)

| Field | Value |
|-------|--------|
| **Date** | 2026-07-17 |
| **Branch** | `fix/remove-commons-httpclient-dependabot-81` |
| **Base** | `development` / `HEAD` uncommitted |
| **Intent** | Close Dependabot alert #81 by removing reintroduced `commons-httpclient:3.1` from `WebUI/pom.xml`; delete dead `PSFormDataServiceTest` |
| **Reviewer** | Erlang (strict, independent) |

## Scope

**In scope (intentional):**

| Path | Change |
|------|--------|
| `WebUI/pom.xml` | Delete direct dependency `commons-httpclient:commons-httpclient:3.1` |
| `projects/sitemanage/src/test/java/com/percussion/assetmanagement/forms/service/impl/PSFormDataServiceTest.java` | Delete entire file (vacuous `testNothing` + large commented legacy suite) |

```text
 WebUI/pom.xml                                      |   5 -
 .../forms/service/impl/PSFormDataServiceTest.java  | 371 ---------------------
 2 files changed, 376 deletions(-)
```

**Out of scope / do not commit:** working-tree noise on

- `WebUI/src/main/webapp/cm/shared-common.js`
- `WebUI/src/main/webapp/cm/shared-common-minuet.js`

These show as modified (line-ending / autocrlf churn, ~100k-line renoise). **Not part of the fix.** Staging must be path-explicit.

## Memory patterns hit

- Hard gate: missing behavioral tests for new non-trivial logic — **N/A** (deletion only; no new logic)
- Hard gate: non-portable paths — **N/A** (no path/file I/O code)
- Security / config: residual vulnerable dependency left declared after “mitigation” — **this PR addresses that**
- Maintainability: vacuous tests (`testNothing` / always-true) — **removal is correct**
- False-positive guards: N/A

## Context verified

1. **Prior mitigation:** Commits `6e78b42653` / `8ecb368bee` (2026-02-27/28) removed `commons-httpclient` and migrated production HTTP to JDK `HttpClient`.
2. **Regression:** Merge `3755de7510` (2026-03-13) re-added the direct WebUI dependency; Dependabot #81 opened the same day.
3. **Live usage:** No production `org.apache.commons.httpclient` imports remain in `*.java`. Only the deleted test file’s **commented** body referenced Commons HttpClient 3 APIs (`HttpClient`, `PostMethod`).
4. **Test under deletion:** Active body was only `testNothing() { assertTrue(true); }` — zero behavioral value. Class Javadoc already stated unreliability and DTS coverage.
5. **Manifest residual:** After this change, no `pom.xml` declares `commons-httpclient` (repo scan of `**/pom.xml` + `**/*.java`).
6. **Cross-platform path checklist:** Not applicable (no I/O / path code in diff). Outcome: clean / N/A.

## Issues

| ID | Severity | Status | Finding |
|----|----------|--------|---------|
| — | — | — | **No bugs.** |

### Suggestions (non-blocking)

| ID | Severity | Finding |
|----|----------|---------|
| S1 | nit | After merge, confirm Dependabot alert #81 auto-closes; if the graph lags, wait for dependency submission refresh rather than manual dismiss. |
| S2 | nit | Working tree may still dirty `shared-common*.js` via CRLF; stage only `WebUI/pom.xml` and the deleted test path. |

## Recommendation

**`approve`**

## Gate

| Check | Result |
|-------|--------|
| Bugs | none |
| Missing behavioral tests for new logic | N/A (removal only) |
| Non-portable path/file I/O | N/A |
| Accidental bundle churn in commit | **Prevented by explicit staging** |

**May commit/push: yes** — for the intentional two-path change only.

Do **not** include `shared-common.js` / `shared-common-minuet.js` in the commit.

## Suggested commit subject

```text
fix(security): remove reintroduced commons-httpclient 3.1 (Dependabot #81)
```
