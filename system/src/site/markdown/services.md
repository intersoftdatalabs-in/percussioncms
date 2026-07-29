# Services Architecture

The system module implements a comprehensive service-oriented architecture providing pluggable, thread-safe access to critical CMS functionality.

## Architecture Overview

### Service Layer Pattern

All services follow a consistent design pattern:

```
┌─────────────────────────────────────────────┐
│  Client Code                                │
│  var service = PSXxxServiceLocator.getXxx() │
└────────────────────┬────────────────────────┘
                     │ uses
┌────────────────────▼────────────────────────┐
│  Service Locator (Thread-Safe)              │
│  PSXxxServiceLocator                        │
│  - Lazy initialization via AtomicReference  │
│  - Thread-safe getInstance()                │
│  - Synchronized initialization              │
└────────────────────┬────────────────────────┘
                     │ returns
┌────────────────────▼────────────────────────┐
│  Service Interface                          │
│  IPSXxxService                              │
│  - Defines public contract                  │
│  - Throws service-specific exceptions       │
│  - May return Optional<T> for safety        │
└────────────────────┬────────────────────────┘
                     │ implements
┌────────────────────▼────────────────────────┐
│  Service Implementation                     │
│  PSXxxService (or similar)                  │
│  - Actual business logic                    │
│  - Data access (Hibernate, JDBC)            │
│  - Service-to-service calls via locators    │
└─────────────────────────────────────────────┘
```

### Service Locator Implementation

**Pattern:**

```java
public final class PSXxxServiceLocator {
    private static final AtomicReference<IPSXxxService> SERVICE =
        new AtomicReference<>();

    public static IPSXxxService getXxxService() {
        IPSXxxService svc = SERVICE.get();
        if (svc == null) {
            synchronized (PSXxxServiceLocator.class) {
                svc = SERVICE.get();
                if (svc == null) {
                    svc = new PSXxxService();
                    SERVICE.set(svc);
                }
            }
        }
        return svc;
    }
}
```

**Benefits:**

- **Thread-safe** – Lazy initialization without explicit locking
- **Pluggable** – Can be overridden for testing (though not typically done)
- **Consistent** – Same pattern across all services
- **Minimal overhead** – Only one AtomicReference per service

### Service Exception Hierarchy

Services define domain-specific exception hierarchies:

```java
public class PSXxxException extends PSServiceException {
    // Service-specific error handling
}
```

**Key Points:**

- Always catch **specific exceptions** (not generic Exception)
- Use service-specific exceptions for better error context
- Log exceptions appropriately (DEBUG, INFO, WARN, ERROR levels)

## Core Services

### 1. Catalog Service

**Package:** `com.percussion.services.catalog`

**Purpose:** Discover and enumerate CMS objects; manage type information

**Key Interfaces:**

- `IPSCataloger` – Main interface for object discovery
- `IPSCatalogSummary` – Summary information for cataloged objects
- `IPSCatalogItem` – Full object details and permissions

**Key Methods:**

```java
IPSCataloger cat = PSCatalogLocator.getCataloger();

// Find objects by type
var items = cat.find(PSDbComponent.getComponentType(PSItemDefinition.class));

// Get object summary
Optional<IPSCatalogSummary> summary = cat.getSummary(componentId);

// List all objects of a type
var allItems = cat.getObjects(type)
    .stream()
    .filter(item -> item.getPermissions().canUpdate())
    .collect(Collectors.toList());
```

**Usage Scenarios:**

- Enumerate all content types
- Find items by ID or name
- Check object permissions and metadata
- Validate object existence before operations

### 2. Assembly Service

**Package:** `com.percussion.services.assembly`

**Purpose:** Process templates, bind content to templates, render output

**Key Interfaces:**

- `IPSAssemblyService` – Main interface
- `IPSAssemblyItem` – Item for assembly
- `PSAssemblyTemplate` – Template definition

**Key Methods:**

```java
IPSAssemblyService svc = PSAssemblyServiceLocator.getAssemblyService();

// Assemble content with template
var result = svc.assemble(item, template, context);

// Get available templates for an item
var templates = svc.getTemplates(item)
    .stream()
    .filter(t -> t.isActive())
    .collect(Collectors.toList());

// Render to specific format
var html = svc.assemble(item, htmlTemplate);
var xml = svc.assemble(item, xmlTemplate);
```

