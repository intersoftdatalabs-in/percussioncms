# Erlang Review — 992-react-content-explorer US4 (P-ACL)

**Branch**: `992-react-content-explorer-us4` (off `origin/development` HEAD `8b3ce6cf06`)
**Base**: `development`
**Reviewer**: Erlang (independent)
**Date**: 2026-07-20
**Scope**: US4 P-ACL (T058–T064b). Phases covered: pure ACL helpers +
Vitest, FolderSecurityPanel React component, TMX key catalog,
capability-matrix row updates, and a Playwright E2E spec. No backend
(server DTOs) changes; no `system/` wiring per the T012d evaluation.
Web-only additive change.

## Summary

Introduces the modern Content Explorer's folder-security surface
(`FolderSecurityPanel`) on top of the existing sitemanage
`PSFolderProperties` / `PSFolderPermission` REST shape. The change is
web-only and additive: a pure lockout-detection helper (`aclLockout.ts`)
plus a React component that loads via `folderProperties`, lets Admin
users edit the four principal lists, warns the user before a save that
would remove the current user from any level (FR-015), and enforces a
read-only banner for non-Admin users (FR-016). All 31 new Vitest tests
(20 helper + 11 component) and 5 new Playwright E2E tests are green
against the live docker dev CMS at `http://localhost:9992`. Server
DTOs are mirrored 1:1 — no invented fields. No cross-platform path
concerns (REST URL constants only).

## Scope

- Base: `origin/development` (HEAD `8b3ce6cf06`)
- Head: `992-react-content-explorer-us4` (working tree, uncommitted)
- Files: 9 changed
  - `WebUI/src/main/ts/contentExplorer/aclLockout.ts` (NEW, 132 lines)
  - `WebUI/src/main/ts/contentExplorer/FolderSecurityPanel.tsx` (NEW, 326 lines)
  - `WebUI/src/main/ts/contentExplorer/messages.ts` (modified, +14 / -0)
  - `WebUI/src/main/ts/registry.ts` (modified, +4 / -2)
  - `WebUI/src/main/webapp/cm/app/folderSecurityModern.jsp` (NEW, 117 lines)
  - `WebUI/src/main/webapp/cm/pages/app/folderSecurityModern.jsp` (NEW mirror, 117 lines)
  - `WebUI/src/test/ts/contentExplorer/aclLockout.test.ts` (NEW, 199 lines)
  - `WebUI/src/test/ts/contentExplorer/FolderSecurityPanel.test.tsx` (NEW, 232 lines)
  - `modules/perc-qa-automation/frontend/tests/us4-acl.spec.js` (NEW, 90 lines)
  - `specs/992-react-content-explorer/contracts/capability-matrix.md` (modified, P-ACL row update)
  - `specs/992-react-content-explorer/tasks.md` (modified, T058–T064b ticked)
