# Erlang pre-push review — issue #5584

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD (`99d99c05d5` PublishingShell set an S3 delivery-server bucket (#5584))
- Files: 10 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Change class: PublishingShell field-only S3 delivery-server bucket update (existing publishmanagement PUT)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- In-diff behavioral tests: present (`PSAmazonS3DeliveryBucketUpdateTest`, Vitest helper + panel, Playwright `sitesDeliveryServerS3Bucket.spec.js`)
- Non-portable path/file I/O: none in this diff
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=31 (max 15), cyclomatic=31 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Gate: **not blocking** — preexisting on the full-update dispatcher; this slice adds one more field-only branch matching Oracle schema / MSSQL owner peers.

## Companions

- `PSAmazonS3DeliveryBucketUpdate` + unit tests (bucket-only detection, trim/replace, leave region/secrets/name/driver, blank/overlong no-write, Local/FTP/Oracle reject, secrets not in errors)
- `PSPubServerService` dispatch `isBucketOnly` → `updateS3BucketOnly`
- Vitest `deliveryServerS3Bucket` + `S3DeliveryBucketPanel`
- Playwright `sitesDeliveryServerS3Bucket.spec.js` (H2 overlay GET of a Local server)
- `product-docs/8.2/admin/publishing.md` section “Set an S3 delivery-server bucket (Sites)”
- Wired in `SiteWorkspace` next to the database/FTP field-only panels
- No rest module change (existing publishmanagement PUT)
- No rule files
- No new filesystem path joins

## CLI markdown (verbatim)

```
## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/pubserver/impl/PSPubServerService.java:357 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updatePubServer` cognitive=31 (max 15), cyclomatic=31 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
```
