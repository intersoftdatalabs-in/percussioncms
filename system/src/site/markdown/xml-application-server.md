# XML Application Server (XAS)

## Overview

The **XML Application Server (XAS)** is the foundational content type engine of Percussion CMS. Unlike traditional relational database schemas, XAS provides a declarative, XML-based system for defining custom content types, fields, workflows, and business rules without requiring code changes or application redeployment.

XAS has been the backbone of Percussion since its Rhythmyx days and remains the core technology for content management operations in the current platform.

## Strategic Role

### Content Type Definition

XAS allows administrators to define unlimited custom content types through XML configuration rather than database schema modifications. Each content type describes:

- Field definitions (name, type, constraints, relationships)
- Display configurations (editors, formatting rules)
- Workflow states and transitions
- Access control rules
- Validation rules

### Separation of Concerns

XAS enforces a clean separation between:

- **Structure Definition** (XML configuration)
- **Data Storage** (relational database)
- **Processing Logic** (Java services)

This separation enables administrators to modify content types without involving developers (except for complex business logic).

### System Foundation

XAS integration touches:

- **PSTypeConfiguration** – Loads and caches content type definitions
- **PSContentMgr** – CRUD operations for content items
- **PSWorkflowMgr** – Workflow state transitions
- **PSRelationshipMgr** – Content relationships and references
- **PSAclMgr** – Security enforcement

## Historical Context: E2 - The Original XML Application Server

The XML Application Server was historically known as **E2** in the Rhythmyx platform (1999-present). The `PSServer` class is the original entry point into this architecture, documented as "the main E2 server application." While modernized for current Java versions and cloud deployment, the fundamental E2 architecture remains the core of how XML applications are executed in Percussion CMS.

## Architecture Overview

### E2 Request Handling Pipeline

The E2 architecture implements a sophisticated multi-layer request routing and processing pipeline:

```text
HTTP Request
    │
    ├─ Security Filter (PSSecurityFilter)
    │   └─ Creates PSRequest and stores in thread-local
    │
    ├─ PSAppServlet.service()
    │   └─ HTTP request entry point
    │
    ├─ PSServer.getRequestHandler()
    │   ├─ Extract application name from URL
    │   ├─ Look up PSApplicationHandler
    │   └─ Get data set-specific handler
    │
    ├─ PSApplicationHandler.getRequestHandler()
    │   ├─ Parse request page name
    │   ├─ Find matching data set
    │   └─ Return appropriate handler (Query/Update/Delete)
    │
    ├─ Handler.processRequest()
    │   ├─ Execute query/update
    │   ├─ Build result XML
    │   └─ Store in PSResponse
    │
    └─ PSAppServlet outputs response
        ├─ Set status code
        ├─ Output headers
        └─ Stream XML body
```

### Layer 1: Servlet Entry Point (PSAppServlet)

**Location:** `system/src/main/java/com/percussion/servlets/PSAppServlet.java`

**Role:** HTTP request entry point and response handler.

The PSAppServlet intercepts all HTTP requests and delegates to the appropriate request handler. It:

- Implements `jakarta.servlet.http.HttpServlet`
- Retrieves `PSRequest` from thread-local storage (set by security filter)
- Parses request body
- Routes to appropriate handler via `PSServer.getRequestHandler()`
- Manages response output and headers
- Handles client disconnects and error conditions

### Layer 2: Request Object (PSRequest)

**Location:** `system/src/main/java/com/percussion/server/PSRequest.java`

**Role:** Wraps HTTP request data and provides unified access to request context.

A PSRequest object wraps the HTTP request and provides:

- **Application Name** (`m_appName`) – Extracted from request URL path
- **Request Page** (`m_requestPage`) – Resource name within application
- **Parameters** (`m_params`) – Query string and form data
- **Input Document** (`m_inputDocument`) – Parsed XML body (if applicable)
- **Security Context** – User session, authentication token
- **Response Object** (`m_response`) – Associated PSResponse for output

**Request Path Parsing:**

```text
URL: http://localhost:9992/Rhythmyx/MyApp/getArticles.xml?id=123
     
Breakdown:
  /Rhythmyx/           = webapp context
  MyApp/               = appName
  getArticles.xml      = requestPage (becomes "getArticles" after parsing)
  ?id=123              = parameters
```

### Layer 3: Request Handler Routing (PSServer)

**Location:** `system/src/main/java/com/percussion/server/PSServer.java`

**Role:** Main server singleton; manages applications and routes requests to handlers.

PSServer implements the global singleton that manages the E2 architecture:

- **Maintains Application Registry** – Map of `PSApplicationHandler` by application name
- **Routes Requests** – Routes PSRequest to appropriate handler based on app and page name
- **Manages Virtual Directories** – Maps logical application paths to physical file locations (via PSFileSystemDriver)
- **Coordinates Services** – Manages initialization and shutdown of core services
- **Tracks Statistics** – Monitors request queuing and performance

**Request Routing Logic:**

```java
// Extract application name from request
String appName = psreq.getAppName();

// Look up application handler
PSApplicationHandler handler = PSServer.getApplicationHandler(appName);

// Get handler for specific request page
IPSRequestHandler pageHandler = handler.getRequestHandler(psreq);

// Process request
pageHandler.processRequest(psreq);
```

### Layer 4: Application Handler (PSApplicationHandler)

**Location:** `system/src/main/java/com/percussion/server/PSApplicationHandler.java`

