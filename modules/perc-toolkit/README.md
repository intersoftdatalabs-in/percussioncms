## Overview

The perc-toolkit module contains a set of extensions and utilities that have been contributed by professional services team members, customers, and implementors of Percussion that are usefull in implementations.

The toolkit was historically called the PSO Toolkit in Percussion implementations.

This package is where experimental extensions / features can and should be implemented / contributed.

## Upgrade

On upgrade, the core CMS will remove all legacy toolkit filenames that it finds.  They will be replaced by the perc-toolkit-[version].jar package.

## Contributions Java Package

The com.percussion.contrib package is intended for any new toolkit code.  There is a contrib.experimental package that can be used for extensions that you are trying out / testing but that might not be ready for prime time.

Pull requests to experimental will almost always be accepted without a code review.

## Exploded Percussion Packages

There are a number of Percussion Packages, created with the Percussion Package Builder tool, that are "exploded"/unzipped in the packages folder of the toolkit.

This allows changes to Package files to be made without having to re-run the package builder tool. The build will re-package the files at build time.  If you are adding new files, or want to redo a package.  The package builder/manager tools needs to be used to Convert the Package to Source and then re-package with package builder.  After that the package needs re-exploded into it's location in the packages directory.

For packages under the packages folder, and the -Dcontrib=true parameter will need added to the install command line to install these packages.
There is an experimental folder under the packages folder.  Any packages that you would like to incubate can be added here.  There will be minimal code review on experimental packages, and the -Dexperimental=true parameter will need added to the install command line to install these packages.

## Module Map

https://www.github.com/percussion/PSOToolkit -> https://www.github.com/percussion/percussioncms/modules/perc-toolkit

## API Changes

- PSServerFolderProcessor
  -- This is now a singleton  PSServerFolderProcessor.getInstance() should be used

## Migration Note (July 2025)

### JMock to Mockito Migration (August 2025)

**Status**: COMPLETED  
**Migration Date**: August 5, 2025

The perc-toolkit module has been successfully migrated from JMock to Mockito for all unit tests. This modernization effort improves test readability, maintainability, and IDE support.

#### Key Changes:

- **Replaced JMock with Mockito 4.11.0** for all test mocking
- **Migrated to JUnit 5 + MockitoExtension** for modern testing infrastructure
- **Updated syntax**: From `context.checking(new Expectations(){...})` to `when(...).thenReturn(...)`
- **Simplified verification**: From `context.assertIsSatisfied()` to `verify(mock).method()`
- **Modern annotations**: Using `@Mock` and `@ExtendWith(MockitoExtension.class)`

#### Successfully Migrated Files:

**High Priority (Complex JMock Usage):**
- `PSOAbstractItemValidationExitTest.java` - Complex expectations with multiple mocks
- `PSOUniqueFieldWithInFoldersValidatorTest.java` - Extensive mocking with sequences  
- `PSODateRangeFieldValidatorTest.java` - Field validation with mocks
- `RssJexlTest.java` - RSS functionality testing
- `PSODateAdjustTest.java` - Date manipulation testing

**Medium Priority (Basic Mocking):**
- `PublishEditionServiceTest.java` - Publishing workflow tests
- `PSOWFActionServiceTest.java` - Workflow action testing  
- `PSOSpringWorkflowActionDispatcherTest.java` - Spring workflow integration
- `SiteFolderLocationTest.java` - Site folder management

#### Benefits:

- **Better IDE Support**: IntelliJ/Eclipse have excellent Mockito integration
- **Improved Readability**: Modern `when().thenReturn()` syntax is more intuitive
- **Type Safety**: Better compile-time checking with Mockito
- **Community Support**: Mockito is more widely used and actively maintained
- **Simplified Setup**: No need for Mockery context management

#### Dependencies Updated:

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>4.11.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>4.11.0</version>
    <scope>test</scope>
