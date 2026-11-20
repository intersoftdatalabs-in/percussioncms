## Summary

Machine analysis found **2** finding(s) (2 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- In-diff: 1 finding(s); preexisting: 1
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
- `paths.hardcoded_sep` on `rest/src/main/java/com/percussion/rest/pipelines/PipelinesResource.java:1183` -- *Justification*: : adjudicated intentional (issue #6759, (via in_source_mkd)

</details>
