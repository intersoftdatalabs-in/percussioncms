# Erlang review — feature/erlang-review-memory-and-harvest

**Date**: 2026-07-16  
**Scope**: Durable Erlang review store, pattern memory, automated Kilo harvest  
**Base**: `origin/development`

## Summary

Adds institutional review memory for Erlang (patterns + durable reports under
`docs/ai-generated/code-reviews/`), Windows-safe review host guidance, and a
portable Python harvester that turns GitHub/Kilo PR review comments into pattern
candidates with optional multi-PR auto-merge. Offline unit tests cover
generalize/cluster/merge/`--fixture` end-to-end. No production CMS runtime code
changed.

## Scope

- Base: `origin/development`
- Head: `feature/erlang-review-memory-and-harvest` (this PR)
- Files: agent/skill/prompt/workflow/docs/scripts + sample reviews + harvest report
- Prior report: none
- Memory patterns hit: paths (repo-relative `/`), tests for non-trivial harvest logic

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Cross-platform path review

Applied. Harvest script uses `pathlib.Path`, writes with `newline="\n"`, and
exposes `.sh` + `.bat` wrappers. Repo paths in docs use `/`. No product
filesystem path construction. Tests use `tempfile` (portable).

## Issues

_(none)_

## Test evidence

```text
python3 scripts/test_erlang_harvest_review_patterns.py
# 10 tests OK
```

Live harvest against `intersoftdatalabs-in/percussioncms` (770 comments) used
to seed candidate report and refine promotion rules before landing curated
`patterns.md`.
