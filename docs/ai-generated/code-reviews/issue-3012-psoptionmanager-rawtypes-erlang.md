# Erlang review — #3012 PSOptionManager rawtypes residual

**Scope:** `modules/DesktopContentExplorer` — `PSOptionManager` + `PSOptionManagerTest`  
**Reviewer persona:** Erlang (independent of implementer)  
**Date:** 2026-08-11

## Change class

Compiler tech-debt: parameterize residual raw `Collection`/`Iterator`/`Map`/`HashMap` on option load/save helpers. No product behavior change intended.

## Findings

| Severity | Finding | Disposition |
|----------|---------|-------------|
| none | Bugs in new logic | Pass — `toXmlCollection` typed to `Collection<? extends IPSClientObjects>`; null element still rejected with `IllegalArgumentException` |
| none | Missing behavioral tests | Pass — `PSOptionManagerTest` covers null args, empty, append, null element, and `compare` cases |
| none | Non-portable paths | N/A — no file I/O |
| none | Blanket class-level `@SuppressWarnings` | Pass — none added |
| none | Product-docs gate | N/A — pure generics; no operator surface |

## Companions checked

- Peer pattern: #2993 / PR #3011 (`PSSearchDialog` + pure-helper unit tests)
- Module standalone `mvnw clean install` required (done)
- Did not touch #2439 `PSFolderAclEditorDialog` (still In Progress)

## Verdict

**Pass** — ready for commit/PR.
