# Java 21 System Module Compilation Fix Plan

**Status:** In Progress
**Created:** 2026-01-24
**Branch:** feature/jdk-21-stabilization
**Target:** Get system module compiling without errors on Java 21

## Overview

This document outlines a comprehensive plan to fix all compilation errors in the `system` module during the Java 21 migration. The errors fall into several categories that need to be addressed systematically.

## Error Categories Summary

1. **SOAP/Web Services Interface Compatibility** (~40 errors)
2. **Type System Issues** (~20 errors)
3. **Missing Symbols/Methods** (~15 errors)
4. **Deprecated API Usage** (~8 errors)
5. **Optional/Stream Type Mismatches** (~5 errors)
6. **JSP Tag Library Issues** (~5 errors)
7. **Minor Issues** (unused imports, raw types, etc.)

---

## Category 1: SOAP/Web Services Interface Compatibility

### Priority: HIGH

### Affected Files:

- `system/webservices/src/com/percussion/webservices/content/ContentSOAPImpl.java`
- `system/webservices/src/com/percussion/webservices/security/SecuritySOAPImpl.java`

### Issues:

The SOAP implementation classes don't properly implement their generated interface contracts. This appears to be caused by:
1. **Return type mismatches**: Methods return arrays (e.g., `PSFolder[]`) but interface expects response wrapper objects (e.g., `LoadFoldersResponse`)
2. **Exception signature mismatches**: Implementation throws exceptions not declared in interface
3. **Missing method implementations**: Some abstract methods from interfaces are not implemented

### Root Cause:

WSDL-to-Java code generation likely changed between Java versions or the interfaces were regenerated but implementations weren't updated.

### Fix Strategy:

#### Phase 1: Analyze Generated Interfaces

1. Locate WSDL files in `system/webservices/schemas/` or similar
2. Check if interfaces are generated or hand-written
3. Compare interface signatures with implementation signatures

#### Phase 2: SecuritySOAPImpl Fixes

**File:** `system/webservices/src/com/percussion/webservices/security/SecuritySOAPImpl.java`

**Issues to fix:**
- [ ] Missing `filterByRuntimeVisibility(FilterByRuntimeVisibilityRequest)` method
- [ ] `loadCommunities()` return type incompatible
- [ ] `loadRoles()` return type incompatible
- [ ] `login()` exception signature mismatch (3 exceptions)
- [ ] PSNotAuthenticatedFault constructor signature changed
- [ ] LoginResponse constructor signature changed
- [ ] `logout()` exception signature mismatch
- [ ] `refreshSession()` exception signature mismatch
- [ ] FilterByRuntimeVisibilityResponse constructor signature changed

**Fix approach:**

```java
// Before: Return array directly
public PSCommunity[] loadCommunities(...) { ... }

// After: Wrap in response object
public LoadCommunitiesResponse loadCommunities(LoadCommunitiesRequest req) {
    PSCommunity[] communities = ... // existing logic
    return new LoadCommunitiesResponse(communities);
}
```

#### Phase 3: ContentSOAPImpl Fixes

**File:** `system/webservices/src/com/percussion/webservices/content/ContentSOAPImpl.java`

**Major issues (26 methods):**
- [ ] Missing `loadContentRelations(LoadContentRelationsRequest)` implementation
- [ ] Return type mismatches for 15+ methods
- [ ] Exception signature mismatches for 10+ methods
- [ ] `@Override` annotations on methods not actually overriding

**Methods requiring return type fixes:**
1. `loadContentRelations()` - return `LoadContentRelationsResponse` not `PSAaRelationship[]`
2. `loadFolders()` - return `LoadFoldersResponse` not `PSFolder[]`
3. `newTranslations()` - return `NewTranslationsResponse` not `PSItem[]`
4. `newPromotableVersions()` - return `NewPromotableVersionsResponse` not `PSItem[]`
5. `viewItems()` - return `ViewItemsResponse` not `PSItem[]`
6. `findParentItems()` - return `FindParentItemsResponse` not `PSItemSummary[]`
7. `createChildEntries()` - return `CreateChildEntriesResponse` not `PSChildEntry[]`
8. `findItems()` - return `FindItemsResponse` not `PSSearchResults[]`
9. `addFolderTree()` - return `AddFolderTreeResponse` not `PSFolder[]`
10. `loadTranslationSettings()` - return `LoadTranslationSettingsResponse` not `PSAutoTranslation[]`
11. `loadLocales()` - return `LoadLocalesResponse` not `PSLocale[]`
12. `getAssemblyUrls()` - return type incompatible
13. `loadKeywords()` - return type incompatible
14. `newCopies()` - return type incompatible
15. `addContentRelations()` - return type incompatible
16. `findPathIds()` - return type incompatible
17. `loadChildEntries()` - return type incompatible

