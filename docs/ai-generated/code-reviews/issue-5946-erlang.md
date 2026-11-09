## Summary

Machine analysis found **5** finding(s) (5 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- In-diff: 5 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No active issues._

### Suppressed Findings (5)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `modules/perc-qa-automation/frontend/tests/helpers/auth.js:531` -- *Justification*: : adjudicated false positive (issue #5946) — runtime credential parameter from env, never a literal secret. (via in_source_mkd)
- `secrets.heuristic` on `modules/perc-qa-automation/frontend/tests/helpers/auth.js:568` -- *Justification*: : adjudicated false positive (issue #5946) — runtime credential parameter from env, never a literal secret. (via in_source_mkd)
- `secrets.heuristic` on `modules/perc-qa-automation/frontend/tests/unit/auth-fill-login-form.test.js:190` -- *Justification*: : adjudicated false positive (issue #5946) — expected fill recording of a dummy test value, no real secret. (via in_source_mkd)
- `secrets.heuristic` on `modules/perc-qa-automation/frontend/tests/unit/auth-fill-login-form.test.js:203` -- *Justification*: : adjudicated false positive (issue #5946) — expected fill recording of a dummy test value, no real secret. (via in_source_mkd)
- `patterns.hard_gate` on `modules/perc-qa-automation/frontend/tests/helpers/auth.js:405` -- *Justification*: : adjudicated intentional tests.empty-catch (issue #5946) — best-effort listbox dismiss; rejection means already closed. (via in_source_mkd)

</details>