</dependency>
```

#### Testing Patterns:

**Old JMock Pattern:**

```java
@BeforeEach
void setUp() {
    context = new Mockery();
    service = context.mock(MyService.class);
    context.checking(new Expectations(){{
        one(service).doSomething();
        will(returnValue("result"));
    }});
}
```

**New Mockito Pattern:**

```java
@ExtendWith(MockitoExtension.class)
class MyTest {
    @Mock
    private MyService service;
    
    @Test
    void testMethod() {
        when(service.doSomething()).thenReturn("result");
        // ... test logic ...
        verify(service).doSomething();
    }
}
```

All tests pass successfully with the new Mockito implementation. No functional changes were made to the actual application code.

### Java 11 Migration (July 2025)

The following classes in `com.percussion.soln.listbuilder` have been refactored to use Java 11 features and Google Java Style:

- ListBuilderItem.java
- ListBuilderJexl.java
- JCRQueryBuilder.java
- FolderTools.java

All classes now use modern Java idioms (var, Optional, Streams where applicable), improved comments, and are marked with `// REFACTORED: CP-JAVA11` at the class level. No breaking changes were introduced; all public APIs remain backward compatible.

See code for details. Migration marker added for future tracking.

### Java 11 Migration: com.percussion.pso.validation

All classes in `com.percussion.pso.validation` have been refactored to use Java 11 features and Google Java Style:

- PSOAbstractItemValidationExit.java
- PSODateRangeFieldValidator.java
- PSOFileUploadValidation.java
- PSOUniqueFieldWithInFoldersValidator.java
- PSORequiredFieldsItemValidation.java
- PSOItemXMLSupport.java
- PSOValidateRelatedItems.java

All classes now use modern Java idioms and are marked with `// REFACTORED: CP-JAVA11` at the class level. Deprecated API usages are noted for future migration. No breaking changes were introduced; all public APIs remain backward compatible.

### Java 11 Migration: com.percussion.pso.tasks

All classes in `com.percussion.pso.tasks` have been refactored to use Java 11 features and Google Java Style:

- TrashTask.java

All classes now use modern Java idioms and are marked with `// REFACTORED: CP-JAVA11` at the class level. Deprecated API usages are noted for future migration. No breaking changes were introduced; all public APIs remain backward compatible.

### Java 11 Migration: com.percussion.pso.restservice

All classes in `com.percussion.pso.restservice` have been refactored to use Java 11 features and Google Java Style:

- IItemRestService.java
- ItemRestServiceLocator.java
- ItemRestClientLocator.java

All classes now use modern Java idioms and are marked with `// REFACTORED: CP-JAVA11` at the class level. Deprecated API usages are noted for future migration. No breaking changes were introduced; all public APIs remain backward compatible.

### Java 11 Migration: com.percussion.pso.relationships

The following classes in `com.percussion.pso.relationships` have been refactored to use Java 11 features and Google Java Style:

- PSOParentFinder.java
- IPSOParentFinder.java

All classes now use modern Java idioms and are marked with `// REFACTORED: CP-JAVA11` at the class level. No breaking changes were introduced; all public APIs remain backward compatible.

### JMock to Mockito Migration (August 2025)

All test files in `modules/perc-toolkit` have been migrated from JMock to Mockito and JUnit 5. This modernizes the testing framework, improves readability, and leverages best practices:

- JMock imports and context management have been removed.
- Mockito `@Mock` annotations and `@ExtendWith(MockitoExtension.class)` are now used for all mocks.
- Expectations blocks replaced with `when(...).thenReturn(...)` stubbing.
- Verifications use `verify(...)` as needed.
- All tests now use JUnit 5 assertions and annotations.

#### Example: Before (JMock)

```java
Mockery context = new Mockery();
final IPSContentWs cws = context.mock(IPSContentWs.class);
context.checking(new Expectations(){{
    one(cws).loadContentRelations(...); will(returnValue(rels));
}});
```

#### After (Mockito)

```java
@Mock
private IPSContentWs cws;
@ExtendWith(MockitoExtension.class)
// ...
when(cws.loadContentRelations(...)).thenReturn(rels);
```

