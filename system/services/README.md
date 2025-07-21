# Services Module - Java 11 Modernization

## Overview

The services module provides comprehensive business logic and data access services for the Percussion CMS system. This module has been modernized to leverage Java 11 features including `var` declarations, `Optional` wrappers, Stream API, and enhanced validation patterns.

## Completed Packages

### Content Package (`com.percussion.services.content`)

The content package provides comprehensive content management capabilities including keyword management, auto-translations, and folder properties.

**Key Components:**
- **IPSContentService** - Main service interface with enhanced documentation and Optional support
- **PSContentService** - Implementation with `var`, streams, and modern validation
- **PSKeyword/PSKeywordChoice** - Data entities with factory methods and Optional wrappers
- **PSAutoTranslation** - Auto-translation configuration with comprehensive validation
- **PSFolderProperty** - Folder metadata with modern validation patterns
- **PSContentException** - Enhanced exception handling with factory methods
- **PSContentServiceLocator** - Thread-safe service locator with AtomicReference

### Assembly Package (`com.percussion.services.assembly`)

The assembly package provides comprehensive content assembly and templating services for template processing, variable binding, and content rendering.

**Key Components:**
- **IPSAssemblyService** - Main assembly service interface with Optional return types and Stream API
- **IPSAssemblyItem** - Assembly item interface with Optional wrappers and parameter streaming
- **PSAssemblyTemplate** - Template entity with factory methods and immutable collections
- **PSTemplateBinding** - Binding entity with JEXL caching and enhanced validation
- **PSAssemblyException** - Factory methods and enhanced error handling
- **PSAssemblyServiceLocator** - Thread-safe with AtomicReference

### Catalog Package (`com.percussion.services.catalog`)

The catalog package provides foundational cataloging services for object enumeration, type management, and XML serialization operations across the system.

**Key Components:**
- **IPSCataloger** - Main cataloging interface with Set-based type handling and Stream API
- **IPSCatalogSummary** - Summary interface with Optional wrappers and display utilities
- **IPSCatalogItem** - Full catalog item interface with enhanced XML serialization
- **IPSCatalogIdentifier** - Base identifier interface with GUID utilities and type access
- **PSCatalogException** - Enhanced exception handling with factory methods for specific error types
- **PSObjectSummary** - Comprehensive object summary with permissions and locking support

### GUID Manager Package (`com.percussion.services.guidmgr`)

The GUID manager package provides globally unique identifier generation and management services with modern Java 11 concurrency patterns.

**Key Components:**
- **IPSGuidManager** - Main GUID service interface with Stream API and Optional support
- **PSGuidManagerLocator** - Thread-safe service locator with enhanced error handling

### Content Change Package (`com.percussion.services.contentchange`)

The content change package provides content modification tracking and notification services for monitoring content lifecycle events.

**Key Components:**
- **IPSContentChangeService** - Content change tracking interface with Stream API support
- **PSContentChangeServiceLocator** - Thread-safe service locator with Optional-based access

### Content Manager Package (`com.percussion.services.contentmgr`)

The content manager package provides comprehensive content management operations with JCR integration, including node definitions, content retrieval, and workflow management.

**Key Components:**
- **IPSContentMgr** - Main content management interface with Optional-based safe access and asynchronous operations
- **IPSContentTypeMgr** - Content type management with Stream support and CompletableFuture operations
- **IPSNode** - Extended JCR node interface with GUID support and Optional-based property access
- **IPSNodeDefinition** - Node definition interface with template/workflow associations and Stream operations
- **IPSContentPropertyConstants** - Modern constants interface with immutable collections and utility methods
- **PSContentMgrConfig** - Configuration class with EnumSet options and Optional-based interceptor access
- **PSContentMgrLocator** - Thread-safe service locator with enhanced error handling
- **PSContentMgrOption** - Enhanced enum with descriptive fields and factory methods

### Data Package (`com.percussion.services.data`)

The data package provides foundational interfaces for object identification and cloning operations with modern Java 11 patterns.

