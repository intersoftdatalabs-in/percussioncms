# Erlang Night Review — PR #6159 (issue #6096)

**Status:** `LGTM-REVIEW-ONLY` (no merge — own-model ban: this PR carries
`model:muse-spark-1.3-contributor` + `operator:opencode`, same as this run).
**Reviewer:** Erlang (independent pass) · **Date:** 2026-11-12 UTC
**Scope:** `origin/main` → PR head `0097890268` · 8 files · machine gate: **0 findings, 0 blocking bugs**
(`mkd-code-review analyze --pack percussion`, persona `erlang 0.1.1`, Ollama dev-coder)

```
ERLANG_VERDICT: LGTM-REVIEW-ONLY (no merge per own-model ban)
```

## Verdict

No in-diff bugs found. Machine analysis (0 findings) agrees with this independent
manual pass. The PR is left open for human morning merge — **not approved, not merged**
by this reviewer per the own-model hard constraint.

## Findings

| # | File:line | Severity | Notes |
|---|-----------|----------|-------|
| 1 | PR body text | nit | Says `` `/category/all/undefined` → 404 ``. Imprecise attribution: the server explicitly normalizes `"undefined"→null` on GET (`PSCategoryService.getCategoryListWithStrng`) and `preserveDeletedNodes` treats `null`/`"undefined"` identically on update — so the suffix was behavior-neutral. The 404/never-load came from the wrong JAX-RS bus prefix (`/services/category/*`, no such server) fixed by the `categorymanagement/` prefix. Code is correct; description wording only. Non-blocking. |

No `bug`, no `suggestion` rows. Residual notes below are out-of-scope by design.

## Independent verification (all pass)

1. **PATHS.CATEGORY_\* prefix fix — CORRECT.** `sitemanage-beans.xml:272`
   `<jaxrs:server address="/categorymanagement">` + `PSCategoryService @Path("/category")`
   ⇒ live endpoints are `/services/categorymanagement/category/*`. Matches legacy
   `perc_path_constants.js` (`CATMGT + "/category/all"`) and the existing
   `PSCategoryServiceRestClient` (`"/services/categorymanagement/category"`).
2. **`/undefined` removal — CORRECT, behavior-preserving.** Bare `@Path("/all")` and
   `@Path("/update")` exist server-side; old suffixed variants normalized to the same
   semantics. `CATEGORY_LOCK_TAB` trailing `/` preserved; call site still appends
   `{date}` matching `@Path("/locktab/{date}")`.
3. **Spec↔product wiring — all 16 sampled `data-testid`s resolve** in
   `AdminShell`, `RolesSection`/`RoleEditor`, `UsersSection`/`UserEditor`/`LdapImportDialog`,
   `CategoriesSection`, `EditorWorkflowPanel`. The us5 cancel selector
   `dialog.locator("button.perc-button-secondary")` matches exactly one button
   (strict-mode safe). us4 helper imports (`TEST_IDS`, `editorSpaUrl`,
   `parseTransitionWithCommentsUrl`, …) all exported by
   `tests/helpers/editor-host-workflow.js`; `expectNoSeriousA11yViolations` exists.
4. **Cross-platform — clean.** New specs contain no `path.join`/`__dirname`/`/tmp`/drive-letter
   constructs; `/` appears only in URL paths and CSS selectors (correct usage).
5. **Copyright headers — correct.** All 4 new specs carry
   `Copyright (c) 2026 Intersoft Data Labs, Inc.` + Apache 2.0 block.
6. **Coverage map accuracy — verified.** US1/US7/US8 pre-existing spec files exist on disk;
   US5 dual-row (`us4-item-transitions` + `editor-host-workflow-transitions`) is real;
   US2-partial and US5-host caveats are honestly documented, with follow-ups filed as
   #6157 (US2 assign flow) and #6158 (WorkflowActionsPanel mount-or-remove) — both OPEN.
7. **product-docs N/A — accepted.** Bug fix restoring intended endpoints + test-only specs;
   no new operator-facing surface.
8. **Non-mutating E2E discipline — good.** Role/user/category specs open dialogs, assert
   validation, and cancel; us4 stubs the backend at the route layer and asserts zero
   legacy-CE requests plus zero console/page errors per test.

## Residuals (out-of-scope, not blocking)

- #6157 (US2 site-assign dialog E2E) and #6158 (`WorkflowActionsPanel` unmounted) — already
  open, linked from the coverage map. No new residual issues filed (at most 3 allowed;
  zero qualifying rows beyond these two tracked items).

## Build evidence (as claimed in PR body; not re-run by this reviewer)

- WebUI `mvnw clean install` BUILD SUCCESS (Vitest 902 files / 8648 passed; Java 73 tests).
- eslint clean on `paths.ts` + `CategoriesSection.tsx`.
- QA H2 live: 13 passed across the 4 new specs (+ golden smoke 15 total), zero console errors,
  zero server.log ERROR/FATAL in the verification window.

---
*Independent Erlang review — findings only. Merge/approve left to human morning review.*
