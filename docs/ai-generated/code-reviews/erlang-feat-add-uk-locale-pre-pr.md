# Erlang Review — feat/add-uk-locale

- **Branch**: `feat/add-uk-locale` (off `origin/development`)
- **Commit on branch**: `54ded5bed8` `merge: sync with origin/development before uk locale PR`
- **Scope**: Uncommitted working tree (12 files). Branch has 1 sync-with-base merge commit; the actual change is staged/unstaged working-tree edits (no authored commits yet).
- **Reviewer persona**: `modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`
- **Skill memory loaded**: `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`
- **Prior topic reports loaded**: `feat-base-locales-and-arabic-erlang.md`, `feat-skill-add-locale-support-prbody.md`, `fix-te-telugu-locale-missing-scaffolding-erlang.md`, `feat-locale-format-table-erlang.md` (peer locale-add PRs).
- **Cross-platform path checklist**: N/A (no new filesystem/path code; seed rows + TS data map + Java defaults only).
- **Author persona**: Hephaestus (implementer). Independent review per Erlang mandate.

## Summary

Adds a new `uk` (base) + `uk-ua` (regional) locale pair through the full scaffold:
TMX headers, RXLOCALE rows, RXLOCALEFORMAT rows, `PSLocaleFormatDefaults` Java
entries, `SHIP_LOCALE_ENDONYMS` / `LANGUAGE_DEFAULT_REGION` WebUI map entries,
and AGENTS.md / README.md doc updates. `uk` endonym is the genuine Ukrainian
self-name (`українська` / `українська (Україна)`), `Europe/Kyiv` is the correct
regional TZ, `LOCALEID` 60/61 and `SORTORDER` 165/166 are unique, login dropdown
is data-driven from `RXLOCALE` + `PSLocaleLoginSelection` (verified).

The change class is complete and the peers are all in place, but the diff
contains **three real defect-class issues** that block the gate:

1. **Markdown bullet dropped** in `modules/perc-i18n/AGENTS.md` line 59 (the
   sub-bullet for the base-locale list lost its `-` prefix).
2. **TMX header indentation inconsistent** in all three TMX files around the new
   `uk`/`uk-ua` `<prop>` lines (6-space indent vs. canonical 4-space; the
   preceding `tr-tr` line was also rewritten to 0-space indent). This is a
   Spotless formatting violation and visually broken.
3. **Agent rule file in the diff**: `modules/perc-i18n/AGENTS.md` is in-scope
   for the root AGENTS.md **Human review of agent rules (HARD GATE)**. The
   author must obtain explicit human approval before committing this file
   (the rule was followed in concept — the file is a draft — but the gate
   requires explicit human sign-off before commit).

## Recommendation

**request-changes**

Fix the three blockers (Markdown bullet, TMX indentation, and resolve the agent
rule file gating with the human owner) and re-run Erlang. Then commit/push.

## Gate

- Blocking bugs: **3** (Markdown bullet removal, TMX indentation drift, agent
  rule file in diff without human sign-off)
- Behavioral test gap: **0** (Vitest `localeLabel`/`localeRegionCode` covers
  `uk`/`uk-ua`; JUnit `productDefaults_coverUkrainian` covers the new
  `PSLocaleFormatDefaults` entries; Playwright not required for a locale-data
  PR — but the existing `bug-1608-1609-login-locale.spec.js` regression guard
  is the canonical live-CMS proof for this surface; consider extending it as
  per the Telugu follow-up precedent)
- Cross-platform path gate: clean
- Wrong-type test fakes / shared-context injection: clean (Java test uses
  `PSLocaleFormatDefaults.shipped()` directly — exact field types)

## Issues

### Issue 1 — Severity: blocker (Markdown formatting bug)

- **File**: `modules/perc-i18n/AGENTS.md:59`
- **Description**: The sub-bullet for the base-locale list lost its leading
  `-` bullet marker. Original:

  ```
  -  **Base / language-only** codes (`ISBASE=1`): `ar`, `bn`, `de`, `es`,
  -  `fr`, `he`, `hi`, `it`, `nl`, `pt`, `ru`, `sv`, `te`, `tr`.
  ```

  New:

  ```
  **Base / language-only** codes (`ISBASE=1`): `ar`, `bn`, `de`, `es`,
  `fr`, `he`, `hi`, `it`, `nl`, `pl`, `pt`, `ru`, `sv`, `te`, `tr`, `uk`.
  ```

  The dash + indent were stripped. This makes the base-locale entry render as
  a paragraph continuation rather than a sub-bullet, breaking the visual
  hierarchy for the entire "Ship locale matrix" block.

