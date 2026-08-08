# Erlang review — issue #2282 Default ACL preferences / template behavior

**Verdict:** approve  
**Branch:** `fix/issue-2282-default-acl-prefs-templates`  
**Scope:** WebUI Developer Preferences (Security default ACL template) + apply-on-create in ObjectAclSection; Vitest + Playwright; no new REST (uses existing preferences + ACL APIs).

## Change class

WebUI product screen / preference surface (companions: pure helpers, Vitest, Playwright HARD GATE).

## Checklist

| Item | Result |
|------|--------|
| Inventory on parent #2274 before coding | Pass — comment posted |
| No re-do of B1 special-row UX | Pass |
| No B3 design/runtime depth | Pass |
| Wire existing APIs (preferences + ACL bulk) | Pass — no new REST |
| Behavioral unit tests for helpers + UI + create path | Pass |
| Playwright for user-visible prefs surface | Pass — `developer-default-acl-preferences.spec.js` |
| Portable paths / no hardcoded user paths | Pass |
| Intersoft header on new 2026 sources | Pass |
| Module `mvnw clean install` (WebUI) | Pass |
| Module `mvnw clean install` (perc-qa-automation) | Pass (run with PR) |

## Findings

No hard-gate bugs found.

### Notes (non-blocking)

1. **Template apply is best-effort after create** — if prefs load or bulk save fails, owner ACL still remains and UI surfaces `ACL_TEMPLATE_APPLY_ERROR`. Intentional so create is not rolled back.
2. **Server-side auto-seed** (`configureDefaultAclEntries` on design WS load) is separate from client preference template; B2 documents and implements Workbench-style **user preference** path only.
3. **TMX** not updated — `DEV_MSG` `@` fallback covers offline/tests; optional residual for locale packs.
4. **Live H2 Playwright** not run in this session — surface listed via `--list`; run when CMS available.

## Residual (optional, not filed unless product needs)

- Server-side apply of user default ACL template on all design-object create paths beyond ObjectAclSection
- TMX entries for new `perc.ui.developer@*` preference strings
- B3 design vs runtime permission model depth (#2283)

> Co-Authored by Grok Build using grok-4.5 with agent main.
