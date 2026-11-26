# Erlang review — issue #7324 (sanitized)

> Sanitized with scripts/sanitize-erlang-review.py. Static gate clean; Codex P2s addressed in-diff.

## Summary

Machine analysis found **30** finding(s) (30 suppressed), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- In-diff: 0 finding(s); preexisting: 30
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No active issues._

### Suppressed Findings (30)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `docker/scripts/perc-devctl.py:812` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - help-text vocabulary, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/perc-devctl.py:2599` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - QA env-parsing helper; password vocabulary in docstring/key names only, value read live from the cell file, never hardcoded. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:86` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:853` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1158` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1159` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1160` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1161` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1162` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1320` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1321` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1322` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1323` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1324` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1449` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1450` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1451` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1452` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1453` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:1721` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_matrix_install_smoke.py:2214` -- *Justification*: : adjudicated FP (issue #6778, precedent #6569/#6610/#6341) - test-only placeholder, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:108` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - subcommand-name literal, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:953` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:954` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:955` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:968` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:969` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:973` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder assertion, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:974` -- *Justification*: : adjudicated FP (issue #6610) - banner label assertion, no credential value. (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/test_perc_devctl.py:866` -- *Justification*: : adjudicated (issue #6570, parent #6341) - container-internal HEALTHCHECK path in mocked docker-inspect JSON, never a host path. (via in_source_mkd)

</details>

### Codex review (sanitized)

> All three [P2] findings were addressed in-diff before the PR:
> (1) readiness probed after every start/restart via probe_readiness;
> (2) step timeouts convert to rc=124 evidence, verdict + teardown always run;
> (3) cell removal asserted via remove_cell_container, teardown failure fails the run.

The lifecycle command can report success without verifying that restarted applications work or that container cleanup succeeded. Timeout failures also discard diagnostic evidence before producing the command's structured result.

Full review comments:

- [P2] Verify application readiness after each service restart — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/docker/scripts/perc-devctl.py:3789-3795
  The invoked lifecycle runner probes HTTP only after the initial start. After the stop/start cycle and restart, it checks only systemd activity and status, so a running JVM with a failed Rhythmyx application context can still produce an OK verdict. Run the existing log-aware in-container health check after every start/restart before declaring the lifecycle successful.

- [P2] Preserve failure evidence when lifecycle commands time out — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/docker/scripts/perc-devctl.py:3795-3803
  If an installer, systemctl command, or readiness request exceeds its timeout, `run_service_lifecycle` propagates `subprocess.TimeoutExpired`. This finally block then removes the container, while `_qa_service_lifecycle_finish` is never reached: no step evidence or structured FAIL result is written, and the remaining diagnostic logs disappear with the cell. Convert timeout exceptions into a failed lifecycle result and capture available evidence before teardown.

- [P2] Check container removal before reporting successful teardown — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/docker/scripts/perc-devctl.py:3798-3803
  When `docker rm -f` fails, its return code and stderr are discarded, and the command still returns OK and prints `cell-removed`. This leaves a privileged systemd cell running while reporting that cleanup succeeded. Check the removal result and report a teardown failure instead of claiming the cell was removed.
