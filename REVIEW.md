# REVIEW.md

Repository-specific guidance for the automated reviewer (`kilo-code-bot`) on
PRs in this repository. The base branch is `development`; this file is read
from the base branch, never from the feature branch under review.

## What matters in this repository

- **Cross-platform portability is mandatory.** Percussion CMS is built,
  tested, installed, and deployed on Windows, Linux, and macOS. Any code,
  test, script, or path assertion that works only on Unix-style paths is a
  defect. No hardcoded `"/"` or `"\\"` separators in filesystem paths; use
  `java.nio.file.Path` / `Paths.get` / `Path.of` / `Files.*`. When a
  separator char is truly required, use `File.separator` /
  `File.pathSeparator`. Use `System.getProperty("java.io.tmpdir")` or
  `Files.createTempFile` for temp locations. Line endings differ by
  platform — normalize in tests.

- **Module boundaries are enforced.** `rest` MUST NOT depend on
  `sitemanage` (see root `AGENTS.md`); the dependency direction is
  `sitemanage → rest`, never the reverse. New Maven modules must be added
  to the root `pom.xml` `<modules>` list or they will not be built.

- **Lockstep contracts must update multiple places at once.** When a
  parent-POM dependency version bumps (canonical example:
  `modules/perc-distribution-tree/README.md:178-189` JDBC drivers):

  1. The version property in the parent POM.
  2. `BundledJdbcDrivers.EXACT_FILENAMES` and `PRIOR_FILENAMES` in
     `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/jdbc/BundledJdbcDrivers.java`.
  3. The `<delete>` block in
     `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/install.xml`.
     `InstallXmlDeleteSetTest` enforces strict set equality; a clean
     per-module `Maven wrapper clean install` in `modules/perc-distribution-tree`
     is the proof of correctness.
- **H2 is intentionally absent from the bundled delete list.** Do not
  "fix" this — H2 is the default embedded engine (`#548`); it has a
  staging glob but is preserved across upgrades.
- **Java 21 stack.** Spring (NOT Spring Boot), Hibernate, JSP, jQuery,
  XML/XSL, Artemis, JUnit 5, Mockito. Use parent-POM-managed plugin
  versions; do not duplicate plugin versions in module `pom.xml`.
- **No new dependencies without justification.** Prefer parent POM
  `<dependencyManagement>` over per-module dependency declarations.
  Dependabot is configured at `.github/dependabot.yml` for `development`.
- **Secrets and tokens never appear in code, logs, or test fixtures.**
  A `MKD-REDACTED` marker in a session indicates a leak.

## Severity calibration

- **Critical:** data loss, SQL injection, XSS, permission or scope
  escalation, remote code execution, broken installer or upgrade path,
  missing lockstep contract propagation, secrets leaked to logs or wire,
  cross-platform path bug (works only on one OS), force-push to
  `origin/development`, bypassing the Erlang pre-commit review gate.
- **Warning:** missing behavioral unit test for new non-trivial logic,
  raw `Throwable` catches that swallow stack traces, `null`-returning
  APIs where `Optional` would prevent misuse, untested error paths,
  `sitemanage` types leaking into `rest`, Spring Boot imports in a
  non-Spring-Boot module.
- **Suggestion:** style cleanups inside legacy code that is documented
  as needing refactoring (root `AGENTS.md` "modernization context"),
  Javadoc additions, optional parameter validation hardening.
- **Do not flag:**
  - Formatter / Spotless / Enforcer violations — tooling reports them in CI.
  - Lint or typecheck errors — CI reports them directly.
  - Surefire test failures — CI reports them directly.
  - Existing pre-modernization patterns that are out of scope for the PR's
    stated goal.
  - Glob-based staging patterns in `BundledJdbcDrivers.STAGING_GLOBS`
    (intentional).
  - Wrapper pairs (`.sh` + `.bat`) when both already exist and behave the
    same.
  - Empty `REVIEW.md` PRs that only reformat or reword existing sections.

## Verification expectations

- **Pre-PR per-module clean install is a HARD GATE.** Before any PR push,
  the implementer must `cd` into each changed module and run the
  repo-root `mvnw` / `mvnw.cmd clean install` standalone (not
  root reactor `-pl … -am` unless justified). BUILD SUCCESS, all
  Surefire tests pass, zero new compiler / warning / enforcer / Spotless
  warnings on the changed modules.
- **Behavioral tests required for new non-trivial logic.** Pure refactors
  that do not change observable behavior may add a characterization test
  instead. Mocks should not duplicate the implementation under test.
- **Cross-platform test coverage** is required for any code that touches
  filesystem paths, `Path` resolution, temp dirs, line endings, path
  normalization, or installer / upgrade logic.
- **Pre-commit Erlang review is mandatory** per root `AGENTS.md`
  "Pre-commit code review (Erlang)" and
  `modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`.
  Bugs, missing behavioral tests, and non-portable path I/O are hard
  gates — do not approve until fixed.
- **PR Review Comment Resolution is part of the change.** Every review
  comment being addressed must receive an inline `**Mitigation (commit
  <sha>):**` reply citing the commit, AND the GraphQL
  `resolveReviewThread` mutation must be called with the thread `id`
  (not the comment `databaseId`). Unresolved threads block the merge.

## Security and platform constraints

- This repository handles CMS tenant content with database, file-system,
  and remote-endpoint integrations; treat any code path that accepts
  caller-supplied identifiers, file paths, or SQL fragments as untrusted.
- Rate-limit detection and exponential backoff are required for any
  third-party API integration (per root `AGENTS.md`).
- CodeQL: default setup is off; the advanced workflow at
  `.github/workflows/codeql.yml` is the source of truth. Do not
  re-enable default setup without the same config and model pack.
  Suppressions go on the sink line (`// codeql[rule-id]`), not above
  multi-line builders.
- Force-push to `origin/development` is forbidden — create a feature
  branch and open a PR instead. See
  `.kilo/rules/no-force-push-development.md`.

## Comment style

- **Be concise, brief, and precise.** Prefer a short paragraph or a few
  tight bullets over essays. Lead with the defect and the fix; skip
  restating the whole file history. Wordy reviews burn review budget and
  bury the signal (Minimax/Kilo: if it needs a TL;DR, rewrite it).
- **Use light humor to soften negative feedback** — never sarcasm at the
  author, never jokes that obscure the finding. One dry line is enough;
  then state the concrete issue and the preferred mitigation.
- Leave comments on the exact line via the GitHub PR review API.
- Frame findings as suggestions the human can accept or reject; never as
  demands.
- Cite the cited rule: `root AGENTS.md §Cross-Platform File I/O`,
  `modules/perc-distribution-tree/README.md:178-189`, etc., so the human
  can confirm without re-reading the spec.
- If the PR is clean against the rules above, leave a short
  acknowledgement so the human does not have to re-derive the result.

