## Summary

Machine analysis found **33** finding(s) (32 suppressed), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 3 finding(s); preexisting: 30
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: docker/scripts/qa_service_lifecycle.py:570 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `run_service_lifecycle` cognitive=22 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (32)

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
- `secrets.heuristic` on `docker/scripts/test_qa_service_lifecycle.py:130` -- *Justification*: : adjudicated FP (issue #7324, (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/qa_service_lifecycle.py:56` -- *Justification*: : adjudicated intentional (issue #7324, (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/test_perc_devctl.py:866` -- *Justification*: : adjudicated (issue #6570, parent #6341) - container-internal HEALTHCHECK path in mocked docker-inspect JSON, never a host path. (via in_source_mkd)

</details>

---

## Codex review (post-static-gate LLM review)

The new harness cannot boot under Docker's default enforcing AppArmor profile and can falsely report successful application-health and journal assertions. The 26 focused lifecycle tests pass, but live Docker behavior was not verified.

Full review comments:

- [P2] Allow the cgroup remount under an appropriate AppArmor profile — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/docker/matrix/Dockerfile.systemd:40-40
  On Docker hosts enforcing the default `docker-default` AppArmor profile, `mount` is denied even with `SYS_ADMIN`. The generated run command does not select another profile, so this remount fails and the `&&` prevents systemd from starting, making both lifecycle products unusable on those hosts. Supply a narrowly scoped security profile permitting the required mount while preserving container isolation.

- [P2] Check application health rather than HTTP success alone — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/docker/scripts/qa_service_lifecycle.py:554-559
  For CMS runs, a successful HTTP response is accepted without checking application-context failure markers or the inherited Docker health check. Consequently, a running JVM with a broken Spring context can pass all three readiness assertions and produce a green lifecycle verdict. Invoke the existing in-container `rhythmyx_healthcheck.py` assessment instead of bare curl; [AGENTS.md:529](AGENTS.md#L529) explicitly requires context-aware health verification after Jetty restarts.

- [P2] Reject journalctl's no-entries placeholder — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/docker/scripts/qa_service_lifecycle.py:727-728
  When no records exist for the unit, `journalctl` can exit successfully and print `-- No entries --`. This nonempty placeholder passes the assertion, so missing unit-journal output is reported as captured evidence. The full fake-runner sequence also returns success with that response. Reject the placeholder or use structured journal output and require at least one actual record.
