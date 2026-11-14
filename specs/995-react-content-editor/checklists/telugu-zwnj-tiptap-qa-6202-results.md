# QA record — #6202: Telugu ZWNJ fix in TipTap segments

Agent execution of the #6202 test plan (QA for merged #6182,
`fix(i18n): replace literal u200c with real ZWNJ in Telugu TipTap segments`,
slice of parent #6172) against `origin/main` at `5e16b06b79`.
**PASS**: zero ASCII `u200c` artifacts remain in any Telugu (`te`) TipTap
segment; the 11 fixed segments carry real U+200C in exactly the 13 positions
#6182 describes; English (`en-us`) toolbar labels intact; TMX valid;
`PSTipTapEditorKeysTmxTest` 4/4 green, full `perc-i18n` module 39/39 green.

> This PR supplies QA evidence only — it does not close #6202; human
> sign-off still required. No production code, REST, WebUI, or Java sources
> changed.

## Environment

- Base: `origin/main` at `5e16b06b79` (post-#6182 merge 2026-11-12; working
  branch `fix/issue-6202-telugu-zwnj-tiptap-qa`).
- Artifact under test:
  `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` (as merged by #6182).
- Guard: `modules/perc-i18n/src/test/java/com/percussion/i18n/PSTipTapEditorKeysTmxTest.java`
  (`tiptapSegments_haveNoAsciiEscapeArtifacts` + 3 pin tests).
- Render-path note: WebUI toolbar labels resolve `perc.ui.editor@` keys via
  `WebUI/src/main/ts/editor/messages.ts`, so the segment bytes verified below
  are byte-identical to what the Telugu toolbar renders — no live-browser
  font shaping is needed to rule out the ASCII-garbage failure mode (see
  step 1 row).

## Test-plan mapping (#6202)

| # | QA step (#6202 test plan) | Result |
|---|---------------------------|--------|
| 1 | Telugu locale: Formatting-toolbar strings render without visible `u200c` garbage (link/inline/template labels) | PASS — scanned all 78 `perc.ui.editor@` `tu`s: **0** `te` segments contain ASCII `u200c`; the 11 fixed segments (`Formatting toolbar`, `Outdent list item`, `Remove link`, `Apply link`, `Close link editor`, `Insert inline link` ×2, `Insert inline image`, `Insert inline template` ×2, `Insert global variable`, `Insert more link`, `Fill in the required fields.`) carry real U+200C in exactly 13 positions, matching #6182's change description 11/11 and 13/13 |
| 2 | No regression in another locale (English toolbar labels intact) | PASS — `en-us` segs intact and non-blank for `Formatting toolbar`, `Link`, `Inline code`, `Insert inline link`, `Insert inline template`, `Bold`, `Undo`, `Remove link`, `Apply link`; none contain `u200c` |
| 3 | Optional: run `PSTipTapEditorKeysTmxTest` guard locally | PASS — `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`; full module `cd modules/perc-i18n && ../../mvnw clean install` → **BUILD SUCCESS**, **Tests run: 39, Failures: 0, Errors: 0, Skipped: 0** (37 at #6182 merge time; +2 tests added on tip since, all green) |

## Extra checks (this run)

- `xmllint --noout CmsUi.tmx` → valid (no output, exit 0).
- No new warnings: only the pre-existing `PSLocaleCodeRewrite` javadoc
  `no main description` noise and the pre-existing unused-h2 dependency note
  already recorded on #6182.
- Out of scope (per #6202): the wider endemic `u200c` hits elsewhere in the
  catalog and non-Telugu locales — untouched.

## Verdict

**PASS** — all three numbered steps green on current `main`; no failure
artifacts to log (no locale/key/screenshot needed).
