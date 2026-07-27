# Research Findings: 993 — Unified Workflow & Admin React UI

**Branch**: `993-workflow-admin-react-ui`
**Date**: 2026-07-20
**Scope**: WebUI shell architecture, API patterns, test patterns, i18n, routing

---

## Decision 1: Shell Registration Pattern

**Decision**: New shells are registered via a single `componentRegistry.set()` call in `WebUI/src/main/ts/registry.ts`.

**Rationale**: The registry is a flat `Map<string, ComponentType<any>>`. New shells require no framework config, just one import and one `.set()` call:

```ts
componentRegistry.set("WorkflowAdminShell", WorkflowAdminShell);
componentRegistry.set("AdminShell", AdminShell);
```

**Alternatives considered**: None — this is the only pattern in the codebase.

---

## Decision 2: Intra-Shell Navigation

**Decision**: Section switching uses a `useState<Section>` state machine — no React Router and no URL hash changes.

**Rationale**: Both `PublishingShell` and `ContentExplorerShell` use the same pattern. No `react-router-dom` dependency exists in `package.json`. Sections are rendered with `{active === "workflow" && <WorkflowSection />}` conditionals. Deep-link props are passed through the JSP `mount()` call at page load.

**Alternatives considered**: React Router was evaluated but rejected — the JSP dispatch table (`index.jsp` view map) already handles top-level navigation; intra-shell routing would add complexity without benefit.

---

## Decision 3: REST API Client

**Decision**: Use the existing `api/client.ts` typed `fetch` wrapper with path constants from `api/paths.ts`.

**Rationale**: `get<T>()`, `post<T>()`, `put<T>()`, `del<T>()` from `client.ts` auto-inject CSRF tokens and handle response parsing. All existing workflow and user management REST endpoints (`/Rhythmyx/services/workflowmanagement/`, `/Rhythmyx/services/user/user/`, `/Rhythmyx/services/rolemanagement/`) are already available and only need path constants added to `paths.ts`.

**Alternatives considered**: None — Axios is not a dependency; the typed fetch wrapper is the project standard.

---

## Decision 4: i18n / Localization

**Decision**: Use `import { message } from "../i18n/message"` with a per-module `const WF_MSG = {...} as const` key object, following the pattern of `contentExplorer/messages.ts`.

**Rationale**: The `message()` utility reads `window.I18N.message(key)` (loaded via TMX JSP tag on line 52 of `adminWorkflow.jsp`) and falls back gracefully to the key string in test environments. Key format: `perc.ui.{namespace}@{Fallback String}`.

**TMX key namespaces in scope**:
- `perc.ui.workflow@*` — workflow definition labels
- `perc.ui.workflow.view@*` — workflow list/editor messages
- `perc.ui.workflow.steps.view@*` — step editor labels
- `perc.ui.users@*` / `perc.ui.users.import.*@*` — user management / LDAP import
- `perc.ui.roles@*` — role management labels
- `perc.ui.admin.workflow@*` — categories / admin combined labels
- New keys to add: `perc.ui.workflowAdmin@*` for the new unified shell labels

**Alternatives considered**: Hard-coded English strings — rejected by FR-016 and Constitution Principle VIII.

---

## Decision 5: Mount Point & JSP Integration

**Decision**: Reuse the existing `perc-admin-workflow-explorer` div in `adminWorkflow.jsp`; replace the `mount("...", "ContentExplorerShell", ...)` call with `mount("perc-admin-workflow-explorer", "WorkflowAdminShell", { section: "<%= section %>" })`.

**Rationale**: The JSP already loads the TMX script, the modern bundle, and the React root element. The only change needed is the component name in the `mount()` call and removal of the legacy jQuery `#tabs` section. A new `adminWorkflowModern.jsp` may be created as a clean replacement (no legacy jQuery/Dojo), with `index.jsp` view map updated from `adminWorkflow.jsp` → `adminWorkflowModern.jsp`.

**Alternatives considered**: Creating a new JSP from scratch — valid but unnecessary given the existing mount infrastructure.

---

## Decision 6: Admin Shell Routing

**Decision**: Create a new `adminModern.jsp` for the AdminShell (scheduled tasks, tools) and add `views.put("admin", "adminModern.jsp")` in `index.jsp`. The existing `admin.jsp` entry becomes `adminModern.jsp`.

**Rationale**: The `index.jsp` view dispatch table already maps view keys to JSP files. Adding the new shell follows the identical pattern used for publishing.

---

## Decision 7: Unit Testing

**Decision**: Vitest + React Testing Library v16. Test files at `WebUI/src/test/ts/workflowAdmin/*.test.{ts,tsx}` mirroring the source structure.

**Rationale**: Matches project standard (`src/test/ts/{module}/{Component}.test.tsx`). Run via `cd WebUI && npm test`.

**Mock pattern**:
- Module-level: `vi.mock("@/api/workflowAdmin/workflowApi", () => ({ fetchWorkflows: vi.fn().mockResolvedValue([]) }))`
- Fetch-level: per-test `mockFetch()` from a local `setup.ts`

---

## Decision 8: E2E Testing

**Decision**: Playwright specs in `modules/perc-qa-automation/frontend/tests/` using `.js` (CommonJS) format following the naming convention `{us-id}-workflow-admin.spec.js`.

**Rationale**: Established pattern from existing specs (`us1-core-explorer.spec.js`, publishing specs). All specs use `loginAsAdmin` + `BASE_URL` from `helpers/auth`.

---

## Unresolved Items

None. All technical questions are resolved from codebase evidence.

---

## Confirmed Patterns Summary

|        Concern         |                                Pattern                                 |                  Evidence                   |
|------------------------|------------------------------------------------------------------------|---------------------------------------------|
| Shell registration     | `componentRegistry.set(name, Component)` in `registry.ts`              | `registry.ts` lines 37–57                   |
| Intra-shell navigation | `useState<Section>` state machine                                      | `PublishingShell.tsx` lines 73–100          |
| REST API calls         | `get<T>(PATHS.X)` from `api/client.ts`                                 | `client.ts`, `paths.ts`                     |
| i18n                   | `message(WF_MSG.KEY)` + per-module `messages.ts`                       | `message.ts`, `contentExplorer/messages.ts` |
| Mount element          | `perc-admin-workflow-explorer`                                         | `adminWorkflow.jsp` lines 219–221           |
| Mount call change      | Replace `ContentExplorerShell` with `WorkflowAdminShell`               | `adminWorkflow.jsp` line 240                |
| Unit tests             | `src/test/ts/workflowAdmin/*.test.tsx` via `npm test`                  | `package.json`, existing test files         |
| E2E specs              | `perc-qa-automation/frontend/tests/{us-id}-workflow-admin.spec.js`     | Existing spec files                         |
| View dispatch          | `views.put("workflowAdmin", "adminWorkflowModern.jsp")` in `index.jsp` | `index.jsp` lines 62–72                     |