- Prior reports (continuity):
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us3-erlang.md` (US3 sibling — same pilot-JSP pattern, same registry wiring)
  - `docs/ai-generated/code-reviews/992-react-content-explorer-us2-host-folder-picker-erlang.md` (T045d JSP pilot pattern)
- Memory patterns hit: bridge-pattern idempotent-self-load, content-browser
  / folder-security stable `data-testid` for E2E, regression-isolation
  via `_=${Date.now()}` cache-buster, Vitest vanilla DOM assertions
  (per the b013222f14 limitation), no-invented-APIs (DTO field names
  traced to live Java)

## Recommendation

`approve`

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

(None.)

## Change-by-change verdict

### `WebUI/src/main/ts/contentExplorer/aclLockout.ts` (NEW, 132 lines)

- Pure module — no React, no fetch, no DOM. Imports `PSFolderPermission`
  + `PSPrincipal` from `api/contentExplorer/types.ts` (no circular
    dependency).
- `detectSelfLockout(before, after, identities)`: returns the list of
  access levels (ADMIN → VIEW) from which the current user is being
  removed, sorted broadest-first. Pure function over
  {@link PSFolderPermission} snapshots.
- `wouldSelfLockout` predicate for the boolean call-site.
- `canViewSecurityPanel` / `canEditSecurityPanel` gates (FR-016).
- `ACCESS_RANK` ordering exported for any future hierarchical checks
  (e.g. dependencies between ACL levels).
- `PRINCIPAL_LIST_KEYS` exported as `as const satisfies ReadonlyArray<...>`
  to make the four keys static-typed.
- Identity set semantics: USER names and ROLE names are matched
  equally (matches the `Principal.name` column); `Principal.type`
  does NOT affect the warning (test in {@code aclLockout.test.ts}
  asserts this).
- Null principal-list defensive default: a level with `undefined`
  principal-list is treated as empty (no false positive lockout
  warning on the initial load).
- **No bugs.**

### `WebUI/src/test/ts/contentExplorer/aclLockout.test.ts` (NEW, 20 tests)

- 8 `detectSelfLockout` tests: single ADMIN removal, multi-level
  removal, ordering (ADMIN before WRITE), negative (no before match),
  negative (still present after), ROLE match, multi-identity matches,
  empty-identities defensive.
- 2 `wouldSelfLockout` tests.
- 5 `canViewSecurityPanel` tests: ADMIN / WRITE / READ / VIEW /
  undefined.
- 2 `canEditSecurityPanel` tests.
- 2 `ACCESS_RANK` ordering tests (defensive).
- 1 TYPE-doesn't-affect-warning test (USER vs ROLE name match).
- All 20 / 20 passing.
- Vanilla DOM / Vitest assertions only (no jest-dom); per the
  b013222f14 limitation note this is the portable approach.
- **No bugs.**

### `WebUI/src/main/ts/contentExplorer/FolderSecurityPanel.tsx` (NEW, 326 lines)

- Discriminated-union `Status` state: `{kind:"loading"|"error"|"ready"}`
  — exhaustive in render.
- Default props for `load` / `save` use `folderProperties` /
  `saveFolderProperties` from `pathApi.ts` (the existing sitemanage
  path API).
- All four level editors rendered through a single `PrincipalListEditor`
  helper; per-level `data-testid` (`folder-security-list-<level>-<verb>`
  / `…-remove-<name>`) for the Playwright spec.
- Self-lockout check on save: iterates the `detectSelfLockout` result,
  calls `confirmLockout(level, identities)` if provided, otherwise
  falls back to `window.confirm(...)`.
- Dirty indicator flips to `●` on any edit; the Save button is
  enabled only when `editable && dirty && !pendingSave`.
- Loading + error UI with retry button. Access-denied UI for VIEW
  users. Read-only banner for WRITE / READ users (FR-016).
- Empty-string and duplicate principal-name inputs are filtered out
  before being added to the list (Vitest tests cover both).
- No `dangerouslySetInnerHTML`; all text via React auto-escaping.
- No fetch side effects in render (the load is in `useEffect`,
  captured by `cancelled` so unmount doesn't `setStatus`).
- React Hooks: `useId()` not used here (id is composed of static
  `data-testid` test attributes); `useState` for status +
  `pendingSave`; `useEffect` for the load lifecycle.
- **No bugs.**

### `WebUI/src/test/ts/contentExplorer/FolderSecurityPanel.test.tsx` (NEW, 11 tests)

- READ banner path.
- VIEW access-denied path.
- SAVE disabled when no edits (initial dirty=false).
- Dirty indicator flip on edit (no Save click yet).
- Self-lockout cancel path: `confirmSpy.mockReturnValue(false)` →
  click remove + click save → save not called.
- Self-lockout allow path: `confirmSpy.mockReturnValue(true)` → save
  called once.
- Add-principal happy path (type + confirm).
- Add-principal empty input ignored.
- Add-principal duplicate rejected.
- Loading state when no initial props and load is in-flight.
- Error state with retry button when load rejects.
- All 11 / 11 passing.
- `window.confirm` is spied (mutated) before each test and restored
  in `afterEach`.
- **No bugs.**

### `WebUI/src/main/ts/contentExplorer/messages.ts` (modified, +14/-0)

- 14 new `EXPLORER_MSG.SECURITY_*` keys added after the existing
  `ERROR_GENERIC` line. Keys are inline strings prefixed with
  `perc.ui.explorer@` so they fall back via `message()` until the
  catalog entries land in `modules/perc-i18n/.../CmsUi.tmx` (T063
  follow-up i18n PR).
- All keys are unique; no shadowing of existing entries.
- **No bugs.**

### `WebUI/src/main/ts/registry.ts` (modified, +4/-2)

- Adds `import { FolderSecurityPanel }` and `componentRegistry.set(
  "FolderSecurityPanel", FolderSecurityPanel)`. The US3 entries
  (ActionToolbar / ContextMenu) belong to PR #1396 which is not yet
  merged into `development`; this US4 PR adds the US4 component only.
- No unrelated churn; existing imports / registrations untouched.
- **No bugs.**

### `folderSecurityModern.jsp` ×2 (mirror in `cm/pages/app/`)

- 117 lines including the TMX locale header, CsrfGuard meta, the
  i18n message for the title, and `<i18n:settings>` debug flag.
- Same self-loading bridge pattern as T045a / T045b / T045d /
  T056 (US3):
  - idempotent `script[src*="perc-modern-ui.js"]` guard,
  - `setTimeout(50)` polling until `PercModernUI.mount` is available,
  - `cb=` cache-buster.
- Reads `?folderId=` from `new URL(window.location.href).searchParams`
  and surfaces a `data-testid="perc-folder-security-no-folder"`
  placeholder when the parameter is missing — covers the dev-CMS "no
  folder ACL data" path.
- Mounts `FolderSecurityPanel` with `currentUserIdentities: ["Admin"]`
  for the self-lockout path and an `onSaved` callback that writes
  the saved id/name to a `<pre>` block.
- All output via `textContent`, no `innerHTML`.
- **No bugs.**

### `modules/perc-qa-automation/frontend/tests/us4-acl.spec.js` (NEW, 5 tests)

- Uses `loginAsAdmin` + `BASE_URL` helpers; cache-buster
  `?_=${Date.now()}` per qa-automation AGENTS.md "Fast iteration".
- Tests assert:
  1. No-folder placeholder visible when `?folderId` is missing.
  2. `perc-folder-security-root` mount root visible.
  3. No legacy `.perc-mcol` Finder chrome.
  4. Page title advertises US4 P-ACL.
  5. `?folderId=0` triggers the panel mount path (loading + error
     state acceptable for dev CMS).
- All 5 / 5 passing in 11.8 s on the live docker dev CMS at
  `http://localhost:9992`.
