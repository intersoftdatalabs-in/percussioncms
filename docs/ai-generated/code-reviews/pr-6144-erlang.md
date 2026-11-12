Status: merged (Erlang LGTM; CodeQL java-kotlin + CodeQL gates green, MERGEABLE)
PR: #6144 — [#6056] deployer vendored HTTPClient → java.net.http migration

## Summary

Machine analysis found **12** finding(s) (7 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 4 analyzed
- In-diff: 7 finding(s); preexisting: 5
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/client/PSDeploymentServerConnection.java:169 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `PSDeploymentServerConnection` cognitive=23 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/client/PSDeploymentServerConnection.java:458 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `execute` cognitive=22 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/client/PSDeploymentServerConnection.java:630 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `execute` cognitive=20 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/client/PSDeploymentServerConnection.java:788 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `execute` cognitive=22 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/client/PSDeploymentServerConnection.java:1625 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getRequestPage` cognitive=18 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

### Suppressed Findings (7)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `paths.hardcoded_sep` on `deployer/src/main/java/com/percussion/deployer/client/PSDeploymentServerConnection.java:1281` -- *Justification*: : RFC 2046 wire literal (CRLF, quoted disposition (via in_source_mkd)
- `paths.hardcoded_sep` on `deployer/src/test/java/com/percussion/deployer/client/PSDeploymentServerConnectionWireTest.java:210` -- *Justification*: : XML attribute wire matcher, not a filesystem (via in_source_mkd)
- `paths.hardcoded_sep` on `deployer/src/test/java/com/percussion/deployer/client/PSDeploymentServerConnectionWireTest.java:237` -- *Justification*: : XML attribute wire matcher, not a filesystem (via in_source_mkd)
- `paths.hardcoded_sep` on `deployer/src/test/java/com/percussion/deployer/client/PSDeploymentServerConnectionWireTest.java:251` -- *Justification*: : XML attribute wire matcher, not a filesystem (via in_source_mkd)
- `paths.hardcoded_sep` on `deployer/src/test/java/com/percussion/deployer/client/PSDeploymentServerConnectionWireTest.java:280` -- *Justification*: : multipart wire matcher, not a filesystem (via in_source_mkd)
- `paths.hardcoded_sep` on `deployer/src/test/java/com/percussion/deployer/client/PSDeploymentServerConnectionXmlMultipartTest.java:61` -- *Justification*: : separator-family fixture vector proving basename (via in_source_mkd)
- `paths.hardcoded_sep` on `deployer/src/test/java/com/percussion/deployer/client/PSDeploymentServerConnectionXmlMultipartTest.java:71` -- *Justification*: : separator-family fixture vector proving basename (via in_source_mkd)

</details>



## Machine analysis — Erlang (approve, 0 blocking bugs)

- Base: origin/main; Files: 4 analyzed; In-diff: 7 findings (all suppressed false positives below); preexisting: 5 complexity suggestions (untouched legacy methods)
- Recommendation: approve — Blocking bugs: 0 — May commit/push: yes
- Suppressed (verified false, documented inline; see report for full text):
  - 2× RFC 2046 multipart wire literals (CRLF/quoted disposition) in the new encoder — protocol bytes, not filesystem joins
  - 1× XML wire-fixture + 4× XML-attribute wire matchers in tests — quotes are XML delimiters / URL-path and MIME assertions, not OS paths

## Notes

- Deliberate wire deltas (behavior-preserving, proven by wire tests): chunked upload body instead of buffered Content-Length (server parses from the de-chunked stream); `Connection: Keep-Alive` no longer sent explicitly (client-managed); `System.out` rePOST notice kept as-is.
- Residual: 8 `PSLegacyEncrypter`/`PSCryptographer` deprecation diagnostics in the same file belong to the crypto slice — open issue #6057, untouched here.
- Product-docs: N/A (internal transport migration; no operator/admin/integrator-facing surface change).

