# JMock to Mockito Migration Plan for perc-toolkit Module

## Overview

This plan outlines the migration from JMock to Mockito for all test files in the `modules/perc-toolkit` module. The migration will modernize the testing framework while maintaining test functionality and improving readability.

## Goals

- Replace JMock with Mockito for all mocking in test files
- Maintain existing test coverage and functionality
- Improve test readability and maintainability
- Use modern JUnit 5 + Mockito best practices

## Prerequisites

✅ **COMPLETED**: JUnit 5 migration for all test files in perc-toolkit
✅ **COMPLETED**: Mockito dependencies added to pom.xml:
- `mockito-core` (4.11.0)
- `mockito-junit-jupiter` (4.11.0)

## Migration Steps

### Step 1: Import Replacements

Replace JMock imports with Mockito equivalents across all test files:

**JMock Imports to Remove:**

```java
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.jmock.Sequence;
```

**Mockito Imports to Add:**

```java
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
```

### Step 2: Class-Level Changes

**Before (JMock):**

```java
public class MyTest {
    Mockery context = new Mockery(){{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
    IPSContentWs contentWs;
    
    @BeforeEach
    void setUp() {
        contentWs = context.mock(IPSContentWs.class);
    }
}
```

**After (Mockito):**

```java
@ExtendWith(MockitoExtension.class)
public class MyTest {
    
    @Mock
    private IPSContentWs contentWs;
    
    @BeforeEach
    void setUp() {
        // Mocks are automatically initialized by MockitoExtension
    }
}
```

### Step 3: Expectation Syntax Migration

#### 3.1 Method Call Expectations

**Before (JMock):**

```java
context.checking(new Expectations(){{
    one(contentWs).loadContent(with(any(IPSGuid.class)));
    will(returnValue(content));
    
    allowing(gmgr).makeGuid(with(any(PSLocator.class)));
    will(returnValue(guid));
}});
```

**After (Mockito):**

```java
when(contentWs.loadContent(any(IPSGuid.class)))
    .thenReturn(content);
    
when(gmgr.makeGuid(any(PSLocator.class)))
    .thenReturn(guid);
```

#### 3.2 Exception Expectations

**Before (JMock):**

```java
one(service).processRequest(with(any(String.class)));
will(throwException(new PSErrorException()));
```

**After (Mockito):**

```java
when(service.processRequest(any(String.class)))
    .thenThrow(new PSErrorException());
```

#### 3.3 Verification

**Before (JMock):**

```java
context.assertIsSatisfied();
```

**After (Mockito):**

```java
verify(contentWs).loadContent(any(IPSGuid.class));
verify(gmgr, times(2)).makeGuid(any(PSLocator.class));
```

### Step 4: Sequence Handling

**Before (JMock):**

```java
final Sequence filterSeq = context.sequence("filter");   
context.checking(new Expectations(){{
    one(systemWs).loadRelationships(with(any(PSRelationshipFilter.class)));
    inSequence(filterSeq);
    will(returnValue(emptyRels));
    
    one(systemWs).loadRelationships(with(any(PSRelationshipFilter.class)));
    inSequence(filterSeq);
    will(returnValue(oneRels)); 
}});
```

**After (Mockito):**

```java
when(systemWs.loadRelationships(any(PSRelationshipFilter.class)))
    .thenReturn(emptyRels)
    .thenReturn(oneRels);
```

### Step 5: Argument Matchers

**JMock to Mockito Matcher Mapping:**
- `with(any(Class.class))` → `any(Class.class)`
- `with(equal(value))` → `eq(value)`
- `with(same(object))` → `same(object)`
- `with(aNull(Class.class))` → `isNull()`
- `with(aNonNull(Class.class))` → `notNull()`

### Step 6: Files Requiring Migration

#### High Priority Files (Complex JMock Usage):

