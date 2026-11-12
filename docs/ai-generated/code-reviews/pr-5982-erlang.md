## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: fix/issue-5964-pubserver-sibling-complexity (PR #5982, issue #5964)
- Files: 3 analyzed (`projects/sitemanage/.../PSPubServerService.java` + new
  `PSPubServerServiceSiblingComplexityTest.java` + author's pre-push report)
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- CLI: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models <models.ollama-dev-coder.toml>` from the PR head checkout, exit 0
- Checks snapshot: CodeQL / Analyze (java-kotlin) SUCCESS at review time

## Recommendation

lgtm (independent Erlang review; **no GitHub approval posted and no merge performed** — own-model-only PR, approve/merge banned for this run)

## Gate

- Blocking bugs: 0
- May commit/push: yes (host decides; this review posts nothing to GitHub)

## Issues

_No issues._

## Independent review notes — branch-by-branch equivalence (what the machine can't see)

This is a pure helper-extraction refactor of three methods; correctness = the
new dispatch deciding exactly as the old inline branches. Verified by reading
both sides:

- `setPublishType` → `resolvePublishType` + per-driver helpers. FTP/XML-default,
  SFTP, FTPS, AMAZONS3, and database outcomes re-derived case by case — all
  identical. Collapsing the two consecutive identical `AMAZONS3` blocks removes
  dead code only (second block was unreachable). Reordering the database check
  ahead of the file-type gate is safe (predicates mutually exclusive on
  normalized `type`). Null-`type` still NPEs at the first dereference, as the
  PR body claims. `null` `secure` is null-guarded exactly as before.
- `validateProperties` → `requireDriver` / `validateNoDuplicateStagingServer` /
  `validateFileTypeProperties` / `validateLocalFilesystemFlags` /
  `validateOptionalPort` / `validateDatabaseTypeProperties` + pure selectors.
  The blank-driver early-throw, the staging-guard inversion, the SFTP
  `secure="true"` upgrade before property-set selection, the FTPS override, and
  the unknown-driver fall-through (validate nothing, return) are all preserved.
  Reassigning the `secure` parameter inside `resolveFtpPropertySet` is
  caller-invisible (Java pass-by-value), same as the original local reassign.
- `toPSPublishServerInfo` → admin-URL / readout / classify helpers. The
  repair-always-append + save-only-on-change semantics, the readout-exclusion
  set (case-insensitive, incl. password), the case-sensitive password-mask
  branch, and the `updatePubServer` catch-only-`PSDataServiceException |
  PSNotFoundException`-and-log (with `PSPubServerServiceException` propagating
  from a method that already declares it) are all preserved.
- `private String[]` → `private static final String[]` for the property-set
  arrays is safe: `validatePropertiesByDriver` only iterates (no in-place
  mutation), and the selector helpers return the shared arrays read-only.
- `equalsIgnoreCase` is `StringUtils.equalsIgnoreCase` (null-safe), so
  `isFilePublishType(null) == false` holds as the new test asserts.
- Behavioral tests present and on point: 15 `@Test`s pin every resolver,
  selector, exclusion predicate, and the SFTP driver-repair path, including
  array-equality on the FTP property set. New test file carries the correct
  Intersoft 2026 header, JUnit 5, same-package placement, no new dependencies.
- Non-blocking nit (not a gate item): the FTP property-set test hardcodes
  `"XML"`, `"HTML"`, `"ownServerFlag"`, `"defaultServerFlag"` literals because
  the constants are private — brittle to a constant rename, but the failure
  mode is an obvious test compile/assert mismatch, not silent wrong behavior.

- Cross-platform checklist: N/A — no file I/O, paths, temp dirs, or
  line-ending assertions in the diff.
- No secrets, no swallowed exceptions (decrypt failure still throws; save
  failure still logs with the message), no rule-file changes.
