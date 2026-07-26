# Erlang Review — 004 T044 java/xss suppression format fix (PSSiteDataRestService)

**Commit**: d77813784b0730068763519369da2f204ee9fe45
**Branch**: 004/us3-t044-pssitedatdarservice-xss-suppressions
**Reviewer**: Erlang
**Date**: 2026-07-18
**Verdict**: pass

## Summary

This commit replaces four inline `// codeql[java/xss] T044 #XXX: ...` comments with the canonical `// codeql[java/xss] justification: ...` form that CodeQL actually recognizes. No production logic changed. The runtime defense (`requireSafeId` allow-list validator) was already in place and is pinned by `PSSiteDataRestServiceXssTest` (16 cases, all passing). All four affected alerts (#750, #1063, #751, #752) should now close on the next CodeQL scan.

## Findings

### Bugs (blocking)

- None.

### Missing / weak tests (blocking under Constitution III)

- None. The change is a pure comment-text swap. The runtime defense (`requireSafeId`) and the data-flow analysis were already pinned by `PSSiteDataRestServiceXssTest` (4 safe-input cases + 3 SecureStringUtils HTML sanitization cases + 9 XSS-payload rejection cases = 16 tests, all green). No additional tests are required for a comment-only change.

### Cross-platform / portability (blocking per AGENTS.md)

- N/A. Pure comment text change, no file I/O or path handling.

### Security / footguns (blocking)

- None. The justification text accurately describes the data flow:
  - All four methods are annotated `@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})` (lines 158-159, 181-182, 259-260, 291-292), so the response body is JSON/XML serialized via Jackson/JAXB — not HTML.
  - The four endpoints accept typed JAXB/JSON DTOs (`PSSite`, `PSSiteProperties`, `PSSitePublishProperties`), not raw user strings, so the taint cannot enter the response payload unmediated by the JAXB/JAXB/Jackson type system.
  - CodeQL's `java/xss` query is taint-based against HTML sinks; JAXB/Jackson structural encoding is not modeled as a sanitizer, so the alerts fire. The suppression documents this design decision rather than re-engineering the response contract.
  - The path-parameter endpoints in this file (`load`, `find`, `delete`, `getSiteProperties`, `getSitePublishProperties`, `getSiteStatistics`, `isSiteBeingImported`) all gate on `requireSafeId(...)` (allow-list `[A-Za-z0-9._:\-]{1,100}`), rejecting XSS payloads with HTTP 400.

### Maintainability / conventions (suggestion)

- Minor: the suppression comment blocks are 6 lines each (163-168, 186-191, 263-268, 295-300), which is consistent with the verbose justification style used elsewhere in the same file (lines 97-106, 122-123, 142-145). Acceptable, no action needed.
- The justification text references `T044 / alert #XXX` in the last line, which provides an audit trail tying each suppression to the spec task and the GitHub alert number. Good practice.

### Nits (non-blocking)

- **Stale line numbers in the task prompt**: the task description cites sink line numbers 164, 182, 254, 281. After the +5 line expansion per hunk, the actual new sink line numbers are 169, 192, 269, 301 (suppression blocks at 163-168, 186-191, 263-268, 295-300 respectively). The suppressions are still positioned correctly (immediately above the sink in every case). This is a doc/prompt accuracy issue, not a code issue — no fix required in the commit itself.

## Suppression format audit

| Alert |                           Sink line (post-fix)                            | Suppression line (post-fix) |                  Format                  |                                         Recognized by CodeQL?                                          |
|-------|---------------------------------------------------------------------------|-----------------------------|------------------------------------------|--------------------------------------------------------------------------------------------------------|
| #750  | 169 (`return siteDataService.save(site);`)                                | 163-168                     | `// codeql[java/xss] justification: ...` | yes (matches existing pattern at lines 97, 122, 142, 194, 204, 219, 229, 245, 279, 325, 351, 369, 385) |
| #1063 | 192 (`return siteDataService.createSiteFromUrl(request, site);`)          | 186-191                     | `// codeql[java/xss] justification: ...` | yes                                                                                                    |
| #751  | 269 (`return siteDataService.updateSiteProperties(props);`)               | 263-268                     | `// codeql[java/xss] justification: ...` | yes                                                                                                    |
| #752  | 301 (`return siteDataService.updateSitePublishProperties(publishProps);`) | 295-300                     | `// codeql[java/xss] justification: ...` | yes                                                                                                    |

CodeQL suppression grammar (per the CodeQL CLI documentation and GitHub code-scanning docs) is `// codeql[<query-id>] <key>: <reason>` where `<key>` is one of the recognized suppression keywords. The legacy `lgtm` keyword is deprecated; the modern keyword is `justification:`. The pre-fix code used an unrecognized prefix `T044 #750:` (no recognized `<key>`), so CodeQL silently ignored the suppression and the alerts remained open. The post-fix code uses `justification:`, which is the canonical keyword and is identical to the form used by the existing 13 successful suppressions in the same file.

## Pre-fix vs post-fix behavior

|                           Aspect                            |             Pre-fix             |                       Post-fix                       |
|-------------------------------------------------------------|---------------------------------|------------------------------------------------------|
| Number of suppressions on the four sinks                    | 4                               | 4                                                    |
| Suppression keyword recognized by CodeQL                    | no (custom `T044 #XXX:` prefix) | yes (`justification:`)                               |
| Runtime defense (`requireSafeId`)                           | present                         | present (unchanged)                                  |
| `@Produces({APPLICATION_JSON, APPLICATION_XML})` on methods | yes                             | yes (unchanged)                                      |
| Typed JAXB/JSON DTO inputs                                  | yes                             | yes (unchanged)                                      |
| Production code logic                                       | unchanged                       | unchanged                                            |
| Comment text                                                | 1 line per suppression          | 6 lines per suppression (more verbose justification) |
| Number of production Java statements changed                | 0                               | 0                                                    |
| Test suite (`PSSiteDataRestServiceXssTest`)                 | 16/16 green                     | 16/16 green (re-verified in this review)             |

Net behavior change: zero. The change converts an unrecognized suppression token into a recognized one, which causes CodeQL to honor the suppression on the next scan. Runtime semantics, control flow, and the security posture are unchanged.

## Spotless / build

- `spotless:check` on `projects/sitemanage`: the touched file `PSSiteDataRestService.java` is clean ("Spotless.Format is keeping 7 files clean - 1 needs changes to be clean"). The single violation is in `projects/sitemanage/src/main/resources/com/percussion/pagemanagement/service/impl/WidgetRegistry.xml`, which is **pre-existing** and unrelated to this commit. No spotless fix required for this PR.
- `PSSiteDataRestServiceXssTest`: 16 tests run, 0 failures, 0 errors, 0 skipped (`Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`). Build success.

## Recommendation

- **May commit/push**: yes

The change is a minimal, surgical fix to the CodeQL suppression format. It correctly aligns with the canonical `// codeql[<id>] justification: ...` syntax that CodeQL recognizes, matches the existing pattern already used by 13 other suppressions in the same file, does not alter production logic, and does not weaken the runtime defense. The pre-existing XSS regression test suite continues to pass. This change should be merged and will close alerts #750, #1063, #751, and #752 on the next CodeQL scan.
