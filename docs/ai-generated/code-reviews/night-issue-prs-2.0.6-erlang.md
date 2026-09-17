# Erlang review: night-issue-prs 2.0.6

**Date:** 2026-09-17  
**Reviewer:** Erlang (independent of implementer)  
**Diff:** `.grok/workflows/night-issue-prs.rhai`, `.grok/workflows/README.md`  
**Base:** local `main` (behind `origin/main`; no product Java in this commit)

## Summary

Prompt-only overnight workflow update. `NotSafe?` is label-only. Primary product tracks are Workbench, Content Explorer, unified publishing, and content editing. Human LGTM given in-session (“commit the workflow change”).

## Scope

- Agent-instruction workflow (`.grok/workflows/`). No runtime Java/TS, no path I/O, no tests required for product logic.
- Cross-platform path review: N/A (prompt strings only; no filesystem joins).
- Prior report / memory: overnight empty-queue and inferred-unsafe stalls (session).

## Recommendation

**Approve** (instruction-only).

## Gate

**May commit/push: yes** for this rule-file commit after human approval.

## Issues

| Sev | Location | Note |
|-----|----------|------|
| nit | `night-issue-prs.rhai` PRIMARY TRACK after item 5 | Numbering jumps to `7)` to keep the existing OVERSIZED list item 7. Harmless. |
| suggestion | OVERSIZED step 6 vs PRIMARY TRACK item 5 | Step 6 still says empty slots beat same-parent PRs. PRIMARY TRACK says unfilled `max_issues` while tracks 1–4 have React work is a defect. Triage may still under-fill same-parent siblings; different-parent fill is the intended 2.0.6 behavior. |

No `bug`. No missing behavioral unit tests for product code (none in diff).
