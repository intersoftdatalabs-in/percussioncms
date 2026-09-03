# Erlang review — #4188 sitemap-xml H2 Playwright rebuild

Date: 2026-09-02
Scope: `modules/perc-qa-automation` Playwright helper/spec + `product-docs/8.2` operator notes.

## Verdict

Pass for this slice. No production Java/WebUI chrome change. Live save/Build/Preview tests for sitemap-xml are unchanged.

## Bugs

None found.

## Tests

- Unit: rebuild fixture files exist, distinct body token, no live crawl URLs.
- Live: first Build + docker cp of current sitemap.xml/pages + second Build without Jetty restart; pagesWritten increases; preview HTML lastmod/body change.

## Cross-platform

- Host paths use `path.join`.
- In-container dest is POSIX `/opt/Percussion/tmp/...` with `/` (Linux QA cell / ZIP-style dest).
- No `os.tmpdir` / `%TEMP%`.

## Product docs

Admin Sites: rebuild after sitemap loc/lastmod/page edit; no CMS/Jetty restart.
