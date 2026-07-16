# Pre-commit review (Erlang)

Applies to **implementer** sessions in this repository (Kilo Code and other agents
that load `.kilocode/rules/`).

## Rule

Before you `git commit`, `git push`, or create/update a GitHub PR for work you
authored in this session:

1. Run a **strict Erlang** review of the changes (uncommitted + branch vs
   `development`, unless the human scoped otherwise).
2. Follow the persona and gate in:

   `modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`

3. Prefer the skill `erlang-review` or Kilo workflow `/erlang-review`.
4. If the recommendation is `request-changes` or Gate says **May commit/push: no**:
   - Fix all **bug** findings (including missing behavioral tests)
   - Re-run Erlang on the fix pack
   - Only then commit / push / open PR
5. Do not treat CI or GitHub bot review as a substitute for this pre-commit pass.

## Exceptions

Skip only when the human explicitly says to skip Erlang for this change
(e.g. docs-only typo they accept, or emergency hotfix they own). Note the skip
in the commit message or PR body.

## Why

Unreviewed commits create long GitHub review cycles (human + bots). Catching
defects and weak tests locally is mandatory team practice for Percussion CMS.
