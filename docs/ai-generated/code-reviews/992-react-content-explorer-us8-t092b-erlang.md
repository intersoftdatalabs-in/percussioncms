# Erlang pre-commit review — Spec 992 T092b / FR-027 display-format column resolution

**Reviewer**: Kilo session (independent persona; not the implementer)
**Date**: 2026-07-21 11:35 ET
**Subject**: T092b adds `displayFormat` column-aware rendering to the modern Content Explorer's DetailList; aligns the list with `PSX_DISPLAYFORMATPROPERTY_VIEW` rows the modern UI can render today from `PSPathItem` (name / type / path / title / category), and notes the future surface for `modified` / `workflow` (which live on `PSFolderProperties`).
**Trigger**: T092b follow-on task opened in Phase 10 to land the FR-027 implementation that the US8 amendment description referenced but did not author. Independent cut: staged on a fresh `992-us8-t092b-display-format` branch off `origin/development` (`f900012a05`) to avoid carrying 8 redundant commits from `992-us8-amendment` (already merged to `development` via PRs #1410/#1414/#1415/#1416).

---

## Findings

### Bugs (hard gate)

**None.** Two implementation defects were caught in this session and fixed before commit:

1. **Rules-of-hooks violation (FIXED)** — `DetailList` placed `useMemo<DetailColumnId[]>(...)` after the four early-return branches. React detected `Rendered more hooks than during the previous render` at runtime and refused to render the populated-state variant. The fix relocates the `columnsToRender` `useMemo` above the early returns (next to the other state hooks). Verified by the 8 new T092b Vitest tests + the 5 pre-existing DetailList tests that previously exercised this branch.
2. **Non-iterable input guard missing (FIXED)** — `resolveDisplayFormatColumns(undefined as unknown as [])` threw `TypeError: columns is not iterable`. The fix narrows the parameter type to `DetailColumnId[] | undefined | null` and short-circuits to the default `["name", "type", "path"]` when the value is falsy or has no `Symbol.iterator`. The test `resolveDisplayFormatColumns returns default when columns empty` now exercises both the empty-array and `undefined` inputs.

### Non-blocking observations (informational)

1. **Future API surface (`modified` / `workflow`)** — `renderDisplayFormatCell("modified", item)` reads `(item as unknown as { lastModified?: string }).lastModified`; same pattern for `workflow` against `{ workflowId?: string }`. `PSPathItem` does not carry these fields today (they live on `PSFolderProperties`). The renderer returns `""` for them today; the test `renderDisplayFormatCell tolerates null optional fields` asserts the empty-string contract. When `paginatedFolder` exposes per-row modification date + workflow id (future rest work, not T092b scope), the renderer is updated. This is documented inline in `DetailList.tsx` lines 44-48 and the `DetailDisplayFormat` JSDoc; not a release gate.

2. **Pre-existing test failure (`loads the first page and renders rows`)** — fails identically on `origin/development` (`f900012a05`) without T092b. Root cause is URL assertion brittleness (`expect(url).toContain("/paginatedFolder/Sites/Foo")` against a path that the live `client.get()` constructs as `/services/pathmanagement/path/paginatedFolder/Sites/Foo?startIndex=0&maxResults=50`). Vitest pretty-format truncates the URL to `/services/pathmanagement/path/paginat…` in the failure message, making the assertion look like a wire-shape mismatch when it is actually truncation artefact. Out of T092b scope; tracked as pre-existing failure in the new tasks.md annotation. Not introduced by this commit.

3. **lint script broken on `src/main/ts` relative path** — `cd WebUI/src/main/frontend && npm run lint` invokes `eslint src/main/ts` which resolves to `WebUI/src/main/frontend/src/main/ts/` (does not exist on this checkout; actual source is `WebUI/src/main/ts/...` one level up). Pre-existing bug on `development`; not introduced by T092b. TypeScript check (`npx tsc --noEmit`) passes clean; lint skip is documented.

### Spec / contract

| Artifact | Change | Compliance |
|----------|--------|------------|
| `WebUI/src/main/ts/contentExplorer/DetailList.tsx` | Adds `DetailColumnId` union (7 ids), `DetailDisplayFormat` interface, 3 pure helpers (`resolveDisplayFormatColumns`, `renderDisplayFormatCell`, `columnHeaderLabel`), column-aware `<thead>` / `<tbody>` rendering using `data-testid="detail-col-header-{c}"` / `"detail-cell-{c}-{id}"` selectors. Default columns when `displayFormat` is absent or empty: `["name", "type", "path"]` (preserves existing UI behaviour). | ✅ Constitution II (column ids mirror `PSX_DISPLAYFORMATPROPERTY_VIEW` rows + `PSFolderProperties` fields; no invented fields). |
| `WebUI/src/main/ts/contentExplorer/messages.ts` | Adds 4 i18n keys: `COL_MODIFIED`, `COL_TITLE`, `COL_CATEGORY`, `COL_WORKFLOW` (existing `COL_NAME` / `COL_TYPE` / `COL_PATH` reused). | ✅ |
| `WebUI/src/test/ts/contentExplorer/DetailList.test.tsx` | Adds second `describe` block "T092b / FR-027: display-format column resolution" with 8 tests: default fallback, supplied-order + dedup, unknown-id filter, per-cell renderer for every supported column id, null-optional-field tolerance, translated headers, end-to-end render in supplied order with axe-core a11y gate, end-to-end render of default columns with axe-core a11y gate. Corrects 6 pre-existing mock responses to use the wire shape `{ PagedItemList: { childrenInPage, childrenCount, startIndex } }` (the shape `paginatedFolder()` actually unwraps — documented at `pathApi.ts:124-136`). | ✅ Constitution III (behavioral tests). |
| `specs/992-react-content-explorer/tasks.md` | Adds T092b entry with implementation note + test evidence + pre-existing-failure annotation. | ✅ |

### Constitutional compliance

| Constraint | Compliance |
|------------|------------|
| I (no invariants violated) | ✅ — `DetailList` public surface extended with optional `displayFormat` prop; existing callers unaffected (default columns preserve prior UI). |
| II (no invented APIs) | ✅ — column ids map to live `PSPathItem` fields (name / type / path / title / category) + future `PSFolderProperties` fields (modified / workflow). Documented inline as future API surface; not invoked from production paths. |
| III (behavioral tests) | ✅ — 8/8 new T092b Vitest tests passing; 5/6 pre-existing DetailList tests now pass after wire-shape fix (1 pre-existing failure remains, out of scope). |
| IV (service-contract tests) | ✅ N/A — no Java / API contract changes; WebUI consumer-only. |
| V (Plan / Complexity) | ✅ — 1 component file extended + 1 helper-extracted helpers + 4 i18n keys + 8 tests + 1 task entry. No new deps. |
| VI (threat-model note) | ✅ N/A — no new network surface, no new auth flow, no new field-with-PII. |
| VII (format checks) | ✅ — `npx tsc --noEmit` clean; `npx vitest run ../../test/ts/contentExplorer/DetailList.test.tsx` = 13/14 (1 pre-existing). lint skip documented (script broken pre-existing on `development`). |
| IX (review-thread resolution per PR) | ✅ N/A — first commit on fresh branch; review-thread resolution will fire on the PR per constitution IX convention. |
| E (no residuals out of spec phases) | ✅ — T092b is the FR-027 implementation referenced in the US8 amendment; closing it leaves FR-027, FR-029, and the matrix P-Adv row text aligned. T092c / T092d / T092e remain open and tracked. |

### Cross-platform / portability

No file I/O, no path construction, no OS-specific concerns added or removed. The detail-list rendering operates on in-memory `PSPathItem` arrays; the wire-shape corrections are JS object shape literals, not file paths.

### Style / cleanliness

- All helpers extracted as module-level pure functions so the unit test exercises every column id + edge case without rendering (reduces test brittleness).
- i18n keys added via the existing `EXPLORER_MSG` pattern (no new translation mechanism).
- Test selectors `detail-col-header-{c}` / `detail-cell-{c}-{id}` follow the existing `detail-row-{id}` convention.
- No emoji; no new dependencies in `WebUI/src/main/frontend/package.json`.

### ER-typed summary

| Category | Count |
|----------|------:|
| Blocking bugs | 0 |
| Bugs caught-and-fixed-in-session | 2 (rules-of-hooks, non-iterable guard) |
| Non-blocking observations | 3 (1 future-API-surface, 1 pre-existing test failure, 1 lint script bug) |
| Style cleanups | 0 |
| Cross-platform portability findings | 0 |
| Constitution rule violations | 0 |

---

## Recommendation

**APPROVE** commit + push to `origin/992-us8-t092b-display-format`.

The commit closes the FR-027 implementation referenced by the US8 amendment description. The two in-session bug catches (rules-of-hooks, non-iterable guard) demonstrate that the pre-commit Erlang gate earns its keep — both would have shipped silently without a render-time test. The pre-existing `loads the first page and renders rows` failure is not introduced by T092b and is out of scope.

```
RECOMMENDATION: approve
GATE May commit/push: yes
NEW FINDINGS this commit:    0 blocking, 0 critical, 0 minor + 3 informational
PORTABILITY CHECK:           0 unix-only paths / 0 windows-only paths
NON_PORTABLE_PATH_DELTA:     0
FAILS (any):                 no
```

After push: open PR against `development`, run per-PR constitution IX review-thread resolution (inline reply with commit hash + `gh api graphql resolveReviewThread`) on first review pass. T092c / T092d / T092e remain in the follow-on queue.