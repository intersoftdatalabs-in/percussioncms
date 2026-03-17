# XML Application Server - Request Handling Architecture

## Overview

The XML Application Server (E2) is the core request processing engine of Percussion CMS. It implements a sophisticated routing and handling system that converts HTTP requests into XML-based processing pipelines for content management operations.

## Architecture Layers

### Layer 1: Servlet Entry Point - PSAppServlet

**Location:** [system/src/main/java/com/percussion/servlets/PSAppServlet.java](system/src/main/java/com/percussion/servlets/PSAppServlet.java)

**Role:** HTTP request entry point and response handler.

**Key Responsibilities:**
- Implements `jakarta.servlet.http.HttpServlet`
- Intercepts all HTTP requests via `service(HttpServletRequest, HttpServletResponse)`
- Retrieves `PSRequest` from thread-local request info (set by security filter)
- Delegates to appropriate request handler
- Manages response output and headers

**Key Methods:**
- `service(HttpServletRequest, HttpServletResponse)` – Request entry point
- Gets `PSRequest` from `PSRequestInfo.KEY_PSREQUEST` (thread-local storage)
- Checks for include requests (supports nested/internal requests)
- Parses request body
- Retrieves handler via `PSServer.getRequestHandler(psreq)`
- Processes request through handler
- Outputs response headers and content
- Manages error handling and client abort detection

```java
// Simplified flow from PSAppServlet
PSRequest psreq = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
if (psreq == null) {
    throw new RuntimeException("The request was not properly initialized by the security filter");
}

IPSRequestHandler rh = PSServer.getRequestHandler(psreq);
if (rh == null) {
    res.sendError(HttpServletResponse.SC_NOT_FOUND);
    return;
}

rh.processRequest(psreq);
PSResponse psresp = psreq.getResponse();
```

### Layer 2: Request Object - PSRequest

**Location:** [system/src/main/java/com/percussion/server/PSRequest.java](system/src/main/java/com/percussion/server/PSRequest.java)

**Role:** Wraps HTTP request data and provides unified access to request context.

**Key Responsibilities:**
- Wraps `HttpServletRequest` and `HttpServletResponse`
- Manages request parameters, headers, and body parsing
- Tracks request metadata (URL, application, page, session)
- Associates with `PSResponse` for output
- Integrates with security context
- Provides performance/statistics tracking

**Key Attributes:**
- `m_appName` – XML application name (from request URL)
- `m_requestPage` – Page/resource name within application
- `m_servletRequest` – Underlying HTTP request
- `m_response` – Associated `PSResponse` object
- `m_requestFileURL` – Normalized request URL path
- `m_params` – Request parameters (query string + form data)
- `m_inputDocument` – Parsed XML input (if applicable)

**Key Methods:**
- `getAppName()` – Get application name from request path
- `getRequestPage()` – Get resource name from application
- `getParameter(String name)` – Get request parameter
- `getResponse()` – Get associated response object
- `parseBody()` – Parse request body based on content type
- `getRequestForIncludeURI(HttpServletRequest)` – Handle internal includes

**Content Parser Integration:**
- `PSXmlContentParser` – Parses XML request bodies
- `PSFormContentParser` – Parses form/URL-encoded bodies
- Automatically detects content type and invokes appropriate parser

### Layer 3: Request Handler Resolution - PSServer

**Location:** [system/src/main/java/com/percussion/server/PSServer.java](system/src/main/java/com/percussion/server/PSServer.java)

**Role:** Main server singleton; manages applications and route requests to handlers.

**Key Responsibilities:**
- Global server state and configuration management
- Application handler registry and lookup
- Request handler routing logic
- Server initialization and shutdown
- File system driver and virtual directory management

**Key Methods:**

#### `public static IPSRequestHandler getRequestHandler(PSRequest req)`

Routes request to the appropriate handler based on request path.

**Routing Logic:**
1. Extract application name from request path (e.g., `/Rhythmyx/AppName/page.xml` → `AppName`)
2. Look up application handler: `getApplicationHandler(appName)`
3. Determine request type (page name, extension)
4. Load application definition
5. Find matching data set (based on request page)
6. Return handler for that data set
7. If resource path is translatable, apply mapping (backward compatibility)

```java
String appName = req.getAppName();           // Extract from URL
PSApplicationHandler appHandler =
    PSServer.getApplicationHandler(appName);  // Lookup handler
IPSRequestHandler rh =
    appHandler.getRequestHandler(req);        // Get specific handler for request
```

#### `public static PSApplicationHandler getApplicationHandler(String appName)`

