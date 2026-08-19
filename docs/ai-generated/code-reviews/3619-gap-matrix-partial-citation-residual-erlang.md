# Erlang review: #3619 gap-matrix Partial citation residual

**Branch:** `fix/issue-3619-gap-matrix-partial-citations`  
**Date:** 2026-08-19  
**Change class:** engineering specs / research notes (docs-only leftover of #3577)  
**Recommendation:** approve  
**Gate:** May commit/push: yes

## Summary

Docs-only residual after #3577 / PR #3594: gap-matrix Partial tables still read as live Failed/open for reconcile-closed QA (`#2588` `#2741` `#2745` `#2783` `#2607` `#2729` `#2989` `#3101`) and cited proof tickets `#3575`/`#3576`/`#3592` as open after they closed. Snapshot banner + live Slice updates. New research note. No product UI/REST. **No row flipped to Present.** Views / Inbox / saved-search stay **Partial** (#2645 still open To Be Tested).

## Scope

- `specs/2400-dce-explorer-parity/contracts/gap-matrix.md`
- `specs/2400-dce-explorer-parity/research/partial-citation-residual-after-3577.md` (new)
- `specs/2400-dce-explorer-parity/research/closed-qa-partial-citation-refresh.md`
- `specs/2400-dce-explorer-parity/research/false-present-qa-reconcile.md`
- `specs/2400-dce-explorer-parity/plan.md` (companion citation only)
- Cross-platform path review: N/A (Markdown only; no file I/O)

## Issues

None. Live GitHub 2026-08-19: closed Failed QA not cited as open; remaining live `#2645` (To Be Tested), `#3617`/`#3618` (open implement PRs). Historical #3109 table kept as dated snapshot with live-state banner.

## Tests / Maven / Playwright

N/A — no Java/UI/module change. Product-docs N/A (engineering gap-matrix, not operator help). C5 N/A.
