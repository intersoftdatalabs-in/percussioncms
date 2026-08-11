# Erlang pre-commit review — issue #2760 (data rawtypes 4g)

**Date:** 2026-08-10  
**Reviewer persona:** Erlang (independent of implementer)  
**Scope:** residual `-Xlint` rawtypes/unchecked in `com.percussion.data` after #2693 / PR #2762

## Change class

Pure tech-debt generics modernization in the data pipeline (stylesheets, SQL builders/optimizers, JDBC type maps, statement extractors, extension runner boundary). No product behavior change intended.

## Companions

| Companion | Status |
|-----------|--------|
| Production generics fixes in data package | Done |
| Unit tests for typed helpers | Done (`*TypedTest`) |
| Product-docs | N/A — no operator/user-facing change |
| Playwright | N/A |
| Downstream modules | API signatures mostly package/private or return-type only; `runSearchResultProcessor` narrowed to `IPSSearchResultRow` matching caller |

## Findings

### Bugs
- None remaining after self-fix of `PSTableUpdateHandlerBase` (preserved original loop control; did **not** set `added=true` which would change multi-table listener registration).

### Tests
- Added behavioral unit tests for stylesheet filter rules, SQL builder context blocks, optimizer empty-col path, update/insert extractors, JDBC type-map signatures.

### Cross-platform paths
- No path I/O changes beyond existing filter singleton; tests use in-memory XML / reflection.

### Hard gates
- Prefer real generics; boundary suppress only where third-party (jericho) or SPI (`List<Object>` search rows) forces it.
- Stay out of security/server/HTTPClient.

## Verdict

**PASS** — ready for `system` standalone `mvnw clean install` gate.