Looks up the application handler for a given application name. Returns `null` if not found.

#### Application Handler Registry

- `private static Map<String, PSApplicationHandler> ms_handlers`
- Populated during `initRequestHandlers(PSApplication[] apps)`
- One handler per application
- Thread-safe concurrent access

### Layer 4: Application Handler - PSApplicationHandler

**Location:** [system/src/main/java/com/percussion/server/PSApplicationHandler.java](system/src/main/java/com/percussion/server/PSApplicationHandler.java)

**Role:** Handles all requests for a specific XML application.

**Key Responsibilities:**
- Manages application definition and metadata
- Creates and maintains handlers for each data set
- Enforces access control (ACL checks)
- Manages request queuing and threading
- Statistics and logging
- Extension and upgrade handling

**Key Attributes:**
- `m_application` – `PSApplication` definition (name, ACL, datasets)
- `m_dataHandlerMap` – Map of data set handlers by request page
- `m_requestRoot` – Application's request root (e.g., `/AppName`)
- `m_maxRequestsQueued` – Queue size limit
- `m_maxRequestTime` – Request timeout in seconds
- `m_errorHandler` – Application-specific error handler
- `m_LogHandler` – Application-specific log handler

**Key Methods:**

#### `public void processRequest(PSRequest request)`

Main entry point for processing application requests.

**Processing Steps:**
1. Initialize request context (set application references)
2. Check request queue (enforce max threads)
3. Locate appropriate data set handler
4. Validate request against request definition
5. Check access control (ACL)
6. Execute request through handler (query/update/insert/delete)
7. Post-process results
8. Update statistics

**Request Type Handling:**
- `REQUEST_TYPE_QUERY` – SELECT operations
- `REQUEST_TYPE_INSERT` – INSERT operations
- `REQUEST_TYPE_UPDATE` – UPDATE operations
- `REQUEST_TYPE_DELETE` – DELETE operations

#### `IPSRequestHandler getRequestHandler(PSRequest request, boolean respondWithError)`

Locates the appropriate handler for a request within this application.

**Search Logic:**
1. Parse request page name (remove extension)
2. Iterate through data set definitions
3. Match request page against requestor definition
4. Return associated handler (`PSDataHandler`, `PSQueryHandler`, `PSUpdateHandler`)
5. If not found, optionally report error

#### Data Set Handler Hierarchy

```
IPSInternalRequestHandler (interface)
    ├─ PSQueryHandler (SELECT queries)
    ├─ PSUpdateHandler (INSERT/UPDATE/DELETE)
    ├─ PSDataHandler (base handler)
    └─ PSContentEditorHandler (special case)
```

**Handler Initialization:**

```java
PSApplicationHandler(PSApplication app, IPSObjectStoreHandler osHandler, IPSExtensionManager extMgr)
    // For each data set in application:
    //   - Create PSDataHandler
    //   - Create request page mapping (PSRequestPageMap)
    //   - Store in m_dataHandlerMap
```

### Layer 5: Request Handler Interface - IPSRequestHandler

**Location:** [system/src/main/java/com/percussion/server/IPSRequestHandler.java](system/src/main/java/com/percussion/server/IPSRequestHandler.java)

**Role:** Standard interface for all request handlers in the system.

```java
public interface IPSRequestHandler {
    /**
     * Process the request using the input context information.
     * Results written to request's PSResponse object.
     * Called immediately after request parsing (no security check yet).
     *
     * @param request the request object with context data
     */
    public void processRequest(PSRequest request);

    /** Shutdown the handler, freeing resources. */
    public void shutdown();
}
```

**Implementations:**
- `PSApplicationHandler` – Routes to data set handlers
- `PSDataHandler` – Processes XML data operations
- `PSQueryHandler` – Executes SELECT queries
- `PSUpdateHandler` – Executes INSERT/UPDATE/DELETE
- `PSFileRequestHandler` – Handles static files
- `PSHookRequestHandler` – Manages request hooks
- `PSRemoteConsoleHandler` – Debug console

## Data Flow Diagram

