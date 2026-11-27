# Erlang review — PR #7384 (leftover pass, cycle stonemonkey-20261127)

> Independent second review of the PR head checkout (`fix/issue-7380-retire-superfish-uploadify`
> @ `81c25b6`). First reviewer left a COMMENTED LGTM at 2026-11-27T19:32Z (approve-only, checks
> pending at the time); this pass re-ran every gate from scratch and does not rubber-stamp it.

## Static gate (sanitized `mkd-code-review --machine-only`)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 4 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

### Codex review

No actionable regressions were identified in the obsolete asset removals, stylesheet reference updates, or audit assertions. Static analysis also reported no findings; runtime tests and a Maven build were not run in this review.

## Independent verification notes (this pass)

- **Diff read in full** (26 files: 21 deletions + 4 retargets + audit-test guards + prior review doc).
  No production logic; deletion-only plus comment/href retargets.
- **Acceptance criteria of #7380, all met:**
  - Both superfish.css orphans deleted from all trees (src twins + `WebUI/war` mirror twin).
  - Both uploadify dirs + both `feature.xml` entries deleted (src twins + `WebUI/war` mirror).
  - Slice-5A pins converted to stays-deleted guards (absence + no-resurrection scans, `path.join`
    throughout — cross-platform checklist clean; web-URL `/` only where URLs are meant).
- **Retarget resolves to a live path:** `../jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish.css`
  (from `cm/widgets/`) is populated at build time from npm `superfish/dist/css/superfish.css` by
  `scripts/build-legacy-bundles.cjs` (stdout map lines 136–145, standalone copies 624–630) and
  overlaid onto the WAR by maven-war-plugin — the same live path the `common_css.jsp` twins already
  serve. The `.js` edit is a prose dependency comment only.
- **Zero dangling references (verified by repo-wide grep + node existence sweep on the PR head):**
  remaining `superfish.css` mentions are the build map, the audit guards, and the fixed files;
  remaining `uploadify` mentions are the audit guards plus the vestigial registry lines below.
- **Nit (non-blocking, no residual filed):** `perc_features.txt` (both trees + war mirror) still lists
  `features/perc/uploadify/feature.xml`. Nothing in the tree reads that registry — no Java, JS, JSP,
  properties, or build wiring references it (only the installer touch-guard, which tolerates an empty
  file), and the stock Shindig `features.txt` beside it already dangles ~40 entries for nonexistent
  dirs. Cosmetic only; deliberately left untouched to keep the slice minimal.
- **Change-class closure:** prod deletions + audit-test guards + inventory-table `#7380 RETIRED` marks
  are the correct companions. Product-docs N/A (invisible dead-payload removal, no operator-facing
  change). No new files → no copyright-header gate. C5: no Playwright (zero reachable-surface change;
  Vitest coverage suffices per `WebUI/AGENTS.md`).
- **Build evidence** (in PR body, not re-run here): `cd WebUI && ../mvnw clean install` BUILD SUCCESS,
  Surefire 84/0, full frontend Vitest 10338/0, focused audit spec 16/16. PR-head node sweep above
  (all deleted paths absent, both retarget hrefs correct) passes; fresh-worktree vitest could not run
  (no `node_modules` in the disposable checkout) — covered by the in-PR evidence instead.
- **Residuals:** 0 machine findings → 0 residual filings (empty by rule).
- **Checks at merge time:** all green (CodeQL, Analyze javascript-typescript, language detection,
  signed-commits ×2). No `blocked` label on PR or issue; 0 review threads; no change requests.

**Verdict: LGTM — approve and squash-merge.**
