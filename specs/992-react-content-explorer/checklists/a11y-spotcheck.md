# Accessibility spot-check (SC-009 / T082)

**Feature**: [spec.md](../spec.md)
**Scope**: FR-024 (a11y), SC-009 (≥95% keyboard-only navigation completion)
**In-scope surfaces**: modern Content Explorer (US1/2/3/4/5/7), ContentBrowser dialog, pilot JSPs.
**Out of scope**: legacy JSP chrome in `cm/app/includes/finder*.jsp` (Finder retired), vendor themes (Bootstrap 5 defaults only).

## Automated gates (T082a / T082b)

|       Layer        |                           Helper                            |                  Spec                  |                                                    Gate                                                     |
|--------------------|-------------------------------------------------------------|----------------------------------------|-------------------------------------------------------------------------------------------------------------|
| Component (Vitest) | `WebUI/src/test/ts/contentExplorer/a11y.ts`                 | every US component spec                | `renderA11yGate(container)` → zero `serious` / `critical` axe violations (WCAG 2.0 A + AA, WCAG 2.1 A + AA) |
| E2E (Playwright)   | `modules/perc-qa-automation/frontend/tests/helpers/a11y.js` | one test per US spec + each host pilot | `expectNoSeriousA11yViolations(page, { scope })` after React bundle mounts                                  |

**Rule set**: `wcag2a`, `wcag2aa`, `wcag21a`, `wcag21aa` tags. `color-contrast` is suppressed in jsdom by default (it requires a rendered canvas / `getComputedStyle`); Playwright scans with full color-contrast.

**Polling**: A failing test fires on the first violation; the test prints rule id, impact, target selector, and a clipped HTML snippet per offender (max 3 nodes per rule) so the failure trace is actionable.

## Manual spot-check matrix (T082)

Manual focus is for surface-only flows that Playwright cannot drive — typically iframe-embedded dialogs, native confirm prompts, OS-level file pickers, or keyboard combinations where the host page steals focus.

|                     Surface                      |                                   Path                                    |                                                 Keyboard verifier                                                 |                                   Manual checklist                                    |   Tested    |
|--------------------------------------------------|---------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|-------------|
| Modern explorer tree                             | `explorerModern.jsp`                                                      | `Tab` → `Enter` (expand) → `↑/↓` (navigate) → `Enter` (select)                                                    | a11y gate (T082b)                                                                     | ✅ automated |
| Detail list                                      | same                                                                      | `Tab` into row → `Enter` (open editor) → arrow keys (page/scroll)                                                 | a11y gate (T082b)                                                                     | ✅ automated |
| Context menu (right-click on row)                | within tree/list                                                          | `Esc` to close; arrow keys via `ContextMenu.handleItemKey` (`ACTIVATE_KEYS = Enter / Space`)                      | a11y gate (T082b) + `ContextMenu.test.tsx` "Escape close"                             | ✅ automated |
| ActionToolbar                                    | header                                                                    | `Tab` through buttons; `Enter`/`Space` activates; `aria-label` per action                                         | a11y gate (T082b) + `ActionToolbar.test.tsx`                                          | ✅ automated |
| FolderSecurityPanel                              | `folderSecurityModern.jsp`                                                | `Tab` through principal lists; `Enter` (remove); `Enter` in add input; `Tab` to save                              | a11y gate (T082b) + `FolderSecurityPanel.test.tsx`                                    | ✅ automated |
| SearchPanel                                      | `searchModern.jsp`                                                        | `Tab` to input → enter query → `Enter` (submit) → `Tab` to result row → `Enter` (open) → retry button after error | a11y gate (T082b) + `SearchPanel.test.tsx`                                            | ✅ automated |
| ContentBrowser dialog (asset/page/folder picker) | `assetPickerModern.jsp`, `pagePickerModern.jsp`, `folderPickerModern.jsp` | `Tab` cycles tree → list → confirm/cancel. Confirm disabled until selection. `Esc` closes.                        | a11y gate (T082b) + keyboard-completable Cancel asserted in `ContentBrowser.test.tsx` | ✅ automated |
| ClipboardPanel                                   | `us7AdvancedModern.jsp`                                                   | `Tab` through items, `Enter` on paste; copy / move radio                                                          | a11y gate (T082b)                                                                     | ✅ automated |
| SiteCopyWizard                                   | same                                                                      | 5-step linear flow; `Next` disabled until step valid; `Tab` cycles fields; `Esc` from confirm is a TODO           | a11y gate (T082b) + `SiteCopyWizard.test.tsx`                                         | ✅ automated |
| SubfolderCopyWizard                              | same                                                                      | 3-step linear flow, same pattern                                                                                  | a11y gate (T082b) + `SubfolderCopyWizard.test.tsx`                                    | ✅ automated |

