# Erlang review: #3088 fold Workflow admin under Admin shell

**Branch:** `fix/issue-3088-fold-workflow-admin-under-admin`  
**Date:** 2026-08-11  
**Reviewer:** Erlang (pre-commit gate, implementer self-review)

## Summary

Folds Workflow / Roles / Users / Categories administration into the unified
`AdminShell` under `/admin/*`, redirects legacy `/workflow` routes and
`view=workflow` deep links, removes sibling shell cross-links, and updates
unit + Playwright + product-docs companions.

## Scope

- WebUI SPA: `AdminShell`, `WorkflowAdminShell` (redirect stub), `WorkflowRoute`,
  `AdminRoute`, `allowlists`, `parseEntryQuery`, `topNavConfig`, `TopNav`, CSS
- Server deep-link allowlists: dual-tree `index.jsp` (`ADMIN_TABS` + workflow view)
- Tests: admin / workflowAdmin / App / topNav / parseEntryQuery
- Playwright: top-nav, us1 workflow defs, bugs 945/2211/2959
- product-docs: `product-docs/8.2/admin/index.md`

**Out of scope (intentional):** item `workflowActions/*`, Developer → Workflows,
legacy jQuery PercWorkflowView.

**Cross-platform path review:** no new path/file I/O; JSP string path segments
only; dual-tree `index.jsp` kept byte-identical via copy.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Issues

### suggestion

1. **AdminShell tab count (8 tabs)** — portrait wrap is covered by GH-945 Playwright
   and CSS `flex-wrap`, but dense chrome may still feel crowded. Acceptable for #3088;
   a later polish could group “system ops” vs “identity/workflow” if UX feedback
   warrants it. No change required for this PR.

2. **In-shell tab clicks do not update the URL** — pre-existing pattern for both
   Admin and former Workflow shells (state-only tabs). Deep links still work via
   `/admin/:tab`. Optional follow-up: sync `activeTab` to router on click.

### nit

1. `WorkflowAdminShell` remains in the component registry as a redirect stub for
   residual loads; acceptable per issue (“reduce to non-product embed/redirect”).

## Test evidence

- `cd WebUI && ../mvnw.cmd clean install` → BUILD SUCCESS (war produced;
  Surefire Java 43 tests, 0 failures)
- Focused Vitest: AdminShell, WorkflowAdminShell, App, topNav*, parseEntryQuery
  → 46 passed

## Memory patterns

None hit for dual-shell chrome specifically; change-class companions (unit +
Playwright + product-docs) applied for WebUI product screen IA change.