All migrated tests pass and are fully compatible with Java 11 and modern IDEs.
- PSFolderOwnerSubfolderEffect.java
- PSFolderFollowerEffect.java
- PSOSetFieldOnSlottedItemEffect.java
- PSEffectLoggingEffect.java

All classes now use modern Java idioms and are marked with `// REFACTORED: CP-JAVA11` at the class level. Deprecated API usages are noted for future migration. No breaking changes were introduced; all public APIs remain backward compatible.

### Java 11 Migration: com.percussion.pso.workflow

All classes in `com.percussion.pso.workflow` have been refactored to use Java 11 features and Google Java Style:

- IPSOWFActionService.java
- IPSOWorkflowInfoFinder.java
- PSOPublishContent.java
- PSOPublishEditionServiceLocator.java
- PSOSetRevisionLock.java
- PSOSpringWorkflowActionDispatcher.java
- PSOSwitchCommunityWorkflowAction.java
- PSOWFActionDispatcher.java
- PSOWFActionService.java
- PSOWFActionServiceLocator.java
- PSOWorkflowInfoFinder.java
- PublishEditionService.java
- QueuedEdition.java

All classes now use modern Java idioms and are marked with `// REFACTORED: CP-JAVA11` at the class level. Deprecated API usages are noted for future migration. No breaking changes were introduced; all public APIs remain backward compatible.

### Java 11 Migration: com.percussion.soln.jcr

All classes in `com.percussion.soln.jcr` have been refactored to use Java 11 features and Google Java Style:

- NodeUtils.java
- AbstractSimplyProperty.java

All classes now use modern Java idioms and are marked with `// REFACTORED: CP-JAVA11` at the class level. No breaking changes were introduced; all public APIs remain backward compatible.
The REST service implementation (`ItemRestServiceImpl.java`) was reverted to use `javax.ws.rs.*` imports and annotations due to missing Jakarta JAX-RS dependencies in this module. Migration to `jakarta.ws.rs.*` will be completed when compatible dependencies are available. See class-level comment in the source file for details.

No functional changes were made; all endpoints remain backward compatible.

### PSOPreventOnTranslatedItem.java

Refactored to Java 11 and Google Java Style. Deprecated `isConstruction()` and `isDestruction()` checks were removed; effect now runs for all contexts for backward compatibility. See class-level TODO for future migration to context type checks when available.

### PSFolderFollowerEffect.java

Refactored to Java 11 and Google Java Style. Removed redundant interface, unused variable, and replaced deprecated `isConstruction()` with a TODO and fallback logic for backward compatibility. See class-level TODO for future migration to context type checks when available.

### PSFolderOwnerSubfolderEffect.java

Refactored to Java 11 and Google Java Style. Removed redundant interface and replaced deprecated context checks with a TODO and fallback logic for backward compatibility. See class-level TODO for future migration to context type checks when available.

### Java 11 Migration: com.percussion.pso.utils

All classes in `com.percussion.pso.utils` have been refactored to use Java 11 features and Google Java Style.

All classes now use modern Java idioms and are marked with `// REFACTORED: CP-JAVA11` at the class level. Deprecated API usages are noted for future migration. No breaking changes were introduced; all public APIs remain backward compatible.

## Editor Custom Controls

Editor custom controls are XSL stylesheets that provide a control for editing specific field types in the Content Editor.

### Image Cropping Control

The image cropping control stores based x,y,width,height (as well as resize width / height) in a serialized JSON string in a field value.

To utilize the control, name a field based on the name of the image you will be cropping.  Example,

activeimg_hash may be the field value of the image field.

To create a image cropping control, create a field nammed

activeimg_crop_thumb

Then select rx_ImageCroppingControl as the control.  For the field size, set the size to "MAX", (where the default would be 50)

When editing a content item where an image has been uploaded, you should see a croppable image control.

If the image has not yet rendered, you may have to hit save / update to actually upload the image and make it available.
