# Erlang review — PR #6324 (issue #6274): retire installer psoimageeditor legacy JS

Independent reviewer: **Erlang Shen** (third eye; did not author the change).
Persona: `erlang 0.1.1` (`~/.agents/skills/erlang/`).
Engine: `mkd-code-review 0.4.0` — `--pack percussion --format markdown --gate advisory --git-base origin/main`
with `--models config/models.ollama-dev-coder.toml` (Ollama `dev-coder:latest`, `localhost:11434`).
LLM residual stage: `outcome=completed findings=0`.
Mode: in-diff bugs gate the merge; preexisting rows never block (recorded as residuals instead).

## Verdict

**LGTM — APPROVE.** 0 in-diff findings, 0 blocking bugs. No preexisting machine
rows in PR-touched files → zero residuals. May merge when CI is green.

Manual read-through (independent of the machine report):

- `image_editor_general.js`: Interface `.Resizable()` → jQuery UI `.resizable()`
  + `.draggable()` is a faithful mapping. Old `onDrag(x, y)` updated only
  background-position + top/left (no size writes on drag); new `drag` callback
  does exactly the same. Old `onResize(size, position)` wrote width/height +
  top/left + ratio; new `resize(event, ui)` writes all four. `ResizableRatio()`
  → `cropBoxRatio()` (h/w, 4dp, null-guarded); `ResizeConstraint(bool)` →
  `setCropBoxConstraint()` (`aspectRatio` option; non-true releases). No
  remaining `jQuery.iUtil` / `jQuery.iResize` / `.Resizable(` /
  `ResizeConstraint` references outside comments (guard test strips block
  comments before asserting, so the mapping note documents history without
  tripping the pin).
- `form.js` is a byte-sync of npm `jquery-form@4.5.0` (historic file name kept
  so deployed editor pages keep working — deliberate, documented).
- `sync-installer-vendored-js.cjs`: portable `path.join` throughout, no
  hardcoded separators; `PAYLOAD_ADDIN_JS_DIR` addition follows the existing
  `COPIES` pattern. Cross-platform checklist: pass.
- Guard test `InstallerPayloadVendoredJsAuditTest`: behavioral, not
  string-theatre — pins the 4.5.0 provenance banner + MIT grant + `ajaxSubmit`
  presence + absence of `jQuery.browser`, asserts the Interface files stay
  deleted, asserts the glue binds `.resizable(` / `.draggable(` /
  `cropBoxRatio` / `setCropBoxConstraint` / `ajaxSubmit`. Copyright header
  untouched on edit (pre-2023 file keeps its lineage header per the repo rule).
- Change-class closure: payload bytes + sync script + README pair +
  package.json/lock + guard test + no product-docs (installer payload, not
  operator-facing — exempt with reason). Module `clean install` green
  (257 run / 0 failures) per the PR body.

## Full machine report

```text
## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._
```
