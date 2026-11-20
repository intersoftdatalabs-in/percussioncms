# Pre-push local code review — issue #6806 (residual: parity-test stale comments)

Slice: comment-only reword in `WebUI/src/main/frontend/src/test/js/widgetPayloadNpmParity.test.js`
(header slice-14 paragraph + MiniColors CSS-twin comment → 2.3.6 byte-identity reality).
Parent: #6734. Branch: `fix/issue-6806-parity-comments` off `origin/main`.

## Sanitized `mkd-code-review` report (advisory gate, local Ollama pool)

## Summary

Machine analysis found **1** finding(s), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 1 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/frontend/src/test/js/widgetPayloadNpmParity.test.js:1845
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: The comment about the sprite being inlined as a data URI is incorrect. The current implementation references an external URL, which means the PNG file should be included as a payload twin.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

## Worker adjudication (verified false positive — stale LLM read, no code change needed)

- The flagged text ("sprite inlined as a data URI", "no png ships") does not
  exist anywhere in the post-edit file (`grep -c` returns 0) — it is the
  pre-fix wording this slice replaces.
- The in-diff comment at the flagged site already states the recommended
  remediation verbatim: stock 2.3.6 references the sprite as external
  `url(jquery.minicolors.png)` and the png ships as a payload twin next to
  the CSS.
- Behavior is pinned, not just commented: the suite asserts
  `url(jquery.minicolors.png)` presence, absence of `data:image/png;base64,`,
  and png-twin existence beside each CSS copy — green (74/74, see PR body).
- Deterministic machine rows in-diff: 0. Per the night-issue-prs gate
  (in-diff bugs / missing behavioral tests / non-portable paths only), this
  single `llm.*`-tool row does not block; recorded here and disclosed in the
  PR body instead of an inline suppression (no stable rule id to pin, and the
  underlying concern is already implemented + tested in-diff).
- No second LLM re-route attempted (paid fallbacks disallowed; local pool
  result stands as machine-only).
