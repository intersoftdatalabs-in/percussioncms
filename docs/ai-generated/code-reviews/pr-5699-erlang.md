# Erlang review: PR #5699 (this-run, independent)

### Erlang verdict (independent review, 2026-11-07)
- Persona: erlang 0.1.1 | Pack: percussion | Gate: advisory(tool) + strict(manual) | Models: dev-coder via Ollama
- Machine: 1 finding, 0 bugs (in-diff: 0; preexisting: 1) -> Recommendation: approve
- Manual: community NUL gate mirrors the keyword/link NUL slices (same `collect*FieldErrors` peer shape, same save-flow position, same message/i18n pattern). `communityContainsNul` null-guards via `??`; empty stays in the required-field gate (verified: `"".includes(NUL)` is false and required-empty path is untouched). Only `EditorHost` mounts the widget (single save path — no bypass). Closure complete: unit tests for both helpers + EditorHost save-gate tests (5 new) + Playwright surface spec + product-docs community-picker row. No file I/O (cross-platform N/A), no secrets.
- The single machine finding (`handleSave` cognitive=160, preexisting) is out of diff scope — adding one guard clause to an already-gated save flow does not meaningfully change that metric, and chain-wide extraction belongs to a dedicated slice.
- **LGTM. No blocking findings. (Review-only: own-model PR, no APPROVE/merge per hard ban.)**

---

## Full machine report (`mkd-code-review analyze --pack percussion --gate advisory`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
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

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1466 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=160 (max 15), cyclomatic=203 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Erlang: PREEXISTING, out of scope (see verdict).
