# Erlang review — 991-system-java-home US5 + US6 (re-point + legacy compatibility)

## Summary

US5 documents the post-install re-point path (edit `java.properties`,
restart console, re-run service installer when needed) and is already
covered by the Phase 2 / US1 / US2 README sections plus the
`ResolutionResult.renderFailure(...)` helper that lists attempted sources
in failure messages. US6 keeps legacy operators working: (a) the
runtime contract already accepts `<InstallDir>/JRE|JRE64` as a fallback;
(b) the install.xml JRE/lib/ext block is soft-gated and now regression-
proofed via `InstallXmlJreSoftGateTest`; (c) the legacy
`system/release/installer/Linux/` helpers no longer say "Must be
version 1.8" and instead point operators to the new resolution contract.
No new runtime paths or external dependencies. Recommend approve.

## Scope

- Base: `development`
- Head: `991-system-java-home` (uncommitted at review time)
- Files: 4 changed (1 new test + 2 legacy helper messaging updates + 1 task checklist)
- Prior reports: see `991-system-java-home-phase2-us1-erlang.md`,
  `991-system-java-home-us2-erlang.md`,
  `991-system-java-home-us3-us4-erlang.md`
- Memory patterns hit: `cross-platform.io (Path, Files)`, `legacy-compat (soft-gate)`

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: **yes**

## Issues

### Issue 1 -- Severity: nit
- File: `system/release/installer/Linux/install-service.sh:127`
- Description: Updated the "Must be version 1.8" error message to point at
  Java 21 and reference `specs/991-system-java-home/`. The legacy
  script's catalog of assumptions (e.g. the JRE/JRE64 fallback
  heuristic) remains since the new world lets the contract replace it
  piecemeal.
- Suggestion: Document the full migration from this legacy script to
  the modern `install-jetty-service.sh` in `system/release/installer/`
  README (does not currently exist).
- Status: open (non-blocking)

### Issue 2 -- Severity: nit
- File: `system/release/installer/unix/` and `system/release/installer/windows/`
- Description: Same "1.8" wording remains in the unix / windows siblings
  of `percussion-service.sh` / `install-jetty-service.sh`. Those
  directories are legacy and only used in-place by some integrators.
- Suggestion: Phase 9 polish could sweep all four siblings.
- Status: open (non-blocking — out of scope for this PR)

## Cross-platform path review

- install.xml change: none (the soft-gate behavior already existed; the
  new test makes it regression-proof). No new path strings introduced.
- Legacy helper message updates: text-only edits, no path code changes.

## Non-portable pattern hits: none

## Behavioral test coverage

- `InstallXmlJreSoftGateTest` — asserts the JRE/lib/ext block in
  install.xml is wrapped in `failonerror="false"` and scans both
  `JRE/lib/ext` and `JRE64/lib/ext`. Regression-proof.
- `JavaHomeResolverTest.legacyJreUsedWhenHigherSourcesAbsent` and
  `legacyJre64UsedAfterJreWhenOnlyJre64Valid` — verify the legacy
  fallback contract.

## Author is also reviewer (disclosed)