- **Suggestion**: Restore the `-  ` prefix on both lines (the original
  2-space + dash pattern) and keep `, uk` added to the list. The cleaner
  fix is to fold the new `uk` into the existing list and preserve the
  surrounding bullet structure exactly.

- **Status**: open

### Issue 2 — Severity: blocker (TMX formatting / Spotless)

- **Files**:
  - `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx:84-86`
  - `modules/perc-i18n/src/main/resources/i18n/SystemResources.tmx:64-66`
  - `modules/perc-i18n/src/main/resources/i18n/DeveloperUi.tmx:63-65`
- **Description**: The new `<prop type="supportedlanguage">uk</prop>` and
  `<prop type="supportedlanguage">uk-ua</prop>` lines are indented with 6
  spaces; every other line in the same header uses 4-space indent. Worse,
  the preceding line (`<prop type="supportedlanguage">tr-tr</prop>`) was
  rewritten to 0-space indent by the `i18n_translate.py` merge — the rest of
  the header is still 4-space. Verified by `cat -A`:

  ```
  <prop type="supportedlanguage">tr-tr</prop>$    ← 0-space (broken)
        <prop type="supportedlanguage">uk</prop>$   ← 6-space (broken)
        <prop type="supportedlanguage">uk-ua</prop>$← 6-space (broken)
        <prop type="supportedlanguage">zh-cn</prop>$← 6-space (broken)
    <prop type="supportedlanguage">zh-tw</prop>$   ← 4-space (canonical)
  ```

  This is a Spotless-style formatting violation and visually inconsistent,
  and `i18n_translate.py` should be re-run with the file layout normalized
  first (the merge appears to have rewritten the closing lines of the header
  block).

- **Suggestion**: Run `xmllint --format` or a Tidy pass to re-normalize all
  three TMX files' header `<prop>` lines to consistent 4-space indent before
  commit. Per `modules/perc-i18n/AGENTS.md` §3, all `<prop>` lines must
  follow the canonical TMX layout. The Spotless check in pre-PR will likely
  flag this if `markdown` / `xml` formatting is enabled — verify by running
  `./mvnw spotless:check` on `modules/perc-i18n` and
  `modules/perc-distribution-tree`.

- **Status**: open

### Issue 3 — Severity: blocker (agent rule file in diff)

- **File**: `modules/perc-i18n/AGENTS.md` (and `modules/perc-i18n/README.md`,
  the latter is documentation, not a rule file)
- **Description**: Root AGENTS.md **Human review of agent rules (HARD GATE)**
  requires that any change to `AGENTS.md` (or `AGENTS.local.md`, `Claude.md`,
  module `AGENTS.md`, AI skills/agents/prompts, review pattern memory, etc.)
  must be **explicitly reviewed and approved by the human owner** before
  commit. The author's `modules/perc-i18n/AGENTS.md` changes (base-locale
  list update + regionals list update) are agent-instruction edits and
  therefore fall under this gate.
- **Suggestion**: Before commit, call out the `AGENTS.md` content delta in
  the session summary and obtain explicit human approval (the rule explicitly
  says "do not commit agent rule changes until the human has explicitly
  reviewed and approved those rule diffs"). For the README.md (developer
  docs, not a rule file) the same review is good practice but the gate does
  not technically apply. Ideally the agent rule delta should be a separate
  commit (or uncommitted draft) from the product-code change so the rule
  diff is visible in isolation.
- **Status**: open

### Issue 4 — Severity: suggestion (test coverage is minimal)

- **File**: `system/src/test/java/com/percussion/i18n/PSLocaleFormatResolverTest.java:129-143`
- **Description**: The new `productDefaults_coverUkrainian` test only asserts
  `currency`, `textDir`, `datePattern`, and `defaultTz` for `uk` and `uk-ua`.
  Compared to peer tests like `productDefaults_coverShipMatrix` (which
  exercises `getCurrencyCode`, `getDecimalSeparator`, `getGroupingSeparator`,
  `getDatePattern`, `getTimePattern`, `getFirstDayOfWeek`, etc.), the new
  test is thinner. The hard-gate (`Missing behavioral unit tests for new/
  changed non-trivial logic`) is satisfied, but adding separator / first-day
  / measurement assertions would lock in the full RXLOCALEFORMAT shape and
  catch a future drift in the seed row without needing a separate test.
- **Suggestion**: Extend the test to assert at least `decimalSep = ","`,
  `groupingSep = " "`, `firstDayOfWeek = MONDAY`, `measurement = METRIC`,
  `numberingSystem = "latn"`, `calendar = "gregory"`. Mirror the format
  defaults block at `PSLocaleFormatDefaults.java:732-760`.
