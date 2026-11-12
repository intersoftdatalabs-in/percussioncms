# Erlang review — PR #6171 (issue #6115, parent #6070)

Date: 2026-11-12 UTC
Reviewer: Erlang (independent; own-model ban applies — model:muse-spark-1.3-contributor + operator:opencode)
PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/6171 —
  `fix(sitemanage): decompose PipelinesAdaptor.putResultPage (#6115)`
State at review: OPEN, unmerged. Head `b7e096873f`, base `main`.
Checks at review: Detect language changes SUCCESS · Analyze (java-kotlin) SUCCESS ·
  Analyze (javascript-typescript) SKIPPED · CodeQL SUCCESS.

## Machine pass

Command (run from PR head worktree):
`mkd-code-review analyze --pack percussion --format markdown --gate advisory
--git-base origin/main --models
/home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`

- Persona: erlang 0.1.1 · Files analyzed: 2 · Findings: **0** · Blocking bugs: **0**
- Recommendation: approve (advisory only; merge barred by own-model ban, see verdict).

## Manual review (in-diff only)

Diff: `PipelinesAdaptor.java` (+204/−63 across the one file) + new
`docs/ai-generated/code-reviews/issue-6115-erlang.md` (report, no product code).
No test files touched. Compared new code line-by-line against
`origin/main:…/PipelinesAdaptor.java#putResultPage`:

1. **Guard order and messages preserved** — appName → resourceName → body-null,
   same 400 messages; trim values identical (`appName.trim()` still feeds
   `resolveApplicationName`; `resourceName.trim()` still feeds find-or-create).
   Moving the trim into the guards is pure (no side effects), so computing
   `safeResource` before token/app resolution is not an observable reorder.
2. **Validation-before-token precedence preserved** — `validateResultPageBody`
   runs before `currentResultPageToken()`, exactly as in the original, so a
   400 validation failure still precedes the `IllegalStateException` path.
   `validateResultPageBody` body is verbatim (Slice D comment moved to the
   method javadoc — comment-only).
3. **IR load/import/setSource/app-name fixup** — verbatim inside
   `loadOrImportResultPageDocument`; runtime exceptions from
   `applicationLoader`/`importClassicApplication` still propagate past the
   `PSPipelineIrException`-only catch, as before.
4. **find-or-create + kind repair** — verbatim; unknown/null kind still
   repaired to `KIND_QUERY`.
5. **Unbound branch** (`none && isBlank(stylesheetUri)` → clear + save + return
   `PRESENTATION_NONE` shell) and **stored/response field mapping** (stylesheet,
   extension, MIME, presentation, in order) — identical. Catch block identical.
6. **`ValidatedResultPage` holder** — `private static final`, all fields
   `final`, no mutators; `toStoredIr`/`toResponse` allocate fresh objects per
   call. Effectively immutable, thread-confined — sane.
7. **Scope hygiene** — private helpers only, no `rest` interface change, no
   file I/O (cross-platform N/A), no new files needing Apache headers, no
   operator-facing surface (product-docs N/A — pure refactor).
8. **Tests** — 14 `putResultPage` references in `PipelinesAdaptorTest`; no test
   changes required for a behavior-preserving decompose. Build evidence in PR
   body: `cd projects/sitemanage && ../../mvnw clean install` → BUILD SUCCESS,
   Tests run: 4250 / Failures: 0 / Errors: 0 / Skipped: 125;
   `PipelinesAdaptorTest` 87/87 green.

## Findings

No in-diff bugs. No gate blockers.

## Residuals

Machine pass reported 0 preexisting/out-of-diff rows for PR-touched files;
no residual issues filed (zero qualifying rows).

## Verdict

`ERLANG_VERDICT: LGTM-REVIEW-ONLY (no merge per own-model ban)`
