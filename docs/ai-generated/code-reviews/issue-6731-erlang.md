# Erlang review — issue #6731 (PSSitemgrXmlSerializationTest.java `paths.hardcoded_sep`)

Adjudication slice: `system/src/test/java/com/percussion/services/sitemgr/data/PSSitemgrXmlSerializationTest.java:373`
(`assertElementTreeEquals` recursion `path + "/" + tag + "[" + i + "]"`) — verified false positive:
the join builds an XPath-style XML element-tree diagnostic path for assertion
messages, never an OS filesystem path (same FP class as #5898/#5910/#5927/#6325/#6358/#6359,
extends the mkd-6729 ledger row family). `Path.resolve`/`File.separator` would
corrupt the diagnostic on Windows.

Command (models path redacted per sanitize policy):

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base "origin/main" \
  --models <models-dir>/models.ollama-dev-coder.toml
```

First run flagged the sink as blocking (suppression comment sat two lines above
the `/` literal after the multi-line call); restructured the sink into a named
`childPath` local directly below the suppression so the matcher sees it.
Second run: 0 blocking, approve.

## Sanitized report
## Summary

Machine analysis found **2** finding(s) (2 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- In-diff: 2 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No active issues._

### Suppressed Findings (2)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md:117` -- *Justification*: ` with issue + precedent citation on the sink; the `String` field maps a CMS page reference like its `LOGIN_PAGE`/`REGISTRATION_PAGE`/`RESET_PAGE` siblings. Tests: `PSSiteResetPageColumnMappingTest` (column-name pin, String page-path type, getter/setter round-trip, sibling page-column family). | 2026-11-20 | fix/issue-6728-pssite-secret | 2027-11-20 | (via in_source_mkd)
- `paths.hardcoded_sep` on `system/src/test/java/com/percussion/services/sitemgr/data/PSSitemgrXmlSerializationTest.java:390` -- *Justification*: : adjudicated intentional (issue #6731, precedent #5898/#5910/#5927/#6325/#6358/#6359 + mkd-6729) - XPath-style XML element-tree diagnostic path for assertion messages, never an OS filesystem path; File.separator would corrupt the diagnostic on Windows. (via in_source_mkd)

</details>

## Notes

- No LLM fallback was needed; machine-only result short-circuits as clean
  (0 in-diff blocking findings). No paid/OpenRouter route used.
- Change-class closure for this adjudication slice: inline suppression +
  `suppressions.md` ledger row (`mkd-6731`) + pinning unit test
  (`elementTreeMismatchPathUsesXmlSlashRegardlessOfOsSeparator`). No product-docs (no behavior change),
  no QA cell (no UI), no downstream signature changes.