**Role:** Handles all requests for a specific XML application.

Each XML application has exactly one PSApplicationHandler that:

- **Manages Data Sets** – Maintains handlers for each data set definition
- **Enforces Access Control** – Checks ACLs before allowing requests
- **Manages Threading** – Enforces request queue limits and timeouts
- **Routes Data Set Requests** – Finds appropriate handler for request page
- **Tracks Statistics** – Monitors performance and queue depth

**Key Attributes:**

- `m_application` – PSApplication definition (name, ACLs, datasets)
- `m_dataHandlerMap` – Map of handlers by request page name
- `m_maxRequestsQueued` – Max concurrent requests allowed
- `m_maxRequestTime` – Request timeout in seconds

**Processing Steps:**

1. Check request queue (enforce max threads)
2. Find appropriate data set handler by request page
3. Validate request against request definition
4. Check ACL/access control
5. Execute request through handler
6. Post-process results
7. Update statistics

**Request Type Resolution:**

- **Query Handler** – For SELECT operations (returns data)
- **Update Handler** – For INSERT/UPDATE/DELETE operations
- **File Handler** – For static file requests
- **Content Editor Handler** – For special content editing requests

### Layer 5: Handler Interface (IPSRequestHandler)

**Location:** `system/src/main/java/com/percussion/server/IPSRequestHandler.java`

**Role:** Standard interface for all request handlers in the system.

All handlers implement this simple interface:

```java
public interface IPSRequestHandler {
    public void processRequest(PSRequest request);
    public void shutdown();
}
```

**Implementations:**

- `PSApplicationHandler` – Routes to data set handlers
- `PSDataHandler` – Executes XML data operations
- `PSQueryHandler` – Executes SELECT queries
- `PSUpdateHandler` – Executes INSERT/UPDATE/DELETE
- `PSFileRequestHandler` – Handles static file requests
- Custom extension handlers – Application-specific logic

## File System Integration

### PSFileSystemDriver and Virtual Directories

**Location:** `system/src/main/java/com/percussion/data/jdbc/PSFileSystemDriver.java`

The file system driver provides JDBC-based file system access for XML applications using a virtual directory abstraction:

- **Virtual Directory Mapping** – Maps application names to physical directories
- **Security Integration** – Validates user permissions at file system level
- **Dynamic Mapping** – Supports runtime directory registration and renaming
- **JDBC Interface** – Implements standard JDBC connection interface

**Virtual Directory Interface:**

```java
public interface IPSVirtualDirectory {
    public File getPhysicalPath(File relPath);
    public File getPhysicalLocation();
    public boolean hasPermissions(PSUserSession session, int permissions);
    public String getVirtualDirectory();
}
```

This abstraction decouples logical application paths from physical file locations, enabling flexible deployment and dynamic application discovery.

## Content Type System

### Content Type Definition (XML)

Content types are defined in XML configuration files, typically located in:

```text
system/config/ContentTypes/*.xml
```

A content type definition includes all metadata needed to manage content:

```xml
<PSXContentType id="100" name="Article">
  <PSXDataTypeSet>
    <PSXDataType id="1" name="title" type="text">
      <validation required="true" maxLength="255"/>
    </PSXDataType>
    <PSXDataType id="2" name="body" type="richtext">
      <validation required="true"/>
    </PSXDataType>
    <PSXDataType id="3" name="author" type="reference" refType="AuthorType"/>
  </PSXDataTypeSet>
  <PSXWorkflowAssociation>
    <PSXWorkflow id="EditorialWorkflow"/>
  </PSXWorkflowAssociation>
</PSXContentType>
```

### Type Configuration Service (PSTypeConfiguration)

Loads, parses, and caches content type definitions at startup:

- Reads XML configuration files
- Builds in-memory type catalogs
- Generates dynamic classes for content items (via **PSBeanGenerator** using ByteBuddy)
- Provides lookup services for content type metadata

**Key Methods:**

- `getContentType(int typeId)` – Retrieve type metadata
- `getContentTypeTemplates()` – List available templates
- `findObjectTypeId(String name)` – Lookup by name
- `getFieldCategories()` – Retrieve field metadata

### Dynamic Class Generation

XAS generates Java classes at runtime for each content type:

- Each content type maps to a dynamically generated bean class
- Properties correspond to defined fields
- Provides type-safe access to content item data
- **Migration Note:** Originally used CGLib; now uses **ByteBuddy** for JDK 21 compatibility

### Persistence Layer

Content items are persisted in relational database tables:

- Base tables store common attributes (id, system, type)
- Type-specific tables store field data
- Relationships stored in junction tables
- Full-text indexing for search

## Configuration Structure

### Content Type Configuration Elements

**Content Type Declaration:**

- **id** – Unique numeric type identifier
- **name** – Human-readable type name
- **description** – Documentation
- **icon** – UI representation
- **workflow** – Associated workflow

**Field Definition:**

- **fieldName** – Unique within type
- **dataType** – text, numeric, richtext, reference, date, etc.
- **required** – Boolean constraint
- **unique** – Uniqueness constraint
- **maxLength** – String length limit
- **defaultValue** – Initial value
- **searchable** – Full-text indexable

**Workflow Integration:**

- Associates states and transitions
- Defines permissions per state
- Triggers actions on state changes

**Relationship Definition:**

