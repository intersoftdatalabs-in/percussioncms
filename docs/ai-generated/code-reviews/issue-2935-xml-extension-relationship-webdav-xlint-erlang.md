# Erlang self-review — issue #2935 xml/extension/relationship/webdav Xlint residual

**Change class:** main-source generics / Xlint cleanup (no product-facing behavior change)  
**Module:** `system` / perc-system  
**Parent:** #2877 / epic #2022  

## Verdict

**PASS** for PR. Real generics preferred on a coherent residual package set (xml / extension / relationship / webdav). No intentional behavior change beyond type-safe collection APIs.

## Scope typed

### relationship/effect
- `PSAttachCloneToFolder` — `List<Object>`/`List<PSLocator>`, `Set<String>` categories
- `PSEffectUtils` — `Map<String, Object>`, `Collection<?>`
- `PSEffectTestRunner` — `Iterator<?>`, `List<PSEffectTestResultPair>`
- `PSPromote` / `PSAttachTranslatedFolder` / `PSIsCloneExists` / `PSPublishUnpublishMandatory` — typed `Iterator` over already-generic summaries/relationship sets; `Map<String, Object>` params

### xml
- `PSDtdBuilder` — `HashMap<String, ArrayList<String>>` / occurrence map
- `PSDtdAttribute` — `List<String>` possible values + catalog
- `PSDtd` / `PSDtdElement` / `PSDtdNode*` catalog hierarchy — `List<String>` catalog, `HashMap<Object, Object>` recursion stack
- `PSDtdTree` — typed `m_elements` / `getCatalog`
- `PSXPathEvaluator` — `List<Node>`
- `PSStylesheetCacheManager` — `Map<URL, PSCachedStylesheet>`

### extension
- Database function maps/lists (`PSDatabaseFunction*`)
- `PSExtensionHandler` resource catalog `ArrayList<URL>` / live extension map
- `PSJavaExtensionHandler` loaders map by `PSExtensionRef`
- JS extension param lists; `PSModifyXmlHierarchyExtension` parent/child maps
- `PSExtensionHandlerConfiguration` pending removals `Set<File>`

### webdav
- `PSWebdavConfig` / `PSWebdavConfigDef` / `PSWebdavContentType` maps and iterators
- `PSPropFindMethod` / `PSPropPatchMethod` component/property lists

## Hard gates

| Gate | Result |
|------|--------|
| Bugs | None intentional |
| Behavioral unit tests | `PSXmlExtensionRelationshipWebdavTypedTest` (6 tests) |
| Portable paths | N/A — no path I/O changes |
| Product docs | N/A — pure tech-debt generics |
| API shape / final | No `final`/`sealed`; generics source-compatible for callers using raw types |

## Residual

- Broader extension manager interfaces (`IPSExtensionManager` install `Iterator` resources) still raw in places
- Webdav validator / large method surfaces still have residual rawtypes
- Full DTD tree merge / generator raw collections remain partially typed
- Install residual #2942 / security residual packages still open under #2877

## Companion closure

Production typing + unit test + this review note. Module clean install required before PR.
