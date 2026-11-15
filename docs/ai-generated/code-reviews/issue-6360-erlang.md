## Summary

Machine analysis found **4** finding(s) (4 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- In-diff: 0 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

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

- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/recycle/service/impl/PSRecycleService.java:115` -- *Justification*: : adjudicated intentional (issue #6359, same as #5898/#5910/#5927/#6325/#6358) — CMS repository path, not an OS path. (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/recycle/service/impl/PSRecycleService.java:118` -- *Justification*: : adjudicated intentional (issue #6359, same as #5898/#5910/#5927/#6325/#6358) — CMS repository path, not an OS path. (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/recycle/service/impl/PSRecycleService.java:1061` -- *Justification*: : adjudicated intentional (issue #6359, (via in_source_mkd)
- `paths.hardcoded_sep` on `projects/sitemanage/src/main/java/com/percussion/recycle/service/impl/PSRecycleService.java:1146` -- *Justification*: : adjudicated intentional (issue #6359, (via in_source_mkd)

</details>
