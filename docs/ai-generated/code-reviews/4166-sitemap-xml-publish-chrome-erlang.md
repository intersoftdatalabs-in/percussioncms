# Erlang review: #4166 sitemap-xml Developer Sites Publish chrome

**Change class:** WebUI product screen (Developer Sites Virtual Site Publish chrome) + Playwright companion + product-docs 8.2.

**Memory patterns hit:** change-class companions (WebUI chrome + Vitest + Playwright + product-docs); Playwright HARD GATE; i18n keys (no hard-coded English); consume REST POST `/virtual/publish` (#4126 / cluster #4151) without re-implementing REST; do not steal #4151 ActionMenu files; sibling Build/Preview chrome stay hidden (#4164 / #4165).

## Findings

None that block. Publish chrome is gated by `shouldShowVirtualPublishChrome` including `sitemap-xml`. The action section renders when Publish is shown even if Build/Preview stay hidden, so operators can POST publish without this slice owning Build chrome. REST publish still builds then NIO-copies last-build HTML to `IPSSite.root`; leftover `virtual.remoteUrl` / credentials fail closed on the server.

## Cross-platform

QA fixture host paths use `path.join`; in-container dest is POSIX `/opt/Percussion/...`. `normalizeQaPublishDestPath` rejects Windows drive letters, UNC, relatives, and `..`. Unit tests cover those rejections.

## Tests / docs

- Vitest: `shouldShowVirtualPublishChrome("sitemap-xml")`, panel Publish success dest path, save still omits remotes/credentials.
- Playwright: intercept HTTP 200 files copied + live H2 Publish copies `8.2/index.html`.
- product-docs 8.2 admin Sites / Publishing, developer REST / virtual-sites, reference site-config.

## Hard bans checked

No ActionMenu files. No live crawl. No secrets on the REST envelope. Preview chrome remains a later sibling.
