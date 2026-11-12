# Erlang review — PR #6134

- Date: 2026-11-11
- PR: #6134 `fix(deployer): type shared-group field-group copies, drop unchecked suppression (#6058)`
- Branch: `fix/issue-6058-pscollection-generics`, base: `origin/main`
- Tool: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --diff tmp/pr-6134.diff --models config/models.ollama-dev-coder.toml` (Ollama dev-coder, localhost:11434)
- Persona: erlang 0.1.1

## Summary

Machine analysis found **1** finding(s), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: PR #6134 head (`pr-6134-review`)
- Files: 1 analyzed (`deployer/.../PSSharedGroupDependencyHandler.java`) + pre-existing branch-committed report `docs/ai-generated/code-reviews/issue-6058-erlang.md` (already in PR body)
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1

## Recommendation

approve (see adjudication — the single machine row is preexisting/out-of-slice with residual filed)

## Gate

- Blocking bugs: 0 in-diff
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug (ADJUDICATED: preexisting, not in-diff)

- File: `deployer/src/main/java/com/percussion/deployer/server/dependencies/PSSharedGroupDependencyHandler.java:237`
- Rule: `complexity.cognitive` (arborist-metrics)
- Description: `installDependencyFiles` cognitive=46 (max 15), cyclomatic=27 (max 15)
- Adjudication: the flagged method entry line is **outside the changed lines**. This diff touches two statements inside the method (raw-`PSCollection` construction → `typedFieldGroups(tgtDef)` call, twice), deletes the now-unneeded method-level `@SuppressWarnings("unchecked")`, and moves one loop OUT of the method into the new helper — method complexity strictly decreases. Restructuring a 350-line legacy deploy-critical install path inside a p8 generics slice would trade regression risk for no slice benefit. Recorded as residual #6133 (tech-debt + p8, OPEN). Does not block.

## Independent review (in-diff)

1. `typedFieldGroups` (new `private static`): copies the system-side raw `Iterator` from `PSContentEditorSharedDef.getFieldGroups()` through a checked cast into `PSCollection<PSSharedFieldGroup>`. Semantics equal to the old `new PSCollection<>(rawIterator)` snapshot copy. A wrong element type now fails fast with `ClassCastException` at copy time instead of later at the use-site casts — same invariant the old code relied on, surfaced earlier. No bug.
2. Suppression removal is legitimate: no unchecked operations remain in the method (`javac -Xlint:all` over all 306 deployer sources: 0 `unchecked` / 0 `rawtypes`; 4 diagnostics cleared by this slice).
3. Left-untouched sites (`PSContentRelationDependencyHandler` raw `PSRelationshipSet` receivers, `PSContentTypeDependencyHandler:995` live-list raw `List`) are correctly out of scope — typing them here would need system-side generics or break live-list aliasing. Covered by residual #6132 (tech-debt + p8, OPEN).
4. No API/signature change (`private static` only); no `extends PSCollection` / anonymous-subclass surface. No file I/O or path logic (NIO checklist N/A). No new production files (no header question). No invented APIs.
5. Tests: new helper is branch-free (linear copy + checked cast); covered by the module suite — `cd deployer && ../mvnw clean install` → BUILD SUCCESS, Tests run: 350, Failures: 0, Errors: 0, Skipped: 21 — plus the `-Xlint:all` warning-free proof. No dedicated unit test needed for a 6-line branch-free helper inside a fully green module suite.
6. Change-class closure: complete (no companions — private helper, no shared-context injection, no packaging lockstep).
7. C1: as above, recorded in PR body with exact commands. C2 downstream_checked: none — justified.
8. Product docs: N/A — justified (internal generics cleanup, no operator-facing change).

## Residuals

- #6132 (system-side generics) and #6133 (this method's complexity refactor) already filed, OPEN, correctly labeled. Nothing new to file (cap respected, no duplicates).

## Verdict

**LGTM** — 0 in-diff bugs, evidence complete. Merge once required checks are green (Erlang comment + squash-merge; same-login cannot APPROVE, per workflow exception).