**Usage Scenarios:**

- Render content to HTML, XML, PDF (via appropriate template)
- List available templates for an item
- Assemble complete pages with headers, footers, sidebars
- Process content through template variable binding

### 3. Content Service

**Package:** `com.percussion.services.content`

**Purpose:** Manage content-related metadata (keywords, translations, folders)

**Key Interfaces:**

- `IPSContentService` – Main interface
- `PSKeyword` – Keyword definition
- `PSFolderProperty` – Folder metadata

**Key Methods:**

```java
IPSContentService svc = PSContentServiceLocator.getContentService();

// Manage keywords
List<PSKeyword> keywords = svc.findKeywords("tag1", "tag2");
svc.saveKeyword(newKeyword);

// Handle folders
Optional<PSFolderProperty> folder = svc.getFolderProperty(folderId);
svc.updateFolderProperty(property);

// Auto-translations
var translations = svc.getAutoTranslations(contentId);
```

**Usage Scenarios:**

- Manage content tags and keywords
- Update folder metadata
- Handle content translations
- Query content by metadata criteria

### 4. Content Manager Service

**Package:** `com.percussion.services.contentmgr`

**Purpose:** High-level content operations (read, write, delete)

**Key Interfaces:**

- `IPSContentMgr` – Main interface

**Key Methods:**

```java
IPSContentMgr mgr = PSContentMgrLocator.getContentMgr();

// Load content
Optional<PSItemSummary> item = mgr.findItemSummaries(contentId).stream().findFirst();

// Create content
var newItem = mgr.createItem(contentTypeId);
mgr.saveItem(newItem);

// Update content
item.ifPresent(i -> {
    i.setField("title", "New Title");
    mgr.saveItem(i);
});

// Delete content
mgr.deleteItem(contentId);
```

**Usage Scenarios:**

- Create new content items
- Load and modify existing items
- Delete content with workflow integration
- Query items by status, type, or other criteria

### 5. GUID Manager Service

**Package:** `com.percussion.services.guidmgr`

**Purpose:** Allocate and validate Percussion GUIDs (content, component, design)

**Key Interfaces:**

- `IPSGuidManager` – Main interface
- `PSGuid` – GUID representation
- `PSLegacyGuid` – Legacy GUID format

**Key Methods:**

```java
IPSGuidManager mgr = PSGuidManagerLocator.getGuidManager();

// Create GUID for content
var contentGuid = mgr.createGuid(PSGuid.GUID_TYPE_CONTENT);

// Allocate multiple GUIDs
List<PSGuid> guids = mgr.allocateGuids(10);

// Parse GUID from string
Optional<PSGuid> guid = PSGuidUtils.parseGuid("12345-6789");

// Validate GUID
if (guid.isPresent() && guid.get().isValid()) {
    // ...
}
```

**Usage Scenarios:**

- Generate unique IDs for new content items
- Parse GUIDs from user input
- Validate GUID format before database operations
- Convert between GUID formats (legacy, current)

### 6. Data Service

**Package:** `com.percussion.services.data`

**Purpose:** Generic data access (legacy pattern, often superseded by Hibernate)

**Key Interfaces:**

- `IPSDataService` – Generic data interface

**Note:** Most new code uses Hibernate directly instead of this service.

### 7. Error Service

**Package:** `com.percussion.services.error`

**Purpose:** Centralized error code and message management

**Key Interfaces:**

- `IPSErrorService` – Error code and message lookup

**Key Methods:**

```java
IPSErrorService svc = PSErrorServiceLocator.getErrorService();

// Look up error message
String msg = svc.getErrorMessage(errorCode);

// Get all errors by code prefix
var errors = svc.getErrors("4000")
    .stream()
    .collect(Collectors.toList());
```

**Usage Scenarios:**

- Get user-friendly error messages from error codes
- Query error codes for a specific subsystem
- Format error messages with parameters

### 8. Change Tracking Service

**Package:** `com.percussion.services.contentchange`

**Purpose:** Notify listeners of content changes (create, update, delete)

**Key Interfaces:**

- `IPSContentChangeService` – Event notification interface
- `PSContentChangeEvent` – Change event details

**Key Methods:**

```java
IPSContentChangeService svc = PSContentChangeServiceLocator.getChangeService();

// Register for change notifications
svc.addListener(contentId, changeListener);

// Find changes by date range
var recentChanges = svc.getChanges(startDate, endDate)
    .stream()
    .filter(e -> e.getChangeType() == ChangeType.CONTENT_UPDATED)
    .collect(Collectors.toList());
```

