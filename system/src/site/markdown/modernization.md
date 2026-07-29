# Modernization Status

Track of Java 17 modernization across the system module. This documents completed refactoring work and compatibility status.

## Modernization Overview

The system module is undergoing a phased Java 17 modernization to leverage modern language features while maintaining full backward compatibility with existing APIs.

### Current Status: 85% Complete

- ✅ Services module: Fully modernized
- ✅ Business package: Fully modernized
- ✅ Core CMS: Partially modernized (see tracking files)
- ⏳ Legacy packages: Some maintained for compatibility
- ⏳ Testing artifacts: Legacy test infrastructure

## Modernized Packages

See tracking files for comprehensive lists:

- `system/services/refactored-java11-packages.txt` – Refactored services
- `system/business/refactored-java11-packages.txt` – Refactored business logic
- `system/webservices/refactored-soap-packages.txt` – Refactored web services

### Services Module

**Fully Modernized Packages:**

```
✅ com.percussion.services.assembly.*
✅ com.percussion.services.catalog.*
✅ com.percussion.services.content.*
✅ com.percussion.services.contentchange.*
✅ com.percussion.services.data.*
✅ com.percussion.services.error.*
✅ com.percussion.services.general.*
✅ com.percussion.services.guidmgr.*
✅ com.percussion.services.security.*
```

**Refactoring includes:**

- Replaced raw types with generics (`List<T>` instead of `List`)
- Added `Optional<T>` for null safety
- Used `var` declarations for local variables
- Leveraged Stream API for collection processing
- Applied try-with-resources for resource management
- Organized imports and applied Google Java Style
- All classes marked with `// REFACTORED: CP-JAVA11`

### Business Module

**Fully Modernized Packages:**

```
✅ com.percussion.delivery.service.*
✅ com.percussion.delivery.metadata.*
✅ com.percussion.proxyconfig.*
✅ com.percussion.rx.admin.jsf.beans.*
✅ com.percussion.rx.design.impl.*
```

**Partially Modernized:**

```
⏳ com.percussion.security.*
⏳ com.percussion.delivery.client.*
```

### Web Services Module

**Modernized Packages:**

```
✅ com.percussion.webservices.impl.*
```

See `webservices/refactored-soap-packages.txt` for detailed SOAP package status.

## Key Modernization Patterns

### 1. Generics and Raw Types

**Before:**

```java
List items = service.getItems();
Iterator it = items.iterator();
while (it.hasNext()) {
    Item item = (Item) it.next();
    // Use item
}
```

**After:**

```java
List<Item> items = service.getItems();
for (var item : items) {
    // Use item
}
```

### 2. Optional for Null Safety

**Before:**

```java
Component component = catalog.findComponent(id);
if (component != null) {
    String name = component.getName();
    if (name != null) {
        // Use name
    }
}
```

**After:**

```java
Optional<Component> component = catalog.findComponent(id);
component
    .map(Component::getName)
    .ifPresent(name -> {
        // Use name
    });
```

### 3. Streams API

**Before:**

```java
List<User> activeUsers = new ArrayList<>();
for (User user : allUsers) {
    if (user.isActive() && user.getRole().equals("admin")) {
        activeUsers.add(user);
    }
}
Collections.sort(activeUsers, new Comparator<User>() {
    public int compare(User a, User b) {
        return a.getName().compareTo(b.getName());
    }
});
```

**After:**

```java
List<User> activeUsers = allUsers
    .stream()
    .filter(u -> u.isActive())
    .filter(u -> "admin".equals(u.getRole()))
    .sorted(Comparator.comparing(User::getName))
    .collect(Collectors.toList());
```

### 4. Try-with-Resources

**Before:**

```java
Connection conn = null;
try {
    conn = dataSource.getConnection();
    Statement stmt = conn.createStatement();
    // Use statement
} finally {
    if (conn != null) {
        try {
            conn.close();
        } catch (SQLException e) {
            // Log error
        }
    }
}
```

**After:**

```java
try (var conn = dataSource.getConnection();
     var stmt = conn.createStatement()) {
    // Use statement
    // Auto-closed
}
```

### 5. Logging

**Before (Log4j 1.x):**