**Methods with exception signature issues:**
- `saveContentRelations()` - doesn't throw PSNotAuthorizedFault
- `reorderContentRelations()` - doesn't throw PSInvalidSessionFault
- `loadItems()` - doesn't throw PSErrorResultsFault
- `saveItems()` - doesn't throw PSContractViolationFault
- `moveFolderChildren()` - doesn't throw PSContractViolationFault
- `createItems()` - doesn't throw RemoteException
- `reorderChildEntries()` - doesn't throw PSContractViolationFault
- `addFolder()` - doesn't throw PSContractViolationFault
- `removeFolderChildren()` - doesn't throw PSContractViolationFault
- `checkoutItems()` - doesn't throw PSContractViolationFault
- `saveChildEntries()` - doesn't throw PSContractViolationFault
- `deleteChildEntries()` - doesn't throw PSContractViolationFault
- `releaseFromEdit()` - doesn't throw PSContractViolationFault
- `addFolderChildren()` - doesn't throw PSContractViolationFault
- `checkinItems()` - doesn't throw PSContractViolationFault

#### Phase 4: Response Wrapper Generation

May need to:
1. Regenerate SOAP stubs from WSDL using JAX-WS tools
2. Or manually create response wrapper classes
3. Update all method signatures to match generated interfaces

---

## Category 2: Type System Issues

### Priority: HIGH

### Issues:

#### 2.1: Map Type Incompatibilities

**Affected Files:**
- `system/src/main/java/com/percussion/cms/objectstore/server/PSItemDefManager.java:914`
- `system/src/main/java/com/percussion/fastforward/utils/PSUtils.java:270`
- `system/src/main/java/com/percussion/relationship/effect/PSPublishUnpublishMandatory.java:508,554`
- `system/src/main/java/com/percussion/workflow/PSWorkFlowUtils.java:876,899`

**Error:** `incompatible types: Map<String,String> cannot be converted to Map<String,Object>`
**Error:** `incompatible types: HashMap<String,String> cannot be converted to Map<String,Object>`

**Root Cause:** Methods expect `Map<String, Object>` but code provides `Map<String, String>` or `HashMap<String, String>`

**Fix Strategy:**

```java
// Option 1: Change map type at creation
Map<String, String> params = new HashMap<>(); // Before
Map<String, Object> params = new HashMap<>();  // After

// Option 2: Explicit cast with new HashMap
new HashMap<String, Object>(stringMap)

// Option 3: Use type witness
Map<String, Object> params = new HashMap<String, Object>();
```

**Action Items:**
- [ ] Review each usage to determine if values are really always strings
- [ ] If yes, consider changing method signatures to accept `Map<String, String>`
- [ ] If no, change map type declarations to `Map<String, Object>`

#### 2.2: Stream/Collection Incompatibilities

**Affected Files:**
- `system/src/main/java/com/percussion/server/PSServer.java:3058`
- Error: `Stream<PSCmsObject> cannot be converted to List<PSCmsObject>`
- `system/src/main/java/com/percussion/server/config/PSConfigManager.java:389`
- Error: `Stream<PSConfig> cannot be converted to Collection<PSConfig>`

**Root Cause:** Stream operations not properly collected

**Fix:**

```java
// Before
List<PSCmsObject> list = someStream;

// After
List<PSCmsObject> list = someStream.collect(Collectors.toList());
// or for Java 16+
List<PSCmsObject> list = someStream.toList();
```

