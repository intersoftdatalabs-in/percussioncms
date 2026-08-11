# Erlang review — issue #2465 design.objectstore this-escape (app/editor config)

**Scope:** uncommitted branch `fix/issue-2465-design-objectstore-this-escape-app-config` vs `origin/main`  
**Module:** `system` / `perc-system`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** private base load / final helpers; leaf `final` class for residual this-escape; avoid blanket `@SuppressWarnings("this-escape")`

## Summary

Continues #2022 / #2200 / #2404 / #2466 design.objectstore `-Xlint:this-escape` reduction for app/editor config types.

### Changes

1. **Base leverage:** `PSComponent` / `PSCollectionComponent` — `updateParentList` / `resetParentList` final; `applyId` helper on `PSComponent`.
2. **Private base load / apply\*:** `PSApplication`, `PSServerConfiguration`, `PSRelationship`, `PSRelationshipConfig`, `PSPipe.applyName`.
3. **Leaf types made `final`** (zero monorepo subclasses; clears residual this-escape from `updateParentList` this-leak): Application, ApplicationFlow, Query/UpdatePipe, ContentEditorSharedDef/SystemDef, DataMapping/Mapper, SearchConfig, SharedFieldGroup, WorkflowInfo, ResultPage(s)/Pager, Requestor, MacroDefinition, ControlMeta/Ref, CustomActionGroup, UISet, RelationshipSet, RoleConfiguration, PageDataTank, ResourceCacheSettings, CommandHandlerStylesheets.
4. **Tests:** extended `PSDesignObjectStoreThisEscapeTest` (+9 cases; 28 total).

### Inventory (JDK 21 `-Xlint:this-escape`, design.objectstore only)

| Metric | Before | After |
|--------|--------|-------|
| Primary this-escape | 221 | 170 |
| All this-escape lines (incl. “previous”) | 272 | 216 |

Cleared hotspots include `PSApplication`, pipes, mappers, shared def, workflow info, result pages, etc. Residual remains on non-final bases with subclasses (`PSRelationship`, `PSRelationshipConfig`, `PSServerConfiguration`) and many other package types — file residual slice.

### Cross-platform path checklist

N/A — no new file I/O or path construction.

### C2 API shape (types made final)

- Grep monorepo `extends <Type>` for all finalized types: **zero** production/test subclasses.
- No anonymous double-brace subclasses found.
- Reverse-deps: no compile-time subclasses; system standalone clean install green.

### Product documentation

N/A — pure tech-debt / compile hygiene; no operator-facing behavior change.

### Build evidence

- `cd system && ../mvnw.cmd clean install` → **BUILD SUCCESS**
- Tests run: 1681, Failures: 0 (module suite)
- Focused: `PSDesignObjectStoreThisEscapeTest` Tests run: 28, Failures: 0

### Issues

None blocking.

### Residual

Open residual issue for remaining design.objectstore this-escape (~170 primary) after this app/editor batch.
