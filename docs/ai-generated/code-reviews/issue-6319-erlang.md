## Summary

Machine analysis found **42** finding(s), **11** blocking bug(s). LLM skipped (hard_pattern)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 42 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 11
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 6)
- Status: open

### Issue 2 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:23 (in-diff)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 23)
- Status: open

### Issue 3 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:67 (in-diff)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 67)
- Status: open

### Issue 4 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:74 (in-diff)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 74)
- Status: open

### Issue 5 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:112 (in-diff)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 112)
- Status: open

### Issue 6 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Nt` cognitive=44 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Ht` cognitive=37 (max 15), cyclomatic=47 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 8 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Ft` cognitive=46 (max 15), cyclomatic=35 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:32 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Fr` cognitive=39 (max 15), cyclomatic=29 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 10 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:50 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Lu` cognitive=15 (max 15), cyclomatic=32 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 11 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:62 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `pi` cognitive=33 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 12 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `E` cognitive=5 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 13 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `e` cognitive=17 (max 15), cyclomatic=7 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 14 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Ne` cognitive=10 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 15 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Pe` cognitive=12 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 16 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `yt` cognitive=22 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 17 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `pt` cognitive=16 (max 15), cyclomatic=8 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 18 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Mt` cognitive=17 (max 15), cyclomatic=26 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 19 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Ut` cognitive=25 (max 15), cyclomatic=26 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 20 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Et` cognitive=27 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 21 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `C` cognitive=15 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 22 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Rn` cognitive=6 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 23 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/moment.min.js:6 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Un` cognitive=15 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 24 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:25 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Qt` cognitive=22 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 25 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:29 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Or` cognitive=9 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 26 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:33 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Hr` cognitive=20 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 27 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:36 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `ke` cognitive=25 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 28 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:38 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `We` cognitive=24 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 29 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:39 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Ce` cognitive=22 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 30 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:41 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Ve` cognitive=17 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 31 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:46 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `hu` cognitive=19 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 32 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:47 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `gu` cognitive=28 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 33 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:56 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Yu` cognitive=26 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 34 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:57 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Qu` cognitive=14 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 35 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:61 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `ai` cognitive=22 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 36 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:63 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `_i` cognitive=14 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 37 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:64 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `vi` cognitive=21 (max 15), cyclomatic=25 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 38 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:68 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Si` cognitive=1 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 39 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:71 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `qi` cognitive=18 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 40 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:87 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Wf` cognitive=21 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 41 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:103 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `wa` cognitive=21 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 42 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.directory/sys__UserDependency--web_resources/widgets/directory/js/lodash.min.js:106 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Oa` cognitive=22 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.


## Adjudication (author, 2026-11-14 — NOT CLI output)

All 42 machine findings (11 bug + 31 suggestion) sit inside the two refreshed
npm dist blobs (`lodash.min.js`, `moment.min.js`) and describe upstream
minified code style (`tests.empty-catch`, `complexity.cognitive` on
single-letter minified functions). They are **verified false positives**:

- Both payload files are **byte-identical** to the upstream npm dist
  (`cmp` clean vs `node_modules/lodash/lodash.min.js` 4.17.21 and
  `node_modules/moment/min/moment.min.js` 2.31.0; lockfile integrity
  `sha512-v2kDEe57lecTulaDIuNTPy3Ry4gLGJ6Z1O3vE1krgXZNrsQ+LFTGHVxVjcXPs17LhbZVGedAJv8XZ1tvj5FvSg==`;
  re-asserted by the new vitest byte-identity test). Refactoring dist bytes
  would destroy npm traceability — the opposite of #6319's goal.
- Inline `// mkd-code-review[rule]` suppression is intentionally **not**
  applied: any byte added to the payload breaks the byte-parity pin. This
  report + the PR body section serve as the suppression record (per
  AGENTS.md review-suppression protocol for adjudicated FPs).
- Zero findings on hand-written files (new vitest parity test, guard-test
  pin updates, package.json range additions). Behavioral tests exist
  (vitest 6/6 incl. `_.includes`/`isBefore`/`format` drop-in proof; guard
  3/3). No non-portable paths (Java: `Path.resolve`/`Files`, `/`-normalized
  keys; JS test: `path.join` only).
- Precedent: PR #5859 (issue #5660) merged the same finding class on a
  byte-identical npm dist refresh (`4 findings, 1 bug, LLM skipped
  (hard_pattern)`).
- LLM inference skipped by the harness (`LLM skipped (hard_pattern)`,
  machine-only); no LLM-identified in-diff defect exists to address.

Gate: PASS with adjudicated dist-blob FPs. No author-code defect outstanding.

---

# Slice 10 re-review (lightbox2@2.10.0 rebase, 2026-11-17, branch fix/issue-6319-lightbox-npm)

## Summary

Machine analysis found **3** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 47 analyzed
- In-diff: 0 finding(s); preexisting: 3
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/resources/Rhythmyx/AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/config/spring/projects/sitemanage-beans.xml:1442 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1442)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/design/TemplateSourceEditor.tsx:204 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=37 (max 15), cyclomatic=25 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/services/PSFeedService.java:535 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `generateFeed` cognitive=19 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.
