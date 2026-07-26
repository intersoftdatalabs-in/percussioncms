# Erlang review: fix/perc-distribution-tree-casing-portable

**Date**: 2026-07-20
**Branch**: `fix/perc-distribution-tree-casing-portable` (no commits ahead of `origin/development`; all changes are in the working tree)
**Base**: `origin/development` (fetched successfully on Windows host)
**Reviewer**: Erlang (independent strict subagent — fresh session, not the implementer)

## Summary

Test-only change that removes a Windows-hostile **case-sensitive-only** assertion in
`percussionInstallationAltCasing` and replaces it with a portable, **stronger** behavioral
assertion: (1) the candidate's `relativeName` matches the directory the fixture created,
compared case-insensitively with `Locale.ROOT`; and (2) the candidate's `absolutePath`
resolves to the same on-disk directory as the fixture, asserted via `Files.isSameFile`
(OS inode comparison — the correct portable way to handle Windows' case-insensitive
filesystems). Production code (`ObsoleteInstallDirCleaner.java`) is unchanged and the
canonical-first / ALT-fallback preference ordering is preserved. The author's reported
results (22/22 pass on the test class; 59 tests / 0 failures / 0 errors / 1 skipped in the
full module) match the review.

## Scope

- Base: `origin/development` (HEAD `07de79f0a`, fetched OK)
- Head: working tree on `fix/perc-distribution-tree-casing-portable` (no new commits)
- Files (1 changed in scope, 2 unrelated working-tree mods):
  - `modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/ObsoleteInstallDirCleanerTest.java` — real change (test fix)
  - `WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/jquery.min.js` — line-ending only (LF↔CRLF on Windows checkout; no content diff)
  - `WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/jquery-migrate.min.js` — line-ending only (LF↔CRLF on Windows checkout; no content diff)
- Prior topic report (loaded for continuity):
  - `docs/ai-generated/code-reviews/2026-07-16-erlang-985-clean-install-dir.md` (initial `request-changes` → BUG-1..4 fixed)
  - `docs/ai-generated/code-reviews/2026-07-16-erlang-985-clean-install-dir-rereview.md` (post-fix `approve`)
- Memory patterns hit:
  - `paths.case-sensitive-only-assumption` (root AGENTS § Cross-Platform File I/O & Paths; `patterns.md` cross-platform hard gate) — **the change fixes exactly this class of bug**
  - `tests.behavioral-coverage` — strengthened (added `Files.isSameFile` semantic assertion)
- Author-side validation (informational, not re-run by Erlang):
  - Before fix: `ObsoleteInstallDirCleanerTest` 1 failure (`percussionInstallationAltCasing` on Windows case-insensitive FS)
  - After fix: `ObsoleteInstallDirCleanerTest` 22 pass; module 59 tests / 0 failures / 0 errors / 1 skipped (`MainExtractExecutableTest` unconditional skip)
  - Integrity hash ledger refreshed via `mvn-env.bat validate`
  - Module has no explicit Spotless/Checkstyle invocation — test-only delta is low formatting surface

## Recommendation

`approve`

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Cross-platform path review

Applied root `AGENTS.md` → **Cross-Platform File I/O & Paths** checklist to the changed test
and to the unchanged surrounding production code (read for context only, not modified).

- No hardcoded `"/"` or `"\\"` joins in the diff — `tempDir.resolve(...)` is used.
- No Unix-only absolute roots (`/tmp`, `/var`, `/home`) — the test uses `@TempDir` (portable JUnit).
- No hardcoded Windows-only paths.
- No multi-path list join with `:` / `;` only.
- No regex or path string equality assuming Unix shapes only.
- No reliance on case-insensitive lookup that would silently work only on Windows — on the
  contrary, the diff **removes** a case-sensitive-only assertion that was failing on
  Windows.
- No line-ending assertions on multi-line content.
- The new comparison uses `String.toLowerCase(Locale.ROOT)` on both sides — locale-safe and
  stable across `tr-TR` style locales.
- The new path-equality assertion uses `Files.isSameFile(path1, path2)` — this is the
  recommended portable pattern from `patterns.md` (and `instructions/java-coding-standards.md`).
  `Files.isSameFile` resolves both paths and compares their OS-level inode / file identity,
  which correctly handles case-insensitive filesystems (Windows, default macOS volumes)
  without depending on the textual casing of either operand.
- Production code in `ObsoleteInstallDirCleaner.listEligibleCandidates`
  (`modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/ObsoleteInstallDirCleaner.java:117-137`)
  was reviewed for the cross-platform checklist as context. It already uses `Path` /
  `Files` consistently, prefers canonical casing first with ALT fallback, and is unchanged.
  The cleaner's chosen relative name is reported textually; on Windows it will always be
  the canonical `_Percussion_Installation` because `Files.exists` is case-insensitive on
  NTFS — that behavior is intentional (label normalization) and is what the test now
  accommodates portably.

Cross-platform path review: **no issues.**

## Issues

(none)

## Notes (non-blocking, informational only)

- **Unrelated working-tree mods**: `WebUI/src/main/webapp/cm/jslib/profiles/3x/jquery/jquery.min.js`
  and `.../jquery-migrate.min.js` show as modified but `git diff` on each is empty —
  the change is purely a Windows checkout line-ending normalization (LF↔CRLF). Git prints
  the "LF will be replaced by CRLF" warning for those files. They are **not** part of the
  portability fix and should not be staged. `git add modules/perc-distribution-tree/src/test/java/com/percussion/preinstall/ObsoleteInstallDirCleanerTest.java` is sufficient.
- **Nit — inline `java.util.Locale.ROOT`**: the only style nit in the diff. The full
  qualifier is intentionally not imported; this is a common, readable Java convention and
  is not worth flagging as an issue. Leaving as a note for completeness only.
- **Assertion strengthening (positive observation)**: the original assertion
  (`assertEquals("_Percussion_installation", c.get(0).relativeName())`) was a single
  string equality check that incidentally coupled test correctness to the OS-level
  case-sensitivity of `Files.exists`. The new assertion replaces that with **two**
  checks: a locale-rooted case-insensitive name comparison **plus** a semantic
  `Files.isSameFile` check that the candidate's absolute path resolves to the same
  on-disk directory the fixture created. This is strictly stronger and addresses the
  root cause rather than papering over the symptom.
- **PR review thread protocol (preemptive)**: this is the first review of the branch
  (no PR number yet). When a PR is opened, any reviewer-thread findings must be
  mitigated inline (with commit hash + change description + test pointers) and the
  thread resolved via `resolveReviewThread` per root AGENTS § PR Review Comment
  Resolution. Noted for the author when the PR is opened.
- **Pre-commit review rule compliance**: this is an implementer-initiated session
  invoking Erlang for the pre-commit gate (per `.kilocode/rules/pre-commit-review.md`).
  Author/reviewer independence is satisfied because this Erlang pass was run as a
  fresh subagent in this session — disclosed per the persona's behavioral rules.

## Memory touch

- `patterns.md` already captures the relevant hard gate ("Case-sensitive-only filesystem
  assumptions (distinct `Foo` vs `foo` in same dir)" — root AGENTS Cross-Platform File
  I/O & Paths; also `Wrong-cased paths or imports that only pass on case-insensitive
  volumes` recurring finding). This review is a clean example of *applying* that pattern,
  not a new generalization. **No patterns.md edit needed** per the
  "do not modify patterns.md unless a genuinely new generalized pattern emerges"
  instruction in the task brief.

