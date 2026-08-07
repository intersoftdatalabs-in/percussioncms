# Erlang review: align Erlang base branch to `main`

## Summary

Update agent/skill/prompt/workflow/mirror and related agent-facing docs so
default branch review base is `origin/main` (formerly `development`), matching
root `AGENTS.md`.

## Scope

- Branch: `fix/erlang-base-branch-main` vs `origin/main`
- Uncommitted agent instruction / skill / REVIEW / Kilo rule/workflow docs only
- No product Java/runtime code
- Cross-platform path review: N/A
- Memory: human review of agent rules required — user explicitly requested this fix

## Recommendation

**approve**

## Gate

- May commit/push: **yes**
- Bugs: none
- Missing behavioral tests: N/A (docs/rules only)
- Human AGENTS/rule approval: **yes** (user: "yes please")

## Issues

None.

## Notes

Historical "formerly development" notes retained so older reports remain
interpretable. Filename `.kilo/rules/no-force-push-development.md` kept; body
now targets `main`.