```
HTTP Request
    │
    ├─ Servlet Container
    │
    ├─ PSSecurityFilter
    │   └─ Creates PSRequest and stores in thread-local PSRequestInfo
    │
    ▼
PSAppServlet.service()
    │
    ├─ Extract PSRequest from PSRequestInfo
    │
    ├─ Parse request body
    │
    ├─ Call PSServer.getRequestHandler(psreq)
    │   │
    │   ├─ Extract app name from request URL
    │   │
    │   ├─ Look up PSApplicationHandler
    │   │
    │   ├─ Call handler.getRequestHandler(psreq)
    │   │   │
    │   │   ├─ Extract page name from request
    │   │   │
    │   │   ├─ Find matching data set
    │   │   │
    │   │   └─ Return handler (PSQueryHandler, PSUpdateHandler, etc.)
    │   │
    │   └─ Return handler
    │
    ├─ Call handler.processRequest(psreq)
    │   │
    │   ├─ Execute business logic
    │   │
    │   ├─ Build result XML document
    │   │
    │   └─ Store in PSResponse
    │
    ├─ Get PSResponse from request
    │
    ├─ Output HTTP response
    │   ├─ Set status code
    │   ├─ Output headers
    │   └─ Stream response body
    │
    ▼
HTTP Response
```

## File System Integration

### PSFileSystemDriver

**Location:** [system/src/main/java/com/percussion/data/jdbc/PSFileSystemDriver.java](system/src/main/java/com/percussion/data/jdbc/PSFileSystemDriver.java)

**Role:** JDBC driver for file system access; integrates with virtual directory system.

**Key Responsibilities:**
- Implements JDBC connection interface for file system resources
- Manages virtual directory mappings
- Provides file-based query capabilities

**Virtual Directory Management:**

```java
public static void addVirtualDirectory(IPSVirtualDirectory vdir)
public static IPSVirtualDirectory removeVirtualDirectory(String vdir)
public static void renameVirtualDirectory(String oldVdir, IPSVirtualDirectory newVdir)

private static final Map<String, IPSVirtualDirectory> m_vdirs =
    new Hashtable<String, IPSVirtualDirectory>();
```

**Connection Creation:**

```java
public Connection connect(String url, Properties info) throws SQLException
    // URL format: jdbc:psfilesystem:
    // Properties contain:
    //   - catalog: virtual directory name
    //   - sessionId: for session-based security
    // Returns: PSFileSystemConnection
```

### IPSVirtualDirectory Interface

**Location:** [system/src/main/java/com/percussion/data/vfs/IPSVirtualDirectory.java](system/src/main/java/com/percussion/data/vfs/IPSVirtualDirectory.java)

**Role:** Represents a virtual application directory mapping.

```java
public interface IPSVirtualDirectory {
    /**
     * Get physical path for a relative path within virtual directory.
     * May return null if application exists but has no physical directory.
     */
    public File getPhysicalPath(File relPath);

    /**
     * Get the actual directory this virtual directory represents.
     */
    public File getPhysicalLocation();

    /**
     * Check if user session has permissions on this virtual directory.
     */
    public boolean hasPermissions(PSUserSession session, int permissions);

    /**
     * Get the virtual directory name.
     */
    public String getVirtualDirectory();
}
```

**Key Features:**
- Decouples logical application paths from physical file locations
- Enables application deployment to different directories
- Integrates file system access with security and permission checking
- Supports dynamic directory mapping and renaming

## Request Processing Examples

### Example 1: Query Request

```
URL: http://localhost:9992/Rhythmyx/MyApp/getArticles.xml?id=123

1. PSAppServlet receives HTTP request
2. Extracts PSRequest from thread-local
   - appName = "MyApp"
   - requestPage = "getArticles"
   - params = {id: 123}
3. Calls PSServer.getRequestHandler(psreq)
   - Finds PSApplicationHandler for "MyApp"
   - Calls handler.getRequestHandler(psreq)
   - Finds data set named "getArticles"
   - Returns PSQueryHandler
4. Calls rh.processRequest(psreq)
   - Handler executes SELECT query
   - Builds XML result document
   - Stores in psreq.getResponse()
5. PSAppServlet outputs response
   - HTTP 200 OK
   - XML content type
   - XML body
```

### Example 2: Update Request

```
URL: http://localhost:9992/Rhythmyx/MyApp/updateArticle.xml

Body: XML document with article data

1. PSAppServlet receives HTTP request with XML body
2. Extracts PSRequest
   - appName = "MyApp"
   - requestPage = "updateArticle"
   - inputDocument = parsed XML
3. Gets handler (returns PSUpdateHandler)
4. Handler processes request
   - Parses XML input
   - Executes UPDATE query
   - Returns status in XML
5. Response sent to client
```

### Example 3: Internal/Nested Request

