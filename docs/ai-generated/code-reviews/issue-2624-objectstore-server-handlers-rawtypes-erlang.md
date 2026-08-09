# Erlang-style self-review — issue #2624

**Branch:** fix/issue-2624-objectstore-server-handlers-rawtypes-2h  
**Module:** system / perc-system  
**Change class:** residual rawtypes/unchecked typing in cms.objectstore.server handlers (slice 2h after #2453)

## Scope

- `PSServerItem` — typed field/child/related iterators; `Map<String, Object>` HTML param maps for update paths; `Map<String, ?>` for `makeRequest`; package-visible `populateFieldParams`
- `PSLoadChildDataExit` — `List<Element>` snapshot of base elements; extracted `snapshotElements` for tests
- `PSFieldRetriever` — `Map<String, Object>` `prepareParams` / request helpers; package-visible for tests
- `PSAuthTypes` — `stringPropertyNames()` iteration; extracted `parseAuthTypeProperties`; try-with-resources on config stream
- `PSCatalogServerObjectHandler` — `Collection<String>` init, `Iterator<String>` request roots, `collectFieldNames` for multi-value field-name params

Prefer real generics; no new `@SuppressWarnings` for rawtypes in this batch.

## Gates

- [x] No intentional behavior change beyond typing (authtype parse key rule preserved; field-name list/scalar semantics preserved)
- [x] Real generics preferred
- [x] Out of scope: IPSComponent parentComponents (#2455), data/data.jdbc, this-escape/serial
- [x] Cross-platform: no path I/O changes (auth config still uses `File.separator` via existing `PSObservableFile` construction)
- [x] Unit tests for changed pure logic (`PSObjectStoreServerHandlersRawtypesTest`)
- [x] `cd system && ../mvnw.cmd clean install` green — Tests run: **1441**, Failures: **0**, Errors: **0**, Skipped: 240; BUILD SUCCESS

## Residual

- Named hottest list for this slice (PSServerItem / PSLoadChildDataExit / PSFieldRetriever / PSAuthTypes / PSCatalogServerObjectHandler) is zeroed for the rawtypes in scope.
- `PSServer.getInternalRequest(..., Map, ...)` remains raw at the server API boundary (out of this slice).
- Epic #2022 may still track other perc-system javac residual outside this handler batch; no micro residual filed for zeroed slice scope.

## Verdict

**PASS** for commit/PR.
