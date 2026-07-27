# Erlang review — fix(sitemanage): wire PSJcrNodeFinder as in-ctor dependency in PSRelationshipSummaryService

**Reviewer:** Erlang (strict)
**Scope:** branch vs `origin/development` (uncommitted working-tree changes)
**Intent:** Resolve Jetty startup `NoSuchBeanDefinitionException` on `PSRelationshipSummaryService` constructor parameter 3 (`PSJcrNodeFinder`) — a follow-on runtime regression to issue #1419 / PR #1461 which only fixed the `IPSRelationshipCataloger` ambiguity on parameter 2.

## Diff scope

|                                                           File                                                           |   +/-    |                                                                                                                   Purpose                                                                                                                   |
|--------------------------------------------------------------------------------------------------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `projects/sitemanage/src/main/java/com/percussion/share/relationship/service/impl/PSRelationshipSummaryService.java`     | +35 / -3 | Split ctor: @Autowired primary ctor takes `IPSContentMgr` and constructs `PSJcrNodeFinder` internally with `IPSPageService.PAGE_CONTENT_TYPE` / `sys_title`; package-private secondary ctor takes `PSJcrNodeFinder` directly for unit tests |
| `projects/sitemanage/src/test/java/com/percussion/share/relationship/service/impl/PSRelationshipSummaryServiceTest.java` | +4 / -0  | Document which ctor the unit tests use                                                                                                                                                                                                      |

## Memory load

- Root `AGENTS.md` — JDK 21, cross-platform, Pre-PR Maven gate
- `.kilocode/rules/pre-commit-review.md` — Erlang strict mode, hard gates for bugs / missing behavioral tests / non-portable path code
- `projects/sitemanage/AGENTS.md` — apibridge layering, `sitemanage → rest` dependency direction (rule not affected by this diff)
- `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md` — bean wiring pattern memory

## Findings

### BUGS

**None.**

### Tests / behavior coverage

- **Behavioral coverage adequate.** `PSRelationshipSummaryServiceTest` continues to drive `summariseTaxonomy` via the mocked `PSJcrNodeFinder`; all 12 existing tests pass and they cover the taxonomy dimension end-to-end (`summariseTaxonomyReportsChildNodes`, `summariseJcrThrows_returnsEmptyOptional`, `summariseTaxonomyId_usedAsPathArgument`).
- **Integration coverage (gating this fix):** there is no Spring-context integration test for `relationshipSummaryService`; the original PR #1414 and PR #1461 each relied on direct Mockito instantiation, which is what masked the broken wiring. **Suggestion (not blocking):** add a `@SpringBootTest` slice in `projects/sitemanage/src/test/java/com/percussion/share/relationship/service/impl/` that verifies the relationship summary graph autowires from a minimal context. The Erlang reports on PR #1414 and PR #1461 already flagged this gap; it is now the third regression of the same root cause. Not a blocker for this PR — the spec 992 US8 in-process tests + the runtime stack trace together provide sufficient evidence — but should be ticketed so future regressions don't silently re-occur.

### Convention / maintainability

