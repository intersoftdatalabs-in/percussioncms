# Erlang Code Review — fix/1529-update-startup-license-disclaimer (design pass)

## Summary

Round-2 design pass for #1529 (PR #1552). Maintenance reviewer (natechadwick-intsof,
[bug/design] comments, 2026-07-29 11:21) flagged that the prior commit hardcoded ~20
component versions into the `thirdPartyCopyright` resource string, `NOTICE.txt`, and the
test assertions. The recommended fix: emit a stable attribution blurb in the hand-edited
sources and let the versioned inventory be generated from the project dependency set at
build time. This commit implements that design pass: the bundle string and `NOTICE.txt`
are now version-agnostic, the test asserts structure (sections, credits, dropped components,
paragraph separators) and explicitly forbids version-pin patterns, and the "complete list"
claim is softened to "summary" with a build-time-generated pointer.

Round-1 text refresh (PR #1552 commits 1 and 2) is superseded by the design row of
round 2 — the round-1 lead paragraph and credit lines are preserved, but the per-component
version roster is removed from the hand-edited surfaces.

## Scope

- Base: `origin/development` (`24ee28acf4`; PR branch was authored at this base)
- Head (this commit): `fix/1529-update-startup-license-disclaimer` updated
- Files: 3 changed
  - `system/src/main/resources/com/percussion/server/PSStringResources.properties` — `thirdPartyCopyright` rewritten as a stable attribution blurb, paragraph separators retained, no version pins
  - `NOTICE.txt` — mirrors the new stable attribution blurb; "complete list" softened to "summary"; build-time-generated pointer added
  - `system/src/test/java/com/percussion/server/PSThirdPartyCopyrightTest.java` — replaced version-pin assertions with structure assertions; added `thirdPartyCopyrightHasNoDependencyVersionPins` regression guard; kept Intersoft / JDBC-section / dropped-Lato / paragraph-separator / NOTICE-mirror assertions

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

None.

## Review notes

### Design rationale

The round-2 reviewer's [bug/design] feedback (PR #1552 review threads PRRT_kwDOKZBp3M6UuxJL,
PRRT_kwDOKZBp3M6UuxJR, PRRT_kwDOKZBp3M6UuxJa, PRRT_kwDOKZBp3M6UuxJf, plus the two open
threads PRRT_kwDOKZBp3M6UbQX2 and PRRT_kwDOKZBp3M6UbSiB) converged on:

> "Embedding current dependency versions in `thirdPartyCopyright` couples the startup
> About string to every security/version bump. Prefer a stable attribution blurb
> (product license + Intersoft + pointer to NOTICE/LICENSE) and keep versioned inventory
> out of this resource."

And:

> "Prefer a build-generated NOTICE (or license inventory) so the distribution notice
> cannot drift from the reactor dependencies."

