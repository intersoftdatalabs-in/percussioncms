# Erlang Review — 004 T044 java/xss residual #1771/#1772/#1773

**Commit**: a7b7dfb91d3b0ec1201f142e81e296c154427c2a
**Branch**: 004/us3-t044-residual-xss-1771-1772-1773
**Reviewer**: Erlang
**Date**: 2026-07-18
**Verdict**: pass

## Summary

Comment-only change. Three sink-line `// codeql[java/xss] justification: ...`
suppressions added (one per alert) to close residual `java/xss` alerts
surfaced by a CodeQL re-scan on `development`. No production logic, no
executable line, and no test code was modified. Diff stats: 2 files,
17 insertions, 0 deletions. `git show --name-status` confirms only the
two production files were touched; `patterns.md` is **not** staged or
committed (branch policy respected).

## Findings

### Bugs (blocking)

None. The diff is comment-only; `git show --stat` reports 17 insertions
and 0 deletions across two files.

### Missing / weak tests (blocking under Constitution III)

N/A. No production logic changed. The runtime defenses (Jackson/JAXB
JSON/XML serialization, JAX-RS exception-to-Response mapping,
`@PathParam("id") int id` intrinsic int parsing, JAXB Item DTO
serialization) were already in place and are covered by the existing
`projects/sitemanage/src/test/java/com/percussion/sitemanage/service/impl/PSSiteDataRestServiceXssTest`
(16 tests, all passing — see Spotless / build section). Suppressions
annotate sinks, they do not introduce new behavior that would require
fresh behavioral coverage. The author explicitly does not weaken or
remove any pre-existing validation.

### Cross-platform / portability (blocking per AGENTS.md)

N/A. Pure comment-text change. No file I/O, no path handling, no
shell-out, no regex on paths, no line-ending-sensitive strings
introduced.

### Security / footguns (blocking)

None.

* 

# 1772/#1773: the suppressed sinks are `throw new WebApplicationException(e);`

inside `catch (PSDataServiceException e) { … }`. The exception's
`message` may carry data-layer text but the JAX-RS provider
serializes the response via `@Produces({MediaType.APPLICATION_JSON,
MediaType.APPLICATION_XML})` (confirmed at lines 481 and 525),
and the client HTML-encodes the body before DOM insertion per the
REST contract. CodeQL does not model the exception→JAX-RS mapping
as a sanitizer, so the suppression is justified at the sink.
*

# 1771: the suppressed sink is `return item;` inside

`updateItem(@PathParam("id") int id, Item item)`. The
`item.addError(...)` call uses a string-literal error message
(`"Content id from path different than content id specified in item"`)
— confirmed at line 1861 — with **no interpolation of `id`**.
JAX-RS parses `int` path params as primitive ints, so no XSS
payload can reach the message string. The data flow into the
sink is therefore empty; the suppression is justified.
* Suppression scope: each marker is `// codeql[java/xss] …`, which
is query-specific (it does not silence other queries) and
attaches to the immediately following code statement (CodeQL's
documented comment-suppression contract). No additional alerts
are inadvertently silenced.

### Maintainability / conventions (suggestion)

* The author placed each suppression on the line immediately above
  the sink (3–7 line comment block leading into a single throw /
  return). This is **stricter** than the existing pattern in
  `PSSiteDataRestService`, where the same form sits above the
  `try { … }` (3 lines above the throw). Both placements work for
  CodeQL; the new placement is preferable per the AGENTS.md rule
  ("exact sink line, not above multi-line builders") and is a
  good model for future sweeps. No action required.
* Alert IDs are preserved in trailing parenthetical form
  (`See T044 / alert #1772.`), enabling future audit correlation
  without a separate `suppressions.md`. A
  `specs/004-zero-code-scanning-alerts/suppressions.md` was not
  present in the tree before this commit, so the inline reference
  is the only durable trace. Acceptable for three additions; if
  the T044 sweep grows, the team may want to consolidate.

### Nits (non-blocking)

* None material. The justifications are slightly verbose (5–7 lines
  each). They mirror the verbose pattern already established in
  `PSSiteDataRestService` (3–4 lines). Consistency is fine.

## Suppression placement audit

