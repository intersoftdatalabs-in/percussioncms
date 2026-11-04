## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 2 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/DatabaseDeliverySchemaPanel.test.tsx:63 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 63)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/test/ts/publishing/deliveryServerDatabaseSchema.test.ts:103 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 103)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=30 (max 15), cyclomatic=30 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Agent interpretation

Gate on in-diff **bugs**, missing behavioral tests, and non-portable paths only.

- Issues 1–2 are the same fake test password fixture used by Oracle SID / MSSQL owner peers (`secret-pass` / `s3cret-ora-password`). They exist so Vitest can assert the PUT body and the panel never include the stored secret. Not a real secret. Not a blocker.
- Issue 3 is preexisting cognitive complexity on `updatePubServer` (this slice only adds a schema-only branch peer to SID/owner). Not a blocker.

Behavioral tests are present: `PSDatabaseDeliverySchemaUpdateTest`, Vitest unit + panel tests, and H2 Playwright `sitesDeliveryServerDatabaseSchema.spec.js`. No new filesystem path construction.

**May commit/push: yes.**