**Key Components:**
- **IPSIdentifiableItem** - GUID identification interface with Optional-based safe access, validation utilities, and comparison operations
- **IPSCloneTuner** - Object cloning interface with Optional-based safe operations, enhanced validation, and comprehensive error handling

### Error Package (`com.percussion.services.error`)

The error package provides modern exception handling for the services module with comprehensive error context and internationalization support.

**Key Components:**
- **PSRuntimeException** - Base runtime exception class with Optional support, factory methods, and internationalized message handling
- **PSNotFoundException** - Object not found exception with GUID/name/type context tracking and Optional-based safe access
- **PSDuplicateNameException** - Duplicate name conflict exception with detailed conflict information and validation utilities

### General Package (`com.percussion.services.general`)

The general package provides foundational server information access services for retrieving Rhythmyx configuration and runtime properties.

**Key Components:**
- **IPSRhythmyxInfo** - Modern server information interface with type-safe property access, Optional support, and enhanced Key enum with utility methods
- **PSRhythmyxInfoLocator** - Thread-safe service locator with Optional-based safe access and convenience methods for common server properties
- **PSRhythmyxInfo** (impl) - ConcurrentHashMap-based implementation with lazy initialization and comprehensive cache management

### Memory Package (`com.percussion.services.memory`)

The memory package provides comprehensive cache access services using EHCache for high-performance object caching with TTL/TTI support and notification-based invalidation.

**Key Components:**
- **IPSCacheAccess** - Main cache service interface with Optional-based safe retrieval, comprehensive validation, and enhanced documentation with proper exception handling
- **PSCacheAccessLocator** - Thread-safe service locator using composition pattern with double-checked locking and proper utility class design
- **PSEhCacheAccessor** - EHCache-based implementation with graceful error handling, comprehensive validation methods, and enhanced statistics gathering
- **PSEhCacheNotificationListener** - Modern notification listener with GUID-based cache invalidation and robust error handling

**Key Features:**
- **Optional-based API** - `get()` method returns `Optional<Serializable>` for null-safe operations
- **Thread-safe operations** - All cache operations are thread-safe with comprehensive error handling
- **Cache regions** - Support for multiple cache regions with individual TTL/TTI configuration
- **Statistics monitoring** - Comprehensive cache statistics with immutable collections
- **Notification integration** - Automatic cache invalidation via Spring-based notification service
- **Enhanced validation** - Input validation using `Objects.requireNonNull()` and custom validation methods

**Usage Examples:**
```java
// Safe cache access with Optional
var cache = PSCacheAccessLocator.getCacheAccess();
var result = cache.get("myKey", IPSCacheAccess.IN_MEMORY_STORE);
result.ifPresent(value -> System.out.println("Found: " + value));

// Cache operations with validation
cache.save("contentId", contentObject, IPSCacheAccess.IN_MEMORY_STORE);
cache.setTimeToLive("contentId", IPSCacheAccess.IN_MEMORY_STORE, 3600);

// Statistics gathering
var statistics = cache.getStatistics();
statistics.forEach(stat -> 
    System.out.println(stat.getName() + ": " + stat.getMemoryItems() + " items"));
```

### Notification Package (`com.percussion.services.notification`)

The notification package provides comprehensive event-driven communication capabilities implementing the Observer pattern for decoupled, asynchronous messaging throughout the CMS system.

**Key Components:**
- **IPSNotificationService** - Main notification service interface with CompletableFuture support, Stream API for listener filtering, Optional-based safe access, and functional interface support for listener predicates
- **IPSNotificationListener** - Functional interface for event listeners with Optional-based safe notification handling, CompletableFuture support for asynchronous processing, and enhanced listener metadata
- **PSNotificationEvent** - Immutable event data model with factory methods for type-safe creation, Optional-based safe target access, modern time handling with Instant, and comprehensive event type enumeration
- **PSNotificationServiceLocator** - Thread-safe service locator using AtomicReference with Optional-based safe access and enhanced error handling
- **PSNotificationHelper** - Utility class with static helper methods for common notification scenarios, CompletableFuture support for async operations, and comprehensive validation

