# Erlang review — PR #6114 (issue #6046)

- Status: **LGTM — approved, squash-merged**
- Persona: erlang 0.1.1 (`~/.agents/skills/erlang`)
- Scope: PR head vs `origin/main`, 16 files (rest + sitemanage + WebUI +
  qa-automation + product-docs)
- Verdict: `lgtm_merged`, merged: true, bugs_found: [] (in-diff)

## Gate

- In-diff bugs: **0**. Blocking bugs: 0. Recommendation: approve.

## Machine findings (independent re-run)

- `mkd-code-review analyze --pack percussion --format markdown --gate advisory
  --diff pr6114.diff` (Ollama dev-coder pool): **2 findings, 0 blocking**:
  - `paths.hardcoded_sep` on `PipelinesResource.java` (preexisting HTTP
    `Content-Disposition` header builder — false positive, untouched line;
    no suppression added, no residual: not a defect).
  - `complexity.cognitive` suggestion (in-diff, advisory only) on
    `PipelineDetailPanel.tsx` `onSaveHttpTank` (15/16) — preexisting scale,
    not a blocker.

## Manual verification

- REST `PUT …/enabled`: null body → 400 before adaptor (fail-closed, never a
  silent disable); null detail → 404; WAE/IAE passthrough preserved.
- Adaptor `setApplicationEnabled`: `requireAdmin` first; hidden → 400; unknown
  → null; lock conflict → 409 (acquire-only, no steal); idempotent no-op when
  flag matches; lock released in `finally` (release failure warn-only);
  `detailAfterLifecycle` reloads fresh from object store (no stale read).
- `ApplicationPersistOps.saveEnabledFlag` is a `default` method (interface
  source-compatible); sole production implementor updated; Spring test stub
  `TestPipelinesAdaptor` updated (change-class closure complete).
- New `PipelineEnabledState.java` carries 2026 Intersoft header; no `/`-joined
  filesystem paths in new code; no invented APIs.
- Behavioral tests at every layer (5 new resource tests incl. fail-closed
  null-body; 8 new adaptor tests incl. 409/hidden/idempotent).
- C1 evidence present and complete (rest suite green, sitemanage 4237/0,
  WebUI 894-file vitest green, tsc/eslint clean, H2 Playwright round-trip
  with state restore, 0 console errors, 0 new server.log ERROR — including
  the H2-caught JAXB wrapper fix with regression test).
- C2 downstream_checked: sitemanage + rest Spring test context. product-docs
  checkbox: done.
- Checks: all green. No unresolved review threads. Mergeable at review time.

## Residuals

- None (preexisting FP adjudicated, not filed).