#### 2.3: Optional Type Mismatches

**Affected Files:**
- `system/src/main/java/com/percussion/uicontext/PSFilterContextMenu.java:134`
- Error: `Optional<IPSStatesContext> cannot be converted to IPSStatesContext`
- `system/src/main/java/com/percussion/workflow/PSWorkFlowUtils.java:2124`
- Error: `Optional<IPSWorkflowAppsContext> cannot be converted to IPSWorkflowAppsContext`
- `system/webservices/src/com/percussion/webservices/assembly/impl/PSAssemblyDesignWs.java:1040`
- Error: `Optional<Serializable> cannot be converted to Map<String,String>`

**Fix:**

```java
// Before
IPSStatesContext context = getContext(); // returns Optional<IPSStatesContext>

// After
IPSStatesContext context = getContext().orElse(null);
// or
IPSStatesContext context = getContext().orElseThrow();
// or handle properly with ifPresent()
```

#### 2.4: Specific Type Conversions

- `system/servlet/src/com/percussion/webdav/method/PSCopyMethod.java:276`
  - Error: `PSLocatorWithName cannot be converted to PSLocator`
  - Fix: Check if PSLocatorWithName extends PSLocator, if not, extract PSLocator from it
- `system/src/main/java/com/percussion/content/PSHtmlParser.java:896`
  - Error: `HTMLNode cannot be converted to HTMLElement`
  - Fix: Check type before cast or use different method
- `system/src/main/java/com/percussion/process/PSParamDef.java:104`
  - Error: `Document cannot be converted to Element`
  - Fix: Use `document.getDocumentElement()` instead
- `system/services/src/com/percussion/services/workflow/PSWorkflowActionsHelper.java:199`
  - Error: `int cannot be converted to IPSGuid`
  - Fix: Use `PSGuidUtils.makeGuid(int, PSTypeEnum)` to create GUID from int

---

## Category 3: Missing Symbols/Methods

### Priority: HIGH

#### 3.1: Assembly/WebService Locator Issues

**File:** `system/webservices/src/com/percussion/webservices/assembly/PSAssemblyWsLocator.java`

**Errors:**
- Line 12: `com.percussion.services.shim.ws.assembly.IPSAssemblyWs` cannot be converted to `com.percussion.webservices.assembly.IPSAssemblyWs`
- Line 17: `com.percussion.services.shim.ws.assembly.IPSAssemblyDesignWs` cannot be converted to `com.percussion.webservices.assembly.IPSAssemblyDesignWs`

**Root Cause:** Interface package mismatch between shim and actual interfaces

**Fix Strategy:**
1. Check if shim interfaces are meant to be implementation or wrapper
2. May need adapter pattern or cast through implementation
3. Check if modules have proper dependencies configured

#### 3.2: PSGuidUtils Method Missing

**Files:**
- `system/webservices/src/com/percussion/webservices/assembly/impl/PSAssemblyDesignWs.java:914`
- Missing: `PSGuidUtils.toGuidList(List<IPSTemplateSlot>)`

**Fix:** Check PSGuidUtils in utils module for similar methods, may need to:
- Add method if it doesn't exist
- Use alternative like: `slots.stream().map(IPSTemplateSlot::getGUID).collect(Collectors.toList())`

#### 3.3: Various Missing Symbols

