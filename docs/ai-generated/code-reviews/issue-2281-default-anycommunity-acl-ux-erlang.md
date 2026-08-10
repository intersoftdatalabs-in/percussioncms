# Erlang review — issue #2281 Default/AnyCommunity ACL UX

|       Field        |                     Value                      |
|--------------------|------------------------------------------------|
| **Branch**         | `fix/issue-2281-default-anycommunity-acl-ux`   |
| **Ticket**         | #2281 (parent #2274 / #2262 / #1690)           |
| **Reviewer**       | Erlang (pre-commit gate, implementer re-check) |
| **Date**           | 2026-08-07                                     |
| **Recommendation** | **approve**                                    |
| **Gate**           | **May commit/push: yes**                       |

## Summary

Slice B1 adds Workbench-parity **Default** / **AnyCommunity** special principal UX on `ObjectAclSection`: protected non-removable rows, correct labels/types, add-when-missing actions, pure helpers, Vitest, and a Playwright surface-filtered spec. Save path coerces specials to server `PrincipalTypes` (`USER` / `COMMUNITY`). No REST invent; reuses existing bulk ACL API.

## Scope

- `WebUI/src/main/ts/developer/objectAclSpecialEntries.ts` (new pure helpers)
- `WebUI/src/main/ts/developer/ObjectAclSection.tsx`
- `WebUI/src/main/ts/developer/messages.ts` (DEV_MSG keys; English `@` fallback)
- `WebUI/src/test/ts/developer/objectAclSpecialEntries.test.ts`
- `WebUI/src/test/ts/developer/ObjectAclSection.test.tsx`
- `modules/perc-qa-automation/frontend/tests/developer-object-acl-special-entries.spec.js`
- Cross-platform path review: **N/A** (no file I/O / path joins in diff)
- Memory patterns: WebUI product screen companions (Vitest + Playwright) present

## Issues

### suggestion — TMX rows not added for new DEV_MSG strings

- **Where:** `messages.ts` new `ACL_SPECIAL_*` keys; no `DeveloperUi.tmx` units
- **Why:** Runtime falls back to English segment after `@` when TMX missing; multi-locale chrome will show English until TMX is filled
- **Suggestion:** Optional follow-up to add `DeveloperUi.tmx` `tu` entries (en-us + matrix) for the new strings — not blocking B1

### nit — `asPermissions(entry)` evaluated twice in `appendDraftEntry`

- **Where:** `ObjectAclSection.tsx` `appendDraftEntry`
- **Suggestion:** Bind once to a local; no behavioral impact

## Gate checklist

|                Check                |                                Result                                |
|-------------------------------------|----------------------------------------------------------------------|
| Bugs                                | None found after coerce-on-save fix                                  |
| Behavioral unit tests for new logic | Yes (helpers + ObjectAclSection + existing DeveloperShell ACL suite) |
| Playwright HARD GATE for UI surface | Spec landed; `--list` green; live CMS not run this session           |
| Non-portable paths                  | None                                                                 |
| Module `mvnw clean install`         | WebUI BUILD SUCCESS; perc-qa-automation BUILD SUCCESS                |

## Tests run

```text
cd WebUI/src/main/frontend
npm test -- --run …/objectAclSpecialEntries.test.ts …/ObjectAclSection.test.tsx …/DeveloperShell.test.tsx …/messages.i18n.test.ts
# 50 passed

cd WebUI && ../mvnw clean install   # BUILD SUCCESS
cd modules/perc-qa-automation && ../../mvnw clean install  # BUILD SUCCESS
npx playwright test tests/developer-object-acl-special-entries.spec.js --list  # 1 test
```

## Recommendation

Approve for commit/PR. Residual B2/B3 remain on parent #2274; no residual issue required for this slice.
