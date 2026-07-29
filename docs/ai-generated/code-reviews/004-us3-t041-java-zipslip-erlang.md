# Erlang Review — 004/us3-t041-java-zipslip

**Date**: 2026-07-17  
**Reviewer**: Erlang (strict independent pre-PR)  
**Scope**: Uncommitted ZipSlip fixes on `004/us3-t041-java-zipslip` vs `origin/development`

## Summary

Closes the three open `java/zipslip` code-scanning alerts (#720, #722, #723) by routing archive entry names through `PathValidation.constructSafePath` **before** any `mkdirs` / `FileOutputStream`. Prior partial fix on `PSArchiveFiles` still created parent dirs from the raw entry name; that gap is closed. Behavioral unit tests cover reject-and-no-escape for all three modules.

## Scope

- Base: `origin/development`
- Head: uncommitted working tree on `004/us3-t041-java-zipslip`
- Files: 3 production + 2 new tests (+ this review doc)

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Cross-platform path checklist

- [x] No hardcoded filesystem separators for local path construction in production code (uses `PathValidation` / `File` APIs)
- [x] Tests use `Files.createTempDirectory` / `File` and `File.separatorChar` when building expected paths
- [x] Zip entry names remain `/`-separated (ZIP/URL convention) — correct
- [x] Containment checks rely on `PathValidation` canonicalization (Windows-safe)

## Issues

### Issue 1 — Severity: nit

- File: `system/.../PSArchiveFiles.java`
- Description: Zip directory entries that use a trailing backslash (non-standard; ZIP always uses `/`) are not stripped before validation. Harmless on real archives.
- Suggestion: Optional follow-up only if non-conformant archives appear in the wild.

### Issue 2 — Severity: suggestion

- File: `projects/sitemanage/.../PSWidgetPackageBuilder.java`
- Description: After `xform.transformPath`, re-validation uses `validatePathWithinDirectory` on the absolute `File`. Correct, but transformers that return paths outside `rootDir` now fail closed (good). Document that transformers must stay under `rootDir`.
- Suggestion: Optional one-line javadoc on `setFileTransformers` — not blocking.

## Positive notes

- Validation happens before any filesystem mutation (fixes residual CodeQL flow on #723).
- `PSExtractJarFiles` skips bad entries and continues with safe ones (installer resilience).
- Fail-then-pass tests assert the escape file is absent outside the extract root.

## Tests run

- `./mvnw -pl system -am -Dtest=PSArchiveFilesZipSlipTest -Dsurefire.failIfNoSpecifiedTests=false test` — 4 tests GREEN
- `./mvnw -pl modules/perc-ant -Dtest=PSExtractJarFilesZipSlipTest test` — 1 test GREEN
- `./mvnw -pl projects/sitemanage -am -Dtest=PSWidgetPackageBuilderZipSlipTest -Dsurefire.failIfNoSpecifiedTests=false test` — 1 test GREEN

## Handoff

Safe to commit and open PR against `development`.
