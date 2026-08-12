# Erlang-style review — issue #3061 sitemanage Xlint residual

## Scope
PR-sized residual for `projects/sitemanage` main-source `-Xlint` after service/listener
this-escape (#2999 / PR #3060). Cluster: remaining this-escape DTO/parser + serial-field
non-collection residual.

## Findings
- **Bugs:** none found. Constructor field seeds preserve prior validation/copy semantics
  (folderPaths ArrayList copy, widget builder DAO → DTO mapping, Folders rootName seed).
- **Behavior:** `PSRegionParserAdapter` lazy-inits the parser after subclass construction;
  parse behavior unchanged. Cycle exception still exposes definitions in-process via
  transient list; message uses serializable id list.
- **Serial-field:** nested data holders implement `Serializable`; runtime-only refs
  (`relatedObject`, JCR `Node`, servlet collaborator, CMS `PSObjectAcl`) marked
  `transient`. JPA `PropertyId` retains justified `@SuppressWarnings("serial")` (entity
  association cannot be transient without risking Hibernate).
- **Tests:** `PSThisEscapeDtoConstructorTest` (+3), `PSSerialFieldResidualTest` (7),
  existing Folders path root test.
- **Cross-platform:** N/A (no path/file I/O in this batch).
- **API:** no public method signature removals; `PSMapWrapper` field concrete `HashMap`
  with interface getters/setters unchanged. C2 final/extends greps N/A (no new finals).

## Inventory (main-source)
| Metric | Before | After |
|--------|--------|-------|
| this-escape | **5** | **0** |
| serial-field | **14** | **0** |
| total Xlint warnings (approx) | **57** | **~37** (misc residual) |

## Residual
Misc main-source Xlint (static qualification, Session.get deprecation, equals/hashCode,
fall-through, redundant cast, unchecked, …) and test-source Xlint — file child under
#2032 / #2200 if still large.

## Verdict
PASS for commit/PR.

> Co-Authored by Grok Build using grok-4.5 with agent main.
