# Erlang review — PR #7396 (issue #7387, parent #2626)

Status: **approve** — static gate clean (0 blocking bugs), Codex [P2]s only (recorded, non-blocking).
Erlang persona: 0.1.1. Cycle: stonemonkey-20261128T184051Z. Reviewer: independent erlang-review (not the Work implementer).
Checks at review: CodeQL pass, Analyze (java-kotlin) pass, product-docs build smoke pass, signed-commits pass (ubuntu + windows).

## Summary

Machine analysis found **1** finding(s) (1 suppressed), **0** blocking bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 4 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No active issues._

### Suppressed Findings (1)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `paths.hardcoded_sep` on `modules/perc-packages/src/main/java/com/percussion/packages/pagexml/PSFleetBindingOrderConformance.java:159` -- *Justification*: : adjudicated intentional (issue #7387, (via in_source_mkd)

</details>

Erlang verification of the suppression: VALID. The `/` join in `displayKey(...)` builds a
display-only report key (comment on the method states segments are never passed to
`Path`/`Files`); the inline `mkd-code-review[paths.hardcoded_sep]` cites issue #7387 plus
precedents (#7273/#5897/#5898/#5910/#5927/#6195/#7272). Suppression satisfies the gate.

Change-class closure: new final utility class + new conformance test + pilot doc update;
build evidence (`cd modules/perc-packages && ../../mvnw clean install` → BUILD SUCCESS,
371 tests, 0 failures) recorded in the PR body. No cross-platform path findings.

### Codex review

Codex (gpt-6.1-sol, `codex review --base origin/main`, read-only) returned two [P2]
(non-blocking) findings, reproduced by Codex against a synthetic manifest:

- [P2] Count distinct manifests rather than template findings — `modules/perc-packages/src/main/java/com/percussion/packages/pagexml/PSFleetBindingOrderConformance.java:277-279`
  When a manifest declares multiple templates, `getManifestCount()` counts each template as a separate manifest. A single file containing two templates reports two manifests. This also weakens `fleet_coversAllPagesAndWidgetsManifests`: adding templates can conceal deleted manifests while still satisfying its 77-manifest floor. Track the discovered file count separately from per-template findings.

- [P2] Include invalid bindings in the hard-binding defect report — `modules/perc-packages/src/main/java/com/percussion/packages/pagexml/PSFleetBindingOrderConformance.java:306-308`
  For any null binding entry or blank variable, `PSComponentPackageManifestValidator.validateCollecting()` already adds a manifest error, so this exclusion makes `getHardBindingDefectFindings()` always empty for precisely the defects it promises to report. A manifest containing both defects produces a summary of `0 hard binding defects`, and consumers cannot retrieve the affected templates through this API. Include these findings even when validation also reports errors, and add a malformed-manifest regression test.

Erlang adjudication (verified against the PR head):

- P2-1 is documented behavior, not a defect: the javadoc reads "Number of manifests
  (templates) scanned". A separate discovered-file counter would be a nicer API, but the
  floor-gaming scenario requires deleting a real manifest while adding templates — the
  fleet gate still fails loudly on any unreadable/invalid manifest. Recorded; not blocking.
- P2-2's mechanism is real (validator lines ~177-181 add manifest errors for null/blank
  bindings, and `getHardBindingDefectFindings()` excludes errored findings), but there is
  no data loss: affected templates remain reachable via `getManifestErrorFindings()`.
  Whether the hard-defect accessor should also include errored rows is an API-shape choice
  for a follow-up, not a merge blocker. Recorded; not blocking. No residual issue filed
  (in-diff P2 design note, neither a preexisting machine row nor a complexity row).

Verdict: **approve**. No BLOCK findings. No residual tech-debt rows (0 preexisting /
complexity rows in PR-touched files).
