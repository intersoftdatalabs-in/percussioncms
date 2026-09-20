# Erlang review — issue 4605 EditorHost related content list

**Scope:** uncommitted/branch `fix/issue-4605-editorhost-related-content` vs `origin/main`.
**Spawn note:** host has no separate Erlang spawn tool; this pass follows `erlang-code-review.md` + `skills/erlang-review/SKILL.md` on the branch diff.
**Memory patterns hit:** change-class completeness (WebUI + Playwright + product-docs); explicit 403/empty; no REST-only slice.

## Summary

Browse-only related content list on EditorHost using existing `GET .../slot-relationships/canvas` and `GET .../content-explorer/relationships/{id}/local`. Flatten helper maps 403 vs other errors. Empty and forbidden copy are explicit. Vitest covers flatten, list, empty, 403. Playwright surface spec mocks those endpoints. Product-docs updated on `product-docs/8.2/admin/content-explorer.md` (not thrash `developer/index.md` / `rest.md`).

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None (bugs / missing behavioral tests / non-portable paths).

Cross-platform path checklist: N/A (no filesystem I/O; URL paths use `/`).
