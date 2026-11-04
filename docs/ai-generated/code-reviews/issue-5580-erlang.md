# Erlang review — issue #5580 (locale measurement system)

CLI: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml` scoped to the branch diff (rest, LocalesAdaptor + tests, developer WebUI, product-docs, Playwright spec). Persona: erlang 0.1.1.

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- In-diff: 2 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

Gate on in-diff bugs, missing behavioral tests, and non-portable paths only. Complexity suggestions are not blocking. Preexisting `isFirstDayOfWeekOnlyWrite` cyclomatic is out of scope.

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/LocalesAdaptor.java:492 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `persistFormatIfRequested` cognitive=17 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

Peer pattern: each field-only persist is a sibling `if (isXOnlyWrite)` plus `persistXOnly`. Extracting a dispatcher is a later cleanup across slices 115–119, not this slice.

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/LocalesAdaptor.java:894 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isFirstDayOfWeekOnlyWrite` cognitive=5 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/LocalesAdaptor.java:922 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isMeasurementSystemOnlyWrite` cognitive=4 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

Matches sibling `is*OnlyWrite` field-null checks. No behavior defect.

## Intent / change-class closure

Vertical slice #5580 / parent #1690 slice 117:

- REST javadoc + `LocalesResourceTest` invalid `imperial` → 400
- `LocalesAdaptor.persistMeasurementSystemOnly` / `isMeasurementSystemOnlyWrite` / `normalizedMeasurement`
- `LocalesAdaptorTest` + `LocalesAdaptorDesignWsTest` behavioral coverage
- Dedicated Developer chrome (`localeMeasurementSystem.ts` + `LocaleDetailPanel`) — not `messages.ts` / `paths.ts` / `DeveloperShell.tsx`
- Vitest: `localeMeasurementSystem.test.ts` + `LocaleDetailPanel.test.tsx`
- Playwright: `developer-locale-measurement.spec.js`
- Product docs: `product-docs/8.2/admin/developer-locales.md`

No new filesystem path joins. No public signature break (javadoc + existing PUT). No rule-file changes.

## Verdict

Gate: PASS (advisory). Recommendation: approve. May commit/push: yes.
