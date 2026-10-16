# Erlang review — issue 5472

Independent review of `fix/issue-5472-create-name-nul` (EditorHost refuses a NUL in the new item name). The author report is not this review. Recommendation: **approve**. In-diff bugs: 0. Missing behavioral tests: 0. Non-portable paths: 0.

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Independent reading

`createNameContainsNul` is true only when the new item name contains U+0000. A blank or whitespace-only name is not a NUL, and `buildEditorCreateRequest` still omits that blank name. `handleCreate` returns before `createItem` and before `allowLeave` when the name contains a NUL, shows **That name contains a character that cannot be saved.**, and leaves the create panel open with the content type and folder unchanged. A normal name still creates and opens the item. HTTP 400, 403, and 404 stay on the existing failure path and do not show a content id. Closing the panel does not create. Vitest covers the helper and the host. Playwright (`editor-host-create-name-nul.spec.js`) and `product-docs/8.2/admin/content-explorer.md` are present. No filesystem path joins. No rule files.

Gate: PASS. May commit/push: yes.