- **source** – Content type with reference field
- **target** – Referenced content type
- **cardinality** – 1:1, 1:N, N:N
- **cascadeDelete** – Behavior on source deletion

### Example: Simple Content Type

```xml
<?xml version="1.0" encoding="UTF-8"?>
<PSXContentType id="301" name="BlogPost">
  <description>Blog post with comments and categories</description>

  <PSXDataTypeSet>
    <!-- Title field -->
    <PSXDataType id="1" name="title" type="text">
      <validation required="true" maxLength="200"/>
      <searchable>true</searchable>
    </PSXDataType>

    <!-- Publication date -->
    <PSXDataType id="2" name="publishDate" type="date">
      <validation required="true"/>
    </PSXDataType>

    <!-- Rich text body -->
    <PSXDataType id="3" name="content" type="richtext">
      <validation required="true"/>
      <searchable>true</searchable>
    </PSXDataType>

    <!-- Reference to Author -->
    <PSXDataType id="4" name="author" type="reference" refType="Author">
      <validation required="true"/>
    </PSXDataType>

    <!-- Multiple category references -->
    <PSXDataType id="5" name="categories" type="reference" refType="Category" cardinality="N:N">
      <validation required="true"/>
    </PSXDataType>

    <!-- Tags (comma-separated) -->
    <PSXDataType id="6" name="tags" type="text">
      <validation maxLength="500"/>
    </PSXDataType>
  </PSXDataTypeSet>

  <!-- Attach to Editorial Workflow -->
  <PSXWorkflowAssociation>
    <PSXWorkflow>EditorialWorkflow</PSXWorkflow>
  </PSXWorkflowAssociation>
</PSXContentType>
```

## Content Type Lifecycle

### 1. Registration

1. Administrator creates/modifies content type XML
2. File placed in `system/config/ContentTypes/`
3. CMS startup or hot-deploy reads configuration
4. **PSTypeConfiguration** processes definition
5. Dynamic class generated via ByteBuddy
6. Type registered in service catalog

### 2. Instance Creation

1. Content creator requests new item of type
2. **PSContentMgr.createItem()** invoked
3. Dynamic class instantiated
4. Form UI rendered from field metadata
5. Editor populates fields
6. Item submitted for workflow entry

### 3. Persistence

1. **PSContentMgr.save()** called
2. Dynamic object mapped to database by Hibernate
3. Type-specific data inserted
4. Full-text index updated
5. Workflow engine notified
6. Change events dispatched

### 4. Retrieval & Modification

1. **PSContentMgr.find()** locates item by ID or criteria
2. Hibernate reconstructs dynamic class instance
3. Field values loaded from database
4. Modifications applied
5. **PSContentMgr.save()** persists changes
6. Change events triggered

### 5. Workflow Progression

1. Item transitions between workflow states
2. State-specific permissions enforced
3. Actions triggered (notifications, approvals)
4. Status updated in database

## Integration with System Module

### Service Dependencies

The **system module** provides critical services that XAS depends on:

|         Service         |                 Package                 |                  Role                   |
|-------------------------|-----------------------------------------|-----------------------------------------|
| **PSTypeConfiguration** | `com.percussion.services.contentmgr`    | Load and cache content type definitions |
| **PSContentMgr**        | `com.percussion.services.contentmgr`    | CRUD operations for content items       |
| **PSWorkflowMgr**       | `com.percussion.utils.workflow`         | Manage workflow states and transitions  |
| **PSRelationshipMgr**   | `com.percussion.services.relationship`  | Manage content relationships            |
| **PSAclMgr**            | `com.percussion.services.security.data` | Enforce access control                  |
| **PSGuidMgr**           | `com.percussion.services.guidmgr`       | Generate unique identifiers             |
| **PSCatalog**           | `com.percussion.services.catalog`       | Catalog content objects                 |
| **PSJavaScript**        | `com.percussion.services.general`       | JavaScript execution environment        |

### Service Locator Pattern

XAS uses the **Service Locator Pattern** (implemented in system module) to access services:

```java
// Retrieve content type service
IPSTypeConfiguration typeSvc = PSServiceLocator.getManager(IPSTypeConfiguration.class);

// Load content type
PSContentType contentType = typeSvc.loadContentType(typeId);

// List field definitions
List<PSFieldDefinition> fields = contentType.getFieldDefinitions();
```

### Dynamic Class Integration

When new content types are registered:

1. **PSTypeConfiguration** detects configuration change
2. **PSBeanGenerator** (ByteBuddy-based) creates dynamic class
3. Dynamic class is registered in type cache
4. **PSContentMgr** uses dynamic class for serialization/deserialization
5. ORM framework (Hibernate) maps instances to tables

## Key Technical Details

### Dynamic Class Generation (ByteBuddy)

Content types that are defined via XAS require dynamic Java class generation at runtime. The **PSBeanGenerator** class handles this:

**Before (CGLib - JDK 8 compatible):**

```java
BeanGenerator gen = new BeanGenerator();
gen.setNamingPolicy(new TypeNamingPolicy());
gen.setSuperclass(Object.class);
gen.addProperty("title", String.class);
gen.addProperty("body", String.class);
Class<?> clazz = gen.create();
```

**After (ByteBuddy - JDK 21 compatible):**

```java
PSBeanGenerator gen = new PSBeanGenerator();
gen.setSuperclass(Object.class);
gen.setClassName("ContentType_100_ArticleType");
gen.addProperty("title", String.class);
gen.addProperty("body", String.class);
Class<?> clazz = gen.createClass();
```