**Files and symbols:**
- `system/services/src/com/percussion/services/widgetbuilder/IPSWidgetBuilderDefinitionDao.java:160,161` - cannot find symbol
- `system/services/src/com/percussion/services/workflow/PSWorkflowActionsHelper.java:84` - cannot find symbol
- `system/src/main/java/com/percussion/cms/objectstore/PSCloneSiteFolderRequest.java:225` - cannot find symbol
- `system/src/main/java/com/percussion/data/PSQueryJoiner.java:260` - cannot find symbol
- `system/src/main/java/com/percussion/error/PSBackEndError.java:95` - cannot find symbol
- `system/src/main/java/com/percussion/fastforward/managednav/PSManagedNavService.java:964` - cannot find symbol
- `system/src/main/java/com/percussion/install/*.java` - multiple missing symbols
- `system/src/main/java/com/percussion/process/PSProcessDef.java` - 6 missing symbols
- `system/src/main/java/com/percussion/server/PSUserSessionManager.java:216` - cannot find symbol
- `system/src/main/java/com/percussion/server/command/PSConsoleCommandDumpItemSummaryCache.java:115` - cannot find symbol
- `system/src/main/java/com/percussion/server/webservices/PSFolderHandler.java` - 9 missing symbols
- `system/src/main/java/com/percussion/servlets/taglib/PSUIMenuItem.java:47` - cannot find symbol
- `system/src/main/java/com/percussion/system/utils/PSExtensionInstallTool.java:715` - cannot find symbol
- `system/src/main/java/com/percussion/system/utils/PSModifyStyleSheet.java:135` - cannot find symbol
- `system/src/main/java/com/percussion/tools/Logger.java:287` - `m_outFile` (fixed - was `outFile`)
- `system/src/main/java/com/percussion/workflow/PSWorkFlowUtils.java:2142` - missing `isPublic()` method
- `system/webservices/src/com/percussion/webservices/content/ContentSOAPImpl.java:162` - cannot find symbol

**Action:** Each needs individual investigation to determine:
1. Was class/method removed?
2. Was it moved to different package?
3. Does it need to be reimplemented?

#### 3.4: Workflow Service Method Missing

**File:** `system/src/main/java/com/percussion/workflow/PSWorkFlowUtils.java:2142`

**Error:** `cannot find symbol: method isPublic(IPSGuid,IPSGuid)`

**Fix:** Check IPSWorkflowService interface:
- Method may have been renamed
- May need different approach to check if workflow is public

#### 3.5: PSDataConverter Issues

**Files:**
- `system/src/main/java/com/percussion/data/PSIndexedLookupJoiner.java:168`
- Error: `method convert in class PSDataConverter cannot be applied to given types`
- `system/src/main/java/com/percussion/extension/PSJavaScriptUdfExtension.java:146`
- Error: `parseStringToDate(String) has private access in PSDataConverter`

**Fix:**
- Check PSDataConverter method signatures
- `parseStringToDate()` was made private, need to use public alternative or make it accessible

---

## Category 4: Deprecated API Usage

### Priority: MEDIUM

#### 4.1: Hibernate Session API Deprecations

**Affected Files:**
- `system/services/src/com/percussion/services/security/impl/PSBackEndRoleMgr.java`
- Line 260: `saveOrUpdate(Object)` deprecated since 6.0
- Line 613: `saveOrUpdate(Object)` deprecated since 6.0
- Line 648: `delete(Object)` deprecated since 6.0
- Line 895: `createQuery(String)` deprecated

- `system/services/src/com/percussion/services/workflow/impl/PSWorkflowService.java`
  - Line 696: `createQuery(String)` deprecated
  - Line 888: `delete(Object)` deprecated since 6.0

**Fix Strategy:**

```java
// Before
session.saveOrUpdate(entity);

// After - Hibernate 6.0+
session.merge(entity);

// Before
session.delete(entity);

// After
session.remove(entity);

// Before
Query query = session.createQuery("FROM Entity");

// After
Query<Entity> query = session.createQuery("FROM Entity", Entity.class);
```

#### 4.2: Raw Types Usage

**Files:**
- `system/services/src/com/percussion/services/security/impl/PSBackEndRoleMgr.java`
- Line 112: `Class[]` should be `Class<?>[]`
- Lines 311, 339, 424, 445, 461: `Iterator` should be `Iterator<?>`
- Line 895: `Query` should be `Query<?>`

- `system/services/src/com/percussion/services/workflow/impl/PSWorkflowService.java`
  - Line 117: `Class[]` should be `Class<?>[]`
  - Line 131: `Class` should be `Class<?>`
  - Line 165: `Iterator` should be `Iterator<?>`

**Fix:** Add generic type parameters to all raw type usages

---

## Category 5: JSP Tag Library Issues

### Priority: MEDIUM

### Affected Files:

