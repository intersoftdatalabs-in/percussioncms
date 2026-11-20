# Erlang review — issue #6728 (PSSite.java `secrets.heuristic`)

Adjudication slice: `system/services/src/com/percussion/services/sitemgr/data/PSSite.java:257`
(`@Column(name = "RESET_REQUEST_PASSWORD_PAGE")`) — verified false positive:
JPA column-name schema identifier for the site's password-reset-request page
path, not credential material (same FP class as #52/#50).

Command (models path redacted per sanitize policy):

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base "origin/main" \
  --models <models-dir>/models.ollama-dev-coder.toml
```

## Sanitized report

## Summary

Machine analysis found **2** finding(s) (2 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- In-diff: 2 finding(s); preexisting: 0
- Persona: erlang 0.1.1

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No active issues._

### Suppressed Findings (2)

- `secrets.heuristic` on `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md:117` — ledger row quoting the column name (self-referential heuristic hit on documentation; non-blocking).
- `secrets.heuristic` on `system/services/src/com/percussion/services/sitemgr/data/PSSite.java:258` — adjudicated FP (issue #6728, precedent #52): JPA column name for the page path, not a secret.

## Notes

- No LLM fallback was needed; machine-only result short-circuits as clean
  (0 in-diff blocking findings). No paid/OpenRouter route used.
- Change-class closure for this adjudication slice: inline suppression +
  `suppressions.md` ledger row (`mkd-6728`) + pinning unit test
  (`PSSiteResetPageColumnMappingTest`). No product-docs (no behavior change),
  no QA cell (no UI), no downstream signature changes.
