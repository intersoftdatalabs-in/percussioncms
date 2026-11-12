# Erlang review — PR #6219 (night-issue-prs, REVIEW ONLY)

> **Status: LGTM-REVIEW-ONLY (advisory, own-model PR — no approve, no merge, no LGTM post).**
> Host directive: this PR carries `operator:opencode` + `model:muse-spark-1.3-contributor`
> (own-model). Reviewed only; merge/approve decision is the host's.

- PR: intersoftdatalabs-in/percussioncms#6219 — "fix(webui): surface guard dependency
  errors for role/workflow delete (#6098)"
- Head branch: `fix/issue-6098-guards-verification-bundle` @ `b9cf864e02`
- Persona: erlang 0.1.1 (`/home/nate/.local/share/mkd/agents/erlang`)
- Machine engine: `mkd-code-review 0.1.21`, pack `percussion`, gate `advisory`,
  `--git-base origin/main`, models `config/models.ollama-dev-coder.toml`
  (Ollama `dev-coder-64k:latest` reachable). Ran on a detached temp worktree at the
  PR head so the shared `night-issue-prs` checkout was untouched.
- Checks snapshot (one, at review time): `CodeQL` pass, `Analyze (javascript-typescript)`
  pass, `Analyze (java-kotlin)` skipping (correct — WebUI-only diff).

## FULL CLI markdown (verbatim)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 5 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Erlang independent pass (in-diff bugs gate)

Diff files (5): `RolesSection.tsx`, `WorkflowSection.tsx`, their two Vitest suites,
plus the prior `issue-6098-erlang.md` report artifact. Production delta is two
`catch` blocks (error-text surfacing only); no REST/sitemanage/product-docs changes.

1. **ApiError claim verified (not taken on trust).** `formatApiError` exists in
   `WebUI/src/main/ts/api/client.ts:182`; it handles plain-object `ApiError` via
   `isApiError` + `extractRestErrorMessage(err.body)` and falls back to
   `HTTP <status>` / caller fallback. The old `err?.message` (always `undefined`
   on a plain object) and the ignored-`err` generic `DELETE_FAILED` alert were
   genuine UI information-loss bugs; both fixed at the narrowest layer.
2. **Tests are behavioral, not token-greps.** New cases render the section, click
   the real delete control, and assert the server's 409 body message reaches the
   user (`getByText(...)` / `alertSpy` called with the server string), plus
   validate→delete→reload call order and default-workflow disabled-button no-op
   (`del` not called). The `vi.mock` conversion to `importOriginal` spread is
   required and correct — a full module mock would have stubbed out the real
   `formatApiError` under test.
3. **Types.** Both catches use `catch (err: unknown)` with no unsafe member access
   — clean under strict TS.
4. **Cross-platform / paths / secrets.** No file I/O, no path construction, no
   secrets, no shell. Nothing to gate.
5. **Change-class closure.** Guard logic itself (validate-first ordering,
   `CANNOT_DELETE_DEFAULT` early return, backend 400/404/409 bodies) pre-existed;
   this slice only surfaces existing server text. No new REST surface → no
   adaptor/stub companions owed. No screen/route change (same dialogs, new message
   string) → Playwright companion not required; Vitest at the changed units is the
   right layer. `product-docs` N/A with a stated reason (informative error text
   only, no operator procedure change) — accepted.
6. **Rule files.** None in diff (the `docs/ai-generated/...` addition is a review
   artifact, not an instruction file). Copyright n/a (no new source files).

## Verdict

- **In-diff blocking bugs (`bugs_found`): none.** Gate: PASS (advisory only —
  host owns the merge decision for this own-model PR).
- Residuals: machine reported 0 preexisting rows in touched files → nothing to file.
- No fix spawned (review-only constraint); nothing to hand to erlang-fix.
