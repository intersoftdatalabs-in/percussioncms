# Erlang review: #4114 REST sitemap-xml virtual.sourceKind

**Branch:** `feat/issue-4114-sitemap-xml-virtual-sourcekind`

Independent pre-commit review of REST GET/PUT persist for `virtual.sourceKind=sitemap-xml` (parent #2678; stacks on SPI #4113 / PR #4120).

## Change class

REST persist companion for the sitemap-xml Virtual Site adapter. Factory / helper allow-list already includes `SITEMAP_XML` (SPI sibling). This slice documents and tests GET/PUT `/sites/{nameOrId}/virtual` round-trip of `sourceKind=sitemap-xml` with a portable-safe local `rootPath`. Leftover `virtual.remoteUrl`, credential-like extra properties, and cloud URL `rootPath` remain 400. Unknown kinds remain 400. Other kinds unchanged. REST Build/Preview/Publish and Developer Sites chrome are **not** in this PR.

## Gates

- [x] Bugs: persist uses existing `PSVirtualSiteHelper.validate` (local-only root, remoteUrl reject, cloud root, credentials). No new path concatenation.
- [x] Portable Path/Files: adaptor tests reuse the icalendar peer (`C:/sitemap-docs` as a NIO path string; existence not required). No Unix-only absolute assertions.
- [x] Behavioral tests: rest resource GET/PUT round-trip + 400 propagation; Jackson serial; sitemanage adaptor PUT/GET round-trip + parent root / remoteUrl / cloud / credentials 400; unknown kind already covered.
- [x] OpenAPI PUT persist mentions `sitemap-xml` local fixture / no live crawl; Build/Preview/Publish descriptions unchanged (later slice).
- [x] Product-docs 8.2 REST / admin / developer / reference note GET/PUT persist; Build/chrome later.
- [x] No secrets on the envelope; 400 messages must not echo credential values (asserted).
- [x] Cross-platform path checklist: no new `"/" +` filesystem joins; helper already uses `Path.of` + `normalize`.

## Recommendation

approve — May commit/push: yes

## Notes

- Stacks on `feat/issue-4113-sitemap-xml-virtual-site-source` so `VirtualSiteSourceType.SITEMAP_XML` exists; this PR does not invent a second factory allow-list.
- Exhaustive `switch` on `VirtualSiteSourceType` remains only in `PSVirtualSiteSourceFactory` (SPI). REST persist passes the wire name through helper validation.
