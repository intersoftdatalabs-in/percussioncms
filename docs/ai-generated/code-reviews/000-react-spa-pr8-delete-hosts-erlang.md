# Erlang review — PR-8 delete obsolete SPA product hosts

|        Field        |                                                               Value                                                                |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------|
| **Branch**          | `feat/000-react-spa-pr8-delete-obsolete-hosts`                                                                                     |
| **Base**            | `development` @ `84a140eb4f` (PR-7)                                                                                                |
| **Date**            | 2026-07-27                                                                                                                         |
| **Scope**           | Uncommitted PR-8 cleanup: delete retired `*Modern.jsp` product hosts, classic login, retarget residual links, tests, docs, QA URLs |
| **Recommendation**  | **approve**                                                                                                                        |
| **May commit/push** | **yes**                                                                                                                            |

## Summary

PR-8 ends reference retention for product shells superseded by the SPA. Deletes
are limited to hosts that already only redirected into `index.jsp` / `spa.jsp`
(plus unused classic login markup and dead includes). Residual bridge dialog
hosts and legacy full-page embeds are retained. Tests now assert absence of
deleted files and presence of residual hosts; QA automation targets
`spa.jsp?entry=explorer`.

## Gate

|                  Check                  |                            Result                             |
|-----------------------------------------|---------------------------------------------------------------|
| Bugs (logic / security)                 | None found                                                    |
| Behavioral tests for non-trivial change | Pass — Vitest `spaCutover` + `publishNavRewire` (14 tests)    |
| Cross-platform path/file I/O            | N/A — no production path I/O; tests use `node:path` `resolve` |
| New warnings on module build            | None attributable (WebUI `clean install` BUILD SUCCESS)       |

## Issues

None (blocking).

### Nits / non-blocking

1. **Bookmark 404:** Direct URLs to deleted `*Modern.jsp` product hosts will 404.
   Intentional per design (Phase 4 delete). Documented in residual-bridge-embeds.
2. **`rxlogin.jsp` comment** still names `rxlogin-classic.jsp` historically; file is gone;
   security conf no longer grants anonymous access.

## Memory patterns hit

- Dual-tree alignment for mirrored app JSPs (actionMenu retarget both trees)
- ProxyURL/query SPA contract remains on live `index.jsp` / `spa.jsp` (not re-litigated here)
- Do not delete residual dialog bridge hosts

## Evidence

- `cd WebUI && ../mvnw clean install` → **BUILD SUCCESS**
- `npx vitest run …/spaCutover.test.ts …/publishNavRewire.test.ts` → **14 passed**

