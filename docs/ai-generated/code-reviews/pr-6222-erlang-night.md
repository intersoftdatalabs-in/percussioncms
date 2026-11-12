# Erlang review — PR #6222 (night-issue-prs, REVIEW ONLY)

> **Status:** LGTM-REVIEW-ONLY — review only. This is an OWN-MODEL PR
> (`model:muse-spark-1.3-contributor` + `operator:opencode`); per the hard
> constraint no APPROVE was posted, no merge was performed, no fix was
> implemented in this turn.
> **Manual verification included** (CLI was available and ran clean).

- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/6222
- Title: `refactor(deployer): reduce PSDeploymentServerConnection complexity (#6146)`
- Head: `fix/issue-6146-psdeployment-conn-complexity` @ `fc0fa577bb`
- Base: `main`
- Persona: erlang 0.1.1 (`~/.agents/skills/erlang/`), patterns loaded.
- Review checkout: detached temp worktree at the PR head ref
  (`/tmp/opencode/pr-6222-erlang`, since removed); report filed in the
  `night-issue-prs` worktree.

## Full CLI report (mkd-code-review 0.1.21, pack percussion, gate advisory)

Command:

```bash
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base origin/main \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Ollama `dev-coder` on `localhost:11434` reachable; exit 0.

## Summary

Machine analysis found **1** finding(s) (1 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No active issues._

### Suppressed Findings (1)

- `paths.hardcoded_sep` on
  `deployer/src/main/java/com/percussion/deployer/client/PSDeploymentServerConnection.java:1471`
  — Justification: RFC 2046 wire literal (CRLF, quoted disposition)
  (via in_source_mkd). Exempt from the gate; verified the line is a MIME
  boundary/disposition wire literal, not an OS path join — suppression is
  legitimate.

## Erlang independent read (manual, in-diff bugs gate)

I read the full `origin/main...HEAD` diff of all 3 files (575± lines in
`PSDeploymentServerConnection.java`, 86-line new test, 35-line pre-push
report). Change class: behavior-preserving complexity refactor (extract
method/helpers only). Verdict per extraction:

1. **Ctor → `validateConstructorArgs` / `ConnectionTransport.create` /
   `validateDeploymentInterface` / `readLicensedFlag` / `loadServerVersion` /
   `loadRepositoryInfo` / `startLockerThread`.** All validations run before
   any assignment, exactly as before; same exception types/messages.
   `readLicensedFlag` returns `true` for missing/empty attribute — identical
   to the field default (`private boolean m_bLicensed = true`), so the
   "assign only when present" shape change is a no-op. `loadServerVersion`
   uses a local and returns it; the `< 6` guard and throw args are verbatim.
   No bug.
2. **Document `execute` → `sendWithRepost` + `logFailedRequest` +
   `shouldRetrySession`.** Retry-once-on-`IOException` preserved inside
   `sendWithRepost` (same log lines, same `System.out` rePOST marker, same
   interrupt-restore). One deliberate, PR-body-disclosed mapping change: a
   *request-body build* failure (attachment/multipart `IOException`, which
   previously fell into the rePOST path with a null/partial body → NPE →
   `UNEXPECTED_ERROR`) now maps directly to the same `UNEXPECTED_ERROR` code
   with the real cause message. Same error contract, strictly better
   diagnosis. Not a blocker. `shouldRetrySession` is a verbatim predicate
   extraction (`reconnect && m_sessionId.trim().length() > 0`).
3. **Upload `execute` → `postUploadOnce` + `markUploadRepost` overloads.**
   `resp`/`doRepost` declarations and the `doRepost → execute(..., false)`
   tail are untouched; both overloads replicate the exact log/throw/branch
   matrix of the originals, including `Thread.currentThread().interrupt()`
   before the repost branch. `catch (Exception e)` path (plain log + wrap)
   matches the original, which likewise did not use the debug-dump helper.
   No bug.
4. **Download `execute` → `performDownload` returning
   `DownloadResult(status, errorData)` + `closeQuietly`.** Ordering
   preserved: response stream closed via try-with-resources inside
   `performDownload` (was `finally` close of `in`); `out` closed in the
   caller's `finally` via `closeQuietly` *before* the `parseServerResponse` /
   reconnect tail — exactly the legacy order (reconnect re-executes with an
   already-closed `out` in both versions). `status != 200 || errorData != null`
   guard is equivalent to the legacy `status != 200 || respData != null`.
   No bug.
5. **`getRequestPage` → `resolveRequestRoot`.** Lazy `m_reqRoot` caching logic
   verbatim, only reordered; page selection unchanged. No bug.
6. **Pre-existing `m_version` null-deref quirk** in
   `validateDeploymentInterface` (`SERVER_VERSION_INVALID` branch reads
   `m_version.getVersionString()` while `m_version` is still null) preserved
   verbatim and explicitly disclosed as out of scope in the PR body. In a
   PR-touched file but preexisting, disclosed, and not a machine finding —
   no residual filed here (belongs to a future debt slice, not this gate).
7. **New test `PSDeploymentServerConnectionTransportTest` (4 tests).**
   Behavioral assertions (URI derivation, Basic-auth header bytes,
   case-insensitive https, unsupported-protocol → `UNEXPECTED_ERROR` numeric
   code), not string-grep. Copyright header correct
   (`Copyright (c) 2026 Intersoft Data Labs, Inc.` + Apache block). One nit:
   `createHandlesNullPasswordAsEmpty` passes `""` rather than `null`
   through `create` (null-normalization happens in the constructor, as the
   test comment itself notes) — name slightly overclaims, harmless.
8. **Change-class closure:** pure intra-class refactor; `final` class, no
   subclasses, no signature changes → no adaptor/sitemanage companions, no
   Spring context additions, no packaging lockstep. Product-docs N/A is
   justified (no operator-facing change); Playwright N/A justified (no WebUI).
   Pre-push report file (`issue-6146-erlang.md`) is a durable-report
   artifact, not a rule change. Cross-platform: no filesystem path joins
   added (the single `paths.hardcoded_sep` is a MIME wire literal).

CI snapshot (one read): CodeQL pass, Analyze (java-kotlin) pass,
javascript-typescript skipping (no JS touched), Detect language changes pass.
PR-body build evidence (`cd deployer && ../mvnw clean install` → BUILD
SUCCESS, 372 tests, 0 failures) is consistent with the diff; not re-run in
this review-only turn.

## Findings

- bugs_found: none (0 in-diff; 0 blocking).
- Residuals: none qualifying (the single preexisting machine row is a
  documented suppression, exempt; `nit` rows never file). No issues filed.

## Recommendation

**LGTM-REVIEW-ONLY.** No `request-changes`. No APPROVE posted and no merge
performed per the OWN-MODEL hard constraint — the host decides next steps.
