# Erlang review — issue 4646 EditorHost create item

Scope: uncommitted branch `fix/issue-4646-editorhost-create-item` vs `origin/main`.
Independent of implementer intent. Sub-agent spawn is not available in this host tool set; this report follows `erlang-code-review.md` + SKILL.md.

## Summary

EditorHost gains a New-item form (type + folder) that POSTs existing `createEditorItem` (`/services/itemmanagement/item/create`) and lands on the new `contentId`. Reuses existing REST; no new Java API. Vitest covers ready/incomplete/403. Playwright stubs create + 403. Product-docs updated. Paths in helpers are URL `/` only.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Cross-platform path checklist

- No OS filesystem joins; Playwright helper comments URL `/` only.
- Tests assert URL path segments, not `File.separator`.

Memory patterns hit: map 403/400/404 as failure not success; land on new id after create; do not treat missing type/folder as REST success.