- `system/src/main/java/com/percussion/servlets/taglib/PSCascadeMenuTag.java:26`
- `system/src/main/java/com/percussion/servlets/taglib/PSMenuBarTag.java:25`
- `system/src/main/java/com/percussion/servlets/taglib/PSMenuItemTag.java:52,186`
- `system/src/main/java/com/percussion/servlets/taglib/PSProgressBarTag.java:26`
- `system/src/main/java/com/percussion/servlets/taglib/PSSpanIdTag.java:34`

**Error:** `method does not override or implement a method from a supertype`

**Root Cause:** JSP tag library API changed between Java versions or Servlet API versions

**Fix Strategy:**
1. Check if `@Override` annotations are incorrect
2. Verify superclass/interface methods exist
3. May need to update to newer JSP tag API
4. Check if methods were removed from tag library interfaces

---

## Category 6: Spring Framework Method Override

### Priority: MEDIUM

### File: `system/servlet/src/com/percussion/utils/spring/PSUrlHandlerMapping.java:51`

**Error:** `registerHandler(String,Object) in PSUrlHandlerMapping cannot override registerHandler(String,Object) in AbstractUrlHandlerMapping`

**Root Cause:** Method signature or access modifier changed in Spring Framework

**Fix Strategy:**
1. Check Spring version being used
2. Check AbstractUrlHandlerMapping in that Spring version
3. Update method signature to match parent class
4. May need to change from `public` to `protected`

---

## Category 7: Interface Implementation Issues

### Priority: HIGH

#### 7.1: PSSite Missing Method

**File:** `system/services/src/com/percussion/services/sitemgr/data/PSSite.java:95`

**Error:** `PSSite is not abstract and does not override abstract method getPreviousName() in IPSSite`

**Fix:** Add missing method:

```java
@Override
public String getPreviousName() {
    // Implementation needed
    return previousName;
}
```

#### 7.2: PSSiteManager Missing Method

**File:** `system/services/src/com/percussion/services/sitemgr/impl/PSSiteManager.java:111`

**Error:** `does not override abstract method loadSchemeModifiableImpl(IPSGuid) in IPSSiteManager`

**Fix:** Add missing method implementation

#### 7.3: Ambiguous Method Reference

**File:** `system/src/main/java/com/percussion/cms/handlers/PSActiveAssemblyRequestHandler.java:105`

**Error:** `reference to delete is ambiguous`

**Fix:** Explicitly specify which delete method to call or use method reference with parameter types

---

## Category 8: Functional Interface Issues

### Priority: MEDIUM

### File: `system/services/src/com/percussion/services/widgetbuilder/IPSWidgetBuilderDefinitionDao.java:60`

**Error:** `incompatible thrown types PSDataServiceException in functional expression`

**Root Cause:** Lambda or method reference throws checked exception not compatible with functional interface

**Fix Strategy:**

```java
// Wrap in unchecked exception
() -> {
    try {
        methodThatThrows();
    } catch (PSDataServiceException e) {
        throw new RuntimeException(e);
    }
}

// Or change functional interface to allow checked exception
```

---

## Category 9: Minor Issues

### Priority: LOW

#### 9.1: Unnecessary @SuppressWarnings

**Files:**
- `system/services/src/com/percussion/services/locking/impl/PSObjectLockService.java:374,500,689`
- `system/services/src/com/percussion/services/workflow/impl/PSWorkflowService.java:245`

**Fix:** Remove unnecessary `@SuppressWarnings("unchecked")` annotations

#### 9.2: Unused Imports

**Files:**
- `system/services/src/com/percussion/services/publisher/IPSPublisherService.java` - 6 unused imports
- `system/services/src/com/percussion/services/workflow/impl/PSWorkflowService.java` - 1 unused import

**Fix:** Remove unused import statements

#### 9.3: Unchecked Type Safety Warnings

**File:** `system/services/src/com/percussion/services/security/impl/PSBackEndRoleMgr.java:899`

**Error:** `Type safety: The expression of type List needs unchecked conversion to conform to List<PSCommunityRoleAssociation>`

**Fix:** Add proper generic types to query

---

## Implementation Plan

### Phase 1: Quick Wins (Already Completed)