- **Status**: open

### Issue 5 — Severity: suggestion (package-lock.json version drift)

- **File**: `WebUI/package-lock.json` (entry `vendor/mkd-language`)
- **Description**: The committed package-lock.json records
  `"version": "0.2.0"` for the `vendor/mkd-language` workspace, but the
  vendored `WebUI/vendor/mkd-language/package.json` declares
  `"version": "0.4.0"`. The next `npm install` will regenerate the lockfile
  to 0.4.0 and produce a diff. This is pre-existing drift (the
  package.json/lockfile entries were touched in earlier PRs, not by this
  one), but the working tree currently shows the lockfile in the locale
  branch's diff, so it should be reconciled.
- **Suggestion**: Either regenerate the lockfile against the current vendored
  0.4.0 (so the committed version matches `vendor/mkd-language/package.json`),
  or reset the lockfile to match the vendor. Out of strict scope for a
  locale PR, but if the file is being staged for this PR it should be
  consistent. The `WebUI/AGENTS.md` mkd-language section cites the runtime
  as 0.4, so 0.4 is the source of truth.
- **Status**: open (pre-existing, not introduced by this PR; non-blocking)

### Issue 6 — Severity: nit (test order)

- **File**: `WebUI/src/test/ts/login/localeLabels.test.ts:125-131`
- **Description**: The new `uk` and `uk-ua` assertions are inserted between
  `tr-tr` (alphabetically correct) and `hi-in` (alphabetically wrong — `h` <
  `uk`). The test block was already non-strictly-alphabetical before this
  PR, so this is pre-existing. The new assertions are in correct relative
  position; the `hi-in` after `uk-ua` is just an existing inconsistency.
- **Suggestion**: Optional — sort the assertions in the `uses curated ship
  endonyms` block alphabetically to make future additions obvious. Not a
  blocker.
- **Status**: open

### Issue 7 — Severity: nit (LOCALEID 53 reused, pre-existing)

- **File**: `modules/perc-distribution-tree/.../cmsTableData.xml`
- **Description**: The author's note that `LOCALEID 53` is reused for both
  `fr-us` and `pl` is acknowledged as pre-existing. The chosen IDs 60/61 for
  `uk`/`uk-ua` are unique (verified: only one occurrence of each in the
  file). No action on this PR. If the LOCALEID 53 collision is ever cleaned
  up, treat it as a separate PR.
- **Suggestion**: None for this PR.
- **Status**: closed (acknowledged)

## Detailed observations (no defect)

- **Endonym correctness**: `uk` → `українська`, `uk-ua` → `українська (Україна)`.
  Both are the genuine Ukrainian self-names (no English fallback). Compliant
  with project memory `i18n_endonym_translation_rule`.
- **LOCALEID 60/61**: unique in `cmsTableData.xml` (verified; only one
  occurrence each).
- **SORTORDER 165/166**: unique; no other RXLOCALE row uses these values.
  Adjacent to `tr-tr` (161) which is the alphabetically previous regional.
- **TMX supported-language headers**: `uk` and `uk-ua` are present in all
  three TMX files (CmsUi, SystemResources, DeveloperUi). XML parses cleanly
  with `xml.etree.ElementTree`.
- **No duplicate TUIDs** in any of the three TMX files (CmsUi 1634 TUs,
  SystemResources 634 TUs, DeveloperUi 396 TUs).
- **No merge conflict markers** (`<<<<<<<`, `=======`, `>>>>>>>`) anywhere
  in the diff.
- **Full `uk` back-fill**: 1634 `<tuv xml:lang="uk">` entries in CmsUi.tmx
  (one per TU), 634 in SystemResources, 396 in DeveloperUi. This is a
  complete back-fill across all keys, consistent with the
  `i18n_translate.py` cache update in `scripts/cache/i18n_translate.json`.
- **No `uk-ua` TUVs**: intentional per the AGENTS.md model — regionals hold
  dialect overrides only, not full translations. Lookup chain `uk-ua → uk
  → en-us` resolves all keys via the base `uk` TUVs.
- **`Europe/Kyiv` TZ**: correct tzdb identifier for the regional override;
  matches the existing `ru` entry (which also uses `Europe/Kyiv` as a
  reasonable TZDB proxy). No new tzdb issue.
- **Currency `UAH`**: correct ISO 4217 for Ukrainian hryvnia.
- **`de` row in `LANGUAGE_DEFAULT_REGION` was not touched** — `de: "DE"` is
  already present and was not modified by this PR. The new `uk: "UA"` entry
  is correctly placed between `tr: "TR"` and `zh: "CN"` (alphabetical).