### JDK 21 Compatibility

- CGLib's reflective approach blocked by JDK 21 module system
- ByteBuddy's `INJECTION` strategy uses `Unsafe.defineClass()` which bypasses module restrictions
- All class generation runs with Java's default class loading strategy

### Type Caching

- Content type definitions cached in memory at startup
- Reduces XML parsing overhead
- Hot-reload support via service restart
- Thread-safe implementation using synchronized maps

## Field Types

|     Type      |  Database   |         Use Case          |               Example                |
|---------------|-------------|---------------------------|--------------------------------------|
| **text**      | VARCHAR     | Short text, titles, names | Product name, article title          |
| **richtext**  | LONGTEXT    | Formatted HTML content    | Article body, product description    |
| **numeric**   | DECIMAL/INT | Numbers, quantities       | Price, quantity, rating              |
| **date**      | TIMESTAMP   | Calendar dates            | Publication date, expiry date        |
| **choice**    | VARCHAR     | Enumerated values         | Status (draft/published), category   |
| **reference** | INT/GUID    | Link to other content     | Author reference, category link      |
| **image**     | GUID        | Binary media              | Product photo, featured image        |
| **file**      | GUID        | Binary documents          | PDF attachment, downloadable content |
| **boolean**   | TINYINT     | True/false values         | Featured, published flags            |

## Workflow Integration

### Workflow Association

Content types are associated with workflows that define:

- **States**: draft, review, approved, published, archived
- **Transitions**: Rules for moving between states
- **Actions**: Notifications, approvals, scheduled publishing
- **Permissions**: Who can perform each transition

### Workflow Example

```text
draft → review → approved → published → archived
  ↓        ↓         ↓
pending   pending   pending
approval  approval  approval
```

### Workflow Enforcement

1. User submits content to workflow
2. **PSWorkflowMgr** validates transition
3. Required actions executed (notifications)
4. State updated in database
5. New state permissions applied
6. Content may be hidden/visible based on state

## Security & Access Control

### ACL Integration

- Content types have type-level permissions
- Individual content items may have item-level ACLs
- Workflow states have state-level permissions
- Users or groups granted permissions per state

### Example

```text
ArticleType {
  Draft state: Author + Editor can view/edit
  Review state: Editor + Manager can view/edit
  Published state: Everyone can view, only Admin can edit
}
```

## Performance Considerations

### Caching Strategy

- Content type definitions cached at startup
- Dynamic class cache maintains generated classes
- Metadata caching reduces repeated lookups
- Cache invalidation on configuration changes

### Query Optimization

- Type-specific queries indexed
- Relationship queries use junction table indexes
- Full-text search indexes maintained
- N+1 query prevention via eager loading

### Scalability

- XAS supports thousands of content types
- Each dynamic class instance is lightweight
- Database connections pooled
- Content items distributed across sharded tables (in clustered deployments)

## Troubleshooting

### Common Issues

**Content Type Not Loading:**

- **Symptom:** Type not found in catalog after startup
- **Diagnosis:** Check `/system/config/ContentTypes/` for XML file
- **Diagnosis:** Verify XML syntax validity
- **Diagnosis:** Check server logs for parsing errors
- **Diagnosis:** Verify type ID uniqueness
- **Solution:** Restart CMS to reload type configurations

**Dynamic Class Generation Failures (JDK 21):**

- **Symptom:** `NoClassDefFoundError` for dynamically generated types
- **Root Cause:** Old CGLib-based code trying to use reflective class loading
- **Solution:** Ensure PSBeanGenerator (ByteBuddy-based) is in use

**Workflow State Locked:**

- **Symptom:** Cannot transition content between states
- **Diagnosis:** Check workflow definition for transition rules
- **Diagnosis:** Verify user has permission for this state
- **Diagnosis:** Check required actions (approvals) are satisfied
- **Solution:** Use PSWorkflowMgr to verify transitions

## Migration & Future Direction

### Historical Evolution

1. **Rhythmyx Era (1999-2010):** Original CGLib-based type engine
2. **CM System (2010-2015):** XML-driven type definitions introduced
3. **CM1 (2015-2020):** ORM migration to Hibernate
4. **8.0+ (2020-present):** Modernization for cloud/containers

### JDK 21 Modernization

- Replaced CGLib with ByteBuddy for class generation
- Maintained full backward compatibility with existing type definitions
- No changes required to existing content types
- All dynamic class generation now JDK 21 compatible

### Future Enhancements

- Potential GraphQL schema auto-generation from content types
- Enhanced metadata vocabulary for AI/ML integration
- Event-driven architecture for type changes
- Multi-tenant type isolation

## Related Documentation

- [Services Architecture](services.html) – Content management services details
- [Building & Development](building.html) – Development environment setup
- [Request Handling Architecture](request-handling-flow.html) – E2 request pipeline details
- [Modernization Status](modernization.html) – JDK 21 compatibility updates
- [PSTypeConfiguration Source](../../services/src/main/java/com/percussion/services/contentmgr/PSTypeConfiguration.java) – Implementation details
- [PSContentMgr Source](../../services/src/main/java/com/percussion/services/contentmgr/PSContentMgr.java) – Content operations

## Key Takeaways

