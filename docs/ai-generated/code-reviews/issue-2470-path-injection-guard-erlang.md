# Erlang review — issue #2470 path-injection guard residuals

**Branch:** `fix/issue-2470-path-injection-guard`  
**Scope:** uncommitted vs `HEAD` (vs `origin/main`)  
**Date:** 2026-08-30  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** CodeQL ladder 1–4 (runtime + model + sink-line + query-filter); do not re-edit already-annotated sinks; GHA does not load local model packs.

## Summary

Leftover CodeQL `java/path-injection` fix-pass on singleton #2470 after merged #3978/#3979/#3980.

- **#2031/#2032** (`PSPathInjectionGuard.requireUnderBase` `File.exists`/`isDirectory` on trusted `baseDir`): runtime now canonicalizes via `getCanonicalFile()` then `isDirectory` (no stat of the raw caller `File`). Model pack adds `Argument[0]` barriers (trusted-base contract) beside existing `ReturnValue`. Same-line `// codeql[java/path-injection]` on `isDirectory`. Query-filter + `**/security/io/PSPathInjectionGuard.java` glob.
- **System re-fingerprints** (`PSXmlDatabaseMetaData` / `PSFileSystemDriver` / `PSServerXmlObjectStore` / `PSXmlObjectStoreHandler`): ladder 1–4 already on `main`. This change adds `**/…/ClassName.java` globs only — **does not re-edit annotated Java sinks**.

No public method/ctor signature change. Product-docs N/A (CodeQL sanitizer / scan config, not operator-facing). UI/Playwright N/A.

## Test evidence

Standalone `cd modules/perc-security-utils && ../../mvnw.cmd clean install`: **BUILD SUCCESS**, Tests run: 306, Failures: 0. New tests: file-as-base rejected, String-overload traversal rejected, `requireUnderBasePath` stays under base. Existing traversal / missing-base tests still pass.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Resolution uses `File` / `getCanonicalFile` / `Path` (`requireUnderBasePath`)
- [x] Prefix compare still normalizes `\\` → `/`
- [x] Tests use `Files.createTempDirectory` / `Files.writeString` / `File.separator`
- [x] Line-ending sensitive assertions: none added

## Issues

None (no bugs, no missing behavioral tests for the changed logic, no non-portable I/O).

## Suggestions (non-blocking)

- GitHub Advanced analysis still may not load the local model pack; query-filter + sink-line are the GHA-effective steps. After merge, a default-branch rescan should close #2031/#2032 and (if globs match) leftover system IDs. Do not re-touch annotated system sinks if new IDs appear — dismiss FP per playbook step 5 with tests cited.
