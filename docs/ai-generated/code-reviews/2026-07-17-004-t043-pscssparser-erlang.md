# Erlang Review — 004 T043 java/path-injection #1055/#1056/#1057 (PSCSSParser)

**Commit**: b4b1a987ab9c2261f1926cb6803515c104998edb
**Branch**: 004/us3-t043-pscssparser-path-injection
**Reviewer**: Erlang
**Date**: 2026-07-17
**Verdict**: pass

## Summary

The change closes three `java/path-injection` CodeQL alerts in `PSCSSParser` by inserting
`PSPathInjectionGuard.requireUnderBase(new File(themeRootDirectory), path)` immediately before
each of the three File / FileWriter / FileInputStream sink constructions (`fileExists`,
`saveFile`, `loadFileFromDisk`). The base directory is the same `themeRootDirectory` the
parser already threads through `PSURLConverter` and `PSHTMLHeaderImporter`, so containment
matches the parser's mental model. `requireUnderBase` resolves both sides via
`getCanonicalPath()`, normalizes separators (`replace('\\', '/')`), and enforces a directory
prefix check, so traversal payloads that escape the theme root throw
`IllegalArgumentException` before any I/O. The new test file
(`PSCSSParserPathInjectionTest`, 11 cases) uses `sun.misc.Unsafe.allocateInstance` to bypass
the constructor's `@notNull` preconditions and the logger dependency, then reflectively
invokes each private sink with adversarial and legitimate inputs. All 11 tests pass,
spotless is clean for both touched files (the one spotless failure in the module is in a
pre-existing XML resource unrelated to this commit), and the pre-existing
`PSCSSParserTest` (stubbed) is unchanged.

## Findings

### Bugs (blocking)

`<none>`

### Missing / weak tests (blocking under Constitution III)

`<none>` — 11 behavioral tests cover the three sinks with traversal, NUL byte, null,
relative /etc/passwd traversal, a relative-into-base "escape" path, and an end-to-end
CSS `@import('../../../etc/passwd')` payload. Fail-then-pass is established by the
`assertThrows(IllegalArgumentException.class, ...)` shape: pre-fix code returned
`new File(path).exists()` (silently false or read `/etc/passwd`) and would not throw IAE
on these inputs. The "accepts" cases for `fileExists`, `saveFile`, and
`loadFileFromDisk` prove legitimate in-root paths still flow through the sinks without
regression.

### Cross-platform / portability (blocking per AGENTS.md)

`<none>` — `requireUnderBase` uses `getCanonicalPath()` for both sides and normalizes
backslashes (`PSPathInjectionGuard.java:176-181`), so the same code works on Windows and
Unix. Test traversal payloads use absolute paths under `@TempDir` plus relative `..`
segments, which resolve to platform-portable escape destinations outside the theme root
on both OSes (`themeRoot/sub1/../../../../etc/passwd` canonicalizes to `…/etc/passwd` on
Unix and `…\etc\passwd` on Windows — both outside the temp dir). No new hardcoded `/` or
`\\` joins were introduced; no `Path`/`Files` was retro-fitted into this commit's logic
beyond what the helper already uses.

### Security / footguns (blocking)

`<none>` — The validator is called before any `File` / `FileWriter` / `FileInputStream`
construction in all three sinks (verified at `PSCSSParser.java:236-244`, `:270-280`,
`:286-294`). Null is rejected (`requireUnderBase` line 131-133). NUL bytes are rejected
(line 145-148). `themeRootDirectory` itself is a constructor-supplied server-side
value (not derived from the CSS payload), so a "malicious base" is out of the threat
model; even if the supplied base doesn't exist on disk, `requireUnderBase` line 134-136
raises a clear `IllegalArgumentException` instead of crashing with a generic
`NullPointerException` / `IOException` — a strict improvement over the pre-fix
`new File(path).exists()` (which would silently return `false` on a null path on some
JVMs).

### Maintainability / conventions (suggestion)

- **M-1** `PSCSSParserPathInjectionTest.java:300` (end of file) — the file lacks a
  trailing newline. POSIX convention and the rest of the repo's Java sources end with
  `\n`. Spotless is not flagging it on the touched file (`/D:/…/PSCSSParserPathInjectionTest.java:[37,16]`,
  `[68,24]`, `[75,17]`, `[77,17]` are all `sun.misc.Unsafe` proprietary-API warnings,
  not formatting errors), so this is cosmetic, but worth fixing in a follow-up so
  future `git blame` / line-oriented tools behave.

- **M-2** `PSCSSParserPathInjectionTest.java:104-115` — `cleanup()` walks the
  `@TempDir` and deletes contents. JUnit 5 `@TempDir` already auto-cleans after the
  test class, so this is redundant; it only matters if a mid-test assertion runs
  after another test has already touched the dir (which the current tests do not).
  Harmless; can be removed in a future cleanup. Non-blocking.

