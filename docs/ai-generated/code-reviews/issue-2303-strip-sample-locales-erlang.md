# Erlang review: issue #2303 strip sample locales (pure Java)

**Branch:** `fix/issue-2303-strip-sample-locales-java`  
**Scope:** uncommitted changes for #2303  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** installer/Ant companion tasks; no Nashorn/javax.script on Java 15+; portable Path/UTF-8 I/O

## Summary

Install-blocking bug: ANT `stripSampleLocales` used `<script language="javascript">` (Nashorn via javax.script), removed in Java 15+. Fix promotes the existing Java mirror into production helpers and wires install via perc-ant Ant task `PSStripSampleLocales` (always on install classpath). Distribution-tree helper `SampleSiteLocaleStrip` keeps the same algorithm for CI/CLI.

## Cross-platform path checklist

- Uses `java.nio.file.Path` / `Files` only (no hardcoded separators).
- UTF-8 read/write for XML seed files.
- No Windows-only assumptions in tests (`Path.of`, temp dir).

## Issues

None blocking.

### Note (non-blocking)

Algorithm is intentionally duplicated in `SampleSiteLocaleStrip` (perc-distribution-tree) and `PSStripSampleLocales` (perc-ant) because perc-ant cannot depend on perc-distribution-tree. Both have unit tests with the same fixture shape; install XML wiring test asserts no javascript remains.

## Tests

- `perc-ant`: `PSStripSampleLocalesTest` (3), `AntlibTaskRegistrationTest` (2) — green
- `perc-distribution-tree`: `SampleSiteLocaleStripTest` (5) — green
- Module `mvnw clean install`: perc-ant BUILD SUCCESS; perc-distribution-tree BUILD SUCCESS

## Re-review

N/A (initial approve).