### Specific manual spot-checks required (out of Playwright reach)

|                                                                Item                                                                |                                                           Why manual                                                           |                                                          Verifier                                                          |
|------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `<iframe>`-embedded legacy panels that Playwright can't reach (`perc-mcol` finder-chrome remnants inside Track A residual screens) | Playwright frame-scope rules do not allow deep cross-frame scanning                                                            | operator / UAT tester; cite the screen ID in the cutover-inventory §C row at sign-off                                      |
| `window.confirm` dialog in `ReducedActions.delete` (US1)                                                                           | Native browser dialog is not in the React surface; Playwright hooks it via `dialog` event but axe can't scan the native chrome | Confirm dialog is keyboard-skip-disabled (`text` only); operators confirm visually that the message text reads as expected |
| Native save-success toast on the host shell                                                                                        | Toast is mounted into the JSP-shared chrome below the React root                                                               | Visual + keyboard tab-out regression: it must not steal focus on mount                                                     |
| OS file picker for "Import asset" path (US7 SubfolderCopy step 3)                                                                  | OS-level dialog, out of React DOM                                                                                              | Visual only                                                                                                                |

## T082 a11y-spot-check sign-off

|        Surface        |    Tester    |    Date    | Result  |         Issue ref          |
|-----------------------|--------------|------------|---------|----------------------------|
| Modern explorer tree  | (playwright) | 2026-07-20 | ✅       | —                          |
| Detail list           | (playwright) | 2026-07-20 | ✅       | —                          |
| Context menu          | (playwright) | 2026-07-20 | ✅       | —                          |
| ActionToolbar         | (playwright) | 2026-07-20 | ✅       | —                          |
| FolderSecurityPanel   | (playwright) | 2026-07-20 | ✅       | —                          |
| SearchPanel           | (playwright) | 2026-07-20 | ✅       | —                          |
| ContentBrowser dialog | (playwright) | 2026-07-20 | ✅       | —                          |
| ClipboardPanel        | (playwright) | 2026-07-20 | ✅       | —                          |
| SiteCopyWizard        | (playwright) | 2026-07-20 | ✅       | —                          |
| SubfolderCopyWizard   | (playwright) | 2026-07-20 | ✅       | —                          |
| Manual spot-check     | UAT owner    | TBD        | Pending | tbd at 8.2 candidate build |

## SC-009 acceptance criteria

- Every modern React surface listed above passes the axe-core `wcag2aa` gate with **zero `serious` / `critical` violations** in CI.
- Every modern React surface is **keyboard-completable**: the user can complete the canonical action tree without a mouse.
- Every visible text element has an i18n TMX key (see [i18n-key-presence.md](./i18n-key-presence.md)).
- Page-level landmarks (`<main>`, `<nav>`, `<header>`) are emitted by the JSP shell — modern components use `<section aria-label>` and `role="status"` / `role="alert"` for transient surfaces.

## Known axes excluded

- `color-contrast` in jsdom (Playwright covers it).
- `landmark-no-duplicate-main` / `region` — the JSP chrome owns landmarks; React surfaces scope to their mount target.

