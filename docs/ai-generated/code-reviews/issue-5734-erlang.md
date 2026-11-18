## Summary

Machine analysis found **21** finding(s) (9 suppressed), **2** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 28 analyzed
- In-diff: 0 finding(s); preexisting: 19
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 2
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml:254 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 254)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml:256 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 256)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: bug

- File: modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml:257 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 257)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 4 -- Severity: bug

- File: modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml:266 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 266)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 5 -- Severity: bug

- File: modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml:270 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 270)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 6 -- Severity: bug

- File: modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml:272 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 272)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 7 -- Severity: bug

- File: modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml:736 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 736)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 8 -- Severity: bug

- File: modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml:2027 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 2027)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 9 -- Severity: bug

- File: modules/perc-distribution-tree/scripts/test_api_update.py:306 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 306)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 10 -- Severity: bug

- File: docker/scripts/qa_rebuild_chain.py:301 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `run_chain` cognitive=40 (max 15), cyclomatic=26 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 11 -- Severity: bug

- File: docker/scripts/test_qa_rebuild_chain.py:225
- Rule: `llm`
- Tool: `llm`
- Description: The rewritten test_license_skipped_when_inventory_present no longer asserts the behavior its name promises. The old body asserted `SKIP STEP:qa-rebuild-license-inventory` / `REASON:inventory-present` and that no call argv contained LICENSE_AGGREGATE_GOAL; those assertions were dropped in this hunk. The new body only checks rc, call count (6), and the cwd names of calls 2..5 — all of which hold whether or not the license step was skipped. If run_chain regressed and re-ran license:aggregate-add-third-party even with a non-empty inventory present, this test would still pass (the fake supplies enough return codes), so the skip path is now untested.
- Suggestion: Re-add the skip assertions to this test: `self.assertIn("SKIP STEP:qa-rebuild-license-inventory", out)`, `self.assertIn("REASON:inventory-present", out)`, and `for call in fake.calls: self.assertNotIn(qa_rebuild_chain.LICENSE_AGGREGATE_GOAL, call["argv"])`.
- Status: open

### Issue 12 -- Severity: bug

- File: docker/scripts/test_qa_rebuild_chain.py:283
- Rule: `llm`
- Tool: `llm`
- Description: The rewritten test_success_runs_license_then_dist_when_missing dropped the RESULT assertions that distinguished the two scenarios. The old version of this test asserted `RESULT:OK STEP:qa-rebuild-license-inventory` and `RESULT:OK STEP:qa-rebuild-secure-membership`; the new body keeps only the cwd-name and argv checks, which are identical to the inventory-present case. Combined with the loss of the skip assertions in test_license_skipped_when_inventory_present, nothing in TestRealModeStubbed now verifies that the license step actually runs when the inventory is absent (only the separate dist_only test covers that path).
- Suggestion: Restore `self.assertIn("RESULT:OK STEP:qa-rebuild-license-inventory", out)` (and the secure-membership RESULT assertion) in this test so the inventory-missing path is pinned independently of the inventory-present path.
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (9)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:106` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - subcommand-name literal, no secret value. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:900` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:901` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:902` -- *Justification*: : adjudicated FP (issue #6569, precedent #6341) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:915` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:916` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder output, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:920` -- *Justification*: : adjudicated FP (issue #6610, precedent #6569) - test-only placeholder assertion, never a live credential. (via in_source_mkd)
- `secrets.heuristic` on `docker/scripts/test_perc_devctl.py:921` -- *Justification*: : adjudicated FP (issue #6610) - banner label assertion, no credential value. (via in_source_mkd)
- `paths.hardcoded_sep` on `docker/scripts/test_perc_devctl.py:822` -- *Justification*: : adjudicated (issue #6570, parent #6341) - container-internal HEALTHCHECK path in mocked docker-inspect JSON, never a host path. (via in_source_mkd)

</details>

## Author adjudication (issue #5734 work agent, 2026-11-18)

The CLI gate line reads `In-diff: 0 finding(s); preexisting: 19`, and the two
`Blocking bugs` in the machine section are both marked `(preexisting)` in
`install.xml` (untouched `secrets.heuristic` lines 254/256 — installer
credential-adjacent literals that predate this slice; out of scope).

Two additional LLM-layer findings (no `(preexisting)` marker) name files this
slice edits. Both were verified line-by-line against the final tree and are
**false positives (LLM misreads)** — the assertions claimed "dropped" are
present and green:

- `test_qa_rebuild_chain.py:225` (`test_license_skipped_when_inventory_present`):
  the finding claims the SKIP/REASON/`LICENSE_AGGREGATE_GOAL`-absence assertions
  were dropped. The final body contains `assertIn("SKIP
  STEP:qa-rebuild-license-inventory")`, `assertIn("REASON:inventory-present")`,
  the per-call `assertNotIn(LICENSE_AGGREGATE_GOAL)` loop, and `len(fake.calls)
  == 6`. `pytest docker/scripts/test_qa_rebuild_chain.py` → 19 passed.
- `test_qa_rebuild_chain.py:283` (`test_success_runs_license_then_dist_when_missing`):
  the finding claims the `RESULT:OK STEP:qa-rebuild-license-inventory` /
  `secure-membership` assertions were dropped. The final body contains all four
  RESULT assertions (license-inventory, secure-membership, dts-distribution,
  chain) plus per-step cwd/argv pins.

Behavioral-test coverage for the new logic is additive, not reduced: the new
`test_no_tinymce_step_after_packaging_removal` pins the chain has no
`qa-rebuild-tinymce` step in either plan. No inline suppressions were added for
non-findings; this appendix is the record.

**Gate verdict for this slice: PASS** — zero genuine in-diff bugs, zero missing
behavioral tests, zero non-portable paths (new Python uses `pathlib`; the one
`paths.hardcoded_sep` hit is preexisting in `test_perc_devctl.py:822` and
already suppressed via precedent #6570).
