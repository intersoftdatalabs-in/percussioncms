# Erlang review — PR #6303 (issue #6241): US3 dynamic interaction matrix per DTS service

Independent reviewer: **Erlang Shen** (third eye; did not author the change).
Persona: `erlang 0.1.1` (`~/.agents/skills/erlang/`).
Engine: `mkd-code-review 0.4.0` — `--pack percussion --format markdown --gate advisory --git-base origin/main`
with `--models config/models.ollama-dev-coder.toml` (Ollama `dev-coder:latest`, `localhost:11434`).
LLM residual stage: `outcome=completed findings=0`.
Mode: in-diff bugs gate the merge; preexisting rows never block (recorded as residuals instead).

## Verdict

**BLOCK — request-changes. Do NOT merge.**

Machine gate is clean (0 in-diff findings, 0 blocking bugs), and the manual
read-through below finds the test design sound — but the **required CodeQL
check fails on this head** (`19d2a17`): one high CodeQL finding on in-diff
code. CI red is a hard stop; no APPROVE until it is green.

### Blocking finding (in-diff, from the CodeQL gate)

- **File**: `modules/perc-qa-automation/frontend/tests/unit/dts-interaction-matrix.test.js:95`
  (new file, in-diff)
- **Check**: `CodeQL` → `failure` on head `19d2a17`; annotation:
  `[failure] '[https://dts.example](1)' can be anywhere in the URL, and
  arbitrary hosts may come before or after it`
  (query: incomplete-URL-substring check, reported **high**).
- **Sink**: `skipServiceDown(DTS_SERVICES[0], "https://dts.example").includes("https://dts.example")`
  — a full-origin `.includes(...)` assertion trips the query even though this
  is a test asserting skip-message content, not a security sanitizer.
- **Concrete fix (fix-first, keeps the test behavioral)**: assert on the
  csrf probe path instead of the full example origin —
  ```js
  skipServiceDown(DTS_SERVICES[0], "https://dts.example").includes(DTS_SERVICES[0].csrf)
  ```
  still proves "service-down skip names the probed endpoint" with no
  full-URL substring literal for the query to flag. (Alternative:
  suppression `// codeql[rule]` on the exact sink line + a
  `suppressions.md` row — only if the fixer verifies the exact rule id;
  fix-first is preferred here since the assertion is equally strong
  without the literal.)

Manual read-through (independent of the machine report):

- Helper `dts-interaction-matrix.js`: closed frozen inventory (6 services),
  `HEAD <root>/csrf` liveness, two-level skip contract mirroring US2,
  `dtsBaseUrl()` uses `new URL(...)` (proper parse, not substring logic),
  URL paths use `/` correctly (web origins, documented), file paths use
  `node:path`. New-file copyright header correct (2026 Intersoft).
- Spec: data-driven per service, fixture-gated skips name the exact gap,
  real round-trip asserts (vote persists `before+1`, feed XML has
  items/entries, form submit→re-read, comment visible-or-moderated,
  metadata result-set shape, login→session→logout). `encodeURIComponent`
  on user-controlled segments; secret only via `DTS_FIXTURE_PASSWORD` env.
  `afterAll` maps never-ran/failed cases to "skip" in the matrix artifact
  but notes the run stays red via the failing test — acceptable, documented.
- Unit test (201 lines): inventory closure incl. US2 cross-check, env
  resolution with save/restore, row accept/reject matrix, schema + example
  validation, spec cross-reference. Behavioral, not string-theatre —
  except the one flagged assertion above, which needs the rewording, not
  because it is weak but because CI flags the pattern.
- Change-class closure: spec + helper + unit + schema/example + README +
  package.json entry in the same PR; schema change is additive (`anyOf` +
  new enum values, US1/US2 shape untouched). QA wiring check green,
  `test:unit` 656 passed per the PR body. product-docs N/A with reason
  (QA automation surface, US1/US2 precedent) — accepted.

No preexisting machine rows in PR-touched files → zero residuals.

## Full machine report

```text
## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
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

> Machine says approve; the human-visible gate overrides: **BLOCK on the red
> required CodeQL check** (in-diff high finding above). Re-review after the
> fix lands and CodeQL is green.
