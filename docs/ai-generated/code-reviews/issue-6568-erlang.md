## Summary

Machine analysis found **2** finding(s) (2 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 1 analyzed
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

- `secrets.heuristic` on `docker/scripts/perc-devctl.py:761` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - help-text vocabulary, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/perc-devctl.py:2344` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - QA env-parsing helper; password vocabulary in docstring/key names only, value read live from the cell file, never hardcoded. (via in_source_mkd)

</details>