### Nits (non-blocking)

- `PSCSSParserPathInjectionTest.java:103-115` — `cleanup()` would itself throw
  `IOException` inside the `@AfterEach` if `Files.walk` failed; the test would then
  fail on cleanup rather than on the assertion. Currently masked by the fact that
  nothing in the tests creates a non-deletable file. Defensive: wrap with a try/catch
  or just rely on `@TempDir` auto-cleanup.
- The `parser()` helper comment claims `logger` is unused by the three sink methods
  on success — that is correct for the success path but `loadFileFromDisk` is invoked
  via `process(importPath, cssText, …)` which then calls `process(cssFile, …)` →
  `saveFile` and that path catches `Exception` with `logger.appendLogMessage`. The
  current tests do not exercise that path, so leaving `logger = null` is safe for
  these 11 cases, but a future test that triggers `process()` end-to-end will need a
  real `IPSSiteImportLogger`. Already noted in the test Javadoc.

## Behavior parity check

|                  Input                  |                        Pre-fix                         |          Post-fix          | Correct? |
|-----------------------------------------|--------------------------------------------------------|----------------------------|----------|
| `themeRoot/file.css` (legitimate)       | `FileInputStream` on file                              | `FileInputStream` on file  | yes      |
| `themeRoot/../escape.css`               | `File(path).exists()` returns false or escapes         | `IllegalArgumentException` | yes      |
| `themeRoot/sub1/../../../../etc/passwd` | resolves to `/etc/passwd` then `new FileInputStream`   | `IllegalArgumentException` | yes      |
| `null`                                  | `new File(null)` — NPE / platform-dependent behavior   | `IllegalArgumentException` | yes      |
| `"good.css\0../../etc/passwd"`          | `new File(path).exists()` — NUL byte ignored by `File` | `IllegalArgumentException` | yes      |
| `themeRoot/out.css` (legitimate write)  | `FileWriter` opens, writes                             | `FileWriter` opens, writes | yes      |

## Fail-then-pass verification

Pre-fix behavior reconstructed from the diff (`-` lines at `PSCSSParser.java:236`,
`:264`, `:275`):
- `fileExists` returned `new File(importPath).exists()` — silently false on
`/etc/passwd` (unless root user), never throws on the documented adversarial
inputs.
- `saveFile` opened `new FileWriter(path)` directly with a traversal payload —
CodeQL flagged the construction at line 264 as a sink (alert #1056).
- `loadFileFromDisk` opened `new FileInputStream(new File(path))` directly —
CodeQL flagged the construction at line 275 (alert #1057).

Post-fix, every adversarial test asserts `IllegalArgumentException` (which only the
new `requireUnderBase` call can throw on these inputs — the three sinks otherwise
throw `IOException` or `FileNotFoundException` or return `boolean`). The two "accepts"
tests for `saveFile` and `loadFileFromDisk` further prove the validator does not
over-reject: `saveFile` writes the file (`assertTrue(target.exists())`),
`loadFileFromDisk` surfaces `FileNotFoundException` (not `IllegalArgumentException`)
on a missing in-root file, demonstrating that the validator passes and the
FileInputStream is what fails. `fileExists` returns `false` for a missing in-root
file, demonstrating that the validator passes and `.exists()` is what runs.

Verified by `mvn-env.bat … test -Dtest=PSCSSParserPathInjectionTest`: **11 run, 0
failures, 0 errors, 0 skipped**.

## Spotless / build

`mvn-env.bat -Dai.integrity.skip=true -pl projects/sitemanage spotless:check`
reports `Spotless.Format is keeping 7 files clean - 1 needs changes to be clean, 0
were already clean, 6 were skipped because caching determined they were already
clean`. The single failing file is
`src/main/resources/com/percussion/pagemanagement/service/impl/WidgetRegistry.xml`
(pre-existing XML non-breaking-space formatting unrelated to this commit). The two
files touched by this commit (`PSCSSParser.java`, `PSCSSParserPathInjectionTest.java`)
are clean — verified by grepping the spotless diff for `PSCSSParser` /
`importer.theme` / `sitemanage` (no hits in the violation list).

Compiler warnings on the new test (`sun.misc.Unsafe is internal proprietary API`)
are expected and consistent with the same pattern used by
`PSSiteDataServicePathInjectionTest` (T043). Not blocking.

## Recommendation

- **May commit/push**: yes

Notes:
- This commit is already committed on the branch (`git log --oneline` shows
`b4b1a987a` on top of `f0bf34e5b`); the question is whether to merge / push / open
a PR. Erlang approves.
- M-1 (missing trailing newline) and M-2 (redundant `@AfterEach` cleanup) are
optional polish, not gating.
- No new patterns to add to
`modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md` —
this commit follows the established T043 helper convention from
`PSSiteDataService` / `PSRegionCSSFileService` / `PSThemeService` and reuses the
same test harness shape.