This commit implements the "stable attribution blurb" half of the recommendation. The
"build-generated versioned inventory" half is deferred to a follow-up issue to keep the
PR scope tight; the new prose explicitly states the relationship between the hand-edited
sources and the (future) build-generated file ("The versioned inventory is generated from
the project dependency set at build time; the hand-edited LICENSE and NOTICE files are
the single source of truth.").

### Functional risk

- Pure text / test refactor. No public API changes, no `PSServer` code changes, no
  resource-bundle key additions or removals.
- Startup banner and About dialog will now show a stable attribution blurb instead of
  the per-component version roster. The versioned inventory remains available in the
  bundled `LICENSE` and `NOTICE` files (the pointer text says so explicitly).
- The aggressive `thirdPartyCopyrightHasNoDependencyVersionPins` assertion guards against
  future re-pollution: any future commit that re-introduces a pinned version pattern
  (`v1.3.1`, `v2.3.232`, `13.3.1.jre11-preview`, `v42.7.12`, `v3.5.7`, `v7.2.6`, `2.25.4`,
  `8.11.4`, `v1.4.21`, `v1.84`, `6.8.6`) will fail the system module's clean install.

### Cross-platform path / file I/O

- No file I/O, paths, installers, or packaging logic touched.
- Resource bundle: same Properties line-continuation format as round 1; portable on
  Windows / Linux / macOS.
- `NOTICE.txt`: plain UTF-8 text, no BOM, LF line endings preserved (autocrlf normalizes
  on commit per repo config).
- Test path resolution: `repoRoot()` walks parents using `Path.getParent()` — portable
  on Windows and Unix.

### Spotless

- `mvn spotless:check` over the changed files: clean (no new violations).
- One pre-existing violation on `system/config/config.xml` (PostgreSQL JDBC entry
  line-wrapping) — NOT touched by this PR. Acknowledged by the round-1 Erlang review.

### Build evidence (standalone system module)

```
cd system
..\mvn-env.bat clean install -B -Dai.integrity.skip=true
```

Result: **BUILD SUCCESS** in 6:27 min.
- Tests run: 872, Failures: 0, Errors: 0, Skipped: 244 (baseline 871 → 872 reflects the
  one new structure-assert test case; existing cases pass with the new prose).
- `PSThirdPartyCopyrightTest`: 8/8 pass (was 7/7 in round 1).
- No new compiler warnings on the changed files. Pre-existing raw-type warnings in
  `Tools/Converters/.../PSVariantConverter.java` and `cms/objectstore/client/PSRemoteAgent.java`
  are baseline and not introduced by this PR.

### Alternatives considered

- **Generate `THIRD-PARTY-LICENSES.txt` at build time in this PR.** Rejected —
  introduces a new Maven plugin configuration that touches the distribution-tree
  module; out of scope for #1529 (whose AC is "text refresh"). Deferred to a follow-up
  issue. The new prose explicitly references this artifact, so the foundation is in
  place.
- **Use Maven `templating-maven-plugin` to interpolate `${log4j2.version}` etc. into the
  bundle string.** Rejected — the bundle string is read at runtime via `ResourceBundle`,
  which is not a Maven resource filter, so the interpolation would have to happen at
  build time and the result would be a generated `.properties` file. The cleaner
  answer is to remove the version pins from the resource string entirely (this commit).
- **Keep version pins but auto-update via Dependabot-driven renewal.** Rejected —
  the reviewer's point is precisely that the resource string should not be a version
  inventory at all; the workload of "tracking what versions to put in the resource
  string" is the wrong answer.

### Open threads addressed

The 6 unresolved review threads on PR #1552 are:

1. `PRRT_kwDOKZBp3M6UuxJL` — [bug/design] line 11 — "Embedding current dependency versions…"
2. `PRRT_kwDOKZBp3M6UuxJR` — [bug/design] line 20 — "'Complete list … in LICENSE and NOTICE' is only true if NOTICE is generated…"
3. `PRRT_kwDOKZBp3M6UuxJa` — [bug/design] test line 46 — "This test locks dozens of exact version strings into CI…"
4. `PRRT_kwDOKZBp3M6UuxJf` — [suggestion] NOTICE.txt line 19 — "Same version-pin problem as the resource string."
5. `PRRT_kwDOKZBp3M6UbQX2` — line 14 — "I do not like the idea of embedding the component versions… How would we keep this up to date?"
6. `PRRT_kwDOKZBp3M6UbSiB` — line 20 — "If there is a complete list then it should be a complete list and a file that is generated by the build."

All 6 are addressed by this commit; the follow-up issue for the build-time
generated inventory will be filed in a sibling PR.

## Pre-PR command evidence

```bash
cd system
..\mvn-env.bat clean install -B -Dai.integrity.skip=true
```

Result: `BUILD SUCCESS`. Tests run 872 / Failures 0 / Errors 0. PSThirdPartyCopyrightTest 8/8. No new warnings on the changed files.