```
During request processing, extension/logic needs data from another dataset:

1. Current request handler calls:
   PSServer.getInternalRequest(
       "MyApp/otherDataset",
       currentRequest,
       extraParams,
       inheritParams
   )

2. Creates new PSRequest (cloned from current)
3. Sets up internal request context
4. Looks up handler for "MyApp/otherDataset"
5. Executes handler synchronously
6. Returns PSInternalRequest with results
7. Calling code continues with result data
```

## Key Architectural Principles

### 1. Separation of Concerns

- **Servlet Layer:** HTTP protocol handling
- **Request Object:** Context and metadata
- **Server:** Routing and registry
- **Application Handler:** Application-specific logic
- **Data Handlers:** Query/update execution

### 2. Multi-threaded Access

- `PSRequest` and `PSResponse` are thread-local
- `PSApplicationHandler` enforces queue limits
- Concurrent hash maps for thread-safe lookup
- Request statistics for monitoring

### 3. Security Integration

- Request created by security filter (pre-authentication)
- `PSRequest` contains security token
- `PSApplicationHandler` enforces ACL checks
- Virtual directory permissions validated at file system layer

### 4. Extensibility

- Interface-based handler design (`IPSRequestHandler`)
- Data set definitions allow no-code application changes
- Extension points for pre/post-processing
- Custom handler implementation possible

### 5. Resource Management

- Request context lifetime tracking
- Queue depth monitoring
- Request timeout enforcement
- Connection pooling via JDBC drivers

## Performance Considerations

### Request Queuing

```java
public void processRequest(PSRequest request) {
    synchronized (this) {
        if (m_requestsInProcessing < m_maxUserThreads) {
            m_requestsInProcessing++;
        } else {
            // Queue is full, report error
            throw new PSLargeApplicationRequestQueueError(...)
        }
    }
}
```

### Caching

- Data set definitions cached in application handler
- Virtual directory mappings cached in static map
- Handler instances reused across requests
- Stylesheet caching for XSLT transformations

### Statistics Tracking

```java
PSRequestStatistics stats = request.getStatistics();
// Tracks:
//   - Page setup time
//   - Query execution time
//   - Update handler time
//   - Total request time
//   - Query row counts
```

## Configuration and Loading

### Application Initialization

1. `PSServer.init()` calls `initObjectStore()`
2. `PSXmlObjectStoreHandler` loads `PSApplication` definitions
3. `PSServer.initRequestHandlers()` creates handlers:

   ```java
   PSApplicationHandler handler = new PSApplicationHandler(
       app,
       objectStoreHandler,
       extensionManager
   );
   ```
4. Handler registered in global `PSServer.ms_handlers` map
5. Application ready for requests

### Data Set Handler Creation

```java
For each PSDataSet in PSApplication:
    1. Get requestor (request page definition)
    2. Get page selection criteria
    3. Create appropriate handler:
       - If INSERT/UPDATE/DELETE possible → PSUpdateHandler
       - If only SELECT → PSQueryHandler
    4. Create PSRequestPageMap
    5. Register in m_dataHandlerMap
```

## Error Handling

### Request Validation Errors

- Validation rules checked before handler invocation
- Errors reported through `PSApplicationHandler.reportError()`
- Application-specific error pages displayed
- Custom error handling possible

### Handler Exceptions

- Caught in `PSAppServlet.service()`
- Client abort exceptions detected and suppressed
- Other exceptions wrapped in `ServletException`
- Server error page displayed

### Access Control Failures

- `PSAuthorizationException` thrown if ACL check fails
- Request rejected before handler execution
- Error logged and reported to client

## References

- **XML Application Server Documentation:** [xml-application-server.md](xml-application-server.md)
- **System Module Documentation:** [system-module-documentation.md](../../memories/repo/system-module-documentation.md)
- **Security Filter:** `PSSecurityFilter` (sets up `PSRequest` in thread-local)
- **Object Store Handler:** `PSXmlObjectStoreHandler` (loads application definitions)
- **Data Set Types:** `PSQueryPipe`, `PSUpdatePipe` (define data operations)

## Related Components

### Request Content Parsing

- `PSContentParser` – Base parser interface
- `PSXmlContentParser` – XML request body parsing
- `PSFormContentParser` – URL-encoded and form parsing
- `PSRequestParsingException` – Parse error handling

### Response Handling

- `PSResponse` – Contains response data (status, headers, body)
- `PSBaseResponse` – Base response class
- `PSResponseSendError` – Response output failures

### Extensions and Pre-processing

- `IPSRequestPreProcessor` – Pre-request processing
- `IPSResultDocumentProcessor` – Post-query XML processing
- `IPSUdfProcessor` – User-defined function execution
- `IPSExtensionManager` – Extension registry and lifecycle

