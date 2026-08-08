# Erlang self-review — issue #2023 perc-ant Xlint batch 1

**Verdict:** approve

## Scope
- Module: `modules/perc-ant`
- Clear inventoriable main-source `-Xlint:all` diagnostics (34 deprecation → 0)
- Project parent `-Xlint` / `-Xlint:-deprecation` was already 0 for main sources

## Findings
- No bugs identified in real API migrations (IOTools→NIO/commons-io, Ant getLocation/getFileUtils, File.toURI().toURL, Jericho getElement().getEndTag, WildcardFileFilter.builder, Class.forName without newInstance).
- Portable paths: PSPropagateFile now uses `Path.resolve` instead of `"/" +` string join.
- Intentional `@SuppressWarnings("deprecation")` only on legacy upgrade crypto / PSEntityResolver / Ant Main(String[]) — no blanket suppressions.
- New `PSInstallIoUtils` covered by 6 unit tests; module suite 49 tests green.
- Residual (not module-zero for issue acceptance): javadoc "no main description" on PSMigrateI18nLocaleCodes / PSStripSampleLocales (~8). Original issue "200" was inventory-cap noise; live main Xlint:all was 34.

## Hard gates
- [x] No new non-portable path I/O
- [x] Behavioral tests for new helper
- [x] Standalone `cd modules/perc-ant && ../../mvnw.cmd clean install` BUILD SUCCESS
