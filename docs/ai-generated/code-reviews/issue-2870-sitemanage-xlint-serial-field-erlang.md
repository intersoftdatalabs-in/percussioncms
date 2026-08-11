# Erlang review — issue #2870 sitemanage Xlint serial-field batch 3

**Verdict:** Approve for PR.

## Scope
Main-source `serial-field` cluster on `projects/sitemanage` after #2421 batch 2 (serialVersionUID). Converted ~72 collection fields from bare `List`/`Map`/`Set`/`Collection` interfaces to concrete serializable types (`ArrayList`/`HashMap`/`HashSet`/`Vector`). Public getter return types and setter parameter types remain interfaces.

## Live main-source inventory (uncapped `-Xmaxwarns`)
| Metric | Before | After |
|--------|--------|-------|
| Total main WARNING lines | 227 | 165 |
| serial-field | 86 | 14 |
| this-escape | 55 | 55 |
| unchecked/raw (+cast) | ~53 | ~63 |
| other / serialVersionUID | ~33 | ~33 |

## Correctness
- Setters use **share-if-concrete** assignment: if the argument is already the concrete type, assign the same reference (required for JAXB get/set/add collection population and for tests that mutate a shared `HashMap` after `PSAsset#setFields`). Otherwise copy into a new concrete collection.
- `@SuppressWarnings("unchecked")` on those setters for the generic cast after `instanceof`.
- No REST contract / public API signature changes.
- Product-docs: N/A (compiler lint tech debt).

## Tests
- Extended `PSSerializableListWrappersTest` with field-type guards and defensive-copy behavior for non-concrete inputs (`LinkedList`).
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 970, Failures: 0, Errors: 0, Skipped: 127.

## Residual (next PR-sized slices)
- Remaining **serial-field** on non-collection nested types (`Object`, `PSMapWrapper`, `PSPubInfo`, `PSObjectAcl`, widget nested types, JCR `Node`, etc.).
- **this-escape** (~55) and remaining **unchecked/raw** packages (`pagemanagement.dao.impl`, comments, Siteimprove LinkedHashMap, etc.).
- Test-source Xlint still separate.

## Bugs found
None blocking.
