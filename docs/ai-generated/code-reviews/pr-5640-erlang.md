# Erlang independent review — PR #5640 (fix(tinymce): un-vendor CodeMirror to npm bundle, #5626)

Status: independent Erlang review, machine gate clean, 0 blocking bugs.
Recommendation: **approve** (2 non-blocking suggestions + 1 nit + 1 residual C5 note).

- Persona: erlang 0.1.1 (`~/.agents/skills/erlang/`, engine `mkd-code-review 0.1.18`)
- Scope base: `origin/main` (`c882f5ac`)
- Scope head: `review-5640` (`c778e931bb`)
- Models: `/home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`
  (Ollama `dev-coder` on `localhost:11434`); gate on **in-diff** bugs only.

## Machine report (verbatim, `mkd-code-review analyze --pack percussion`)

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Independent Erlang findings (all non-blocking)

### Suggestion 1 (high) — stale dual-ship mirror still points at removed per-file URLs

- File: `WebUI/war/app/includes/template/editTemplate_js.jsp` (NOT in diff)
- On `origin/main` this file is byte-identical to the in-diff
  `WebUI/src/main/webapp/cm/app/includes/template/editTemplate_js.jsp`
  (verified via `git show` + `diff` at merge-base `c882f5ac`); the PR
  updates the `src` copy to the `codemirror.bundle.js`/`.css` pair but
  leaves the `war` mirror loading the 12 `codemirror/{lib,mode,addon}/*.js`
  scripts + 2 stylesheets the PR deletes from the shipped JAR (the PR's
  own new spec asserts those legacy paths 404). The PR body's Notes claim
  ("`WebUI/war/**` stale mirrors ... already diverged before this
  change") does not hold for this file — it holds for siblings such as
  `editTemplate.jsp`, but `editTemplate_js.jsp` was identical, so this PR
  itself breaks the identity. The body's "no remaining per-file WebJar
  references" claim is likewise inaccurate for tracked source.
- Why non-blocking: the WAR is built from `src/main/webapp` (+
  `target/generated-webui`), never from `WebUI/war/`; the installer only
  cherry-picks `WebUI/war/jslib/*` (see `installDistributionFiles.xml`);
  the migration plan (`docs/ai-generated/tasks/#000-webui-src-layout/`)
  maps `war/app/**` → `src/main/webapp/cm/pages/app/**` (which this PR
  does update) and calls for retiring `war/**`. No proven runtime effect
  today — but the identity the two copies held on `main` is now broken,
  against the repo dual-ship consistency rule.
- Fix (trivial, follow-up): apply the same two-line bundle script/link
  swap to the `war` mirror, or record it as a tracked residual if the
  tree is intentionally frozen pending retirement.

### Suggestion 2 — dead fullscreen-append branch in `source.html`

- File:
  `modules/perc-tinymce/src/main/resources/META-INF/resources/sys_resources/tinymce/plugins/codemirror/source.html`
  (in diff)
- The `if (userSettings.fullscreen && CMsettings.jsFiles.indexOf(
  "codemirror.bundle.js") === -1)` branch can never fire: `jsFiles` is
  initialized with the bundle literal two statements above and nothing
  mutates it before the check (user-list merging and `disableFilesMerge`
  replacement both run later). It also appends standalone
  `addon/display/fullscreen.js`/`.css`, which no longer ship. No behavior
  impact (dead code), but the next reader will misjudge the fullscreen
  story. Fix: delete the branch, or move the check after user-list merge
  and document that custom lists self-supply fullscreen assets.

### Nit — entry-file comment contradicts the bundler config

- File: `modules/perc-tinymce/build-scripts/codemirror-bundle-entry.js`
  (in diff) says the bundle is built with `format: "iife" +
  globalName: "CodeMirror"`, while `bundle-codemirror.mjs` (in diff)
  deliberately does **not** set `globalName` (its comment explains why —
  the entry assigns `window.CodeMirror` itself). One-line doc fix.

## Gate checklist

- Bugs (in-diff): 0 (machine 0; manual 0 — see notes below).
- Behavioral tests for new/changed logic: yes — new
  `editor-codemirror-bundle.spec.js` serves the bundle (200), asserts the
  legacy per-file path 404s, `window.CodeMirror` API, modes/addons, and a
  real-browser `fromTextArea` round-trip with zero console/page errors.
- Change-class closure: production bundle + pom wiring + all live
  consumers repointed (`source.html`, both `editTemplate_js.jsp`
  variants, `sys_Templates.xsl`); package-lock pinned with integrity;
  README updated. Only gap is Suggestion 1 (non-served mirror).
- Portable paths: yes — new `.mjs` uses `node:path` `join`/`dirname`,
  no hardcoded separators; no new OS-specific temp/absolute paths.
- Product docs: N/A (packaging migration, no operator/user-facing
  behavior change; README is the right companion and is updated).
- Headers: yes — both new build scripts carry the Intersoft 2026 Apache
  header; no new Java files.
- Security: no secrets; version pinned `5.65.21`; `npm audit` visible.

## C5 caveat verification (rich-controls failure + 2 server.log ERRORs)

Claim in PR body: pre-existing/environmental, unrelated. **Verified true:**

- `modules/perc-qa-automation/frontend/tests/editor-rich-controls.spec.js`
  is untouched by this PR and stubs `**/sys_resources/tinymce/**` on
  `origin/main` (line 167), so none of this PR's files participate.
- The 4 failing assertions are unmocked editor-host calls for fake
  `contentId=42` (`translations/42`, `getTransitions/42`,
  `allowedWorkflows/42`, `slot-relationships/canvas?ownerId=42`) — the
  spec on `main` has routes for neither; they fall through to live H2
  (404/500/400). Same failure occurs without this change.
- The 2 `server.log` ERRORs (`PSComponentSummary ... id '42' does not
  exist`) are the server side of the same unmocked `getTransitions/42`
  call. No bundle/Jetty/startup errors.
- Recorded as a **residual note** (out-of-scope leftover), not a block.

## Verdict

No material issues. Recommendation: approve. May commit/push: yes
(merge once required checks are green).
