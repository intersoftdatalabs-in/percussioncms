# Erlang review — feat/000-react-spa-pr7-home-gadgets

**Date:** 2026-07-27  
**Scope:** Compose Dashboard gadgets into Home SPA section  
**Recommendation:** **approve**  
**May commit/push:** **yes**

## Summary

PR-7 product lock: gadget utility stays; placement is **Home → gadgets**, not peer SPA `/dashboard`. Reuses `Dashboard` + widget registry with `embedded` compact chrome. TopNav Dashboard → `/home/gadgets`. `?view=dash` and homepage preference “Dashboard” map to `spa.jsp?entry=home&section=gadgets`. Dual-tree index.jsp aligned.

## Gate

|      Check       |                                 Result                                  |
|------------------|-------------------------------------------------------------------------|
| Bugs             | None found                                                              |
| Behavioral tests | HomeShell gadgets section; deepLinkMap aliases; spaCutover dash mapping |
| Cross-platform   | N/A (URL / React only)                                                  |
| Security         | Section allowlisted; no new sinks                                       |

## Issues

None blocking.
