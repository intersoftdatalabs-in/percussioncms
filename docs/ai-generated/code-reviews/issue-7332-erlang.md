## Summary

Machine analysis found **14** finding(s) (14 suppressed), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
- In-diff: 0 finding(s); preexisting: 14
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No active issues._

### Suppressed Findings (14)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `docker/scripts/perc-devctl.py:819` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - help-text vocabulary, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/perc-devctl.py:2700` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - QA env-parsing helper; password vocabulary in docstring/key names only, value read live from the cell file, never hardcoded. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:112` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - subcommand-name literal, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:971` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:972` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:973` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:986` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:987` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:991` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder assertion, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:992` -- *Justification*: : adjudicated FP (issue #6610) - banner label assertion, no credential value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_qa_service_lifecycle.py:130` -- *Justification*: : adjudicated FP (issue #7324, (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/perc-devctl.py:2650` -- *Justification*: : adjudicated FP (issue #7323, (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/qa_service_lifecycle.py:68` -- *Justification*: : adjudicated intentional (issue #7324, (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/test_perc_devctl.py:884` -- *Justification*: : adjudicated (issue #6570, parent #6341) - container-internal HEALTHCHECK path in mocked docker-inspect JSON, never a host path. (via in_source_mkd)

</details>
