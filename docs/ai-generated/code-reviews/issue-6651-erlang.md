# Pre-push local code review — issue #6651

> NOTE: `mkd-code-review` is not installed in this cell (not on `PATH`;
> no `~/.local/bin`, no `~/.local/share/mkd`, no `~/.agents/skills/erlang`
> install; `uv`/`uvx` present but no review package configured). Per the
> night-issue-prs run instructions this section is header-marked
> **cli-unavailable, manual review**: the author performed the strict
> review pass below instead. Host Phase-10 Erlang re-review is the
> backstop. No in-diff finding below blocks the PR; no suppressions
> added.

## Scope

Branch `fix/issue-6651-explorer-generic-checkout-trio` vs `origin/main`.
Six files, all authored in this turn for #6651 (parent #4530):

- `WebUI/src/test/ts/contentExplorer/checkoutGenericItems.test.tsx` (new)
- `modules/perc-qa-automation/frontend/tests/explorer-checkout-generic.spec.js` (new)
- `product-docs/8.2/admin/content-explorer.md` (4 table-row wording updates)
- `docker/scripts/qa_preflight.py` (provenance-check false-positive fix)
- `docker/scripts/test_qa_preflight.py` (2 regression tests)
- `projects/sitemanage/src/main/java/com/percussion/dashboardmanagement/dao/IPSDashboardDataRepository.java`
  (missing `package` statement restored)

No WebUI production sources, no REST/sitemanage production logic, no
`paths.ts` / `messages.ts` / `sitemanage-beans.xml` / registration-test
changes — the checkout-trio enablement + dispatch for generic rows
already shipped on main (#6580 reversal, PR #6653 merged) and is locked,
not re-gated, here.

## Findings

No blocking bugs. No missing behavioral tests. No non-portable paths.

1. **Vitest slice** — asserts `resolvePublishKind(generic()) === "none"`
   (the premise), item-scoped enablement (`isToolbarCheckoutHidden` /
   `isToolbarForceCheckinHidden` false; trio present in toolbar +
   context-menu filters), injection without duplication, single Check In
   via the editor endpoint with trimmed comment, single Force Check-in
   on the default REST path, multi-select inclusion of generics with
   folders skipped, and `CheckoutOwnerPanel` for a generic row behind
   `renderA11yGate`. `afterEach` restores all mocks; no cross-test
   leakage. 7/7 green standalone, paired, and in the full run.
2. **Playwright surface** — hybrid proof documented in the header:
   mocked kind-`"none"` row shape (stable H2 seed has no generic
   folder) with a REAL seeded custom-type id; every checkout call hits
   the real endpoint (`waitForResponse` registered before the click;
   `ok()` asserted; `explorer-server-actions-error` must stay absent).
   No hardcoded ports/credentials (`BASE_URL`, `adminBasicAuthHeaders`);
   routes unrouted and folders recycled in `finally`; dialog accept is
   the established force-checkin pattern; a11y gate scoped to the shell;
   console errors filtered by the established helper; live-proof PNG at
   the assertion point.
3. **Product docs** — prose-only updates to 4 existing action-table rows
   (`Checked out by`, `Check Out`, `Check In`, `Force Check-in`) naming
   generic Items alongside pages/assets. No frontmatter/id/nav change;
   far from the sibling #6814 visibility-table hunks (no merge overlap).
4. **Provenance-check fix** (`qa_preflight.py::_java_source_index`) —
   indexes declared top-level type names per `.java` file (column-0
   declaration regex, comment-stripped) in addition to file stems, so
   package-private sibling types (sitemanage `PSStreamingOutput`,
   `HashCalculator`, `PropertyId`, `PageSaveRunner`, `PerformanceStats`)
   no longer flag as branch-built. Genuinely foreign classes still flag
   (existing `PipelineMapper` tests green; new commented-declaration
   test locks strictness). `pathlib` only; no separators; LF preserved.
5. **Package statement** (`IPSDashboardDataRepository.java`) — the file
   missed its `package` line, compiling to a default-package class at
   the jar root (the last preflight flag). Zero usages repo-wide proven
   by grep, so restoring the package is behavior-neutral; the packaged
   FQN now resolves instead of a dead root entry.
6. **Copyright headers** — both new files carry
   `Copyright (c) 2026 Intersoft Data Labs, Inc.` + Apache 2.0.
   The edited `.java` keeps its pre-2023 Percussion header (untouched
   per the copyright gate). No rule/instruction files touched.

## Behavioral coverage

- `checkoutGenericItems.test.tsx`: 7 tests (enablement, single +
  multi dispatch, owner panel + a11y gate).
- `explorer-checkout-generic.spec.js`: 2 H2 tests (checkout +
  editor check-in; checkout + force check-in) with zero-console-error
  and a11y assertions.
- `test_qa_preflight.py`: 2 regression tests (sibling-type clears;
  commented declaration still flags). Full docker-scripts suite: 39
  passed.

## Portability

No filesystem path construction in any touched file. Python changes
use `pathlib`; the spec uses URL helpers; the Vitest file asserts no
path shapes; docs are prose.

## Gate

Manual review: **PASS** — no in-diff bug, no missing behavioral test,
no portability violation. `mkd-code-review` CLI unavailable in this
cell (disclosed, not substituted); host Erlang re-review requested.
