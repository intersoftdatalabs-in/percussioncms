# Erlang review — qa-health fail-fast after hot-deploy (C5)

**Scope:** uncommitted `docs/qa-health-fail-fast-after-hot-deploy` vs `origin/main`  
**Date:** 2026-08-14  
**Reviewer:** Erlang (independent of implementer)

## Summary

Instruction and operator-doc updates so unattended C5 / QA-mode agents call `perc-devctl qa-health` after `qa-up` and after every jar copy, instead of HTTP-polling `/Rhythmyx/login` while Jetty has already failed ROOT startup. No new runtime logic.

**Memory patterns hit:** agent rule/instruction file changes require explicit human review (root `AGENTS.md`); missing behavioral tests for new logic (N/A — no new executable behavior).

## Recommendation

`approve`

## Gate

May commit/push: **yes** (human explicitly requested this instruction PR).

## Issues

None.

### Cross-platform path checklist

N/A — no new filesystem I/O. Container paths in docs correctly use `/` (URL/container convention). Host commands keep existing `python docker/scripts/perc-devctl.py` form with both Windows/Unix notes already in the surrounding docs.

## Notes

- `.grok/workflows/**` and `AGENTS.md` / module `AGENTS.md` are agent-instruction files; operator asked to land them in this PR.
- `perc-devctl.py` change is a docstring only (`qa-health` already fail-fasts).
- `workflow validate_only name=night-issue-prs` canned path: passed.
- Product-docs / Playwright / Maven module clean install: N/A (docs/rules-only).