- **XAS is the heart of Percussion CMS** – All content operations flow through it
- **E2 Architecture remains core** – Request handling pipeline is proven, decades-old technology
- **XML-based declarations** – No code changes needed to add/modify content types
- **Dynamic class generation** – Provides type-safe Java access to content
- **Workflow integration** – Built-in state management and business rules
- **Multiple field types** – Covers text, media, relationships, structured data
- **Service locator pattern** – Clean abstraction for accessing core services
- **JDK 21 ready** – ByteBuddy-based class generation handles modern Java restrictions
- **Legacy stability** – Backward compatible with all existing content type definitions

```text
HTTP Request
    │
    ├─ Security Filter (PSSecurityFilter)
    │   └─ Creates PSRequest and stores in thread-local
    │
    ├─ PSAppServlet.service()
    │   └─ HTTP request entry point
    │
    ├─ PSServer.getRequestHandler()
    │   ├─ Extract application name from URL
    │   ├─ Look up PSApplicationHandler
    │   └─ Get data set-specific handler
    │
    ├─ PSApplicationHandler.getRequestHandler()
    │   ├─ Parse request page name
    │   ├─ Find matching data set
    │   └─ Return appropriate handler (Query/Update/Delete)
    │
    ├─ Handler.processRequest()
    │   ├─ Execute query/update
    │   ├─ Build result XML
    │   └─ Store in PSResponse
    │
    └─ PSAppServlet outputs response
        ├─ Set status code
        ├─ Output headers
        └─ Stream XML body
```

### Layer 1: Servlet Entry Point (PSAppServlet)

**Location:** `system/src/main/java/com/percussion/servlets/PSAppServlet.java`

**Role:** HTTP request entry point and response handler.

The PSAppServlet intercepts all HTTP requests and delegates to the appropriate request handler. It:

- Implements `jakarta.servlet.http.HttpServlet`
- Retrieves `PSRequest` from thread-local storage (set by security filter)
- Parses request body
- Routes to appropriate handler via `PSServer.getRequestHandler()`
- Manages response output and headers
- Handles client disconnects and error conditions

**Key Methods:**

- `service(HttpServletRequest, HttpServletResponse)` – Main request entry point

### Layer 2: Request Object (PSRequest)

**Location:** `system/src/main/java/com/percussion/server/PSRequest.java`

**Role:** Wraps HTTP request data and provides unified access to request context.

A PSRequest object wraps the HTTP request and provides:

- **Application Name** (`m_appName`) – Extracted from request URL path
- **Request Page** (`m_requestPage`) – Resource name within application
- **Parameters** (`m_params`) – Query string and form data
- **Input Document** (`m_inputDocument`) – Parsed XML body (if applicable)
- **Security Context** – User session, authentication token
- **Response Object** (`m_response`) – Associated PSResponse for output

**Request Path Parsing:**

```text
URL: http://localhost:9992/Rhythmyx/MyApp/getArticles.xml?id=123
     
Breakdown:
  /Rhythmyx/           = webapp context
  MyApp/               = appName
  getArticles.xml      = requestPage (becomes "getArticles" after parsing)
  ?id=123              = parameters
```

### Layer 3: Request Handler Routing (PSServer)

**Location:** `system/src/main/java/com/percussion/server/PSServer.java`

**Role:** Main server singleton; manages applications and routes requests to handlers.

PSServer implements the global singleton that:

- **Maintains Application Registry** – Map of `PSApplicationHandler` by application name
- **Routes Requests** – Routes PSRequest to appropriate handler based on app and page name
- **Manages Virtual Directories** – Maps logical application paths to physical file locations
- **Coordinates Services** – Manages initialization and shutdown of core services
- **Tracks Statistics** – Monitors request queuing and performance

**Request Routing Logic:**

```java
// Extract application name from request
String appName = psreq.getAppName();

// Look up application handler
PSApplicationHandler handler = PSServer.getApplicationHandler(appName);

// Get handler for specific request page
IPSRequestHandler pageHandler = handler.getRequestHandler(psreq);

// Process request
pageHandler.processRequest(psreq);
```

### Layer 4: Application Handler (PSApplicationHandler)

**Location:** `system/src/main/java/com/percussion/server/PSApplicationHandler.java`

**Role:** Handles all requests for a specific XML application.

Each XML application has exactly one PSApplicationHandler that:

- **Manages Data Sets** – Maintains handlers for each data set definition
- **Enforces Access Control** – Checks ACLs before allowing requests
- **Manages Threading** – Enforces request queue limits and timeouts
- **Routes Data Set Requests** – Finds appropriate handler for request page
- **Tracks Statistics** – Monitors performance and queue depth

**Key Attributes:**

- `m_application` – PSApplication definition (name, ACLs, datasets)
- `m_dataHandlerMap` – Map of handlers by request page name
- `m_maxRequestsQueued` – Max concurrent requests allowed
- `m_maxRequestTime` – Request timeout in seconds

**Processing Steps:**

1. Check request queue (enforce max threads)
2. Find appropriate data set handler by request page
3. Validate request against request definition
4. Check ACL/access control
5. Execute request through handler
6. Post-process results
7. Update statistics

**Request Type Resolution:**

- **Query Handler** – For SELECT operations (returns data)
- **Update Handler** – For INSERT/UPDATE/DELETE operations
- **File Handler** – For static file requests
- **Content Editor Handler** – For special content editing requests

### Layer 5: Handler Interface (IPSRequestHandler)

**Location:** `system/src/main/java/com/percussion/server/IPSRequestHandler.java`

