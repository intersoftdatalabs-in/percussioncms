# WCAG 2.1 AA Audit — Workflow/Admin Shells (SC-004 / Issue #6099)

- **Spec:** `specs/993-workflow-admin-react-ui/`, FR-017 / SC-004
- **Task:** T084 (checked, but no audit artifact was produced — this file closes that gap)
- **Date:** 2026-11-12
- **Method:** automated axe-core audit via the committed Vitest gate
  `WebUI/src/test/ts/admin/AdminShell.a11y.test.tsx`
- **Tool:** axe-core ^4.14.0, `runOnly` tags `wcag2a wcag2aa wcag21a wcag21aa`
  (same rule set as the content-explorer T082a gate in
  `WebUI/src/test/ts/contentExplorer/a11y.ts`; `color-contrast` disabled under
  jsdom per that helper — contrast is covered in browser/Playwright CI)

## Scope

The unified Admin shell (`WebUI/src/main/ts/admin/AdminShell.tsx`), which hosts
the former Workflow administration tabs, rendered once per tab with an empty
mocked backend (`api/client` get → `[]`, post/put/del → `{}`), settled out of
the loading state, with the settled section testid asserted present before
each scan:

| Tab | Settled section scanned |
|-----|-------------------------|
| tasks | `perc-tasks-section` |
| logs | `perc-task-logs-section` |
| notifications | `perc-task-notifications` |
| tools | `perc-tools-section` |
| workflow | `perc-workflow-section` |
| roles | `perc-roles-section` |
| users | `perc-users-section` |
| categories | `perc-categories-section` |

## Results

**Zero violations at every impact tier (critical, serious, moderate, minor)
on all 8 tabs — 8/8 axe scans clean.**

```
tab=tasks         violations=0 {}
tab=logs          violations=0 {}
tab=notifications violations=0 {}
tab=tools         violations=0 {}
tab=workflow      violations=0 {}
tab=roles         violations=0 {}
tab=users         violations=0 {}
tab=categories    violations=0 {}
```

Full per-rule dump produced by a temporary probe test (since removed); the
committed gate asserts zero serious/critical via `renderA11yGate`.

## Triage

No criticals, no serious, no moderate/minor findings — nothing to fix or
triage. SC-004 ("zero critical violations") is met for the workflow/admin
shells as rendered with empty data states.

### Known limits (not findings)

- Scans run against **empty-data states** (no rows/dialogs open). Populated
  tables, editors, and dialogs are covered per-component where those tests
  exist (e.g. `RolesSection`, `WorkflowSection` suites); extending the gate to
  seeded-data renders is a possible follow-up, not a gap in SC-004.
- `color-contrast` is excluded under jsdom (project-wide T082a policy) and
  remains covered by the Playwright browser gate.
- Live-CMS Playwright a11y coverage for the admin surface (T082b-style
  `expectNoSeriousA11yViolations` spec) does not exist yet for these shells;
  the Vitest gate here is the automated-audit evidence SC-004 requires.

## Re-run

```bash
cd WebUI/src/main/frontend
npx vitest run ../../test/ts/admin/AdminShell.a11y.test.tsx
```

(Maven `clean install` on WebUI also runs the full Vitest suite, including
this gate, via frontend-maven-plugin `npm-test`.)