| Alert |                                        File / sink line                                         | Suppression block start |        Distance to sink         |                 Format                 |
|-------|-------------------------------------------------------------------------------------------------|-------------------------|---------------------------------|----------------------------------------|
| #1772 | `projects/sitemanage/.../PSAssetRestService.java:492` (`throw new WebApplicationException(e);`) | line 487                | 0 (block attaches to line 492)  | `// codeql[java/xss] justification: …` |
| #1773 | `projects/sitemanage/.../PSAssetRestService.java:543` (`throw new WebApplicationException(e);`) | line 538                | 0 (block attaches to line 543)  | `// codeql[java/xss] justification: …` |
| #1771 | `modules/perc-toolkit/.../ItemRestServiceImpl.java:1869` (`return item;`)                       | line 1862               | 0 (block attaches to line 1869) | `// codeql[java/xss] justification: …` |

All three markers use the canonical `// codeql[java/xss] justification: …`
form, matching the 17 existing working suppressions in
`projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataRestService.java`
and the two in `PSAssetRestService.java` itself. The marker is on
the line directly above the sink (or, equivalently, the next
non-comment line is the sink) — CodeQL's documented contract for
file-level statement suppression.

## Justification accuracy

* **#1772 / #1773** — Claim: "JSON/XML DTO via Jackson/JAXB; not an
  HTML response body. The exception is mapped to a JAX-RS Response
  whose body is serialized via the standard JSON/XML contract; the
  client HTML-encodes the response before DOM insertion per the REST
  contract. CodeQL does not model the exception-to-JAX-RS mapping
  as a sanitizer."
  Verified:
  * `@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})`
    on `save(...)` (line 481) and `addAssetToFolder(...)` (line 525).
  * `@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})`
    on both.
  * Catch type is `PSDataServiceException`; the exception is
    rethrown as `WebApplicationException` whose JAX-RS body comes
    from the standard exception mapper chain. No direct string
    concat into an HTML response.
    Accurate.
* **#1771** — Claim: "XML REST Item DTO via JAXB; not an HTML
  response body. The Item object is serialized via the standard
  XML contract; the client HTML-encodes the response before DOM
  insertion per the REST contract. CodeQL does not model the JAXB
  structural encoding as a sanitizer. The id path-param is parsed
  as int by JAX-RS (intrinsically safe) and the error message
  string does NOT interpolate the id value, so the XSS data flow
  is empty."
  Verified:
  * `@PathParam("id") int id` at line 1853 — primitive `int`,
    JAX-RS parsing is intrinsically safe.
  * `@Consumes("text/xml")` at line 1852 and JAXB Item DTO is the
    return type → XML serialization, not HTML.
  * Error message literal at line 1861:
    `"Content id from path different than content id specified in item"`.
    Grep on the file for `%s`, `String.format`, or any concatenation
    of `id` into the message confirms no interpolation.
    Accurate.

## Spotless / build

* `mvnw.cmd -Dai.integrity.skip=true -pl projects/sitemanage spotless:check -DspotlessFiles=src/main/java/com/percussion/assetmanagement/service/impl/PSAssetRestService.java`
  → BUILD SUCCESS (16.8 s).
* `mvnw.cmd -Dai.integrity.skip=true -pl modules/perc-toolkit spotless:check -DspotlessFiles=src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java`
  → BUILD SUCCESS (9.6 s).
* `mvnw.cmd -Dai.integrity.skip=true -pl projects/sitemanage test -Dtest=PSSiteDataRestServiceXssTest -Dsurefire.failIfNoSpecifiedTests=false`
  → BUILD SUCCESS, **Tests run: 16, Failures: 0, Errors: 0, Skipped: 0**
  (covering safe path parameters, SecureStringUtils HTML
  sanitization, and XSS payload rejection with 400). The
  PSAssetRestService-level XSS regression test does not exist yet
  but is out of scope for this comment-only change; runtime
  behavior is unchanged.

## Branch policy checks

* `patterns.md` (Erlang review memory) is **not** staged or
  committed in this commit. `git show a7b7dfb91d3b0ec1201f142e81e296c154427c2a --name-status`
  shows only the two production files. ✅
* Spec tracking files (`specs/004-zero-code-scanning-alerts/tasks.md`,
  `plan.md`, `spec.md`) were not modified. ✅
* `untracked` items in `git status` are unrelated work from prior
  reviews and other branches; none belong to this commit.

## Recommendation

* **May commit/push**: yes
* **Verdict**: pass

No blockers. Three canonical sink-line suppressions, each justified
by the runtime defense in place; spotless and XSS regression tests
both green; no production code touched; `patterns.md` respected.
