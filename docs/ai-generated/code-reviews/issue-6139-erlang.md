## Summary

Machine analysis found **4** finding(s) (4 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 0 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No active issues._

### Suppressed Findings (4)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `paths.hardcoded_sep` on `WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:279` -- *Justification*: : adjudicated intentional (issue #6145, (via in_source_mkd)
- `paths.hardcoded_sep` on `WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:360` -- *Justification*: : adjudicated intentional (issue #6145, (via in_source_mkd)
- `paths.hardcoded_sep` on `WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:405` -- *Justification*: : adjudicated intentional (issue #6145, (via in_source_mkd)
- `paths.hardcoded_sep` on `WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:413` -- *Justification*: : URL prefix join, not a filesystem path — see adjudication above (issue #6145). (via in_source_mkd)

</details>

