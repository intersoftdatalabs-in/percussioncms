## Summary

Machine analysis found **2** finding(s) (2 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- In-diff: 0 finding(s); preexisting: 2
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

- `paths.hardcoded_sep` on `WebUI/src/main/webapp/cm/widgets/perc_template_metadata_dialog.js:74` -- *Justification*: : adjudicated intentional (issue #6701, same as #5897/#5898/#5910/#5924/#5927) — slash joins URL segments, not an OS path. (via in_source_mkd)
- `paths.hardcoded_sep` on `WebUI/src/main/webapp/cm/widgets/perc_template_metadata_dialog.js:77` -- *Justification*: : adjudicated intentional (issue #6701, same as #5897/#5898/#5910/#5924/#5927) — slash joins URL segments, not an OS path. (via in_source_mkd)

</details>
