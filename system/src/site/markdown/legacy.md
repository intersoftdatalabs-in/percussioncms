# Legacy Code Guidelines

Guidance for working with legacy code in the system module, including backward compatibility, refactoring strategies, and maintenance approaches.

## What is "Legacy Code"?

In the context of the system module, legacy code refers to:

- Code written before the Java 11/17 modernization waves (module now targets JDK 21)
- Code using pre-modern language features (raw types, etc.)
- Code not yet marked with `// REFACTORED: CP-JAVA11`
- Internal packages maintained purely for backward compatibility

### Legacy vs. Active Code

|    Aspect    |             Active Code             |                      Legacy Code                      |
|--------------|-------------------------------------|-------------------------------------------------------|
| Location     | services/, business/, src/main/java | Testing/, Tools/, Docs/, Samples/ or unmarked classes |
| Maintenance  | Regular updates, modern patterns    | Minimal changes, backward-compatible fixes            |
| Java Version | Modern (JDK 21 line)                | Legacy (pre-modernization patterns)                   |
| Status       | Actively developed                  | Maintained for compatibility                          |
| Testing      | JUnit 5 preferred                   | May use JUnit 4                                       |

## Legacy Directories

**These directories contain legacy code and should be modified minimally:**

- `system/Testing/` – Legacy test infrastructure
- `system/Tools/` – Legacy utility tools (HTTPClient, etc.)
- `system/Docs/` – Historical documentation (some may be outdated)
- `system/Samples/` – Legacy sample applications
- `system/FastForward/` – Legacy template system
- `system/Designer/` – Legacy designer resources
- `system/Defaults/` – Default stylesheets and error pages
- `system/VersionControl/` – Version tracking
- `system/lib/` – Legacy JAR libraries

**Principle:** Only modify these if fixing a bug or if specifically asked to refactor.

## Backward Compatibility Strategy

### Core Principle

**All changes to public APIs must maintain backward compatibility.** The system module is the core — breaking changes cascade to dependent modules.

### What is Protected?

**Public API Surface:**

- ✅ All `public` classes (especially service interfaces)
- ✅ All `public` methods and constructors
- ✅ All `public static` members
- ✅ Public constants
- ✅ Service locator behavior

**Internal Implementation:**

- Can be changed more freely (though minimize churn)
- Protected/package-private methods
- Implementation classes ( internal structure)

### How To Maintain Compatibility

#### Pattern 1: Adding New Methods

```java
// ✅ Safe: Adding new public method
public class PSXxxService {
    public void newMethod(String param) {
        // New functionality
    }

    // Existing method stays unchanged
    public void existingMethod() {
        // Unchanged
    }
}

// Clients can call either method
```

#### Pattern 2: Deprecating Old Methods

```java
public class PSXxxService {
    // ✅ Deprecate and delegate
    @Deprecated(since = "8.2.0", forRemoval = false)
    public void oldMethod() {
        // Delegate to new implementation
        newMethod();
    }

    // New method with improved name or signature
    public void newMethod() {
        // Improved implementation
    }
}
```

#### Pattern 3: Enhancing Exceptions

```java
// ✅ Safe: Adding checked exception to throws clause
// (if extending an interface, must update interface first)
public void operation() throws PSXxxException {
    // May throw new exception type
}

// ✅ Safe: Adding runtime exception (don't need to update signature)
public void operation() {
    if (invalid) {
        throw new IllegalArgumentException("Invalid value");
    }
}
```

#### Pattern 4: Adding Generic Types

```java
// ℹ️ Tricky: Adding generics to existing class
// Before:
public List getItems() { return items; }

// After (safe because List is List raw type):
public List<Item> getItems() { return items; }

// Calling code still works:
List rawList = service.getItems();  // Still works
List<Item> typedList = service.getItems();  // Also works
```

#### Pattern 5: API Evolution

**What's safe:**

```java
// ✅ Add new public method
public void newFeature() { ... }

// ✅ Make return type more specific
// Before:
public Object getItem() { ... }
// After (but retain old method for compatibility):
public Item getItem() { ... }  // Now covariant if overriding

// ✅ Add new Exception to throws clause
public void operation() throws PSNewException { ... }
```

**What's NOT safe (without major version bump):**

```java
// ❌ Remove or rename public method
// ❌ Change method signature (params or return type)
// ❌ Change access level (public → private)
// ❌ Remove public class or interface
// ❌ Change field type or visibility
```

## When To Refactor Legacy Code

### Consider Refactoring If:

1. ✅ Code is clearly broken or buggy
2. ✅ Code is security vulnerable
3. ✅ Code is part of a request (agent asked to refactor)
4. ✅ Code is already listed in `refactored-java11-packages.txt`
5. ✅ Code is actively maintained and needs enhancement

### Avoid Refactoring If:

1. ❌ Code "just works" and is rarely touched
2. ❌ Code is in a legacy directory (Testing, Tools, etc.)
3. ❌ Code is undocumented and you don't understand it fully
4. ❌ Code would require changes in dependent modules to maintain compatibility

### Refactoring Process

If you decide to refactor legacy code:

1. **Create test case first**
   - Write test to verify current behavior
   - Ensure test passes before refactoring
2. **Refactor gradually**
   - Use IDE's safe refactoring tools (Rename, Extract Method, etc.)
   - Run tests frequently
   - Make small, focused changes
3. **Modernize patterns**
   - Replace raw types with generics
   - Add Optional for null safety
   - Use Streams and modern language features
   - Apply Google Java Style formatting
4. **Maintain backward compatibility**
   - Don't change public method signatures
   - Preserve exception behavior
   - Keep return types compatible
