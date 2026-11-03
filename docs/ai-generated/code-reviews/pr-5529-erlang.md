# Erlang review — PR #5529 (issue #5500 locale date pattern)

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml \
    WebUI/src/main/ts/developer WebUI/src/test/ts/developer \
    rest/src/main/java/com/percussion/rest/locales \
    projects/sitemanage/src/main/java/com/percussion/apibridge/LocalesAdaptor.java \
    projects/sitemanage/src/test/java/com/percussion/apibridge/LocalesAdaptorDesignWsTest.java \
    modules/perc-qa-automation/frontend/tests/developer-locale-date-pattern.spec.js \
    product-docs/8.2/admin/developer-locales.md
```

Cwd: PR head `c71c184c4443f28a09df11dacafa3ea7d7884859` (`fix/issue-5500-locale-date-pattern`).

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 4 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Pack: percussion
- CLI: mkd-code-review 0.1.18
- Gate: advisory (in-diff bugs only)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/LocalesAdaptor.java:525 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isDatePatternOnlyWrite` cognitive=4 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

Disposition: not a bug. The method is a field-null conjunction for the date-pattern-only PUT shape. Same peer style as other omitted-field detectors. Does not change merge gate.

## Independent reading

Overnight Erlang (Grok Build 1.0.70, grok-4.6). Did not author the PR. CLI analyzed 4 files under the pack; remaining companions were read by hand. `llm.error` not observed.

Date-pattern-only save on the existing locale PUT:

- `localeDatePatternWrite` sends `{ format: { datePattern } }` only (trimmed; blank clears) and returns `"unchanged"` when the stored pattern matches.
- `savedLocaleDatePattern` refuses a success paint when the sent body includes language/label/description/status/hasFormatProfile or other format fields, when the saved date pattern is not the one sent, or when language, label, description, status, baseLocale, or other format fields changed.
- Cancel does not call `updateLocale`. HTTP 400 / 403 / 409 restore the previous pattern. Create locale still hides the dedicated control.
- Adaptor `isDatePatternOnlyWrite` + `persistDatePatternOnly` merge `datePattern` onto the existing `RXLOCALEFORMAT` row (`PSLocaleFormat.setDatePattern` uses `emptyToNull`, so blank clears). Time pattern, separators, and currency stay. `hasFormatProfile` must be omitted or the full format write path runs.
- REST surface is javadoc / `ILocalesAdaptor` contract on the existing PUT — no new resource. No extra Spring rest stub required beyond the existing adaptor interface.
- Preexisting `paths.hardcoded_sep` hits on `LocalesResource` (when that file is scanned) are JAX-RS `@Path` URL templates, not filesystem joins.

Companions: helper + Vitest, panel tests, sitemanage adaptor tests, Playwright `developer-locale-date-pattern.spec.js`, `product-docs/8.2/admin/developer-locales.md`. No filesystem path joins. No rule files.

Gate: PASS. Recommendation: approve. May merge: yes.

> Co-Authored by Grok Build 1.0.70 using grok-4.6 with agent night-issue-prs-erlang.