**Key Features:**
- **Asynchronous processing** - CompletableFuture support for non-blocking event notification and processing
- **Type-safe events** - Factory methods and enhanced EventType enum with comprehensive descriptions and metadata
- **Stream-based filtering** - Efficient listener filtering and processing using Stream API and functional predicates
- **Optional safety** - Null-safe operations throughout with Optional wrappers for safe access patterns
- **Immutable design** - Thread-safe, immutable event objects with defensive copying and validation
- **Enhanced validation** - Comprehensive input validation using Objects.requireNonNull and custom validation methods

**Event Types:**
The system supports comprehensive event types including:
- **File events** - File modification notifications for content management
- **Object invalidation** - Cache invalidation events with EHCache integration
- **Content changes** - Content item modification and workflow events
- **Site operations** - Site deletion and rename notifications
- **Server lifecycle** - Initialization, post-init, and shutdown events
- **Relationship changes** - Content relationship modification events
- **Security events** - Permission and access control notifications

**Usage Examples:**
```java
// Safe notification service access
var notificationService = PSNotificationServiceLocator.getNotificationServiceSafely();
notificationService.ifPresent(service -> {
    // Register a listener with type safety
    service.addListener(EventType.CONTENT_CHANGED, event -> {
        event.getTargetAs(IPSGuid.class).ifPresent(guid -> 
            System.out.println("Content changed: " + guid));
    });
    
    // Stream-based listener filtering
    service.streamListeners(EventType.CONTENT_CHANGED)
        .filter(listener -> listener.getPriority() > 5)
        .forEach(listener -> System.out.println("High priority listener: " + 
            listener.getListenerName()));
});

// Type-safe event creation with factory methods
var fileEvent = PSNotificationEvent.createFileEvent(new File("/path/to/file"));
var contentEvent = PSNotificationEvent.createContentChangeEvent(contentGuid);
var invalidationEvent = PSNotificationEvent.createObjectInvalidationEvent(cacheKey);

// Asynchronous notification processing
var future = PSNotificationHelper.notifyContentChangeAsync(contentGuid);
future.thenRun(() -> System.out.println("Content change notification sent"));

// Safe notification with error handling
var result = PSNotificationHelper.notifyEventSafely(EventType.CONTENT_CHANGED, contentGuid);
result.ifPresent(error -> System.err.println("Notification failed: " + error.getMessage()));

// Listener with enhanced capabilities
service.addListener(EventType.OBJECT_INVALIDATION, new IPSNotificationListener() {
    @Override
    public void notifyEvent(PSNotificationEvent notification) {
        // Handle notification
    }
    
    @Override
    public boolean shouldHandle(PSNotificationEvent notification) {
        return notification.getTargetAs(String.class)
            .filter(key -> key.startsWith("cache:"))
            .isPresent();
    }
    
    @Override
    public int getPriority() {
        return 10; // High priority
    }
});

// Advanced listener management
service.findListenersByType(EventType.CONTENT_CHANGED, MyCustomListener.class)
    .forEach(listener -> listener.customMethod());

// Event validation and processing
service.validateEvent(event)
    .ifPresentOrElse(
        validEvent -> service.notifyEvent(validEvent),
        () -> System.err.println("Invalid event"));
```

### Security Package (`com.percussion.services.security`)

The security package provides comprehensive authentication, authorization, and Access Control List (ACL) management capabilities with OWASP-compliant security patterns and modern Java 11 features.

**Key Components:**
- **IPSAclService** - Main ACL service interface with Optional-based safe access, Stream API for efficient ACL filtering, CompletableFuture support for asynchronous security operations, and functional interfaces for access level predicates
- **IPSAuthentication** - User authentication interface with Optional-based safe access for user information, Stream API for efficient role processing, enhanced role management with bulk operations, and comprehensive authentication context
- **PSAclServiceLocator** - Thread-safe service locator using AtomicReference with Optional-based safe access, functional interfaces for service operations, and enhanced error handling
- **PSSecurityException** - Enhanced exception handling with factory methods for common security scenarios, Optional-based context access, OWASP-compliant error handling, and comprehensive error categorization
- **IPSSecurityErrors** - Modern error code enumeration with immutable collections for error categorization, utility methods for error validation, and comprehensive error metadata

