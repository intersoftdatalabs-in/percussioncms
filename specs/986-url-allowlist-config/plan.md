# Implementation Plan: Configurable Allowed and Blocked URL Lists

**Branch**: `986-url-allowlist-config` | **Date**: 2026-07-16 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `specs/986-url-allowlist-config/spec.md` (issue #1205)  
**Related research**: [research.md](./research.md)

## Summary

Improve shared SSRF URL validation so customers can **allow** legitimate outbound integration URLs (external APIs, internal HR APIs, i18n services) after upgrade without turning off security. Policy lives in install-root files `rxconfig/Server/allowedUrls.properties` and `blockedUrls.properties` (full-URL globs, additive allow, block precedence, private unlock via allow only). Unreleased JVM system properties for URL validation are **removed**. Defaults: block file active for dangerous targets; allow file comments/examples only. Install/upgrade **create-if-absent**, never overwrite.

## Technical Context

- **Language/Version**: Java 21 (`development`)
- **Owning Module(s)**: `modules/perc-security-utils` (primary); packaging `modules/perc-distribution-tree` / installer config copy; optional CMS server init wiring in `system/`; docs/release notes
- **AGENTS Hierarchy**: root `AGENTS.md`; no module AGENTS under perc-security-utils
- **Dependencies & Storage**: No new third-party deps; line-oriented files on disk under install root; `rxdeploydir` for path resolution (no Maven dep on `utils` — cycle risk)
- **Testing**: JUnit 5 in `perc-security-utils`; extend `URLValidationTest`; temp-dir seed tests; keep consumer SSRF tests green
- **Scale/Impact**: All server-initiated callers of `URLValidation`; admin ops config; upgrade path for every CMS install

## Constitution Check

- [x] **I. Module-First Boundaries** — primary work in `perc-security-utils`; packaging secondary
- [x] **II. Evidence Over Invention** — cites `URLValidation`, `URLValidationConfig`, `PSCopy replaceType=never`, `rxdeploydir`
- [x] **III. Test Discipline** — unit/seed/precedence tests planned (FR-013)
- [x] **IV. Contract & Integration Integrity** — public method signatures preserved; file contracts documented
- [x] **V. Safe Modernization** — no Spring Boot; incremental change to existing validator
- [x] **VI. Security by Default** — block wins; hard metadata deny; no secrets in logs; additive not exclusive open
- [x] **VII. Build & Dependency Hygiene** — JDK 21; avoid utils↔security-utils cycle
- [x] **VIII. Documentation & Operability** — release notes + admin docs; diagnosable denials
- [x] **IX. PR Review Comment Resolution** — apply when PR is opened
- [x] **Complexity Budget** — no constitution exceptions required

### Post-design re-check

Gates still pass after research/contracts. Complexity Tracking empty.

## Project Structure

### Documentation (this feature)

```text
specs/986-url-allowlist-config/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── url-list-files.md
│   └── url-validation-decision.md
├── checklists/requirements.md
└── tasks.md                 # via /speckit-tasks
```

### Source Code (affected paths)

```text
modules/perc-security-utils/
  src/main/java/com/percussion/security/validation/
    URLValidation.java              # decision order + list matching
    URLValidationConfig.java        # drop system props; hold list patterns
    URLListFileLoader.java          # NEW: parse/load/seed helpers
    URLGlobMatcher.java             # NEW: normalize + glob match (or package-private)
  src/main/resources/               # optional classpath default templates
  src/test/java/...                 # URLValidationTest + list/seed tests

modules/perc-distribution-tree/ (and/or installer config source dir)
  .../rxconfig/Server/allowedUrls.properties   # NEW default template
  .../rxconfig/Server/blockedUrls.properties   # NEW default template
  installDistributionFiles.xml                 # PSCopy replaceType=never

system/ (optional)
  server init hook to ensure config load after Rx dir known

docs / release notes
  8.2 notes + admin help for URL lists
```

## Implementation approach

### Phase A — Matcher and loader (perc-security-utils)

1. Implement normalize + glob match (full absolute URL string).
2. Implement file parser (comments, blanks, ignore lone `*`).
3. Seed helper: write classpath or embedded default bytes if file missing; no-op if exists.
4. Unit tests with temp directories.

### Phase B — Wire into URLValidation

1. On config construction / first validate (or explicit `setDefault`), load allow/block from `{rxdeploydir}/rxconfig/Server/...`.
2. Apply decision order from [contracts/url-validation-decision.md](./contracts/url-validation-decision.md).
3. Remove system-property loading from `URLValidationConfig`.
4. Keep hard metadata deny + loopback baseline.
5. Update `URLValidationTest` and add allow/block cases.

### Phase C — Packaging and upgrade

1. Add default property files to distribution config source.
2. Register in installer `PSCopy` with **replaceType=never** under `rxconfig/Server`.
3. Verify create-if-absent on upgrade scenarios (unit for seed + install doc).

### Phase D — Documentation

1. Release notes: security enhancement, paths, additive allow, private unlock, block wins, globs, no system props, non-overwrite.
2. Admin/help: examples for weather / internal API / i18n allow lines.

### Phase E — Consumer smoke

1. Run existing SSRF tests for proxy / document utils / DTD tree.
2. Fix any message text that still references removed system properties.

## Complexity Tracking

*(None — no constitution exceptions.)*

## Next

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
