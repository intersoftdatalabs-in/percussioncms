# Erlang review — #4140 REST action-menu persist residual

**Memory patterns hit:** change-class closure (rest JsonReader + sitemanage adaptor + system JDBC persist + adaptor tests + H2 Playwright persist spec + product-docs); lock-no-steal 409; Hibernate catalog vs design-WS XML miss; packaged Copy 404 vs 409.

## Verdict

Pass for this slice. Cluster persist (`ActionMenuJsonReader`, `RXMENUACTION` JDBC) is consumed from #4139 and tightened so GET catalog sees POST (own Hibernate session commit + `CacheMode.IGNORE` on `findActionMenus` + L2 region evict). Packaged `Copy` DELETE/PUT is 409 via Hibernate-first identity (design-WS miss no longer 404). REST user DELETE still JDBC-deletes when XML load misses. `overrideLock=false` unchanged. Finder helpers not reimplemented. SPA chrome not re-litigated.

## Checks

- Behavioral unit tests: `PSUiDesignWsActionPersistTest`, `ActionMenuAdaptorWriteTest` (Copy 409 when design-WS misses, rest-user DELETE still calls `deleteActions`), `ActionMenuJsonReaderTest`.
- Portable paths: JDBC SQL identifiers only; Playwright uses existing auth helpers.
- Product-docs 8.2 `developer/rest.md` + `admin/developer-action-menus.md`: durable catalog after POST; packaged Copy 409.
- Playwright: `developer-action-menu-persist.spec.js`.

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
