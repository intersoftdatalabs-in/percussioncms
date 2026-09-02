# Erlang review: #4126 REST sitemap-xml virtual/publish

**Branch:** `feat/issue-4126-sitemap-xml-virtual-publish`

Independent pre-commit review of REST `POST /sites/{nameOrId}/virtual/publish` for `virtual.sourceKind=sitemap-xml` (parent #2678). Consumes persist #4114 (merged) and generic REST Build via the existing factory (sibling #4124 / cluster #4139 — not re-implemented).

## Change class

REST Publish companion for the sitemap-xml Virtual Site adapter. `SitesAdaptor.publishVirtualSite` already builds then NIO-copies to `IPSSite.root` for any allow-listed virtual kind. This slice documents and tests that path for `sitemap-xml`: local `sitemap.xml` / `sitemap.file` fixture; leftover `virtual.remoteUrl`, credential properties, and cloud URL `rootPath` remain 400. No live crawl. No Developer Sites Publish chrome.

## Gates

- [x] Bugs: publish uses existing `buildVirtualSite` + `PSVirtualSiteFilesystemPublisher`. `validate(site)` now runs **before** `selectFilesystemTarget` so leftover cloud URL `rootPath` is HTTP 400 (not Windows `InvalidPathException` from `Path.of("https://…")`). RemoteUrl / credentials already fail closed. No new path concatenation.
- [x] Portable Path/Files: adaptor tests use NIO `Path` / `Files` and `Path.of("a", "..", "..", "etc")` for unsafe Site root. No Unix-only absolute assertions.
- [x] Behavioral tests: resource delegate + 400 propagation (remoteUrl / cloud / credentials) + OpenAPI; adaptor injected-runner copy, real factory build+copy, unsafe Site root, remoteUrl / credentials / cloud rootPath 400. Other kinds unchanged.
- [x] OpenAPI publish description mentions `sitemap-xml` local fixture / no live crawl.
- [x] Product-docs 8.2 REST / Virtual Sites / admin Sites / publishing / site-config: REST Publish copies HTML to `IPSSite.root`; Developer Sites chrome later.
- [x] No secrets on the envelope; 400 messages must not echo credential values (asserted).
- [x] Cross-platform path checklist: no new `"/" +` filesystem joins; publish copy already uses NIO Path.
- [x] C2: no public signature / `final` / `sealed` change; reverse-dep is sitemanage (standalone clean install).

## Recommendation

approve — May commit/push: yes

## Notes

- Real-build publish fixture uses `pages/index.md` so assemble writes `8.2/index.html` (same home path as icalendar/rss-atom peers).
- Does not add REST Build/Preview tests (siblings) or Developer Sites chrome (#4115 source; Publish chrome later).
