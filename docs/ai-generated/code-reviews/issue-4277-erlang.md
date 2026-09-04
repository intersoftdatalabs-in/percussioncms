# Erlang review — issue #4277 (Playwright SY-02 server config write H2)

**Date:** 2026-09-04  
**Branch:** `fix/issue-4277-playwright-sy02-server-config-write`  
**Reviewer persona:** Erlang (pre-commit)

## Scope

Surface-filtered Playwright for Developer → Configurations allow-listed save
(`developer-server-configs-write.spec.js`), plus C5 wire fix: SPA PUT wraps
`{ ServerConfig: { content } }` for CXF UNWRAP_ROOT_VALUE. Stacks open tips
#4275 / #4276 (#4280 / #4281). Adds `data-cfg-name` on catalog open buttons.

## Findings

| Severity | Finding | Disposition |
|----------|---------|-------------|
| Bug (C5) | Bare `{ content }` PUT → HTTP 400 JAXB root | **Fixed** — `wrapServerConfigForWire` + Vitest + product-docs |
| Portability | Spec uses `URLSearchParams` / Playwright; no OS path joins | OK |
| Companion | Smoke-set + unit assertion for `server-configs-write` | Done |
| Residual | `TIDY_CONFIG` / `SERVER_PAGE_TAGS` under `rxconfig/XSpLit` — parent dir missing on fresh H2 → FileOutputStream 500 | **Residual** — create parent dirs on save (system) |

## Gates

- No behavioral bugs left in the exercised NAV_CONFIG path.
- Playwright C5 green: 1 passed; console-clean; no feature ERROR on pass window.
- Product-docs: REST write contract documents Jackson wrap.

**Verdict:** pass for commit/PR (residual XSpLit parent-dir filed separately).
