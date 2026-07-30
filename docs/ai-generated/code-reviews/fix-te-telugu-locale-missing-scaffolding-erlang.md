# Erlang Review — fix/te-telugu-locale-missing-scaffolding

- **Branch**: `fix/te-telugu-locale-missing-scaffolding` (off `origin/development` @ `bfa50b1df8`)
- **Scope**: uncommitted working tree (4 files). No prior commits on this branch.
- **Reviewer persona**: `modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`
- **Skill memory loaded**: `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`
- **Prior topic report**: `docs/ai-generated/code-reviews/feat-te-telugu-locale-erlang.md` (approved; merged as PR #1685).
- **Cross-platform path checklist**: not applicable (no path/I/O code introduced; XML seed data + TS data map only).

## Summary

Follow-up fix for two scaffolding rows that were missed from the merged Telugu
PR (#1685 / commit `2ab8e37cd1`):

1. **`WebUI/src/main/ts/login/localeLabels.ts`** — adds `te: "తెలుగు"` to
   `SHIP_LOCALE_ENDONYMS` so the login locale dropdown labels the Telugu option
   with its own native endonym instead of falling back to the server English
   `displayName` ("Telugu"). Inserted alphabetically between `pt-pt` and `tr-tr`,
   matching the existing convention.
2. **`modules/perc-distribution-tree/.../cmsTableData.xml`** — adds the
   matching `LOCALECONTENTSTYLE` row for `LANGUAGESTRING=te` (TEXTDIR, date/time
   patterns, decimal/grouping separators, INR currency, Asia/Kolkata TZ,
   latn numbering, gregory calendar). Inserted alphabetically between `pt-pt`
   and `tr-tr`. Values mirror the sibling `hi` / `hi-in` India-localized rows
   (`dd/MM/yyyy`, INR, metric, gregory, latn) with `DEFAULTTZ=Asia/Kolkata`
   (more accurate than the blank on `hi`/`hi-in`).
3. **`WebUI/src/test/ts/login/localeLabels.test.ts`** — adds an explicit
   `expect(localeLabel("te", "en-us", "Telugu")).toBe("te - తెలుగు")` to the
   `uses curated ship endonyms for the product locale matrix` block. The
   `covers every key in SHIP_LOCALE_ENDONYMS` test already iterates the full
   map, so the new entry is exercised there too.
4. **`modules/perc-qa-automation/frontend/tests/bugs/bug-1608-1609-login-locale.spec.js`** —
   extends the existing GH-1608 endonym-stability test with
   `expect(joinedBefore).toMatch(/^te\s*-\s*తెలుగు/m)` so the live CMS Playwright
   run verifies the new option label is rendered.

Net effect: `te` is now first-class in the runtime locale dropdown
(user-visible endonym) and in the per-locale formatting table (date/time/TZ).
The `RXLOCALE` row + TMX `<tuv>` translations that *did* land in #1685 are
unchanged.

## Recommendation

**approve**

All hard-gate checks pass. The diff is data-only (TS map entry, XML seed row,
two small test assertions). The change is scoped strictly to the
Telugu-related files identified by the user; no incidental reformatting.

## Gate

- Blocking bugs: 0
- May commit/push: **yes**

## Issues

### Bugs

None.

The behavioral-test gate is satisfied: `localeLabels.test.ts` already iterates
`SHIP_LOCALE_ENDONYMS` (so `te` is exercised) and the new explicit assertion
documents the contract. The Playwright spec is the live-CMS regression guard
required by `WebUI/AGENTS.md` → **Playwright (HARD GATE)** for a screen-data
change. Cross-platform path review is N/A (no new I/O code).

### Suggestions

None of substance.

### Nits

- The `te` row's `DEFAULTTZ` is `Asia/Kolkata` while the sibling `hi` / `hi-in`
  rows use an empty `DEFAULTTZ`. This is **correct** for Telugu (Telugu is
  spoken only in India, single IST = `Asia/Kolkata`), but it does diverge from
  the inconsistent blank in the older India rows. Out of scope to fix here; if
  desired, a follow-up could back-fill `Asia/Kolkata` for `hi` / `hi-in`.

## Verification commands run

- `git diff` (working tree, 4 files, +19/-0 lines).
- `git diff origin/development...HEAD --stat` (empty: this branch has no commits yet).
- Manual review of `WebUI/src/main/ts/login/localeLabels.ts` (callers:
  `LoginPage.tsx:23`, `tmxLoader.ts:18`) and
  `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml`
  (sibling rows at lines 12422-12438, 12439-12455, 12541-12557 for `hi`/`hi-in`/`te`).

