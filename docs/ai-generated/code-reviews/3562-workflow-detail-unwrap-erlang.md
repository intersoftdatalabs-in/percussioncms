# Erlang review — #3562 Workflow detail missing workflowName unwrap

**Branch:** `fix/issue-3562-workflow-detail-unwrap`  
**Date:** 2026-08-18  
**Reviewer:** Erlang (pre-commit, independent of implementer)

## Summary

`getWorkflowDetail` required a top-level `workflowName` on the GET
`/services/workflowmanagement/workflows/{name}` body. Jackson WRAP_ROOT /
`@XmlRootElement(name = "Workflow")` returns `{ Workflow: { workflowName, … } }`,
so Developer workflow detail failed with **Workflow response missing
workflowName** and QA could not complete #2640 step 5 (no Object ACL).

The change adds `parseWorkflowDetail` that mirrors list unwrap: `Workflow`
root, nested wrappers, one-item arrays, and the `name` alias. Catalog open
buttons now expose `data-wf-name`. Vitest covers wrapped payloads; Playwright
smokes Default/Simple detail and asserts no Object ACL section.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No bugs, no missing behavioral tests for the unwrap logic, no non-portable
path/file I/O.

## Cross-platform path checklist

Not applicable — no filesystem path joins, temp dirs, or path assertions.

## Memory patterns hit

- Behavioral unit tests for new/changed non-trivial logic (`parseWorkflowDetail`)
- Change-class closure: Vitest + Playwright companion + product-docs REST note
- Do not treat focused `-Dtest` as sufficient — full `WebUI` `mvnw clean install` green

## Issues

None (hard-gate).

### Notes (not blocking)

- List parse is unchanged; catalog already binds `workflowName` from metadata.
- Workflow still has no Object ACL / GUID (out of #2640 / #3562 scope).

## Build evidence (C1)

- `cd WebUI && ../mvnw.cmd clean install` → **BUILD SUCCESS**
- Vitest (Maven test phase): Tests 2862 passed / 380 files
- Playwright: `npm run test:surface -- --path tests/bugs/bug-3562-developer-workflow-detail.spec.js` → **1 passed**
  (H2 QA `perc-matrix-cms-h2`, `TEST_CMS_URL=http://127.0.0.1:9993`; console-clean=yes;
  FastForward `PSDbStorageService` import + search-index last-modifier ERRORs pre-exist / not feature-related)
