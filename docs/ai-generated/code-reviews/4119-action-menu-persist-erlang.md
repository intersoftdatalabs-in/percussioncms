# Erlang review — #4119 REST action menu persist after POST/DELETE

**Memory patterns hit:** change-class closure (sitemanage adaptor + system JDBC persist peer #4101 + adaptor tests + H2 Playwright persist + product-docs); lock-no-steal 409; Hibernate catalog vs design-WS XML miss.

## Verdict

Pass for this slice. `saveActions` / `deleteActions` now write durable `RXMENUACTION` rows (same class as display-format JDBC persist). GET catalog / GET by name use Hibernate tree; write identity falls back to that tree when design-WS `findActions` misses (system Edit 409, not 404). `overrideLock=false` unchanged. Finder helpers (`/find`) not reimplemented.

## Checks

- Behavioral unit tests: `PSUiDesignWsActionPersistTest` (H2 INSERT/UPDATE/DELETE), `ActionMenuAdaptorWriteTest` (Hibernate miss → system 409, duplicate 409, nested GET key).
- Portable paths: JDBC SQL identifiers only; Playwright uses `path.join` via existing auth helpers.
- Product-docs 8.2 `developer/rest.md` notes durable `RXMENUACTION`.
- Playwright: `developer-action-menu-persist.spec.js` (POST catalog, DELETE 404, system 409, Editor 403).

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
