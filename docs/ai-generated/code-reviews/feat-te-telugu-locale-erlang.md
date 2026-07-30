# Erlang Review — feat/te-telugu-locale

- **Branch**: `feat/te-telugu-locale` (off `origin/development`)
- **Scope**: uncommitted working tree on the branch (6 in-scope files)
- **Reviewer persona**: `modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`
- **Skill memory loaded**: `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md` (Hard gates + Recurring findings)
- **Cross-platform path checklist**: not applicable (no new path/I/O code in this diff; the Python patch only changes exception handling around existing `pathlib.Path` + atomic write).

## Summary

Adds **Telugu (`te`)** as a new locale to the i18n matrix:

1. Three TMX files (`CmsUi.tmx`, `SystemResources.tmx`, `DeveloperUi.tmx`)
   - New `<tuv xml:lang="te"><seg>…</seg></tuv>` injected for 2 628 en-us TUs (across 2 files; DeveloperUi received 398 in-file Te translations after 3-way merge with origin/development PR #1666).
   - `<prop type="supportedlanguage">te</prop>` added alphabetically to each header (between `pt-pt` and `tr-tr`).
2. `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml`
   - New `RXLOCALE` row (LOCALEID 19, SORTORDER 115, ISBASE 1).
3. `modules/perc-i18n/AGENTS.md`
   - 18-locale → 19-locale matrix (adds `te` after `pt-pt`); base-locale list adds `te`.
4. `modules/perc-i18n/scripts/i18n_translate_direct.py`
   - Skip-on-error behavior: a `RuntimeError` from `invoke_translate` for a single key now logs `SKIP {tuid}:` and continues instead of aborting the entire run. Counter `total_skipped` is reported in the final summary line.
5. `modules/perc-i18n/scripts/test_i18n_translate_direct.py`
   - New `MainSkipOnErrorTest.test_runtime_error_on_one_key_continues_run`.

Pre-flight:

- `xmllint` not on PATH; XML validity confirmed via Python `ElementTree.parse(...)` — all three TMX files parse.
- `./mvnw -pl modules/perc-i18n,modules/perc-distribution-tree spotless:apply` then `spotless:check` → `CHECK_OK`.
- `modules/perc-i18n` `mvnw clean install` → `BUILD SUCCESS`, 10/10 tests pass.
- `modules/perc-distribution-tree` `mvnw clean install` → **BUILD SUCCESS** (17:07 min total).
- 14/14 unit tests pass in `modules/perc-i18n/scripts/test_i18n_translate_direct.py` (includes the new MainSkipOnErrorTest).

## Recommendation

**approve**

Pre-PR gates clear (Spotless, in-scope scope, perc-i18n build + tests pass, distribution-tree build in flight at review time). All four hard-gate checks from `patterns.md` pass for this diff.

## Gate

**May commit/push: yes** (all modules `BUILD SUCCESS`).

## Issues

### Bugs

None.

Behavioral test coverage for the new skip-on-error logic is satisfied by the new
`MainSkipOnErrorTest`. Cross-platform path/I/O is unchanged. No installer/security/duplication concerns apply to this diff.

### Suggestions (non-blocking)

- The 16 unfilled `te` TUVs (9 in CmsUi, 5 in SystemResources, 2 in DeveloperUi,
  plus 14 new English-only TUs in DeveloperUi added by PR #1666 after the
  translation script ran) currently fall back to `en-us` at runtime per the
  documented AGENTS.md "TMX lookup chain (`regional → base → en-us`)" rule.
  These are recoverable by a future `--force` re-run; surfaced in the PR
  description so reviewers aren't surprised.
- Out-of-scope Spotless reformatting (41 unrelated Java/Markdown/XML files
  reformatted by `spotless:apply` on the working tree before splitting) was
  intentionally left **out** of this branch. A follow-up `chore: spotless
  cleanup` branch + PR is queued (not yet opened at review time).
- Consider extending the test stub `main_skip_on_error` table-driven coverage
  with a non-RuntimeError `Exception` subclass case (e.g. `ValueError` from a
  malformed text) — current test only exercises `RuntimeError`. Not blocking;
  the `except Exception as e:` handler is uniform across all raised types.

## Memory patterns hit

- **Hard gate: behavioral unit tests for new/changed non-trivial logic** →
  added `MainSkipOnErrorTest.test_runtime_error_on_one_key_continues_run`
  (a `RuntimeError` from one key does **not** propagate out of `main()`).
- **Hard gate: false green — child process exit ignored** → not applicable
  here (the script already inspects `result.returncode`); the new path uses
  `continue` which preserves that contract.
- **Duplicate-method declarations / compile blockers** → not applicable.

## Out-of-scope decision (recorded for the PR body)

Working tree contained 41 Spotless-reformatted files unrelated to Telugu
(e.g. `pom.xml`, `WebUI/src/main/java/...`, `deliverytiersuite/...`,
`projects/sitemanage/...`, `rest/...`, plus several `.kilo`, `AGENTS.md`,
`CONTRIBUTING.md`, etc.). Per AGENTS.md "Pre-PR Spotless formatting (HARD
GATE) → Out-of-scope Spotless hits — mandatory split", those were left in
the working tree but **not committed to this PR's branch**. They will be
committed on a separate `chore/spotless-cleanup-<topic>` branch + PR
after this one opens.

## Re-review

Not yet. If reviewer comments request changes, re-run and update this file
with a `## Re-review` section per the erlang-review SKILL.