**Usage Scenarios:**

- Invalidate caches when content changes
- Trigger re-indexing on content updates
- Audit content modifications
- Update dependent systems on content changes

### 9. General/System Info Service

**Package:** `com.percussion.services.general`

**Purpose:** Access system information, configuration, and metadata

**Key Interfaces:**

- `IPSRhythmyxInfo` – System info interface

**Key Methods:**

```java
IPSRhythmyxInfo info = PSRhythmyxInfoLocator.getRhythmyxInfo();

// Get component info
Optional<PSComponentSummary> summary = info.getComponent(componentId);

// Get system properties
Optional<String> version = info.getProperty("system.version");

// List available components
var components = info.getComponents()
    .stream()
    .filter(c -> c.isPublished())
    .collect(Collectors.toList());
```

**Usage Scenarios:**

- Retrieve system configuration at runtime
- Check component metadata
- Access version and deployment information
- Query available components and their properties

## Service-to-Service Communication

Services may depend on other services. Follow these guidelines:

### Within a Service

```java
public class PSXxxService implements IPSXxxService {
    public void processItem(PSItem item) {
        // Call another service via locator
        IPSContentService contentSvc =
            PSContentServiceLocator.getContentService();

        var keywords = contentSvc.findKeywords(item.getKeywords());
        // Use keywords...
    }
}
```

### Important Rules

1. **Use locators**, not constructor injection – Services are singletons initialized lazily
2. **Handle service exceptions** – Each service may throw domain-specific exceptions
3. **Chain locator calls carefully** – Avoid circular dependencies between services
4. **Log carefully** – Don't leak sensitive information in error logs
5. **Test with mocks** – Mock service dependencies in unit tests (though not a primary pattern)

## Service Initialization & Lifecycle

### Initialization

- Services are lazily initialized on first access
- Locators use double-checked locking for thread safety
- Constructor may be called multiple times during testing, so avoid expensive operations
- No guaranteed initialization order between services

### Lifecycle

Services typically:

1. **Initialize** on first locator call
2. **Load configuration** from database or files
3. **Cache data** in memory or via Hibernate session caches
4. **Remain active** for the lifetime of the JVM

No explicit shutdown mechanism is typically provided.

### Testing

For unit tests:

```java
@Before
public void setup() {
    // Optionally inject mock service (not the standard pattern)
    // But more typically, mock the lower-level dependencies
}

@Test
public void testServiceBehavior() {
    // Use real service locator in tests
    IPSXxxService svc = PSXxxServiceLocator.getXxxService();
    // Test service behavior
}
```

## Exception Handling

### Service Exception Hierarchy

```
Throwable
└── Exception
    └── RuntimeException (typically)
        └── PSServiceException
            ├── PSContentException
            ├── PSCatalogException
            ├── PSAssemblyException
            ├── PSGuidException
            └── [other domain exceptions]
```

### Best Practices

```java
try {
    var result = service.someOperation();
} catch (PSContentException e) {
    // Handle content-specific error
    log.error("Content operation failed", e);
    // User-friendly error message
} catch (PSServiceException e) {
    // Handle general service error (fallback)
    log.error("Service error", e);
}
```

## Performance Considerations

### Caching

- Services typically cache configuration and metadata
- Use Hibernate's second-level cache for frequently accessed objects
- Be aware of cache invalidation when modifying shared data

### Batch Operations

```java
IPSContentMgr mgr = PSContentMgrLocator.getContentMgr();

// Good: batch multiple operations
List<PSItem> items = mgr.findItems(predicate);
for (var item : items) {
    item.setField("status", "Active");
    mgr.saveItem(item);  // Each save is a database round-trip
}

// Better: use bulk operations if available
mgr.bulkUpdate(items);
```

### Transaction Management

- Services typically manage transactions at method boundaries
- Long-running operations may tie up database connections
- Consider pagination for large result sets

## Documentation References

- [Package Reference](packages.html) – Detailed package documentation
- [Building & Development](building.html) – Build and test guidelines
- [Modernization Status](modernization.html) – JDK 21 baseline and modernization notes

---

**Module Version:** 8.2.0-SNAPSHOT | **Last Updated:** March 2026
