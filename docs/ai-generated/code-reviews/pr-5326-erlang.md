<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5326

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5326
- Base: origin/main
- Head: 2f0f4887ef5d7ac882aba3af517c46197ff6b77e
- Files analyzed: 16
- In-diff bugs: 0 (preexisting rows do not block)
- Reviewer: independent Erlang pass. Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 16 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/RoleAdaptor.java:243 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `addRoleUser` cognitive=18 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Interpreter

Independent read of the diff (not the author). `PUT /services/roles/?addUser=true` calls `IRoleAdaptor.addRoleUser` and rejects combined create/update/homePage/addUser flags. `RoleAdaptor.addRoleUser` requires Admin, exactly one user name, an existing role, and a catalog user. It copies the stored description, home page, and members, appends the catalog spelling, and does not apply a client description or home page. Unknown user is HTTP 400, already-a-member is HTTP 409, missing role is HTTP 404, and none of those call `roleService.update`. The production `@Autowired` constructor injects `IPSUserService`. The four-arg test constructor that passes a null user service is not the Spring bean. The panel updates the member list only after a successful read-back and restores the previous list on failure. Description and home-page saves still omit `users`. Companions are present: `RolesResource` plus `RoleTestAdaptor` and `RolesResourceAddUserTest`, `RoleAdaptorAddUserTest`, Vitest, Playwright `developer-roles-add-user.spec.js`, and product-docs under `product-docs/8.2/admin/developer-roles.md` and `product-docs/8.2/developer/rest.md`. No new filesystem path joins. No agent rule files.

The in-diff machine row is `complexity.cognitive` on `addRoleUser` (cognitive 18). Severity suggestion, not a bug.

Recommendation: approve. May commit/push: yes.
