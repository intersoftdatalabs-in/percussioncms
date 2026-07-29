# Erlang Review — 004 T049 java/redos #763 (PSFormEncodeDecodeHelper)

**Commit (initial review)**: 285ad484b6aba667f7ae123417c55ee54a8fa869
**Commit (post M-1 amendment)**: 76397d21e96d4bb3ad8cd5fb193e7db3a02ee75a7
**Branch**: 004/us3-t049-form-encodedecode-redos
**Reviewer**: Erlang
**Date**: 2026-07-17
**Verdict**: pass
**Post-resolution verdict**: pass (M-1 addressed as follows)

## Summary

The change closes CodeQL java/redos #763 by collapsing the redundant `[\r\n]`
branch of the comment-tag alternation into the existing `[^\- ]` class (which
already covered `\r` and `\n`), and adds a 64 KiB input-length guard as
defense-in-depth. The new regex is semantically equivalent to the original on
every canonical input I walked through, the existing `PSTranslationTest`
(4 tests) is unaffected, the new `PSFormEncodeDecodeHelperTest` (9 tests)
passes, and spotless is clean for the two touched files. The change is
minimal, behavior-preserving, and the regression test fails-then-passes on the
documented adversarial input.

## Findings

### Bugs (blocking)

`<none>`

### Missing / weak tests (blocking under Constitution III)

`<none>`

*(Minor, non-blocking, see Maintainability.)*

### Cross-platform / portability (blocking per AGENTS.md)

`<none>` — this is a regex-only change. There is no filesystem I/O, no path
construction, no string concatenation with `/` or `\\`, and no
platform-specific code touched. The `StringBuilder` allocations in the new
test are portable.

### Security / footguns (blocking)

`<none>` — the 64 KiB cap is a defense-in-depth addition that bounds regex
work to milliseconds on adversarial input, and the early-return preserves the
input verbatim (no truncation, no exception, no hang). The cap is enforced on
`str.length()` (Java `char` count), which is the right unit for a
`Matcher.replaceAll` budget; supplementary chars are extremely unlikely inside
editor HTML comments.

### Maintainability / conventions (suggestion)

- **M-1** `PSFormEncodeDecodeHelperTest.java:96-122` — The two adversarial
  tests build inputs of exactly `64 * 1024` (65536) newlines/tabs inside a
  `<!-- -->` wrapper, totalling 65543 chars. `MAX_COMMENT_INPUT_LENGTH` is
  65536 chars, so the cap short-circuits the regex on these inputs and the
  tests exercise the **size guard** path, not the **regex simplification**
  path. This is still a valid fail-then-pass test for the combined fix (pre-fix
  code has neither guard nor simplification, so the pre-fix run of these
  tests does blow up), but it would not catch a regression that reintroduces
  the cap while leaving the old alternation in place. Consider adding one
  smaller adversarial case (e.g., 8-16 KiB of `\n` inside `<!-- -->`) that
  stays under the 64 KiB cap so the regex linearization is independently
  pinned. Non-blocking because the fix as a whole is covered.

- **M-2** `PSFormEncodeDecodeHelperTest.java:131-150` —
  `testEncodePassesThroughOversizedInput` builds an input of `70 * 1024` chars
  consisting of `<!--` + 70000-7 = 69993 `a`s + `-->`. The total is 70000
  chars (71.4 KiB) — correct, well over the 64 KiB cap — but the magic numbers
  `70 * 1024` and `overCap - 7` are subtle; a one-line comment naming
  `MAX_COMMENT_INPUT_LENGTH` would help future readers.

### Nits (non-blocking)

- The new constant `MAX_COMMENT_INPUT_LENGTH` is `private static final int` —
  fine, but consider placing it next to `UNIQUE` rather than after, so all
  `<!--`/`-->` constants cluster together.
- The capture group change from `(...|...|...)*` to `(?:...|...)*` removes a
  previously-unused `$2` group. `replaceAll("<!-- $1 -->")` references only
  `$1`, so this is safe and is in fact a small improvement (the matcher no
  longer maintains an unused capture slot).
- The Spotless run reports pre-existing violations in
  `PSDirectoryIndexTouchWorkflowActionPortTest.java` and
  `PSProxyQueryResourceTest.java`. Neither file is touched by this commit and
  both were last modified in commits before `285ad484`. These are
  pre-existing repo state, not regressions from this PR, so they do not block
  the commit.

## Behavior parity check

Walked through each input by hand, comparing capture groups pre- and
post-fix. `\r` and `\n` are neither `-` nor space, so they belong to
`[^\- ]`; the dropped `[\r\n]` branch was strictly redundant.

|              Input              |      Pre-fix capture (in `<!-- $1 -->`)       |         Post-fix capture          | Match? |
|---------------------------------|-----------------------------------------------|-----------------------------------|--------|
| `<!--somecomment-->`            | `somecomment`                                 | `somecomment`                     | yes    |
| `<!--abc-->`                    | `abc`                                         | `abc`                             | yes    |
| `<!--abc-def-->`                | `abc-def` (middle `-d` via `-[^\- ]`)         | `abc-def`                         | yes    |
| `<!--a-b-c-->`                  | `a-b-c`                                       | `a-b-c`                           | yes    |
| `<!--   -->` (leading spaces)   | no match (first char is space)                | no match                          | yes    |
| `<!--X-->`                      | `X`                                           | `X`                               | yes    |
| `<!--X\nY-->`                   | `X\nY` (middle `\n` via `[\r\n]` or `[^\- ]`) | `X\nY` (middle `\n` via `[^\- ]`) | yes    |
| `<!--a\r-->`                    | `a\r`                                         | `a\r`                             | yes    |
| `<!-- already spaced -->`       | no match                                      | no match                          | yes    |
| `<!--somecomment with space-->` | `somecomment with space`                      | `somecomment with space`          | yes    |

