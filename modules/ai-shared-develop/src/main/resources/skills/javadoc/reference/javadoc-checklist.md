# Javadoc Checklist for TechDocMonkey

Use this checklist when adding or updating Javadoc comments in percussioncms.

## Pre-Work

- [ ] Identify the JDK version (check `pom.xml` or parent POM for `<source>`/`<target>`/`<release>`)
- [ ] Determine if it's JDK 8 (no records), JDK 21 (traditional only), or JDK 25+ (Markdown allowed)

## Per-File Checklist

### Class/Interface
- [ ] First sentence describes what the class/interface *does* (not what it *is*)
- [ ] Include `@since` tag if public API
- [ ] Include `@author` if required by project standards
- [ ] Add `@see` references for related classes
- [ ] Note thread-safety if applicable

### Methods
- [ ] First sentence is a concise summary (no more than ~2 lines)
- [ ] Describe the contract:
  - `@param` - all parameters with constraints (null, empty, range)
  - `@return` - return value description (never `@returns`)
  - `@throws` - all checked exceptions, include conditions
- [ ] Use `{@link ClassName#method}` for cross-references
- [ ] Use `{@code expression}` for code snippets
- [ ] Include `@deprecated` with `@deprecated as of version X, use Y` if applicable
- [ ] Note nullability (use `@Nullable`, `@NonNull` if annotations exist)

### Fields/Constants
- [ ] Describe what the value represents
- [ ] Include units if applicable (ms, bytes, etc.)
- [ ] Note valid ranges

## Common Mistakes to Avoid

| Mistake | Correction |
|---------|------------|
| `@returns` | Use `@return` (singular) |
| "This method returns..." | Start with verb: "Returns the user..." |
| Implementation details | Describe behavior, not how |
| Missing `@param` | Always document all params |
| Empty `@return` for void | Omit `@return` for void methods |
| HTML in JDK 25+ Markdown | Use Markdown syntax instead |
| Missing throws for checked exceptions | Document all checked exceptions |

## percussioncms-Specific Patterns

### Service Interfaces (e.g., `*ServiceInf.java`)
```java
/**
 * Service for managing [entity type] operations.
 * 
 * <p>All methods in this service are guaranteed to be thread-safe
 * unless otherwise noted.</p>
 *
 * @param id the unique identifier of the [entity]
 * @param filter optional filter criteria (may be null)
 * @return list of matching [entities], never null
 * @throws PSEntryNotFoundException if the specified [entity] does not exist
 * @throws PSAuthorizationException if the user lacks permission
 * @since 8.0.0
 * @see RelatedServiceInf
 */
List<SomeEntity> getEntities(String id, SomeFilter filter) throws ...;
```

### Action Classes (e.g., `PSGet*Action.java`)
```java
/**
 * Retrieves [specific value] for use in the content editor.
 * 
 * <p>This action is called asynchronously when the editor loads
 * the specified field.</p>
 *
 * @param request the servlet request containing field parameters
 * @return JSON object with [value], or error object on failure
 * @throws PSInvalidActionParameterException if parameters are invalid
 */
public PSResponse execute(PSRequest request) throws ...;
```

## Maven Commands

```bash
# Generate Javadoc for the module
mvn javadoc:javadoc

# Generate Javadoc with links to other JDK docs
mvn javadoc:javadoc -DdetectLinks=true

# Generate Javadoc JAR
mvn javadoc:jar

# Check for missing Javadoc (if plugin configured)
mvn javadoc:check
```

## Quick Reference

- **JDK 8/21**: Only `/** ... */` comments, HTML allowed
- **JDK 25+**: `/** ... */` OR `/// ...` (Markdown)
- **Inline tags**: `{@link}`, `{@code}`, `{@literal}`, `{@value}`
- **Block tags**: `@param`, `@return`, `@throws`, `@see`, `@since`, `@version`, `@deprecated`