**Role:** Standard interface for all request handlers in the system.

All handlers implement this simple interface:

```java
public interface IPSRequestHandler {
    public void processRequest(PSRequest request);
    public void shutdown();
}
```

**Implementations:**

- `PSApplicationHandler` – Routes to data set handlers
- `PSDataHandler` – Executes XML data operations
- `PSQueryHandler` – Executes SELECT queries
- `PSUpdateHandler` – Executes INSERT/UPDATE/DELETE
- `PSFileRequestHandler` – Handles static file requests
- Custom extension handlers – Application-specific logic

### File System Integration (Virtual Directories)

**Location:** `system/src/main/java/com/percussion/data/jdbc/PSFileSystemDriver.java`

**Role:** JDBC driver providing file system access for XML applications.

The file system driver integrates with the virtual directory system to map logical application paths to physical file locations:

- **Virtual Directory Mapping** – Maps application names to physical directories
- **Security Integration** – Validates user permissions at file system level
- **Dynamic Mapping** – Supports runtime directory registration and renaming
- **JDBC Interface** – Implements standard JDBC connection interface

**Virtual Directory Interface:**

```java
public interface IPSVirtualDirectory {
    public File getPhysicalPath(File relPath);
    public File getPhysicalLocation();
    public boolean hasPermissions(PSUserSession session, int permissions);
    public String getVirtualDirectory();
}
```

This abstraction decouples logical application paths from physical file locations, enabling flexible deployment and dynamic application discovery.

### Core Components

#### 1. Content Type Definition (XML)

Content types are defined in XML configuration files, typically located in:

```
system/config/ContentTypes/*.xml
```

A content type definition includes:

```xml
<PSXContentType id="100" name="Article">
  <PSXDataTypeSet>
    <PSXDataType id="1" name="title" type="text">
      <validation required="true" maxLength="255"/>
    </PSXDataType>
    <PSXDataType id="2" name="body" type="richtext">
      <validation required="true"/>
    </PSXDataType>
    <PSXDataType id="3" name="author" type="reference" refType="AuthorType"/>
  </PSXDataTypeSet>
  <PSXWorkflowAssociation>
    <PSXWorkflow id="EditorialWorkflow"/>
  </PSXWorkflowAssociation>
</PSXContentType>
```

#### 2. Type Configuration Service (PSTypeConfiguration)

Loads, parses, and caches content type definitions at startup:

- Reads XML configuration files
- Builds in-memory type catalogs
- Generates dynamic classes for
  content items (via

**PSBeanGenerator** using ByteBuddy)

- Provides lookup services for content type metadata

**Key Methods:**

- `getContentType(int typeId)` – Retrieve type metadata
- `getContentTypeTemplates()` – List available templates
- `findObjectTypeId(String name)` – Lookup by name
- `getFieldCategories()` – Retrieve field metadata

#### 3. Dynamic Class Generation

XAS generates Java classes at runtime for each content type:
- Each content type maps to a dynamically generated bean class
- Properties correspond to defined fields
- Provides type-safe access to content item data

- **Migration Note:** Originally used CGLib; now uses
  **ByteBuddy** for JDK 21 compatibility

#### 4. Persistence Layer

Content items are persisted in relational database tables:

- Base tables store common attributes (id, system, type)
- Type-specific tables store field data
- Relationships stored in junction tables
- Full-text indexing for search

### Data Flow

```
User Request
    ↓
Content Type Lookup (PSTypeConfiguration)
    ↓
Dynamic Class Resolution (ByteBuddy-generated class)
    ↓
Content Manager (PSContentMgr)
    ↓
ORM Mapping (Hibernate 7.x)
    ↓
Database Persistence
    ↓
Search Index Update
    ↓
Workflow Event Trigger
    ↓
Response
```

## Configuration

### Location

Content type configurations are typically located in:

```
system/config/ContentTypes/
system/config/SharedFieldDefs/
system/config/Workflows/
```

### Key Configuration Elements

#### Content Type Declaration

- **id** – Unique numeric type identifier
- **name** – Human-readable type name
- **description** – Documentation
- **icon** – UI representation
- **workflow** – Associated workflow

#### Field Definition

- **fieldName** – Unique within type
- **dataType** – text, numeric, richtext, reference, date, etc.
- **required** – Boolean constraint
- **unique** – Uniqueness constraint
- **maxLength** – String length limit
- **defaultValue** – Initial value
- **searchable** – Full-text indexable

#### Workflow Integration

- Associates states and transitions
- Defines permissions per state
- Triggers actions on state changes

#### Relationship Definition

- **source** – Content type with reference field
- **target** – Referenced content type
- **cardinality** – 1:1, 1:N, N:N
- **cascadeDelete** – Behavior on source deletion

### Example: Simple Content Type

