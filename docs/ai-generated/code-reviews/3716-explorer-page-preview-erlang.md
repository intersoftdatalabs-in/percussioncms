# Erlang review: #3716 Explorer page Preview uses product preview URL

**Branch:** `fix/issue-3716-explorer-page-preview`  
**Base:** `origin/main` (`fb0e95fec3`)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Cross-platform path checklist:** N/A for OS filesystem I/O. CMS/URL paths use `/` (correct). `withCmsContextPrefix` prefixes `/Rhythmyx` from `window.location.pathname`; tests run without `window` and keep logical `/Sites/…` URLs.

## Summary

Residual of parent QA #2745 / #2733. Explorer **Preview** for a listed page opened the React editor host (`spa.jsp?entry=editor&mode=view` rewritten to `/cm/app/editor?…&mode=view`) as a blank field form. QA does not treat that as a product page preview.

Product: `resolvePreviewTarget` / `openPreviewItem` now prefer Finder site-path (`/Sites/…?percmobilepreview=`) then Page Management render (`GET …/pagemanagement/render/page/{id}`). Editor host is not used for Preview. `openPreviewItem` applies `withCmsContextPrefix` so site-path opens under `/Rhythmyx` when the SPA is there.

Tests: Vitest `previewItem.test.ts` asserts site-path / render and rejects `entry=editor` / `/cm/app/editor`. Playwright helper `isProductPagePreviewUrl` no longer accepts editor-view; spec asserts popup is not `[data-testid=editor-host]` and is not blank.

Product docs: `product-docs/8.2/admin/content-explorer.md` Preview row + admin index (Preview is not the editor).

Change-class companions: WebUI Vitest + perc-qa-automation helper/unit/spec + product-docs. No Java public API change.

## Issues

None that block commit.

## Memory patterns hit

- Playwright companion required for WebUI screen work (change-class completeness).
- H2 QA must fail, not skip, when demo-sites have a previewable row (#3627 peer).
- Editor host is Open/Edit, not assembled Preview (#3716 vs #3627 workaround).

## Evidence

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS; Vitest Tests 3007 passed; Java Tests run: 63, Failures: 0.
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` — BUILD SUCCESS.
- `npm run test:unit` (perc-qa-automation frontend) — 438 passed (includes explorer-preview-view helpers).
- C5 Playwright: shell chrome passed. Preview popup now opens Finder site-path (`/Rhythmyx/Sites/CorporateInvestments/Corporate Investments Home?percmobilepreview=false`) — not `/cm/app/editor`. HTTP 500 is FastForward assembly NPE for item 551 (`The validated object is null`) plus `error.jsp` `StringEscapeUtils.escapeHtml` compile failure. Residual of this slice, not a WebUI URL-target bug.