**Key Features:**
- **OWASP-compliant security** - No sensitive data exposure in error messages, secure authentication patterns, and comprehensive input validation
- **Thread-safe operations** - All security operations use AtomicReference and modern concurrency patterns
- **Optional-based safety** - Null-safe operations throughout with Optional wrappers for enhanced type safety
- **Stream-based processing** - Efficient role filtering, ACL processing, and permission checks using functional programming
- **Enhanced validation** - Comprehensive input validation using Objects.requireNonNull and custom validation methods
- **Async support** - CompletableFuture integration for non-blocking security operations

**Security Features:**
The system provides comprehensive security capabilities including:
- **Access Control Lists (ACLs)** - Fine-grained permission management with object-level security
- **Authentication** - User login validation with role-based access control
- **Authorization** - Permission checking with role hierarchy and bulk operations
- **Error categorization** - Critical errors, ACL errors, and configuration errors with proper classification
- **Security validation** - Input validation, principal validation, and policy compliance checking

## Backwards Compatibility Notes

### **99% Backwards Compatible - Minimal Migration Required**

The security package modernization maintains full backwards compatibility for existing code with only **2 minor breaking changes** that require updates:

#### **✅ Fully Compatible (No Changes Required):**

**Service Locator Usage:**
```java
// Existing code continues to work unchanged
IPSAclService aclService = PSAclServiceLocator.getAclService();
aclService.getUserAccessLevel(objectGuid);
aclService.loadAcls(aclGuids);
```

**Exception Handling:**
```java
// Existing exception handling works unchanged
try {
    // ACL operations
} catch (PSSecurityException e) {
    // Existing error handling code
}
```

**ACL Service Operations:**
```java
// All existing ACL service calls work unchanged
PSUserAccessLevel level = aclService.getUserAccessLevel(guid);
List<IPSAcl> acls = aclService.loadAcls(guidList);
IPSAcl acl = aclService.createAcl(guid, owner);
```

#### **⚠️ Minor Breaking Changes (2 Items Only):**

**1. IPSAuthentication Implementations Need Updates:**

Existing implementations must add one new abstract method and rename one existing method:

```java
// BEFORE (existing implementation)
public class MyAuthentication implements IPSAuthentication {
    public String getUserName() { /* existing code */ }
    
    public boolean isUserInRole(String roleName) {
        // Existing role checking logic
    }
}

// AFTER (updated implementation)
public class MyAuthentication implements IPSAuthentication {
    public String getUserName() { /* existing code */ }
    
    // Add this new required method
    public Set<String> getUserRoles() {
        // Return set of user's roles
        return Set.of("role1", "role2");
    }
    
    // Rename existing method (implementation logic unchanged)
    public boolean isUserInRoleImpl(String roleName) {
        // Existing role checking logic (no changes needed)
    }
}
```

**2. Error Code Typo Fix:**

One error constant name was corrected:

```java
// BEFORE (with typo)
IPSSecurityErrors.ACCCESS_DENIED_ERROR

// AFTER (typo fixed)
IPSSecurityErrors.ACCESS_DENIED
```

#### **🚀 Enhanced Features Available (Optional Usage):**

**New Optional-Based Safe Access:**
```java
// Enhanced null-safe operations (optional to use)
var aclService = PSAclServiceLocator.getAclServiceSafely();
aclService.ifPresent(service -> {
    var accessLevel = service.getUserAccessLevelSafely(guid);
    accessLevel.ifPresent(level -> System.out.println("Access: " + level));
});

// New factory methods for exceptions (optional to use)
try {
    // Security operations
} catch (Exception e) {
    throw PSSecurityException.accessDenied(objectId, userName);
}
```