5. **Update documentation**
   - Add `// REFACTORED: CP-JAVA11` marker
   - Update `refactored-java11-packages.txt`
   - Update README and site docs if appropriate
6. **Test thoroughly**
   - Run all tests: `./mvnw clean verify`
   - Write new test cases for edge cases
   - Aim for 85%+ coverage

## Working with Legacy Code

### Code That Uses Raw Types

```java
// Legacy code:
List items = service.getItems();  // Raw type (unchecked warning)
for (Object obj : items) {
    Item item = (Item) obj;
    // Use item
}
```

**When modifying:**

```java
// Modernized version:
List<Item> items = service.getItems();  // Generified (backward compat)
for (var item : items) {
    // Use item directly (no cast)
}
```

### Code with Null Checks

```java
// Legacy code:
public void process(Item item) {
    if (item == null) {
        return;  // Early exit for null
    }
    // ... process item
}

User user = catalog.findUser(id);  // May return null
if (user != null) {
    process(user);
}
```

**When modernizing:**

```java
// Modern version:
public void process(Item item) {
    // Rely on callers to use Optional
}

Optional<User> user = catalog.findUser(id);  // Return Optional
user.ifPresent(this::process);  // Only call if present
```

### Code with No Tests

**If you encounter legacy code with no tests:**

1. Write tests first (to document behavior)
2. Then refactor with confidence
3. Ensure tests still pass

### Documentation Issues

Legacy code may have outdated or missing documentation:

- ✅ Update inline comments if refactoring
- ✅ Add JavaDoc to public methods if absent
- ✅ Update README or site docs if needed
- ✅ Note any discovered patterns or gotchas

## Testing Legacy Code

### Old Tests (JUnit 3/4 Mixed with JUnit 5)

The module supports both JUnit 4 and JUnit 5:

```java
// ✅ JUnit 5 (preferred for new tests)
import org.junit.jupiter.api.Test;

@Test
void newStyleTest() {
    // ...
}

// JUnit 4 (legacy, still works)
import org.junit.Test;

@Test
public void oldStyleTest() {
    // ...
}
```

**When refactoring tests:**

- Update to JUnit 5 if possible
- Use `@Test` from `org.junit.jupiter.api`
- Follow modern assertions: `Assertions.assertEquals()` or `assertThat()` (AssertJ)

### Mocking Legacy Dependencies

```java
@Test
void testWithMockedService() {
    // Mock a legacy service
    PSLegacyService mockService = Mockito.mock(PSLegacyService.class);
    when(mockService.getData()).thenReturn(someData);

    // Test your code that uses the mocked service
    MyClass obj = new MyClass(mockService);
    obj.process();

    // Verify interactions
    verify(mockService).getData();
}
```

## Common Legacy Patterns

### Pattern: Logger Declaration

**Legacy (Log4j 1.x):**

```java
Logger log = Logger.getLogger(MyClass.class);
```

**Modern (Log4j 2.x):**

```java
private static final Logger log = LogManager.getLogger(MyClass.class);
```

### Pattern: Resource Cleanup

**Legacy:**

```java
Connection conn = null;
try {
    conn = getConnection();
    // Use conn
} finally {
    if (conn != null) {
        try {
            conn.close();
        } catch (SQLException e) {
            log.error("Close failed", e);
        }
    }
}
```

**Modern:**

```java
try (var conn = getConnection()) {
    // Use conn
    // Auto-closed
}
```

### Pattern: Type Casting

**Legacy:**

```java
Object obj = getObject();
if (obj instanceof String) {
    String str = (String) obj;
    process(str);
}
```

**Modern:**

```java
Object obj = getObject();
if (obj instanceof String str) {  // Pattern matching (Java 16+)
    process(str);
}
```

## Documentation for Legacy Code

If you modify legacy code:

- ✅ Add or update inline comments
- ✅ Add JavaDoc to public methods
- ✅ Note any gotchas or surprising behavior
- ✅ Document why the code exists if non-obvious

Example comment:

```java
/**
 * Legacy method maintained for backward compatibility.
 * New code should use {@link #newMethod()} instead.
 *
 * @deprecated Use {@link #newMethod()} instead. Will be removed in 9.0.0.
 */
@Deprecated(since = "8.2.0", forRemoval = true)
public void oldMethod() {
    // Delegate to new implementation
    newMethod();
}
```

## When You Must Break Compatibility

**In extremely rare cases, you may need to break backward compatibility.**

**Process:**

1. **Document the breaking change** prominently
   - Include in release notes
   - Mention in module README
   - Add deprecation warnings first (if possible)
2. **Provide migration path**

   ```java
   /**
    * @deprecated Changed in 8.3.0 to return Optional instead of null.
    *
    * Migration: Use the result directly instead of null check:
    * <pre>
    *   // Old:
    *   Item item = service.find(id);  // null if not found
    *   if (item != null) { ... }
    *
    *   // New:
    *   Optional&lt;Item&gt; item = service.find(id);
    *   item.ifPresent(...);
    * </pre>
    */
   ```
3. **Plan removal timeline**
   - Announce deprecation at least one major version before removal
   - Example: Deprecate in 8.2, remove in 9.0

## Resources

- [Modernization Status](modernization.html) – Modernization history (JDK 21 baseline today)
- [Building & Development](building.html) – Testing and refactoring guidelines
- [Services Architecture](services.html) – Service patterns and evolving APIs

---

**Module Version:** 8.2.0-SNAPSHOT | **Last Updated:** March 2026

**Remember:** When in doubt, prioritize backward compatibility and code maintainability over modernization.