```xml
<?xml version="1.0" encoding="UTF-8"?>
<PSXContentType id="301" name="BlogPost">
  <description>Blog post with comments and categories</description>

  <PSXDataTypeSet>
    <!-- Title field -->
    <PSXDataType id="1" name="title" type="text">
      <validation required="true" maxLength="200"/>
      <searchable>true</searchable>
    </PSXDataType>

    <!-- Publication date -->
    <PSXDataType id="2" name="publishDate" type="date">
      <validation required="true"/>
    </PSXDataType>

    <!-- Rich text body -->
    <PSXDataType id="3" name="content" type="richtext">
      <validation required="true"/>
      <searchable>true</searchable>
    </PSXDataType>

    <!-- Reference to Author -->
    <PSXDataType id="4" name="author" type="reference" refType="Author">
      <validation required="true"/>
    </PSXDataType>

    <!-- Multiple category references -->
    <PSXDataType id="5" name="categories" type="reference" refType="Category" cardinality="N:N">
      <validation required="true"/>
    </PSXDataType>

    <!-- Tags (comma-separated) -->
    <PSXDataType id="6" name="tags" type="text">
      <validation maxLength="500"/>
    </PSXDataType>
  </PSXDataTypeSet>

  <!-- Attach to Editorial Workflow -->
  <PSXWorkflowAssociation>
    <PSXWorkflow>EditorialWorkflow</PSXWorkflow>
  </PSXWorkflowAssociation>
</PSXContentType>
```

## Content Type Lifecycle

### 1. Registration

1. Administrator creates/modifies content type XML
2. File placed in `system/config/ContentTypes/`
3. CMS startup or hot-deploy reads configuration
4. **PSTypeConfiguration** processes definition
5. Dynamic class generated via ByteBuddy
6. Type registered in service catalog

### 2. Instance Creation

1. Content creator requests new item of type
2. **PSContentMgr.createItem()** invoked
3. Dynamic class instantiated
4. Form UI rendered from field metadata
5. Editor populates fields
6. Item submitted for workflow entry

### 3. Persistence

1. **PSContentMgr.save()** called
2. Dynamic object mapped to database by Hibernate
3. Type-specific data inserted
4. Full-text index updated
5. Workflow engine notified
6. Change events dispatched

### 4. Retrieval & Modification

1. **PSContentMgr.find()** locates item by ID or criteria
2. Hibernate reconstructs dynamic class instance
3. Field values loaded from database
4. Modifications applied
5. **PSContentMgr.save()** persists changes
6. Change events triggered

### 5. Workflow Progression

1. Item transitions between workflow states
2. State-specific permissions enforced
3. Actions triggered (notifications, approvals)
4. Status updated in database

## Integration with System Module

### Service Dependencies

The **system module** provides critical services that XAS depends on:

|         Service         |                 Package                 |                  Role                   |
|-------------------------|-----------------------------------------|-----------------------------------------|
| **PSTypeConfiguration** | `com.percussion.services.contentmgr`    | Load and cache content type definitions |
| **PSContentMgr**        | `com.percussion.services.contentmgr`    | CRUD operations for content items       |
| **PSWorkflowMgr**       | `com.percussion.utils.workflow`         | Manage workflow states and transitions  |
| **PSRelationshipMgr**   | `com.percussion.services.relationship`  | Manage content relationships            |
| **PSAclMgr**            | `com.percussion.services.security.data` | Enforce access control                  |
| **PSGuidMgr**           | `com.percussion.services.guidmgr`       | Generate unique identifiers             |
| **PSCatalog**           | `com.percussion.services.catalog`       | Catalog content objects                 |
| **PSJavaScript**        | `com.percussion.services.general`       | JavaScript execution environment        |

### Service Locator Pattern

XAS uses the **Service Locator Pattern** (implemented in system module) to access services:

```java
// Retrieve content type service
IPSTypeConfiguration typeSvc = PSServiceLocator.getManager(IPSTypeConfiguration.class);

// Load content type
PSContentType contentType = typeSvc.loadContentType(typeId);

// List field definitions
List<PSFieldDefinition> fields = contentType.getFieldDefinitions();
```

### Dynamic Class Integration

When new content types are registered:
1. **PSTypeConfiguration** detects configuration change
2. **PSBeanGenerator** (ByteBuddy-based) creates dynamic class
3. Dynamic class is registered in type cache
4. **PSContentMgr** uses dynamic class for serialization/deserialization
5. ORM framework (Hibernate) maps instances to tables

## Key Technical Details

### Dynamic Class Generation (ByteBuddy)

Content types that are defined via XAS require dynamic Java class generation at runtime. The **PSBeanGenerator** class handles this:

**Before (CGLib - JDK 8 compatible):**

```java
BeanGenerator gen = new BeanGenerator();
gen.setNamingPolicy(new TypeNamingPolicy());
gen.setSuperclass(Object.class);
gen.addProperty("title", String.class);
gen.addProperty("body", String.class);
Class<?> clazz = gen.create(); // Uses reflective ClassLoader.defineClass()
```

**After (ByteBuddy - JDK 21 compatible):**

```java
PSBeanGenerator gen = new PSBeanGenerator();
gen.setSuperclass(Object.class);
gen.setClassName("ContentType_100_ArticleType");
gen.addProperty("title", String.class);
gen.addProperty("body", String.class);
Class<?> clazz = gen.createClass(); // Uses Unsafe.defineClass() compatible with modules
```

### JDK 21 Compatibility

- CGLib's reflective approach blocked by JDK 21 module system
- ByteBuddy's `INJECTION` strategy uses `Unsafe.defineClass()` which bypasses module restrictions
- All class generation runs with Java's default class loading strategy

### Type Caching

- Content type definitions cached in memory at startup
- Reduces XML parsing overhead
- Hot-reload support via service restart
- Thread-safe implementation using synchronized maps

## Field Types