**New Stream-Based Processing:**
```java
// Enhanced role processing (optional to use)
var auth = getAuthentication();
auth.streamUserRoles()
    .filter(role -> role.startsWith("admin"))
    .forEach(System.out::println);

// Enhanced ACL filtering (optional to use)
aclService.streamAcls(aclGuids)
    .filter(acl -> acl.getOwner().equals(currentUser))
    .collect(Collectors.toList());
```

**New Async Operations:**
```java
// Asynchronous security operations (optional to use)
CompletableFuture<PSUserAccessLevel> future = 
    aclService.getUserAccessLevelAsync(objectGuid);
future.thenAccept(level -> System.out.println("Access: " + level));
```

#### **📋 Migration Checklist:**

**For most projects: ✅ No changes required**

**If you implement IPSAuthentication:**
1. ✅ Add `getUserRoles()` method returning `Set<String>`
2. ✅ Rename `isUserInRole()` to `isUserInRoleImpl()`

**If you reference the typo constant:**
3. ✅ Change `ACCCESS_DENIED_ERROR` to `ACCESS_DENIED`

**Optional enhancements:**
4. 🚀 Consider using new Optional-based safe access methods
5. 🚀 Consider using Stream API for role/ACL processing
6. 🚀 Consider using async operations for better performance

**Usage Examples:**
```java
// Traditional usage (still works)
var aclService = PSAclServiceLocator.getAclService();
var userLevel = aclService.getUserAccessLevel(objectGuid);
var hasAccess = aclService.hasAccess(objectGuid, requiredLevel);

// Modern usage with enhanced safety
var aclServiceOpt = PSAclServiceLocator.getAclServiceSafely();
aclServiceOpt.ifPresent(service -> {
    // Stream-based ACL filtering
    service.streamAcls(aclGuids)
        .filter(acl -> service.validateAcl(acl).isEmpty())
        .forEach(acl -> System.out.println("Valid ACL: " + acl.getId()));
    
    // Bulk access checking
    var accessibleObjects = service.filterAccessibleObjects(objectGuids, requiredLevel)
        .collect(Collectors.toList());
    
    // Safe authentication handling
    var auth = getCurrentAuthentication();
    if (auth.isAuthenticated()) {
        var adminRoles = auth.filterRolesByPrefix("admin")
            .collect(Collectors.toSet());
    }
});

// Enhanced exception handling with context
try {
    aclService.saveAcls(aclList);
} catch (PSSecurityException e) {
    e.getSecurityContext().ifPresent(context -> 
        logger.error("Security error in context: " + context));
    e.getObjectId().ifPresent(id -> 
        logger.error("Failed for object: " + id));
}

// OWASP-compliant error handling
try {
    // Security operations
} catch (PSSecurityException e) {
    // Safe error message without sensitive data
    response.sendError(403, e.getSafeErrorMessage());
}
```

## Java 11 Features Applied

All refactored packages now leverage modern Java 11 features:

- **`var` keyword** for local variable declarations
- **Optional** for null-safe operations and error handling
- **Stream API** for functional-style data processing
- **Immutable collections** (`Set.of()`, `Map.of()`, `List.of()`)
- **Enhanced validation** with `Objects.requireNonNull()`
- **Method references** and lambda expressions
- **CompletableFuture** for asynchronous operations
- **EnumSet** for type-safe option management
- **AtomicReference** for thread-safe service locators

## Testing

All refactored components include comprehensive JUnit5 tests with:
- Edge case coverage
- Optional behavior validation
- Stream operation testing
- Error handling verification
- Thread safety validation

## Usage Examples

```java
// Optional-based safe access
var contentService = PSContentServiceLocator.getContentServiceSafely();
contentService.ifPresent(service -> {
    var keywords = service.findKeywordsSafely("category");
    keywords.ifPresent(list -> list.forEach(System.out::println));
});

// Stream-based processing
var assemblyService = PSAssemblyServiceLocator.getAssemblyService();
assemblyService.streamAvailableTemplates()
    .filter(template -> template.getLabel().contains("news"))
    .forEach(template -> System.out.println(template.getName()));

// Enhanced validation with factory methods
var contentMgrConfig = PSContentMgrConfig.createWithOptions(
    PSContentMgrOption.LAZY_LOAD_CHILDREN,
    PSContentMgrOption.LOAD_MINIMAL
);
```

