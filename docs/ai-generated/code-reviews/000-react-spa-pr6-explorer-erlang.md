# Erlang review — feat/000-react-spa-pr6-explorer (+ login CSS on PR-5)

**Date:** 2026-07-27  
**Scope:** Explorer SPA route; residual bridge doc; (PR-5 follow-up) modern CSS load  
**Recommendation:** **approve**  
**May commit/push:** **yes**

## Summary

- **Explorer:** `ExplorerRoute` lazy-loads `ContentExplorerShell` with allowlisted `path` from client search; `explorerModern.jsp` (both trees) 302 → `spa.jsp?entry=explorer` with proxyURL; residual bridge embeds documented.
- **Login CSS (PR-5 commit):** Root cause confirmed — Vite entry CSS never linked on thin JSPs. Stable `perc-modern-ui.css` + JSP link + `ensureModernStyles` + logo max dimensions.

## Gate

|        Check         |                                           Result                                            |
|----------------------|---------------------------------------------------------------------------------------------|
| Bugs                 | None                                                                                        |
| Behavioral tests     | App explorer entry; spaCutover explorer redirect; ensureModernStyles; login styles contract |
| Cross-platform paths | N/A (URL only)                                                                              |
| Security             | Explorer path allowlist + encode; redirect query only                                       |

## Issues

None blocking.