- [x] Fix PSUrlUtils - add PSURLEncoder import
- [x] Fix RemoteConsole - add PSProperties import
- [x] Fix PSExplicitEtagFilter - remove deprecated setStatus method
- [x] Fix Logger.java - change m_outFile to outFile

### Phase 2: Type System Fixes (Next Priority)

**Estimated Time:** 2-3 hours

1. [ ] Fix all Map<String,String> vs Map<String,Object> issues (6 files)
2. [ ] Fix Stream to Collection conversions (2 files)
3. [ ] Fix Optional unwrapping issues (3 files)
4. [ ] Fix specific type conversions (4 files)

### Phase 3: Missing Methods/Symbols Investigation

**Estimated Time:** 4-6 hours

1. [ ] Investigate all "cannot find symbol" errors
2. [ ] Create list of moved/removed/renamed classes
3. [ ] Fix or find replacements for each missing symbol
4. [ ] Add missing interface methods (PSSite, PSSiteManager)

### Phase 4: SOAP/WebServices Overhaul

**Estimated Time:** 8-12 hours

1. [ ] Analyze WSDL files and generated interfaces
2. [ ] Fix SecuritySOAPImpl (10 errors)
3. [ ] Fix ContentSOAPImpl (30+ errors)
4. [ ] Test web service functionality
5. [ ] May require regenerating SOAP stubs

### Phase 5: Deprecated API Updates

**Estimated Time:** 2-3 hours

1. [ ] Replace Hibernate deprecated methods (6 files)
2. [ ] Fix raw type usages (2 files)
3. [ ] Update Spring method overrides (1 file)

### Phase 6: JSP Tag Library Updates

**Estimated Time:** 1-2 hours

1. [ ] Fix tag library @Override issues (6 files)
2. [ ] Update to newer JSP API if needed

### Phase 7: Final Cleanup

**Estimated Time:** 1 hour

1. [ ] Remove unnecessary @SuppressWarnings
2. [ ] Remove unused imports
3. [ ] Run spotless:apply
4. [ ] Full compile and test

---

## Testing Strategy

### After Each Phase:

```bash
cd /home/nate/projects/percussioncms
./mvnw -pl system compile -DskipTests
```

### After All Phases:

```bash
# Full compile
./mvnw -pl system clean compile

# Run tests
./mvnw -pl system test

# Check for spotless violations
./mvnw -pl system spotless:check

# If violations, apply fixes
./mvnw -pl system spotless:apply
```

---

## Risk Assessment

### HIGH RISK:

- **SOAP/WebServices changes**: May break external API consumers
- **Missing symbols**: May indicate architectural changes needed
- **Hibernate API changes**: Could affect data persistence behavior

### MEDIUM RISK:

- **Type system changes**: Could cause runtime ClassCastException if incorrect
- **Deprecated API usage**: Old code paths may have subtle behavior differences

### LOW RISK:

- **Import fixes**: No functional impact
- **@SuppressWarnings removal**: No functional impact
- **Raw type fixes**: Compile-time only improvements

---

## Dependencies to Verify

Ensure these modules compile first:
1. `modules/utils` - Contains PSURLEncoder, PSProperties
2. `modules/perc-i18n` - Internationalization
3. All other `modules/*` that system depends on

---

## Notes

- Some errors may resolve automatically once earlier errors are fixed
- Web services code may need complete regeneration from WSDL
- Consider creating backup branch before major SOAP changes
- Document any API changes that affect external consumers

---

## Success Criteria

- [ ] `./mvnw -pl system clean compile` succeeds with 0 errors
- [ ] `./mvnw -pl system spotless:check` passes
- [ ] `./mvnw -pl system test` runs (may have test failures to address separately)
- [ ] No new compiler warnings introduced
- [ ] All deprecated API usages documented or replaced

---

## Related Documents

- Java 11 to 17 upgrade instructions: `.github/instructions/java-11-to-java-17-upgrade.instructions.md`
- Java 17 to 21 upgrade instructions: `.github/instructions/java-17-to-java-21-upgrade.instructions.md`
- OWASP security guidelines: `.github/instructions/security-and-owasp.instructions.md`
- Copilot instructions: `.github/copilot-instructions.md`