- **Login dropdown will pick up `uk`/`uk-ua` automatically** —
  `PSLocaleLoginSelection.forLoginDropdown` (read in full) iterates all
  active `PSLocale` rows and shows base locales only when no regional
  sibling exists. Since `uk-ua` is active, `uk` will be hidden on the login
  dropdown and `uk-ua` will be shown — which is the desired product
  behavior. The `LANGUAGE_DEFAULT_REGION["uk"] = "UA"` ensures the regional
  emoji flag renders.
- **Calendar widget** `perc.widget.calendar/percCalendarTwo.xml` already had
  `uk` in its enum (line 163 — `Ukrainian Ukraine (UA)`). No change needed.
- **`RXLOCALE` upgrade story**: All new rows use `onTableCreateOnly="no"`
  and `action="i"`, so they only insert on installs that don't already have
  the row. Existing installs get the new locales via `action="i"` only if
  the install is fresh; upgrades need an `action="u"` or a manual schema
  migration. This is consistent with the surrounding peer rows (`tr`/`tr-tr`
  pattern at `cmsTableData.xml:12575-12594`), so no defect, but worth noting
  that the upgrade story for an existing-CMS install is "operator must run
  the install schema migration or manually add the rows." Same as every
  shipped locale addition.

## Cross-platform path checklist

Not applicable. The diff touches:
- XML seed data (no I/O)
- Java `Map` literals (no I/O)
- TypeScript data maps (no I/O)
- TMX files (text content, no path handling)
- `WebUI/package-lock.json` (dependency metadata)
- Markdown docs

No new filesystem/path code; no `Path` / `Files` / `Path.of` / `File.separator`
additions. Cross-platform review is clean.

## Tests observed

- `system/src/test/java/com/percussion/i18n/PSLocaleFormatResolverTest.java`:
  new `productDefaults_coverUkrainian` test.
- `WebUI/src/test/ts/login/localeLabels.test.ts`: new `localeLabel` and
  `localeRegionCode` assertions for `uk` / `uk-ua`.
- Pre-existing behavioral coverage (not modified):
  - `PSLocaleLoginSelectionTest` (verified `uk`/`uk-ua` will exercise via
    base+regional filter)
  - `PSTmxResourceBundleTest` (will exercise `uk` TUVs on lookup)
  - Live-CMS regression: `modules/perc-qa-automation/frontend/tests/bugs/
    bug-1608-1609-login-locale.spec.js` (per the Telugu fix precedent, this
    spec should be extended to assert `uk - українська` and `uk-ua -
    українська (Україна)` are present in the rendered dropdown — consider
    for follow-up)

## Memory touch

**Existing entries consulted** (no new pattern added):
- `ERLANG patterns.md` → **Hard gate: agent rule/instruction file changes
committed without explicit human review** (already covered by root
AGENTS.md gate; this PR's `AGENTS.md` diff is the trigger).
- `ERLANG patterns.md` → **Hard gate: missing behavioral unit tests for
new/changed non-trivial logic** (satisfied; `productDefaults_coverUkrainian`
+ Vitest assertions).
- `ERLANG patterns.md` → **Hard gate: in-place Spotless reformat re-flowed
out-of-scope files** (relevant to the TMX indentation issue: the existing
`tr-tr` line was re-indented, and the new `uk`/`uk-ua` lines are at
inconsistent indent — this is a `i18n_translate.py` output drift, not a
Spotless reformat, but the same principle applies: do not ship
inconsistent formatting in the locale PR; re-run with a normalized input).

**Possible new pattern** (not added — too narrow to generalize):
"TMX header `<prop>` lines must use a consistent indent (4-space in this
repo); `i18n_translate.py` re-merge can re-indent pre-existing lines and
introduce visual drift. Spotless / Tidy should re-normalize before commit."
Could be folded into the existing **Hard gate: visual formatting drift in
generated files** if a similar pattern recurs, but for now flag-and-fix is
sufficient.

## Re-review delta

None — first review of this branch.

## Handoff

- **Author (Hephaestus)**: fix the three blockers (Markdown bullet, TMX
  indent, agent rule file gate); re-run Erlang on the fix; then commit +
  push.
- **Orchestrator (Tank)**: no release-risk side note; per `#1608/#1609`
  precedent, the new locale should be exercised by the live-CMS Playwright
  regression guard before merge.
- **Human owner**: please review the `modules/perc-i18n/AGENTS.md` diff
  before Hephaestus commits it (root AGENTS.md Human review of agent
  rules). The content change is purely a list update (`uk` /
  `uk-ua` added to the base/regional tables), so the review is short.