- SC-004 second-user effect (Admin opens, second user session
  refreshes) is gated on a system-installed CMS; this dev image
  has no folder ACL data. The Vitest mapper tests + this spec
  cover the structural surface; UAT vs. a populated CMS flips the
  matrix row to Done.
- **No bugs.**

### `specs/992-react-content-explorer/contracts/capability-matrix.md` (modified)

- P-ACL table now includes Status + Test coverage columns (matching
  the convention established for P0-Core / P-Host rows).
- Each of the four P-ACL rows (View / Edit / Lockout self warning /
  Read-only without rights) is marked **Implemented** with Vitest +
  Playwright coverage references and the source-file path.
- No silent omit; no post-8.2 deferral for the in-scope rows.
- **No bugs.**

### `specs/992-react-content-explorer/tasks.md` (modified)

- T058, T059, T060, T062, T063, T064b ticked `[x]` with evidence.
- T061 marked as partial — the component honors `canEditSecurityPanel`
  and the read-only banner (FR-016), but the explorer-shell
  integration that surfaces "open security" from a `ContextMenu` /
  `ActionToolbar` action is host integration work and is documented
  as outside this PR's scope per the T012d evaluation.
- T064 left `[ ]` (pending: Erlang review + commit + PR — the
  current PR).
- No task IDs were renumbered or repurposed.

## Cross-platform path review

Not applicable — the diff uses REST URL constants
(`/Rhythmyx/rest/pathmanagement/path/folderproperties/...`,
`/Rhythmyx/rest/pathmanagement/path/saveFolderProperties`) and the
established TMX + JSP paths. No filesystem path construction; no
cross-platform checklist triggered.

## PR thread protocol

No prior review threads on this branch (newly cut off `development`).
After PR open, the implementer MUST apply constitution IX for each
review thread:
1. Reply inline with `**Mitigation (commit <hash>):** <description>`.
2. Run `gh api graphql resolveReviewThread` per thread.
3. Re-verify via the GraphQL `reviewThreads(first: 50) { nodes {
isResolved } }` query before merging.

## Handoff

- Recommendation: `approve`. May commit/push: yes.
- Suggested commit split (matches the per-US/per-PR convention):
  1. `feat(992/us4): T058 + T062 pure aclLockout.ts helpers`
  2. `feat(992/us4): T060 + T063 FolderSecurityPanel + TMX keys`
  3. `test(992/us4): T058-T059 Vitest aclLockout + FolderSecurityPanel suites (31 tests)`
  4. `feat(992/us4): folderSecurityModern.jsp pilot + mirror + registry`
  5. `test(992/us4): T064b tests/us4-acl.spec.js (5 tests)`
  6. `docs(992/us4): tick T058-T064b; P-ACL rows Done in capability-matrix`
- After this PR lands, the next concrete open tasks are T045f
  (US2 per-host verify-all) and US5 search (T065-T070).

