# Erlang + Codex pre-push review — issue #7323 (QA Apache frontend)

Static gate: `mkd-code-review analyze --pack percussion --format markdown
--gate advisory --git-base origin/main --machine-only` (no Ollama, no
`--require-llm`; the skipped LLM stage does not block when machine
findings are 0 in-diff). Follow-up passes adjudicated the container-path
`paths.hardcoded_sep` findings as false positives with inline
suppressions (issue #7323, precedent #6570) and fixed the residual
Windows-host test literals with Path-derived expectations. LLM review:
`codex review --base origin/main` (read-only); all three [P2] findings
were fixed in follow-up commits (suffix-scoped staging, no-follow
parity probe, Path-derived mount assertions).

## Summary

Machine analysis found **14** finding(s) (14 suppressed), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 3 finding(s); preexisting: 11
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

- `secrets.heuristic` on `docker/scripts/perc-devctl.py:818` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - help-text vocabulary, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/perc-devctl.py:2630` -- *Justification*: : adjudicated FP (issue #6568, precedent #6569/#6341) - QA env-parsing helper; password vocabulary in docstring/key names only, value read live from the cell file, never hardcoded. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:112` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - subcommand-name literal, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:936` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:937` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:938` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:951` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:952` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:956` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder assertion, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:957` -- *Justification*: : adjudicated FP (issue #6610) - banner label assertion, no credential value. (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/perc-devctl.py:2580` -- *Justification*: : adjudicated FP (issue #7323, (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/qa_apache_cell.py:108` -- *Justification*: : adjudicated FP (issue #7323, precedent #6570) — Linux-cell mount path, never a host join. (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/qa_apache_cell.py:109` -- *Justification*: : adjudicated FP (issue #7323, precedent #6570) — Linux-cell mount path, never a host join. (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/test_perc_devctl.py:849` -- *Justification*: : adjudicated (issue #6570, parent #6341) - container-internal HEALTHCHECK path in mocked docker-inspect JSON, never a host path. (via in_source_mkd)

</details>

### Codex review

```
The patch introduces shared staging across isolated cells, follows redirects in a first-hop parity check, and adds a test assertion that fails on Windows.

Full review comments:

- [P2] Isolate Apache fixture staging by cell suffix — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/docker/scripts/perc-devctl.py:2934-2934
  When two suffixed cells run from the same checkout, both use this staging directory and bind-mount its `site` tree. Starting the second cell invokes `build_fixture_tree`, which deletes and replaces the first cell's live `web_resources` directory, causing transient 404s and potentially replacing its assets with another revision's bytes. Scope both staging and pull directories by `ctx.cell_suffix`.

- [P2] Disable redirects for the service-parity probe — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/docker/scripts/qa_apache_cell.py:467-469
  `_qa_up_apache_service_parity` uses this helper to compare first-hop statuses, but `urlopen` follows redirects automatically. For the documented unseeded `/feeds/rss` redirect, it probes the error-document chain instead, potentially generating repeated backend errors or accepting different initial statuses because both eventually reach the same response. Use a non-redirecting request for parity, matching the Playwright test's `maxRedirects: 0`.

- [P2] Derive the certificate mount assertion from its Path — ~/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs/docker/scripts/test_qa_apache_cell.py:220-220
  On Windows, `cert_dir` stringifies with backslashes, so this hardcoded Unix-path assertion fails even when `build_apache_run_argv` produces the expected mount. Derive the expected string from `cert_dir` and `APACHE_CERTS_CONTAINER_PATH`, as the neighboring assertions do. This also satisfies the explicit cross-platform test requirement in [AGENTS.md:562](../../AGENTS.md#L562).

```
