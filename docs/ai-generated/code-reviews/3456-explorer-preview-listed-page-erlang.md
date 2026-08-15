# Erlang review: #3456 Explorer Preview for a listed page

**Branch:** `fix/issue-3456-explorer-preview-page`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Reviewer persona:** Erlang (independent of implementer)  
**Date:** 2026-08-15

## Summary

Parent #2745 slice 2: after a page row exists, Explorer Preview must enable and open product preview (page render or site-path URL). Folders stay disabled. Playwright must not soft-skip solely because the Sites-root list has no pages when `/Pages` listing is on the tip.

**Memory patterns hit:** missing behavioral tests; WebUI Playwright companion; CMS `/` paths (not OS file I/O); change-class closure (Vitest + Playwright).

## Recommendation

**approve**

## Gate

- Bugs: none found after review
- Behavioral tests: present (Vitest + Playwright helper/spec)
- Cross-platform paths: N/A for filesystem — CMS logical `/` paths only
- **May commit/push:** yes

## Issues

None.

## Notes (non-blocking)

- `isFolder` now treats `percPage` / `Page` / asset content types as items even when `leaf` is false or `hasFolderChildren` is set. That is the right listed-row heuristic; folder types (`Folder` / `FSFolder` / `site`) still win first.
- Playwright skip is now gated on a REST walk of Sites/Pages, not “first 8 Sites-root rows.” If listing (#3457) is on the tip and a page row exists, skip is a defect.
- Product-docs: operator Preview steps unchanged (`product-docs/8.2/admin/content-explorer.md` already documents Preview). N/A this PR.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] CMS path helpers use `/` only (URL/repository form)
- [x] Tests do not assert OS-only absolute path shapes