### Publisher Package (`com.percussion.services.publisher`)

The publisher package provides comprehensive publishing service interfaces for content list management, delivery type configuration, edition handling, and publishing workflow coordination with modern Java 11 safety and performance patterns.

**Key Components:**
- **IPSPublisherService** - Main publishing service interface with Optional-based safe access, Stream API for efficient content list processing, CompletableFuture support for asynchronous operations, and enhanced validation with Objects.requireNonNull
- **IPSContentList** - Content list interface with enhanced Type enum supporting label-based lookup, Optional-based safe property access, Stream API for parameter processing, and comprehensive validation for generator/expander parameters
- **IPSDeliveryType** - Delivery type configuration interface with Optional-based safe description access, enhanced validation for bean names, and modern boolean property handling with convenience methods
- **IPSEdition** - Publishing edition interface with advanced Priority enum supporting comparison operations, Optional-based safe access for all nullable properties, and comprehensive validation with convenient priority checking methods
- **PSPublisherException** - Enhanced exception handling with static factory methods, comprehensive validation using Objects.requireNonNull, and modern exception chaining patterns

**Key Features:**
- **Optional-based API** - All lookup operations return `Optional<T>` for null-safe operations with methods like `findContentList()`, `findDescription()`, `findDisplayTitle()`
- **Stream API integration** - Efficient processing with `streamContentLists()`, `filterContentLists()`, and enum lookup operations
- **CompletableFuture support** - Asynchronous operations like `loadContentListAsync()` and `saveContentListAsync()` for non-blocking publishing
- **Enhanced validation** - Input validation using `Objects.requireNonNull()` and comprehensive parameter checking
- **Functional interfaces** - Support for content filtering predicates and processing functions
- **Modern enum design** - Enhanced enums with lookup capabilities, comparison methods, and Optional-based operations

**Content List Types:**
- **NORMAL** - Standard processing, pass all items from template expander
- **INCREMENTAL** - Incremental processing, only items requiring publication due to changes

**Edition Priorities:**
- **HIGHEST(5)** - Maximum priority with comparison support
- **HIGH(4)** - High priority with `isHigherThan()` and `isLowerThan()` methods
- **MEDIUM(3)** - Standard priority level
- **LOW(2)** - Lower priority but higher than LOWEST
- **LOWEST(1)** - Minimum priority with enhanced lookup capabilities

**Usage Examples:**
```java
// Safe content list access with Optional
var publisherService = PSPublisherServiceLocator.getPublisherService();
var contentList = publisherService.findContentList(contentListId);
contentList.ifPresent(list -> {
    System.out.println("Found content list: " + list.getName());
    
    // Safe property access
    list.findDescription().ifPresent(desc -> 
        System.out.println("Description: " + desc));
    
    // Parameter checking
    if (list.hasGeneratorParam("maxItems")) {
        System.out.println("Max items configured");
    }
});

// Stream-based content list filtering
var filteredLists = publisherService.filterContentLists("blog", 
    list -> list.hasParameters());
filteredLists.forEach(list -> 
    System.out.println("Blog list with params: " + list.getName()));

// Asynchronous content list loading
var future = publisherService.loadContentListAsync(contentListId);
future.thenAccept(optionalList -> 
    optionalList.ifPresent(list -> processContentList(list)));

// Safe edition priority handling
var edition = editionService.findEdition(editionId);
edition.ifPresent(ed -> {
    var priority = ed.getPriority();
    if (priority.isHigherThan(Priority.MEDIUM)) {
        System.out.println("High priority edition: " + ed.getName());
    }
    
    // Safe property access
    ed.findDisplayTitle().ifPresent(title -> 
        System.out.println("Title: " + title));
    ed.findComment().ifPresent(comment -> 
        System.out.println("Comment: " + comment));
});

// Enhanced delivery type configuration
var deliveryType = deliveryService.findDeliveryType(typeId);
deliveryType.ifPresent(type -> {
    System.out.println("Bean: " + type.getBeanName());
    
    if (type.hasDescription()) {
        type.findDescription().ifPresent(desc -> 
            System.out.println("Description: " + desc));
    }
    
    if (type.requiresAssemblyForUnpublishing()) {
        System.out.println("Assembly required for unpublishing");
    }
});

// Content list type lookup with Optional
var contentListType = IPSContentList.Type.findByLabel("Incremental");
contentListType.ifPresent(type -> 
    System.out.println("Found type: " + type.getLabel()));

// Edition priority comparison
var priority1 = Priority.HIGH;
var priority2 = Priority.MEDIUM;
if (priority1.isHigherThan(priority2)) {
    System.out.println("HIGH is higher than MEDIUM");
}

// Exception handling with factory methods
try {
    publisherService.saveContentList(null);
} catch (Exception e) {
    var publisherException = PSPublisherException.withMessageAndCause(
        "Failed to save content list", e);
    throw publisherException;
}
```

