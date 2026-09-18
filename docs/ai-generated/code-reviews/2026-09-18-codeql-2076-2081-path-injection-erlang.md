# Erlang review — CodeQL #2076–#2081 path-injection residuals

**Date:** 2026-09-18
**Branch:** `fix/codeql-alert-2077-path-injection`
**Base:** `origin/main` (commit `c3de332665`)
**Reviewer:** Erlang (independent of implementer)

## Summary

Default-branch CodeQL `java/path-injection` alerts #2077–#2081 (cluster on
`PSServerXmlObjectStore.java:730/731/741/742`) and #2076
(`ApplicationFileAdaptor.java:1026`) are residuals after ladder steps 1–4
already on `main` (PR #4571 / #4352 / #4578). This change does **not**
re-edit annotated sinks (which would re-fingerprint and reopen new alert
IDs — see playbook row 184). It only:

1. Extends the `PSServerXmlObjectStore.java` `query-filters` reason to mention #2077–#2081.
2. Extends the `ApplicationFileAdaptor.java` `query-filters` reason to mention #2076 and the
   `deleteRecursively_removesTreeUnderTempDir` sibling test.
3. Adds two `suppressions.md` rows documenting the new alert numbers and their disposition.

No Java source files touched. No sink lines touched.

## Scope

- `.github/codeql/codeql-config.yml` — 2 reason-text additions (no schema change)
- `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md` — 2 rows added

## Recommendation

approve

## Gate

May commit/push: **yes** (no `bug` findings, no rule changes, docs-only diff)

## Issues

None.

### Hard-gate checks (per root `AGENTS.md`)

| Gate | Status | Note |
|------|--------|------|
| Default setup stays `not-configured` | OK | `gh api .../default-setup --jq .state` → `not-configured` |
| Sink-line annotations on sink (not above multi-line builders) | n/a | no source change |
| Annotations are short (`// codeql[rule-id]` only) | n/a | no source change |
| `paths-ignore` / `query-filters` reason text expanded only | OK | no schema/glob/paths change |
| `suppressions.md` updated | OK | 2 new rows matching format of #2074–#2075 |
| No AGENTS.md / rule-file changes | OK | docs/config only |
| No copyright header changes | OK | no source change |
| Cross-platform paths in tests | n/a | no test change |
| Erlang model-pack barrier maintained | OK | pre-existing entries untouched |

## Evidence

- `cd system && JAVA_HOME=/usr/lib/jvm/java-21-openjdk ../mvnw -B -Dtest='PSServerXmlObjectStorePathInjectionTest,PSXmlObjectStoreHandlerPathInjectionTest' test`
  → **BUILD SUCCESS**; Tests run: 16, Failures: 0, Errors: 0, Skipped: 0.
- `cd rest && JAVA_HOME=/usr/lib/jvm/java-21-openjdk ../mvnw -B -DskipTests=true clean install`
  → **BUILD SUCCESS** (needed to refresh `rest-8.2.0-SNAPSHOT.jar` with `ApplicationFileSummary.setLock`
  from PR #4582 — pre-existing baseline dependency for sitemanage).
- `cd projects/sitemanage && JAVA_HOME=/usr/lib/jvm/java-21-openjdk ../../mvnw -B -Dtest='ApplicationFileAdaptorTest' test`
  → **BUILD SUCCESS**; Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
  (covers `deleteRecursively_removesTreeUnderTempDir` line 602 + ObjectStore exists/rename/delete under thread RxDir;
  pre-existing suite covers the alerts via `requireFileUnderRxDir` / `requireUnderBase` / `resolveUnderAppRoot`).

## Cross-references

- Playbook row 184: "Do not re-edit annotated sinks only to chase IDs."
- `suppressions.md` rows 88–103 (ObjectStore + ApplicationFileAdaptor residual thrash history).
- `path-injection-guard.model.yml` lines 207–226 (PSServerXmlObjectStore.getAppRootDir + requireFileUnderRxDir
  ReturnValue barriers) and lines 299–342 (ApplicationFileAdaptor.resolveAppRootDir + resolveUnderAppRoot +
  requireSafeRelativePath + normalizeSafeRelativePath barriers).
