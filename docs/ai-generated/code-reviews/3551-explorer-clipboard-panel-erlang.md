# Erlang review — #3551 Explorer clipboard panel after Add

**Branch:** `fix/issue-3551-explorer-clipboard-panel`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Reviewer:** Erlang (independent of implementer)  
**Date:** 2026-08-18  
**Memory patterns hit:** missing behavioral tests; incomplete change-class closure (WebUI + Playwright + product-docs); Playwright for WebUI screens; no path/file I/O

## Summary

Content → Add to clipboard now always mounts `explorer-clipboard-panel` (`setShowClipboard(true)`). View → Clipboard is no longer gated on `noClipboardContext`, so an empty panel is a valid mount. `toClipboardItem` maps Sites / `FSFolder` / `site` rows as folders and keeps a name fallback so the multi-select list used by the spec is not dropped. Playwright no longer clicks View → Clipboard after Add (that toggle would hide an already-open panel).

## Recommendation

approve

## Gate

**May commit/push: yes**

- Bugs: none
- Behavioral tests: present (mapper unit, ClipboardPanel empty + Sites rows, ExplorerMenuBar toggle, shell Add + empty toggle)
- Cross-platform paths: N/A (no filesystem path construction)
- Change-class companions: WebUI Vitest, perc-qa-automation Playwright, `product-docs/8.2/admin/content-explorer.md`

## Issues

None.

## Cross-platform path checklist

Not applicable — no new file I/O, path joins, installer, or path assertions.

## Verification (implementer evidence, reviewed)

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS; Surefire Tests run: 61, Failures: 0; Vitest Tests 2783 passed (375 files)
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` — BUILD SUCCESS
- H2 QA Playwright: explorer-multiselect 3 passed; Cycle Verify surface set 15 passed (menu-bar empty toggle + add-to-clipboard); no `Failed startup of context`