1. **PSOSlotContentsTest.java** - Complex expectations with multiple mocks
2. **PSONodeCatalogerTest.java** - Multiple mock interactions
3. **PSORevisionCorrectingItemFilterTest.java** - Complex mock setup
4. **PSOUniqueFieldWithInFoldersValidatorTest.java** - Extensive mocking
5. **PSOAbstractItemValidationExitTest.java** - Mock expectations

#### Medium Priority Files:

6. **PublishEditionServiceTest.java** - Basic mocking
7. **PSOWFActionServiceTest.java** - Sequence expectations
8. **PSOSpringWorkflowActionDispatcherTest.java** - JMock with JUnit integration
9. **PSOProxyQueryResourceTest.java** - @RunWith(SpringJUnit4ClassRunner.class)

#### Low Priority Files:

10. **RssJexlTest.java** - Has @RunWith(JMock.class) annotation
11. **PSODateAdjustTest.java** - Simple mock usage
12. **PSOThumbnailGeneratorTest.java** - Minimal mocking
13. **PSODateRangeFieldValidatorTest.java** - Basic expectations

### Step 7: Testing Strategy

For each migrated test file:
1. **Run individual test**: `mvn test -Dtest=ClassName`
2. **Verify all assertions pass**
3. **Check mock interactions are correct**
4. **Ensure no deprecated warnings**

### Step 8: Search and Replace Patterns

Use VS Code "Search and Replace" task with these patterns:

#### Pattern 1: Remove Mockery Declaration

- **Find:** `Mockery context = new Mockery.*?;`
- **Replace:** `// Mockery replaced with @Mock annotations`

#### Pattern 2: Replace Mock Creation

- **Find:** `context\.mock\(([^,]+)\.class(?:, "([^"]+)")?\)`
- **Replace:** `mock($1.class)`

#### Pattern 3: Replace Expectations Block

- **Find:** `context\.checking\(new Expectations\(\)\{\{`
- **Replace:** `// Using Mockito when().thenReturn() syntax`

#### Pattern 4: Replace Method Expectations

- **Find:** `one\(([^)]+)\)\.([^(]+)\(([^)]*)\);\s*will\(returnValue\(([^)]+)\)\);`
- **Replace:** `when($1.$2($3)).thenReturn($4);`

### Step 9: Clean Up

After migration:
1. **Remove unused JMock imports**
2. **Remove context.assertIsSatisfied() calls**
3. **Add appropriate Mockito verify() calls**
4. **Remove @RunWith annotations for JMock**
5. **Add @ExtendWith(MockitoExtension.class)**

### Step 10: Documentation Updates

Update `modules/perc-toolkit/README.md` with:
- Migration completion status
- New testing patterns with Mockito
- Examples of common Mockito usage

## Benefits of Migration

- **Modern syntax**: More readable and intuitive than JMock
- **Better IDE support**: IntelliJ/Eclipse have excellent Mockito integration
- **Simplified setup**: No need for Mockery context management
- **Type safety**: Better compile-time checking with Mockito
- **Community support**: Mockito is more widely used and supported

## Rollback Plan

If issues arise during migration:
1. Keep JMock dependencies in pom.xml during transition
2. Revert individual files if test failures occur
3. Use git to restore previous working state
4. Complete migration in smaller batches if needed

## Timeline

- **Phase 1** (Day 1): Migrate 4-5 high priority files
- **Phase 2** (Day 2): Migrate remaining medium priority files
- **Phase 3** (Day 3): Migrate low priority files and cleanup
- **Phase 4** (Day 4): Testing, documentation, and verification

## Success Criteria

✅ All tests pass with Mockito
✅ No JMock imports remain in test files
✅ All @ExtendWith(MockitoExtension.class) annotations added
✅ README.md updated with new testing patterns
✅ No compilation errors or warnings
✅ Test coverage maintained or improved

---

**Status**: Ready to begin migration
**Created**: August 5, 2025
**Last Updated**: August 5, 2025