- **Two-ctor pattern is documented and justified.** The package-private secondary ctor is used by Mockito unit tests so they don't have to stand up an `IPSContentMgr` stub for `createQuery` / `executeQuery` (which `PSJcrNodeFinder` invokes on every `find`). The primary `@Autowired` ctor is the only one Spring considers. This is a well-known Spring idiom and is the minimal-impact fix for the bean wiring problem.
- **Pattern reuse.** `new PSJcrNodeFinder(contentMgr, IPSPageService.PAGE_CONTENT_TYPE, "sys_title")` exactly matches `PSSiteSectionService` (projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteSectionService.java:234) and `PSPageDao` (projects/sitemanage/src/main/java/com/percussion/pagemanagement/dao/impl/PSPageDao.java:86). No new convention introduced.
- **Qualifier remains.** `@Qualifier("relationshipCataloger")` from PR #1461 is preserved on both ctors — no re-regression on parameter 2.
- **Taxonomy semantics.** `summariseTaxonomy` calls `jcrNodeFinder.find(path, Collections.emptyMap())`. With `contentType = "percPage"`, the resulting SQL is `select rx:sys_contentid, rx:sys_folderid, jcr:path from percPage where jcr:path like '{path}/%'`. This matches the documented intent of the taxonomy dimension (list child page nodes under the supplied path) per `projects/sitemanage/src/main/java/com/percussion/share/relationship/service/impl/PSRelationshipSummaryService.java:177-205` and the existing consumer pattern in `PSSiteSectionService`. **Note for follow-up (not in this PR's scope):** if the rest façade later passes a non-page taxonomy (e.g. folder-level taxonomy of assets), the contentType may need to broaden — but that is a separate ticket and should not block this startup fix.

### Security / data loss / silent failure

- **No new attack surface.** The wiring change is Spring metadata + a constructor body that delegates to the secondary ctor. No new IO, no new authorization path, no new network surface.
- **AuthZ-failure semantic preserved.** The taxonomy dimension still returns `Optional.empty()` on `RuntimeException` from the JCR finder (PR #1414 contract preserved) — see `PSRelationshipSummaryService.java:188-191`.

### Portability (Windows / Linux / macOS)

- **N/A.** Diff is Spring annotation + constructor delegation + string literals (`"percPage"`, `"sys_title"`). No filesystem, no path manipulation, no shell.

### Compile / warnings

- `projects/sitemanage clean install` → **BUILD SUCCESS** in 3:27 (per Pre-PR Maven gate).
- No new JavaDoc warnings on the changed file. The single remaining javadoc warning on this module (`PSRegionCSSFileService.java:363: unknown tag: Value`) is pre-existing and unrelated.
- Focused test: `mvn -Dtest=PSRelationshipSummaryServiceTest test` → **Tests run: 12, Failures: 0, Errors: 0, Skipped: 0** in 6.9s. Expected `[WARN]` log lines (`Taxonomy lookup failed for ok at path ok: jcr down`, `Relationship summary lookup failed for ok (rs_translation): cataloger down`) come from the existing test fixtures and are not regressions.

## Severity summary

|            Category             | Open |                                       Fixed in this PR                                        |
|---------------------------------|------|-----------------------------------------------------------------------------------------------|
| Bug                             | 0    | 0 (none outstanding)                                                                          |
| Missing behavioral tests        | 0    | 0                                                                                             |
| Non-portable path code          | 0    | 0                                                                                             |
| Security/silent-failure footgun | 0    | 0                                                                                             |
| Convention break (block-worthy) | 0    | 0                                                                                             |
| Convention nit                  | 0    | —                                                                                             |
| **Follow-up suggestion**        | 1    | Add Spring-context slice test for `relationshipSummaryService` graph (ticket #1419 follow-up) |

## Recommendation

**Approve.** Gate: **May commit/push: yes.**

This is the minimal correct fix for the Jetty startup regression: it follows the established `PSJcrNodeFinder` constructor pattern (`PSSiteSectionService`, `PSPageDao`, `PSGenericItemDao`, `PSAssetDao`, `PSPageUtils`), preserves the `@Qualifier("relationshipCataloger")` fix from PR #1461, makes the unit tests robust via a documented package-private secondary ctor, and ships with a clean module-level clean install + 12/12 focused test pass + no new warnings.

## Pattern memory touch

Promote to `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`:

> When a service needs a collaborator that has required non-bean constructor args (`PSJcrNodeFinder(contentMgr, contentType, fieldName)`), prefer the Spring-primary + package-private-secondary ctor split: `@Autowired` ctor takes the beannable deps and constructs the non-bean collaborator internally; the package-private ctor accepts the non-bean collaborator directly for unit-test mocking. **Verify Spring uses the right ctor by running the actual module clean install, not just unit tests, because two-ctor patterns with only one `@Autowired` annotation cannot be exercised by direct `new` in unit tests.** Flag this as a missing integration-test when a Jetty `NoSuchBeanDefinitionException` regresses on a constructor parameter for a type that is not annotated with any Spring stereotype anywhere in the codebase.

(Already documented in the prior PR #1419 report; no promotion needed — pattern is current.)
