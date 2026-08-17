# Erlang review — #3504 Role editor available users

- **Branch:** `fix/issue-3504-role-editor-available-users`
- **Date:** 2026-08-17
- **Reviewer:** Erlang (pre-commit, implementer session)
- **Recommendation:** approve
- **May commit/push:** yes
- **Gate:** pass

## Summary

Admin Role editor cleared **Available Users** after the first Add because
`RoleEditor` re-POSTed `/rolemanagement/role/availableUsers` on every
`assignedUsers` change and treated any bind/unwrap/error as `[]`. The fix
loads all users once via `GET /user/user/users` and filters locally
(`availableUsersMinusAssigned`), matching the legacy create-role path.

Change class: WebUI product screen (Admin → Roles membership dual-list).

Companions present:

- Behavioral Vitest for helper + RoleEditor add-two / remove-returns
- Playwright `tests/bugs/bug-3504-role-editor-members.spec.js`
- `product-docs/8.2/admin/users-roles.md` operator steps
- No REST envelope change; sitemanage not required

Memory patterns hit: missing behavioral tests (covered); change-class
closure (Playwright + product-docs); empty catch (GET failure now surfaces
`formatApiError`).

Cross-platform path checklist: N/A — no filesystem path I/O in the diff
(URL path `/user/user/users` is a REST path).

## Issues

None blocking.

### nit

- Role editor still has a few pre-existing hardcoded English strings
  (`Description`, `Add`, `Remove`). Out of scope for this membership fix.

## Test evidence (at review)

- Focused Vitest: 10 passed (`RoleEditor`, `roleUsers`, `RolesSection`)
- Standalone `cd WebUI && ../mvnw.cmd clean install`: BUILD SUCCESS,
  Tests 2664 passed
