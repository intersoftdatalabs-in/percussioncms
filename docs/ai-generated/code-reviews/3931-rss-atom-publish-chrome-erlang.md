# Erlang review — #3931 Developer Sites rss-atom Publish chrome

**Branch:** `fix/issue-3931-rss-atom-publish-chrome` (stacked on cluster #3937)  
**Date:** 2026-08-28  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI chrome + Vitest + Playwright + product-docs); Playwright HARD GATE live H2 QA; consume REST publish sibling without re-implementing adaptor; portable fixture paths (POSIX in-container, `path.join` on host).

## Summary

Parent #2678 slice. Developer Sites **Publish Virtual Site** is shown for `sourceKind=rss-atom` after a successful **Build** (peer object-storage #3879). REST `POST /virtual/publish` is consumed from #3917 / cluster #3937 (not re-implemented). Repository / blank / unknown kinds still hide Preview/Build/Publish. Local RSS/Atom fixture only; `virtual.remoteUrl` / credentials stay off the envelope. Product-docs 8.2 admin Sites / Publishing / developer Virtual Sites / REST / reference site-config drop remaining “later phase” / “Publish hidden” wording for this kind.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — Publish allow-list includes `rss-atom`
- `WebUI/src/main/ts/developer/messages.ts` — rss-atom hint + Publish hint include filesystem target
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — comments
- Vitest: `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx` — Build then Publish dest path for rss-atom; repository hidden
- Playwright: `developer-site-virtual-source.spec.js` — intercept #3931 + live Build then Publish + on-disk HTML
- Fixture helper: `normalizeQaPublishDestPath` / `assertPublishedRssAtomFilesOnQaCell` (fail-closed traversal)
- Product-docs 8.2: admin Sites, admin Publishing, developer Virtual Sites / REST, reference site-config
- Stacked consume: REST #3917 / cluster #3937

## Issues

None.

## Cross-platform path review

- [x] In-container dest/HTML paths are POSIX (`/` join after rejecting `..` and drive letters)
- [x] Host fixture paths use `path.join(__dirname, …)`
- [x] `docker exec` / `docker cp` use arg arrays and `container:posixDest`
- [x] Unit tests reject blank, relative, `C:/…`, and `..` dests
- [x] No Unix-only `/tmp` hardcodes in production UI

## Tests

- Vitest: `shouldShowVirtualPublishChrome("rss-atom")` true; repository / blank / unknown hidden
- Vitest: rss-atom Publish remains after Build success; dest/filesCopied shown
- Playwright intercept: rss-atom POST `/virtual/publish` HTTP 200 filesCopied
- Playwright live: save rss-atom, Build, Publish, dest files exist with fixture marker, restore repository
- Helper unit: POSIX dest normalize + posixJoin
- Envelope: save PUT has empty `remoteUrl` and no credential keys

## Change-class closure

| Companion | Status |
|-----------|--------|
| Publish chrome for rss-atom | yes (stacked REST #3917 + this live proof) |
| Vitest allow-list + panel | yes |
| Playwright intercept + live H2 | yes (C5) |
| Product-docs 8.2 admin Sites/Publishing | yes |
| Fixture helper dest assert | yes |
| REST `POST /virtual/publish` | out of scope (cluster #3937) |
