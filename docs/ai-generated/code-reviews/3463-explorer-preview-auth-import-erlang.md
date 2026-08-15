# Erlang review: #3463 Explorer preview Playwright import + Pages nav

**Branch:** `fix/issue-3463-preview-auth-import`  
**Scope:** uncommitted vs `HEAD` / commits not in `origin/main`  
**Reviewer persona:** Erlang (independent of implementer)  
**Date:** 2026-08-15

## Summary

Cycle Verify residual of PR #3461 (`fb68b7f511`): `fetchFolderChildren` called `adminBasicAuthHeaders()` without importing it. Tip `b19eabc` already restored the import. This branch rebases that tip onto `main` and keeps the import. Live H2 then failed because Playwright matched `Pages` with whole-row `/^Pages$/` (Type + Path cells), so the listed `rffHome` was never opened. Helpers now match the Name cell; folder-icon open is used (peer #3328).

**Memory patterns hit:** missing behavioral tests; WebUI Playwright companion; CMS `/` paths (not OS file I/O); Cycle Verify import drift.

## Recommendation

**approve**

## Gate

- Bugs: none found after review
- Behavioral tests: present (unit import + Name-cell match; Playwright 2 passed on H2)
- Cross-platform paths: spec-source read uses `path.join`; CMS logical `/` paths only
- **May commit/push:** yes

## Issues

None.

## Notes (non-blocking)

- Soft-skip remains gated on a successful REST listing with no page-type child. This H2 cell listed `Corporate Investments Home`; Preview opened; folders stayed `data-previewable=false`.
- Product-docs: N/A (test/import residual; operator Preview steps unchanged).

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Spec-file read uses `path.join(__dirname, "..", "explorer-preview-view.spec.js")`
- [x] CMS path helpers use `/` only (URL/repository form)
- [x] Tests do not assert OS-only absolute path shapes