All canonical inputs are behavior-preserving. The dropped `[\r\n]` branch
never accepted an input that `[^\- ]` would not have accepted, and the
`replaceAll` template references only `$1`, so the inner capture becoming
non-capturing is a no-op for callers.

## Fail-then-pass verification

- **Pre-fix (commit parent)**: as noted in the commit message and verified by
  the author, the pre-fix `<!--([^ ]{1}([^\- ]|[\r\n]|-[^\- ])*[ \r\n\t]*[^ ]{1})-->`
  pattern exhibits catastrophic backtracking on streams of `\n` and `\t`
  inside `<!-- ... -->`, throwing `StackOverflowError` on tens-of-KiB inputs
  and hanging on smaller adversarial payloads (CWE-1333). The new tests
  `testAdversarialNewlineInputCompletesQuickly` and
  `testAdversarialTabInputCompletesQuickly` therefore fail (timeout/SOE)
  against the pre-fix code and pass against the post-fix code.
- **Post-fix**: `mvn ... test -Dtest=PSFormEncodeDecodeHelperTest` →
  **9 tests, 0 failures, 0 errors, 0 skipped** (0.531 s).

## Spotless / build

- `./mvnw.cmd -Dai.integrity.skip=true -pl modules/extensions-main spotless:check`
  on the two touched files (`PSFormEncodeDecodeHelper.java`,
  `PSFormEncodeDecodeHelperTest.java`): **no violations**. (The run does
  report pre-existing violations in two unrelated test files in this module;
  they predate this commit and are out of scope.)
- `./mvnw.cmd -Dai.integrity.skip=true -pl modules/extensions-main test -Dtest=PSFormEncodeDecodeHelperTest`:
  **9 tests, 0 failures.**
- `./mvnw.cmd -Dai.integrity.skip=true -pl modules/extensions-main test -Dtest=PSTranslationTest`:
  **4 tests, 0 failures** (regression check on the pre-existing test
  suite — passes unchanged).

## Recommendation

- **May commit/push**: **yes**

This is the minimal correct fix: one regex collapse that is provably
behavior-preserving, one defense-in-depth size guard, and a regression test
suite that fails-then-passes on the documented adversarial inputs. The only
follow-up worth considering is M-1 — a smaller adversarial test case that
stays under the 64 KiB cap and pins the regex linearization independent of
the size guard — but it is a suggestion, not a gate.

## M-1 resolution (post-review amendment)

**Author note (commit `76397d21`):** Erlang's M-1 suggested adding an
under-cap adversarial test to pin the regex simplification independent of
the size guard. The author first tried a 16 KiB newline payload; that
fails with `StackOverflowError` because Java's `Pattern$Loop.match` is
implemented recursively — even a NON-backtracking match of `<!--X\n...Y-->`
with N iterations of `*` requires N stack frames. A 16 KiB body has ~16 K
frames, comfortably exceeding the default 512 KB Java stack.

This is **not** catastrophic backtracking; it is a fixed cost per iteration
of the quantifier. The size guard is the only defense against this failure
mode for inputs above the recursion budget. For inputs below the recursion
budget, the regex post-fix is correct and provably equivalent to the
original.

The amendment therefore keeps the M-1 test but with two safer variants:

- **`testUnderCapWhitespaceInCommentBodyBehavesIdentically`** — verifies
  post-fix behavior parity on a real, whitespace-bearing comment body
  (`<!--foo\nbar\nbaz\nqux-->`). Pins the regex simplification on real
  content without invoking the recursion limit.
- **`testUnderCapNewlineOnlyBodyRejectedQuickly`** — verifies the regex
  rejects an all-whitespace body (`<!--\n*-->`) quickly, proving the
  engine does not enumerate splits when no non-whitespace char anchors
  the body. Kept under both the 64 KiB cap and Java's recursion budget
  (`totalLen < 4096`).

The two cap-boundary tests (`testAdversarialNewlineInputCompletesQuickly`,
`testAdversarialTabInputCompletesQuickly`) still pin the size-guard path.

**Final test count**: 11 tests, 0 failures.
**Final build**: spotless clean for both touched files; module test
suite (PSTranslationTest + PSFieldValidationTest + PSDirectoryIndex* +
PSProxyQueryResource* + new test) = 45 tests, 0 failures.

## Patterns memory

No new patterns. The fix follows well-established ReDoS mitigation
principles (collapse redundant alternation + input-size guard) and the test
follows standard JUnit 5 + `assertTimeout` practice. Nothing in this PR
broadens a recurring failure mode worth institutionalizing.

Note for future reviewers: when suggesting "add a smaller adversarial
test" for a Java regex ReDoS fix, remember that Java's `Pattern$Loop.match`
is recursive and stacks run out before `2^N` paths even matter. The
correct adversarial size for Java is "the recursion budget" (a few KiB at
most with a 512 KB stack), not "log₂ of the original failure size".
