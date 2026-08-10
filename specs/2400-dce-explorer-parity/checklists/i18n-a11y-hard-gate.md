# Explorer parity (#2400) — i18n + 508 / accessibility HARD GATE

**Parent:** [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400)  
**Aligns with:** FR-026 / T083 (i18n), FR-024 / SC-009 / T082 (a11y) from `specs/992-react-content-explorer/`  
**WebUI agent rule:** `WebUI/AGENTS.md` → **Content Explorer — i18n + 508 / accessibility**

## Mandatory for every Explorer UI slice

### i18n

1. Add keys to `WebUI/src/main/ts/contentExplorer/messages.ts` (`EXPLORER_MSG`).
2. Shape: `perc.ui.explorer@<English default text>`.
3. Render only via `message(EXPLORER_MSG.KEY)` (including `aria-label` text).
4. Do **not** invent parallel catalogs for Explorer chrome.
5. CMS design data (display format names, server menu labels, item titles) may display raw — product chrome may not.

### Accessibility (Section 508 / WCAG 2.1 AA target)

1. Keyboard path for every new control (toggle, select, panel, menu).
2. Toggles: `aria-pressed`, `aria-expanded`, `aria-controls` when they show/hide regions.
3. Regions/panels: `aria-label` (or labelled heading) via `EXPLORER_MSG`.
4. Errors: `role="alert"` (and `aria-live` when appropriate); status hints: `role="status"`.
5. Associate form controls with visible labels (`htmlFor` / `aria-labelledby`).

### Tests (non-negotiable)

|   Layer    |                                      Gate                                       |
|------------|---------------------------------------------------------------------------------|
| Vitest     | `renderA11yGate(container)` on new/changed surfaces (`contentExplorer/a11y.ts`) |
| Vitest     | Assert new chrome keys are `perc.ui.explorer@…` when adding shell chrome        |
| Playwright | `expectNoSeriousA11yViolations` on Explorer shell / expanded panels             |
| Playwright | Behavioral coverage of user-visible flows (`perc-qa-automation`)                |

## References

- `specs/992-react-content-explorer/checklists/a11y-spotcheck.md`
- `specs/992-react-content-explorer/checklists/i18n-key-presence.md`
- `docs/ai-generated/tasks/gh-codeql-alerts/` — unrelated security; do not confuse with a11y

## Sign-off (update per slice PR)

|       Slice / PR        |                                             i18n keys                                              |          Vitest a11y          |              Playwright a11y              |     Notes     |
|-------------------------|----------------------------------------------------------------------------------------------------|-------------------------------|-------------------------------------------|---------------|
| #2407 shell composition | DISPLAY_FORMAT_*, SERVER_ACTIONS_*, VIEW_TOOLS_*, TOGGLE_*, PANEL_REGION_*, SECURITY_SELECT_FOLDER | ContentExplorerShell.test.tsx | us1-core-explorer shell + search expanded | Product shell |

