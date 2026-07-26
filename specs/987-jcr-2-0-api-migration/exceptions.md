# JCR 2.0 API Migration Exceptions Register

Per **FR-013** and **SC-007** of Spec 987, this document registers any non-critical call sites or features where a direct JCR 2.0 replacement was not implemented or where an unsupported operation stub (`UnsupportedRepositoryOperationException` / empty return) is intentionally maintained.

> [!NOTE]
> Per specification rules, **zero exceptions** are permitted on critical editor, assembly, and publish paths.

## Current Registered Exceptions

|   ID   |           Location / Component           |                Feature / Method                 |                                                                                            Rationale                                                                                             |  Owner   | Target Phase / Follow-up |
|--------|------------------------------------------|-------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------------------------|
| EX-001 | `PSContentNode.getNodes(String[])`       | Glob name filtering (`Node.getNodes(String[])`) | Optional JCR 2.0 feature; product callers use XPath/SQL queries for glob matching rather than node-level array filtering. Returns empty iterator.                                                | CMS Core | Non-critical / Backlog   |
| EX-002 | `PSContentMgr.getQOMFactory()`           | JCR Query Object Model (`getQOMFactory()`)      | Percussion uses SQL/XPath repository queries and native content manager finders; JQOM is not used by any product caller or built-in extension. Throws `UnsupportedRepositoryOperationException`. | CMS Core | Non-critical / Backlog   |
| EX-003 | `PSValueFactory.createValue(BigDecimal)` | Double precision conversion                     | Product uses `doubleValue()` for JCR 2.0 BigDecimal value wrapping; full arbitrary-precision BigDecimal storage is not required by current content field types.                                  | CMS Core | Non-critical / Backlog   |

## Verification

- Critical editor paths (create/save, open item, preview): **0 exceptions**
- Critical assembly and publishing paths: **0 exceptions**

