# Erlang independent review — PR #5727 (issue #5718, parent #4531)

- Persona: erlang 0.1.1 (`~/.agents/skills/erlang/ERLANG.md` + `PATTERNS.md`)
- Reviewed commit: `d323bb9535` (`fix/issue-5718-sftp-delivery-server-port`)
- Method: `mkd-code-review analyze --pack percussion --format markdown
  --gate advisory --git-base origin/main` on the PR head worktree (Ollama
  `dev-coder-64k:latest` reachable; machine findings gate), plus manual diff
  read of all 11 files.
- Work-agent pre-push report: `docs/ai-generated/code-reviews/issue-5718-erlang.md`
  (already on branch; this file is the independent re-review, not a duplicate).

## Verdict: LGTM (comment only — no APPROVE/merge: own-model night PR, left open for human/other-model merge)

## bugs_found: none

## Manual findings

- Dispatch is correct: port-only shape is shared by FTP/SFTP/database, and
  `updateDeliveryPortOnly` selects database → SFTP → FTP on the **stored**
  driver, so an SFTP server can no longer fall into the FTP-only apply
  (which threw). Previously a port-only PUT to an SFTP server errored; now
  it applies — the intended behavior change.
- `PSSftpDeliveryPortUpdate.apply` validates blank → overlong → non-numeric
  before any write; wrong-driver bodies reject before `savePubServer`;
  secrets never enter errors (asserted in tests). `addProperty` replaces in
  place (behaviorally proven: property count stays 7).
- `reject()` (validate + dead-code `IllegalStateException` tail) is verbatim
  peer-consistent with `PSFtpDeliveryPortUpdate.reject` — not a defect.
- Frontend mirrors peer slices (#5467 FTP-port, #5717 SFTP-host): port-only
  PUT body, value shown only after success, blank/cancel/400/403/409 never
  write; passphrase/private key never rendered. Hooks all precede the early
  `return null`. `cancel()` restores draft from loaded state — correct.
- Change-class closure complete: Java impl + `PSSftpDeliveryPortUpdateTest`
  (6 tests) + panel + helper + 2 vitest files (13 tests) + Playwright
  surface spec + product-docs + `SiteWorkspace` registration. Copyright
  headers correct (2026 Intersoft) on all new files.
- C1/C3/C5 evidence present: sitemanage `clean install` BUILD SUCCESS
  (4040 tests, 0 fail), WebUI vitest + `tsc --noEmit` + `package`, H2 QA
  surface 2 passed + golden smoke 2 passed, zero console errors,
  `server.log` ERROR/FATAL 0.
- No file I/O / path handling in diff — cross-platform checklist N/A.
- Only CLI finding is preexisting complexity (out of scope, not a blocker).

## Full CLI report (HEAD at review time)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=38 (max 15), cyclomatic=39 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
