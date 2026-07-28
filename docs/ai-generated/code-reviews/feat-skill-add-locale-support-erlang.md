## Summary

Adds `modules/ai-shared-develop/src/main/resources/skills/add-locale-support/SKILL.md`, a develop checklist that wraps the existing workflow in `modules/perc-i18n/AGENTS.md` §2b for adding a new locale. It enumerates the six concrete touch-points (TMX header, RXLOCALE row, optional calendar picker, optional Lucene analyzer branch, translation back-fill, documentation) and references the canonical Docker-based `i18n_translate.py` script and the standalone `perc-i18n` + `perc-distribution-tree` Maven builds. No Java, XML, or path code is touched; this is a docs-only contribution whose runtime surface is "an LLM agent reading it".

## Scope

- Base: `origin/development` @ `7ffcd68b09`
- Head: branch `feat/skill-add-locale-support` (working tree, untracked file)
- Files: 1 added (`modules/ai-shared-develop/src/main/resources/skills/add-locale-support/SKILL.md`, 317 lines)
- Prior report: none for this branch slug
- Memory patterns hit: `installer.port-detection-false-positive` (cross-references the previous fix-pack report path), no path/hard-gate ones hit

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

None.

### Notes

- **Source-of-truth alignment** — Every numbered step in the SKILL traces back to a specific clause in `modules/perc-i18n/AGENTS.md` (sections 1a, 2b, 3, 4, 6, 8) or `modules/perc-distribution-tree/AGENTS.md`, and the SKILL explicitly defers to those files when they disagree. No fabricated workflow.
- **Activation surface** — Frontmatter `description` lists concrete trigger phrases ("add a new locale", "RXLOCALE row for \<code\>", etc.), matching the convention used by `erlang-review`, `codeql-pr`, `maven-integrity-validator`, and `patch` SKILLs in the same directory.
- **Cross-platform** — Skill recommends both `xmllint --noout` and `python3 -c xml.etree.ElementTree` so Windows CI hosts without libxml2 still have a portable XML check (verified locally with `python3 -c "import xml.etree.ElementTree as ET; ET.parse(...)"` exit 0 on both canonical TMX files).
- **Secrets / process invocation** — Skill points at `docker run --rm soimort/translate-shell`, which is the existing contract per `modules/perc-i18n/scripts/README.md`. No new external service, no tokens to surface. The script's own rate-limit / exponential-backoff / cache contracts are restated verbatim.
- **Inventory accuracy** — Concrete file paths were each confirmed against the current tree (`RXLOCALE` block in `cmsTableData.xml` line 12054, `percCalendarTwo.xml` line 130 `hi-in` template, `PSLocaleSpecificLuceneAnalyzer.java` `case` lines 115-181, `perc-i18n/.gitignore` line 61 ignoring `.cache/`).
- **Cross-platform path review** — N/A; the diff is a markdown skill, no filesystem path or I/O logic introduced.
- **Behavioral test analogue** — Skills are agent-prompt documents and don't compile; Erlang's "missing behavioral test" gate does not apply here. The hard path / non-portable-I-O gate also does not apply (no code touched).
- **Style** — Markdown renders correctly in current `CommonMark`; frontmatter ends with `---` block; no emojis; `description` is the same multi-line `>-` style as the other developer skills. Two minor nits that do not block:
  - §5 references `mvn-env.sh` while the development working tree currently uses `.bat` (`mvn-env.bat`). Per root `AGENTS.md` the wrapper has both entry points and the `.sh` form is documented as canonical in the helpers repo; this skill matches the convention used by `perc-i18n/AGENTS.md` itself. Leaving as-is.
  - §2 step b says "calendar widget picker — only if you are adding a calendar regional variant or your code needs a visible widget entry". The "code needs" branch is vague; an implementer might over-extend. Consider tightening to "only if the new code is itself a calendar region or your UI team confirms a picker entry is required". Suggestion, not blocking.

## Re-review

Triggered by `kilo-code-bot` review comments on PR #1562. Both items were captured as `suggestion` in the original report, not blocking.

- **Line 110 — vague "your code needs a visible widget entry"** — Tightened the pre-condition to "only if the new locale is itself a calendar region, **or** your UI team has confirmed that a picker entry is required for this locale". The over-extension surface is gone.
- **Line 117-122 — contradictory mirror-copy instruction** — Split into two enumerated branches: (a) lockstep confirmed → update both; (b) not in lockstep → update only `Widgets/` (source of truth) and explicitly forbid editing `Resources/` from this skill. The original "otherwise … canonical for the legacy UI" was leaving the `Widgets/` copy's treatment ambiguous; that ambiguity is now resolved.

Re-review diff is limited to the §3a "Calendar widget picker" bullet. No other SKILL section changes; no other product code touched.

### Scope (re-review)

- Base: PR head `feat/skill-add-locale-support` @ `2ae589dc84`
- Head: branch `fix/skill-add-locale-support-pr-1562`, working tree → single-file hunks in `SKILL.md`
- Files: 1 changed in `SKILL.md`; Erlang report appended (this section); no new files

### Recommendation (re-review)

approve — gate still `May commit/push: yes`. Both `kilo-code-bot` `suggestion` findings are closed with concrete fixes; no new bugs, no path/I/O regressions, no behavioral-test analogue required (docs-only change).
