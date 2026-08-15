# Erlang review — #3410 Explorer Sites list + Create Site

**Scope:** `fix/issue-3410-explorer-sites-list-create` vs `origin/main`.
**Memory patterns hit:** missing behavioral tests; Playwright companions for Explorer chrome; change-class closure (product-docs + surface spec); Spring TX rollback-only from swallowed RuntimeException.
**Cross-platform path checklist:** CMS finder `/` paths only; helpers normalize `\` / drive letters; no OS filesystem I/O.

## Summary

Cycle-verify residuals: empty CorporateInvestments children, wizard `panel.or(wizard)` strict-mode, Create Site submit `error`.

Root cause of empty children was not a missing seed row: H2 has folder 523 and children, but `PSContentWs.getItemSummaries` wrapped missing FF nav types 313–315 in `RuntimeException`, marking the listing TX rollback-only (`UnexpectedRollbackException` → HTTP 404). Swallowing in `getNavFolderType` after `getNavonProperties` was too late.

This change:

- Skips unregistered content types in `PSContentWs.getItemSummaries` (checked `PSInvalidContentTypeException`, no TX poison).
- Pre-checks nav child type before `getNavonProperties`; skips unknown folder children in `PSItemSummaryService`.
- Recovers site folder children when SITENAME (`Corporate_Investments`) ≠ FOLDER_ROOT (`//Sites/CorporateInvestments`).
- Nests wizard locator; expands Explorer via disclosure toggle; Create Site happy path already on main via #3393/#3408.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None (hard-gate).

Behavioral tests: `PSContentWsItemSummaryResilienceTest`, `PSItemSummaryServiceNavTypeResilienceTest`, `PSSitePathItemServicePathParseTest` (no-slash site-only + sitename/folder match). Node helper unit for `siteChildListCandidates` + console-noise filter. Product-docs browse/create steps updated.

C5: `npm run test:surface -- --path tests/explorer-sites-list-create.spec.js` → 7 passed; `npm run test:golden` → 2 passed. Remaining server WARNs for skipped 313–315 items are related #3326 (do not close).

> Co-Authored by Grok Build 1.0.4 using grok-4.6 with agent night-issue-prs.
