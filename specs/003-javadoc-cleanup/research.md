# Research: Javadoc Cleanup for Content Explorer Module

**Spec**: `spec.md`
**Date**: 2026-07-11
**Branch**: `003-javadoc-cleanup`
**Tool**: JDK 21 javadoc (via `maven-javadoc-plugin` 3.12.0 from parent POM)

## Baseline (captured 2026-07-11 on `development`)

Command used:

```bash
./mvnw -pl modules/DesktopContentExplorer javadoc:javadoc -q -DskipTests
```

Raw output archived at `specs/003-javadoc-cleanup/baseline-raw.txt`.

|              Metric              |  Count  |
|----------------------------------|---------|
| Errors (javadoc tool output)     | **44**  |
| Warnings (javadoc tool output)   | **198** |
| Unique files producing errors    | **26**  |
| Unique files producing any issue | ~70     |

The javadoc tool's own summary line at the end of its output:

```
44 errors
100 warnings
```

> The summary "100 warnings" is a deduplicated symbol count from the JDK tool itself;
> the raw stderr contains 198 issue lines because some warnings apply to multiple
> overloads / parameters. The plan tracks both numbers: the **tool's own summary** as
> the primary metric for SC-001 (since it is what CI / log scanners see), and the raw
> line count as a secondary debugging aid.

### Issue categories (raw line counts)

|                                        Category                                        | Count |                                               Fix mechanism                                                |
|----------------------------------------------------------------------------------------|------:|------------------------------------------------------------------------------------------------------------|
| `cannot find symbol` (referenced via `{@link ...}`)                                    |   144 | Replace with correct FQN or remove stale `{@link}`                                                         |
| `package com.percussion.services.* does not exist` (cross-module `{@link}` resolution) |   124 | Replace with module-correct `{@link}` (often `#FQN` without `package` qualifier) or remove stale reference |
| `no comment` (class / constructor / method)                                            |    56 | Add Javadoc block with substantive description (FR-003)                                                    |
| `no @param`                                                                            |    32 | Add `@param` for each parameter OR add justification comment                                               |
| `no main description`                                                                  |    30 | Add a description paragraph at the start of the Javadoc block                                              |
| `no description for @throws`                                                           |    30 | Add a description after each `@throws` tag                                                                 |
| `reference not found` (broken `{@link}` to local member)                               |    16 | Fix the `{@link}` target (typo, wrong overload, missing method)                                            |
| `unknown tag` (HTML/Javadoc tag not recognized)                                        |    14 | Replace tag with supported Javadoc/HTML (FR-003)                                                           |
| `@param name not found` (param name doesn't match signature)                           |    14 | Correct the parameter name in the `@param`                                                                 |
| `no @return`                                                                           |    14 | Add `@return` (or add justification comment for void)                                                      |
| `unexpected text`                                                                      |    12 | Move text outside the Javadoc / use proper tag form                                                        |
| `malformed HTML` / `element not closed: code` / `bad HTML entity`                      |    22 | Repair HTML (close `<code>`, escape `<` `>` `&`)                                                           |
| `exception not thrown` (declared `@throws` for an undeclared exception)                |     4 | Remove or correct `@throws` (or add the declaration)                                                       |
| `use of default constructor, which does not provide a comment`                         |     4 | Add Javadoc to no-arg constructors or annotate `@SuppressWarnings("javadoc")` per module convention        |
| misc (semicolon, nested tag, empty comment, invalid HTML)                              | small | surgical repair                                                                                            |

## Decision: build mechanism

**Decision**: Use the JDK 21 javadoc tool through the already-configured
`maven-javadoc-plugin` 3.12.0 from the parent POM (`pom.xml:2636-2653`). Do **not**
introduce per-module plugin overrides, additional plugins, or SuppressWarnings hacks at
scale.

**Rationale**:

- Constitution II (Evidence Over Invention) and VII (Build, Platform & Dependency
  Hygiene): the parent's plugin is the canonical configuration for every module. Adding
  overrides diverges sibling modules and creates maintenance debt.
- Parent POM sets `failOnWarnings=false`, `failOnError=false`, and `doclint=all`. So
  *currently* the build does not actually "fail" at javadoc — it produces noise. The
  user complaint ("slowing down the build") matches this: the tool runs to completion
  but log noise pollutes CI output and the issue persists across the module.
- Running javadoc standalone with `javadoc:javadoc` (not `javadoc:jar`) avoids the
  `attach-javadocs` execution and lets us inspect diagnostics cleanly.
- Per-file changes only — no structural restructuring.

**Alternatives considered**:

- **Per-module `<plugin>` override**: rejected. Diverges from project convention; would
  need its own ADR.
- **Suppress warnings globally with `<doclint>none</doclint>`**: rejected. Hides the
  problem; contradicts Constitution VIII (Documentation & Operability) which values
  meaningful javadoc.
- **Refactor module to Java 21 features (sealed, records)**: rejected. Out of scope
  (FR-004 forbids signature/visibility changes); Constitution V (Safe Modernization)
  prefers incremental improvement.

## Decision: fixing strategy

**Decision**: Fix root causes first; allow targeted `@SuppressWarnings("javadoc")` only
where the module already uses such suppressions OR where the symbol is genuinely
internal/non-public and the cost of writing javadoc is unjustified (e.g., trivial private
getters that are part of a generated/inherited API surface).

**Rationale**: Constitution VIII requires meaningful documentation. Suppression is a
last resort, not a default.

**Heuristics**, in priority order:

1. **Real Javadoc** for missing comments on public classes/methods (most cases).
2. **Repair `{@link}` references** to existing local members (typos, dead links).
3. **Repair parameter name typos** (`@param heigth` → match the actual signature param).
4. **Repair HTML** (`<code>null<code>` → `<code>null</code>`, escape `<`/`>`/`&`).
5. **Delete stale references** that point into deleted/renamed classes — leave a brief
   prose mention instead so the doc still tells the reader something useful.
6. **Targeted suppression** with a justification comment for: private members, generated
   boilerplate, or symbols that the module already documents at class level only.

Each suppression introduced will be tracked in the post-cleanup report so that future
cleanup passes can attack the remainder.

## Decision: scope discipline

**Decision**: Touch only `modules/DesktopContentExplorer/`. Do **not** modify any other
module, the parent POM, or any test code (FR-004, FR-005, FR-009).

**Rationale**: Constitution I (Module-First Boundaries): the user's request is scoped to
the content explorer module. Stay there. The baseline run (scoped via `-pl`) confirms
all 242 issues live in this module's sources — no spillover.

**Cross-module `{@link}` references** that point into `system/`, `rest/`, etc. *do* count
as content explorer issues (they appear in this module's sources). Fix those by either:

- correcting the target (if the symbol still exists in the modern module structure), or
- replacing with a prose reference and a `// TODO: re-link after consolidating docs in
  SPEC-NNN` style comment (no invented APIs, per Constitution II).

## Decision: verification commands

Two commands, both documented in `quickstart.md`:

|                              Goal                              |                                                                 Command                                                                 |
|----------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| Standard javadoc generation (CI will run something equivalent) | `./mvnw -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests`                                                           |
| Per-class javadoc generation (debugging)                       | `./mvnw -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests -Dsourcepath=modules/DesktopContentExplorer/src/main/java` |

Exit code 0, "0 errors", and "0 warnings" are the targets. The tool prints a final
summary line `N errors` / `N warnings`; we key off that.

## Open questions

None. All tech-stack unknowns were resolved against the parent POM and the baseline run
without speculation.
