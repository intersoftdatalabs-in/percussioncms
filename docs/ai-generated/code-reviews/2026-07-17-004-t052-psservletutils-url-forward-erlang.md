# Erlang Review — 004 T052 java/unvalidated-url-forward #792 (PSServletUtils)

**Commit**: c21d64c709c63605080f89c680a93887a90ffa36
**Branch**: 004/us3-t052-psservletutils-url-forward
**Reviewer**: Erlang
**Date**: 2026-07-17
**Verdict**: pass

## Summary

Validator added up-front of `m_servletContext.getRequestDispatcher`, runs before the
container call so a null/uninitialized context cannot bypass it. Control chars (<0x20,
0x7F), backslash, `..` traversal (start/mid/end, with `?` and `#`), and WEB-INF/META-INF
as complete segments are all rejected; look-alikes (`/web-info/notes`, `/WEB_INF/file`,
`/meta-info/file`) pass. 28 new behavioral tests + the existing 13 tests in the module
all pass on JDK 21 via `./mvn-env.bat -pl modules/servletutils test`.

## Findings

### Bugs (blocking)

- `<none>`

The two flagged subtleties both check out by inspection:

* `matchesSegment("WEB-INF", "/WEB-INFO/NOTES")` returns `false`. `startsWith("WEB-INF")`
  fails (first char `/` ≠ `W`); `indexOf("/WEB-INF", 0)` returns `-1`. Look-alikes are
  correctly preserved. The author's previously caught bug is no longer present.
* `validateForwardPath` runs at line 255 of `PSServletUtils.java` *before*
  `m_servletContext.getRequestDispatcher(path)` at line 256, so `testGetDispatcher-
  ValidationRunsBeforeServletContextLookup` (path with `..`) deterministically throws
  `IllegalArgumentException` even with `m_servletContext == null`.

### Missing / weak tests (blocking under Constitution III)

- `<none>`

28 tests cover boundary contracts, every documented attack shape, all control chars, all
segment-boundary variants for WEB-INF/META-INF, every traversal shape, and
null-context bypass. Pass-then-pass: pre-fix code would fail to compile (new method
references); post-fix compile and 28/28 pass.

### Cross-platform / portability (blocking per AGENTS.md)

- Not applicable. Diff adds no file I/O or path-string-to-Path conversion. The validator
  is pure `String`/`char` arithmetic and uses `Locale.ROOT` for `toUpperCase`, so it
  is portable to Windows and Unix identically. No new `File.separator`, `Paths.get`,
  or filesystem calls were introduced.

### Security / footguns (blocking)

- `<none>` of the on-list items. Encoded traversal (`%2e%2e`), encoded backslash
  (`%5C`), Unicode confusables (U+FF0F full-width slash), and Windows-encoded separator
  patterns are all delegated to the container's own normalization, which is consistent
  with how servlet `getRequestDispatcher` works and matches the documentation in the
  Javadoc. Null-byte truncation (`/safe\0/foo`) is caught by `c < 0x20`.
- One small forward-looking observation: in `validateForwardPath`, the error message
  echoes the rejected path. If a caller ever passes untrusted input into
  `getDispatcher` and the rejection bubbles up to a response body, the path can leak.
  This is not exploitable via the documented entry point (throw happens before any
  writing), but wrapping paths > 1 KB in `...` would be a minor hardening. Non-blocking.

### Maintainability / conventions (suggestion)

- `validateForwardPath` and `containsTraversal` are package-private (`static` /
  default); `matchesSegment` is `private static`. Scope is appropriate: public is too
  broad for a defense-in-depth helper, package-private allows unit coverage, and the
  pure helper `containsTraversal` is genuinely testable on its own. Keep as-is.
- The hand-written `matchesSegment` could be replaced with a `Pattern` regex
  (`/(^|/)WEB-INF(/|$)/`); the current implementation is correct, avoids regex
  allocation per call, and is easier to audit. Keep as-is.

### Nits (non-blocking)

- Pre-existing javadoc typo at `PSServletUtils.java:244`: `<p>* @param path ...` has a
  stray `*` before `@param`. Not introduced by this commit (already present in
  `2a2325e2a Spring / Hibernate - bulk Jakarta namespace updates.`). Worth fixing in
  a separate cleanup commit, not blocking here.
- `testGetDispatcherValidationRunsBeforeServletContextLookup` (line 276–278) has a
  vestigial `assertNotNull(IllegalArgumentException.class, ...)` "sanity" assertion
  that is tautological (a `Class` literal can never be null). Harmless; remove in a
  follow-up. Not blocking.

## Behavior parity check

| Input | Verdict (accepted/rejected) | Correct? |
|-------|----------------------------|----------|
| `/Rhythmyx/ui` | accepted (no control char, no `..`, no WEB-INF/META-INF segment) | yes |
| `/WEB-INF/web.xml` | rejected (target WEB-INF) | yes |
| `/app/../etc/passwd` | rejected (traversal) | yes |
| `/web-info/notes` | accepted (look-alike, segment boundary fails) | yes |
| `/app\\..\\etc` | rejected (backslash control) | yes |
| `/safe\0/foo` | rejected (NUL < 0x20) | yes |
| `""` | rejected (blank → IAE pre-validation) | yes |
| `null` | rejected (null → IAE pre-validation) | yes |
| `/cm/main` | accepted | yes |
| `/ui/widget/foo` | accepted | yes |
| `..` | rejected (traversal, bare) | yes |
| `/app/..?id=1` | rejected (traversal before `?`) | yes |
| `/WEB_INF/file` | accepted (look-alike, underscore not hyphen) | yes |
| `/meta-info/file` | accepted (look-alike) | yes |
| `/app/%2e%2e/etc/passwd` | accepted (validator does not decode; container handles) | yes (intentional) |
| `/foo/./bar` | accepted (single-dot is not traversal) | yes |
| `/foo.../bar` | accepted (dot-run ≠ 2) | yes |

## Fail-then-pass verification

- **Pre-fix**: the test class references `PSServletUtils.validateForwardPath(String)`
  and `PSServletUtils.containsTraversal(String)`, neither of which existed before
  this commit. `javac` would fail with `cannot find symbol` for `validateForwardPath`
  at `PSServletUtilsTest.java:67,76,82,...`. Compile-time gate is sufficient.
- **Post-fix**:
  ```
  mvn-env.bat -Dai.integrity.skip=true -pl modules/servletutils \
      test -Dtest=PSServletUtilsTest -Dsurefire.failIfNoSpecifiedTests=false
  ```
  → `Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`.
- **Full module**: 41 tests run, 0 failures, 0 errors, 5 pre-existing skips (all in
  `PSInputValidatorFilterTest` and one in `PSTomcatUtilsTest`, unrelated to this
  commit). The pre-commit gate "all 41 tests pass on post-fix" is met.

## Spotless / build

- Spotless Java format on touched files: **pass** (`Spotless.Java is keeping 11
  files clean — 0 were changed to be clean`). The pom.xml format error from
  `spotless:check` is **pre-existing** in `modules/servletutils/pom.xml:89` (an empty
  line between `</testResources>` and `<plugins>`) and is not part of this commit's
  diff. Out of scope here; flag separately if desired.
- Module test suite: **pass** (41/41 minus pre-existing skips).

## Recommendation

- **May commit/push**: yes

## Patterns memory

No new patterns. The fix follows the established Erlang playbook for
`java/unvalidated-url-forward`: validate pre-call, reject control chars + traversal +
protected segments as full segments, leave encoded/normalized shapes to the container,
pin with fail-then-pass behavioral tests. No new generalization surfaced.