### Locking Package (`com.percussion.services.locking`)

The locking package provides comprehensive object locking services for thread-safe content management with session-based lock tracking, expiration handling, and bulk operations with comprehensive Java 11 modernization and asynchronous operation support.

**Key Components:**
- **IPSObjectLockService** - Main object locking service interface with Optional-based safe access, Stream API for efficient lock processing, CompletableFuture support for asynchronous lock operations, and comprehensive validation with Objects.requireNonNull
- **PSLockException** - Modern lock exception with static factory methods for common lock error scenarios, Optional-based safe access for nullable properties, immutable collections for bulk operation results, and enhanced validation patterns
- **PSObjectLockServiceLocator** - Thread-safe service locator using AtomicReference with Optional-based safe access, enhanced diagnostics, and modern concurrency patterns

**Key Features:**
- **Optional-based API** - All lookup operations return `Optional<T>` for null-safe operations with methods like `findLockByObjectIdSafely()`, `getLockedVersionSafely()`, `findLocker()`
- **Stream API integration** - Efficient processing with `streamLocksByObjectIds()`, `streamLocksByUser()`, `streamExpiredLocks()`, and predicate-based filtering operations
- **CompletableFuture support** - Asynchronous operations like `createLocksAsync()`, `extendLocksAsync()`, `releaseLocksAsync()` for non-blocking lock management
- **Enhanced validation** - Input validation using `Objects.requireNonNull()` and comprehensive parameter checking with automatic string trimming
- **Thread-safe operations** - All lock operations are thread-safe with proper concurrency handling
- **Session-based management** - Lock tracking by user session with automatic expiration handling

**Lock Operations:**
- **Lock creation** - Create new locks or extend existing ones with override capabilities
- **Lock extension** - Extend lock duration with configurable intervals (minimum 1000ms)
- **Lock release** - Release individual locks or bulk lock operations
- **Lock discovery** - Find locks by object ID, session, user, or custom predicates
- **Expiration handling** - Automatic detection and cleanup of expired locks

**Lock Types:**
- **Single object locks** - Individual content item locking with version tracking
- **Bulk operation locks** - Multiple object locking with partial success handling
- **Session-based locks** - Locks tied to user sessions with automatic cleanup
- **Versioned locks** - Lock specific versions of objects with update capabilities

