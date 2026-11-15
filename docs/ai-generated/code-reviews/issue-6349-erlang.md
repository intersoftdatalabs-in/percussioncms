## Summary

Machine analysis found **2** finding(s), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 1 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/perc-i18n/src/test/java/com/percussion/i18n/PSTmxResourceBundleScanTest.java:177
- Rule: `llm`
- Tool: `llm`
- Description: Test expects key 'perc.ui.dashboard.modern@None' but this key is not added to CmsUi.tmx in the same change. The TMX diff only adds 6 community switcher keys, but the test requires 7 keys including 'None'.
- Suggestion: Either add 'perc.ui.dashboard.modern@None' to CmsUi.tmx with an en-us translation, or remove it from the required set in the test if it's not needed for the community switcher feature.
- Status: open

### Issue 2 -- Severity: suggestion

- File: modules/perc-i18n/src/test/java/com/percussion/i18n/PSTmxResourceBundleScanTest.java:94 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `cmsUi_shellChromeKeys_haveEnUsAndHindi` cognitive=19 (max 15), cyclomatic=8 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.


## Adjudication (author, 2026-11-15)

**Issue 1 (`llm`, "bug", request-changes) is a verified false
positive — does not block.** The reviewer saw only the 6 added `<tu>` hunks
and concluded the 7th required key `perc.ui.dashboard.modern@None` is absent
from `CmsUi.tmx`. It is present on `origin/main` (added by merged PR #6346,
line 26705) with `en-us` "None" plus the full locale matrix; exact tuid count
in file = 1 (no duplicate introduced). Hard evidence: the new
`cmsUi_userCommunityKeys_haveEnUsSource` test requires all 7 keys and
**passes** (`PSTmxResourceBundleScanTest`: Tests run: 5, Failures: 0 in the
`perc-i18n` clean install). Adding the TU again would trip
`catalogTuids_areUniqueWithinEachFile`; dropping the pin would weaken the
test. A clarifying comment was added above the required set instead.

**Issue 2 (`complexity.cognitive`, suggestion) is preexisting** (line 94,
`cmsUi_shellChromeKeys_haveEnUsAndHindi`, untouched by this diff) — does not
block per the in-diff-only gate.

Deterministic gate: **in-diff machine findings = 0**; behavioral test added
and green; no path/file-I/O changes. Gate: PASS (advisory).