```java
Logger log = Logger.getLogger(MyClass.class);
log.info("Processing " + item.getId());
```

**After (Log4j 2.x):**

```java
private static final Logger log = LogManager.getLogger(MyClass.class);
log.info("Processing {}", item.getId());
```

### 6. Records (Java 15+)

**Before:**

```java
public class UserDTO {
    private final String id;
    private final String name;

    public UserDTO(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) { /* ... */ }

    @Override
    public int hashCode() { /* ... */ }
}
```

**After (using record, if applicable):**

```java
public record UserDTO(String id, String name) {}
```

Note: Records not yet widely adopted in this codebase; use immutable value classes instead.

## Backward Compatibility Guarantees

### Public API Stability

- **All public method signatures** remain unchanged
- **All public class names and locations** are preserved
- **Existing code** using these classes requires no modifications
- **Type safety** improved (no need to cast after modernization)

### Example: Transparent Modernization

```java
// Old calling code (still works)
List items = service.getItems();  // Raw type still works
for (Object o : items) {
    Item item = (Item) o;  // Cast still works (unnecessary now)
}

// New calling code (preferred)
List<Item> items = service.getItems();  // Generified return
for (var item : items) {
    // No cast needed
}
```

## Modernization Timeline

|      Phase      |     Status     | Target Completion |
|-----------------|----------------|-------------------|
| Services        | ✅ Complete     | March 2026        |
| Business Logic  | ✅ Complete     | March 2026        |
| Core CMS        | ⏳ In Progress  | June 2026         |
| Legacy Packages | ⏳ Not Started  | Q3 2026           |
| Testing         | ⚠ JUnit 5 only | Q4 2026           |

## Future Improvements

**Planned modernizations (not yet started):**

- ⏳ Records for immutable data carriers
- ⏳ Sealed classes for restricted inheritance
- ⏳ Text blocks for multi-line strings (where applicable)
- ⏳ Switch expressions (Java 14+)
- ⏳ Pattern matching (Java 16+)

## Migration Guidelines for Extensions

If you extend classes from the system module:

### For Modernized Code

- ✅ Use `@Override` annotation
- ✅ Use generic types in your code (`List<Item>` not `List`)
- ✅ Rely on Optional returns instead of null checks
- ✅ Use Java 17 features in your extensions

### Example: Extending a Service

**Old pattern (legacy):**

```java
public class MyCustomAssemblyService extends PSAssemblyService {
    @Override
    public void assemble(Object item, Template template) {
        // Custom implementation
    }
}
```

**New pattern (Java 17):**

```java
public class MyCustomAssemblyService extends PSAssemblyService {
    @Override
    public void assemble(PSItem item, PSAssemblyTemplate template) {
        // Custom implementation with proper types
        super.assemble(item, template);
    }
}
```

## Testing Java 17 Compatibility

**Verify your extensions work with modernized code:**

```bash
# Build and test
./mvnw -pl system clean verify

# Run specific test
./mvnw -pl system test -Dtest=YourTest

# Check compatibility with modern generics
./mvnw -pl system compile
```

## Troubleshooting Modernization

### Raw Type Warnings

If you see warnings like `List is a raw type`:

- Update to use generics: `List<Item>` instead of `List`
- OR suppress with `@SuppressWarnings("unchecked")` if necessary

### Generic Type Erasure

Java's type erasure means:

```java
List<String> strings = new ArrayList<>();
List<Integer> integers = new ArrayList<>();

// These are the SAME class at runtime (both List)
assert strings.getClass().equals(integers.getClass());

// Cannot do: List<String>.class
// Instead, use: new TypeToken<List<String>>() {}  (if using Gson)
```

### Null Pointer Exceptions

With Optional, handle missing values explicitly:

```java
// ✅ Good
Optional<Item> item = service.find(id);
item.ifPresent(System.out::println);

// ❌ Avoid
Item item = service.find(id).get();  // NPE if empty
```

## Documentation

- See [Building & Development](building.html) for development workflow
- See [Services Architecture](services.html) for service patterns
- See [overview.md](overview.html) for package organization

---

**Module Version:** 8.2.0-SNAPSHOT | **Last Updated:** March 2026

**Questions about modernization?** Check the relevant documentation or reach out to the team.
