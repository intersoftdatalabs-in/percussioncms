# Quickstart validation: Home + Widget Builder React migration

**Feature**: 989-react-cui-widget-builder  
**Audience**: Implementers and QA validating the plan after code lands.

## Prerequisites

- JDK 21 toolchain via `./mvn-env.sh` (or `.bat` on Windows)
- Node/npm for WebUI frontend (module already uses Vite)
- Running CMS instance with at least one site (for Library/Recent) and Widget Builder enabled when testing US2 (`WidgetBuilderActive`)
- Auth: contributor for Home; admin/designer for Widget Builder

## Build / test (dev)

```bash
# From repo root — WebUI modern unit tests (exact Maven goal may vary; use module docs)
./mvn-env.sh -pl WebUI -am test

# Frontend-focused (from WebUI or frontend package as documented in WebUI/AGENTS.md)
cd WebUI/src/main/frontend && npm test
```

Expect: modern Home and Widget Builder Vitest suites green; **no** failing legacy `percWidget*.test.js` after US3 (those files removed).

## Smoke paths (manual)

### Home (US1 / SC-001)

1. Sign in → open **Home** from main nav.
2. Confirm single modern shell (no CUI iframe).
3. **Recent**: list loads or empty state with clear messaging.
4. **Library**: browse site/folder; open a content item into existing editor flow.
5. **Search**: run a query; results openable.
6. **Create**: create at least one of page / asset / blog; locate or open result.
7. Deep links: `view=home&initialScreen=library|list|search|newitem` land on correct sections.

### Widget Builder (US2 / SC-002)

1. Enable Widget Builder; open from nav.
2. Create simple definition → save → reload → still present.
3. Validate invalid data → errors shown.
4. Deploy/package succeeds for valid definition.
5. Disable Widget Builder → entry hidden or access denied (SC-006).

### Removal (US3 / SC-003)

1. Complete and sign [removal-inventory.md](./checklists/removal-inventory.md).
2. Confirm production distribution has no live CUI SPA / classic WB packs / classic entry JSPs.
3. Orphan vendors: removed only if inventory shows zero consumers; else listed as retained.
4. Main nav smoke: Home, Dashboard, WB (SC-005).

### Deep links (SC-007)

1. Known legacy Home/WB URLs from inventory resolve to modern destinations.
2. One deliberate unmapped legacy path shows a clear **on-page** unavailable/moved message (not a blank page).

### Main-nav smoke (SC-005 / FR-020)

1. Dashboard opens from main nav.
2. At least one other non-Home tab (e.g. editor/Web Management or Design) opens for the tester’s roles.
3. Home still opens; Widget Builder opens only when enabled.
4. Record pass/fail with the PR or release notes.

### i18n key presence (SC-008 / FR-024)

1. Complete [checklists/i18n-key-checklist.md](./checklists/i18n-key-checklist.md): primary Home/WB chrome maps to TMX keys.
2. Confirm modern shells load `tmx.jsp` with session locale.
3. Optional: non-default locale spot-check if environment supports it.
4. Sign off on the shippable PR (not a multi-locale Vitest requirement).

## Contract references

- [contracts/widget-builder-api.md](./contracts/widget-builder-api.md)
- [contracts/home-deep-links.md](./contracts/home-deep-links.md)
- [data-model.md](./data-model.md)

## Out of scope for this quickstart

- Full Web Management finder admin parity
- Dashboard gadget modernization
- Active Assembly / Dojo Track A

