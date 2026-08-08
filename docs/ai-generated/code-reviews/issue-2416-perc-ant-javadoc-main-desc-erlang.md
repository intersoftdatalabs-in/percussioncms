# Erlang self-review — issue #2416 perc-ant javadoc main descriptions

**Verdict:** approve

## Scope
- Module: `modules/perc-ant`
- Add one-line main description above `@param`/`@return`-only Javadoc on:
  - `PSMigrateI18nLocaleCodes`: `setDryRun` / `isDryRun` / `setFailOnError` / `isFailOnError`
  - `PSStripSampleLocales`: `setInputFile` / `getInputFile` / `setStagingFile` / `getStagingFile`
- No Xlint scope change; no behavior change.

## Findings
- Documentation-only; no path I/O, no new logic, no companions beyond existing suite.
- Existing `PSStripSampleLocalesTest` + suite (49) remain green; no new behavioral tests required for comment text.
- Standalone clean install: BUILD SUCCESS; no "no main description" warnings for the listed methods.

## Hard gates
- [x] No new non-portable path I/O
- [x] Javadoc-only — existing tests cover methods
- [x] Standalone `cd modules/perc-ant && ../../mvnw.cmd clean install` BUILD SUCCESS

> Co-Authored by Grok Build using grok-4.5 with agent main.