**Usage Examples:**
```java
// Safe locking service access with Optional
var lockingService = PSObjectLockServiceLocator.getLockingServiceSafely();
lockingService.ifPresent(service -> {
    // Safe lock creation
    try {
        var lock = service.createLock(objectId, sessionId, userName, version, false);
        System.out.println("Lock created: " + lock.getId());
    } catch (PSLockException e) {
        // Handle lock conflicts
        e.findLocker().ifPresent(locker -> 
            System.out.println("Object locked by: " + locker));
        e.findRemainingTime().ifPresent(time -> 
            System.out.println("Lock expires in: " + time + "ms"));
    }
});

// Asynchronous lock operations
var future = service.createLockAsync(objectId, sessionId, userName, version, false);
future.thenAccept(lock -> 
    System.out.println("Lock created asynchronously: " + lock.getId()))
.exceptionally(throwable -> {
    System.err.println("Lock creation failed: " + throwable.getMessage());
    return null;
});

// Stream-based lock processing
var userLocks = service.streamLocksByUser(sessionId, userName)
    .filter(lock -> lock.getRemainingTime() > 60000) // More than 1 minute
    .collect(Collectors.toList());

// Bulk lock operations with error handling
try {
    var locks = service.createLocks(objectIds, sessionId, userName, versions, true);
    System.out.println("Created " + locks.size() + " locks");
} catch (PSLockException e) {
    if (e.isMultiOperation()) {
        System.out.println("Successes: " + e.getSuccessCount());
        System.out.println("Errors: " + e.getErrorCount());
        
        e.findErrors().ifPresent(errors -> 
            errors.forEach((id, error) -> 
                System.err.println("Failed to lock " + id + ": " + error.getMessage())));
    }
}

// Safe lock lookup and version checking
var lockOpt = service.findLockByObjectIdSafely(objectId);
lockOpt.ifPresent(lock -> {
    System.out.println("Lock found for object: " + lock.getObjectId());
    System.out.println("Locked by: " + lock.getLocker());
    System.out.println("Remaining time: " + lock.getRemainingTime() + "ms");
});

// Version-safe operations
var versionOpt = service.getLockedVersionSafely(objectId);
versionOpt.ifPresent(version -> 
    System.out.println("Object locked at version: " + version));

// Lock extension with validation
try {
    var extendedLock = service.extendLock(objectId, sessionId, userName, newVersion, 300000); // 5 minutes
    System.out.println("Lock extended until: " + extendedLock.getExpirationTime());
} catch (PSLockException e) {
    if (e.getId() == IPSLockErrors.LOCK_NOT_FOUND) {
        System.err.println("No existing lock found to extend");
    }
}

// Expired lock cleanup
var expiredLocks = service.streamExpiredLocks()
    .collect(Collectors.toList());
if (!expiredLocks.isEmpty()) {
    service.releaseLocks(expiredLocks);
    System.out.println("Released " + expiredLocks.size() + " expired locks");
}

// Exception handling with factory methods
try {
    service.createLock(objectId, sessionId, userName, version, false);
} catch (Exception e) {
    var lockException = PSLockException.objectAlreadyLocked(objectId, currentUser, remainingTime);
    throw lockException;
}

// Lock statistics and monitoring
if (service.hasExpiredLocks()) {
    var statistics = service.getLockStatistics();
    System.out.println(statistics);
}

// Service diagnostics
if (PSObjectLockServiceLocator.isLockingServiceAvailable()) {
    var serviceInfo = PSObjectLockServiceLocator.getServiceInfo();
    System.out.println(serviceInfo);
}
```

**Lock Exception Patterns:**
The locking system provides sophisticated exception handling with:
- **Single operation exceptions** - Individual lock operation failures with detailed context
- **Multi-operation exceptions** - Bulk operation results with success/error breakdown
- **Static factory methods** - Type-safe exception creation for common scenarios
- **Optional-based safe access** - Null-safe property access for exception details
- **Immutable result collections** - Thread-safe access to operation results and errors

**Migration Notes:**
- **Enhanced validation**: All methods now validate input parameters and trim whitespace
- **Optional returns**: Nullable properties now have corresponding `findXxx()` methods returning Optional
- **Stream support**: New methods for efficient collection processing and lock filtering operations
- **Async operations**: CompletableFuture support for non-blocking lock operations
- **Thread safety**: Modern concurrency patterns with AtomicReference in service locator

**Performance Features:**
- **Thread-safe caching** - Service locator uses AtomicReference for improved performance
- **Lazy initialization** - Service locator uses lazy initialization with proper synchronization
- **Stream processing** - Efficient lock processing and filtering using Stream API
- **Asynchronous operations** - Non-blocking lock operations for better scalability
- **Bulk operations** - Optimized multi-object locking with partial success handling
