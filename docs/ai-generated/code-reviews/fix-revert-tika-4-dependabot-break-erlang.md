# Erlang review: fix/revert-tika-4-dependabot-break

**Scope:** uncommitted diff vs `HEAD` on `fix/revert-tika-4-dependabot-break` (parent `tika.version` + Dependabot major freeze). Base `origin/main`.
**Change class:** library major-version revert + Dependabot ignore (same class as SolrJ freeze).
**Memory patterns hit:** incomplete change-class closure (N/A — no new production type); missing behavioral tests for new/changed non-trivial logic (N/A — no new product logic; Tika 3.x API already compiled in `perc-system`); agent rule files (not in diff).
**Cross-platform path checklist:** N/A (no file I/O / path / installer / packaging code).

## Summary

Dependabot #4474 bumped `tika.version` 3.3.1 → 4.0.0. Tika 4.x is not a drop-in: `tika-parsers-standard-package` is a POM (TIKA-4712), `TikaConfig` is removed, XML `tika-config.xml` is unsupported, Parser SPI takes `TikaInputStream`, default extract is Markdown, metadata keys renamed. Maven cannot resolve `tika-parsers-standard-package:jar:4.0.0` (WebUI depends on that artifact as a JAR).

This change pins **3.3.2** (current 3.x maintenance) and freezes Dependabot **semver-major** for `org.apache.tika:*`, matching the SolrJ major freeze. Other recent Dependabot bumps (CommonMark 0.30, TwelveMonkeys 3.15, maven-resources-plugin 3.5.0, SLF4J 2.0.19) compile; Markdown assembler tests pass.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Issues

None.

### Notes (non-blocking)

- Existing `PSTextConverterTest` Tika cases are `@Disabled` (SAX factory / classpath); not newly broken. A new TikaConfig smoke test was attempted and dropped after Saxon AElfred `ParserConfigurationException` on the `perc-system` test classpath.
- 3.3.2 is one patch ahead of the pre-#4474 pin (3.3.1); still Tika 3.x, JAR packaging intact.
- Product-docs N/A: operators never ran Tika 4.x; this restores the 3.x line.

## Verification (author)

- `org.apache.tika:tika-parsers-standard-package:jar:4.0.0` — missing on Central
- `org.apache.tika:tika-parsers-standard-package:jar:3.3.2` — present; WebUI `dependency:tree` shows `tika-parsers-standard-package:jar:3.3.2`
- `cd system && ../mvnw -DskipTests install` — BUILD SUCCESS (Tika + CommonMark compile)
- `cd rest && ../mvnw clean install` — BUILD SUCCESS, Tests run: 1396, Failures: 0
- `cd system && ../mvnw test -Dtest=PSMarkdownAssemblerTest` — Tests run: 4, Failures: 0
- `cd system && ../mvnw test` — BUILD SUCCESS, Tests run: 3075, Failures: 0, Errors: 0, Skipped: 251