|     Type      |  Database   |         Use Case          |               Example                |
|---------------|-------------|---------------------------|--------------------------------------|
| **text**      | VARCHAR     | Short text, titles, names | Product name, article title          |
| **richtext**  | LONGTEXT    | Formatted HTML content    | Article body, product description    |
| **numeric**   | DECIMAL/INT | Numbers, quantities       | Price, quantity, rating              |
| **date**      | TIMESTAMP   | Calendar dates            | Publication date, expiry date        |
| **choice**    | VARCHAR     | Enumerated values         | Status (draft/published), category   |
| **reference** | INT/GUID    | Link to other content     | Author reference, category link      |
| **image**     | GUID        | Binary media              | Product photo, featured image        |
| **file**      | GUID        | Binary documents          | PDF attachment, downloadable content |
| **boolean**   | TINYINT     | True/false values         | Featured, published flags            |

## Workflow Integration

### Workflow Association

Content types are associated with workflows that define:
- **States**: draft, review, approved, published, archived
- **Transitions**: Rules for moving between states
- **Actions**: Notifications, approvals, scheduled publishing
- **Permissions**: Who can perform each transition

### Workflow Example

```
draft → review → approved → published → archived
  ↓        ↓         ↓
pending   pending   pending
approval  approval  approval
```

### Workflow Enforcement

1. User submits content to workflow
2. **PSWorkflowMgr** validates transition
3. Required actions executed (notifications)
4. State updated in database
5. New state permissions applied
6. Content may be hidden/visible based on state

## Security & Access Control

### ACL Integration

- Content types have type-level permissions
- Individual content items may have item-level ACLs
- Workflow states have state-level permissions
- Users or groups granted permissions per state

### Example

```
ArticleType {
  Draft state: Author + Editor can view/edit
  Review state: Editor + Manager can view/edit
  Published state: Everyone can view, only Admin can edit
}
```

## Performance Considerations

### Caching Strategy

- Content type definitions cached at startup
- Dynamic class cache maintains generated classes
- Metadata caching reduces repeated lookups
- Cache invalidation on configuration changes

### Query Optimization

- Type-specific queries indexed
- Relationship queries use junction table indexes
- Full-text search indexes maintained
- N+1 query prevention via eager loading

### Scalability

- XAS supports thousands of content types
- Each dynamic class instance is lightweight
- Database connections pooled
- Content items distributed across sharded tables (in clustered deployments)

## Troubleshooting

### Common Issues

#### Content Type Not Loading

**Symptom:** Type not found in catalog after startup
**Diagnosis:**
1. Check `/system/config/ContentTypes/` for XML file
2. Verify XML syntax validity
3. Check server logs for parsing errors
4. Verify type ID uniqueness

**Solution:**

```bash
# Restart CMS to reload type configurations
# Check logs:
tail -f catalina.out | grep "PSTypeConfiguration"
```

#### Dynamic Class Generation Failures (JDK 21)

**Symptom:** `NoClassDefFoundError` for dynamically generated types
**Root Cause:** Old CGLib-based code trying to use reflective class loading
**Solution:** Ensure PSBeanGenerator (ByteBuddy-based) is in use

#### Workflow State Locked

**Symptom:** Cannot transition content between states
**Diagnosis:**
1. Check workflow definition for transition rules
2. Verify user has permission for this state
3. Check required actions (approvals) are satisfied

**Solution:**

```java
// Verify transition available
IPSWorkflowMgr wfMgr = PSServiceLocator.getManager(IPSWorkflowMgr.class);
PSWorkflowTransition trans = wfMgr.getTransition(fromState, toState);
if (trans != null) {
    // Transition valid, check permissions
}
```

## Migration & Future Direction

### Historical Evolution

1. **Rhythmyx Era (1999-2010):** Original CGLib-based type engine
2. **CM System (2010-2015):** XML-driven type definitions introduced
3. **CM1 (2015-2020):** ORM migration to Hibernate
4. **8.0+ (2020-present):** Modernization for cloud/containers

### JDK 21 Modernization

- Replaced CGLib with ByteBuddy for class generation
- Maintained full backward compatibility with existing type definitions
- No changes required to existing content types
- All dynamic class generation now JDK 21 compatible

### Future Enhancements

- Potential GraphQL schema auto-generation from content types
- Enhanced metadata vocabulary for AI/ML integration
- Event-driven architecture for type changes
- Multi-tenant type isolation

## Related Documentation

- [Services Architecture](services.md) – Content management services details
- [Building & Development](building.md) – Development environment setup
- [Modernization Status](modernization.md) – JDK 21 compatibility updates
- [PSTypeConfiguration Source](../../services/src/main/java/com/percussion/services/contentmgr/PSTypeConfiguration.java) – Implementation details
- [PSContentMgr Source](../../services/src/main/java/com/percussion/services/contentmgr/PSContentMgr.java) – Content operations

## Key Takeaways

- **XAS is the heart of Percussion CMS** – All content operations flow through it
- **XML-based declarations** – No code changes needed to add/modify content types
- **Dynamic class generation** – Provides type-safe Java access to content
- **Workflow integration** – Built-in state management and business rules
- **Multiple field types** – Covers text, media, relationships, structured data
- **Service locator pattern** – Clean abstraction for accessing core services
- **JDK 21 ready** – ByteBuddy-based class generation handles modern Java restrictions
- **Legacy stability** – Backward compatible with all existing content type definitions

Thank you for the reminder about the existing directory structure documentation. This XML Application Server topic completes the documentation suite for the system module.
