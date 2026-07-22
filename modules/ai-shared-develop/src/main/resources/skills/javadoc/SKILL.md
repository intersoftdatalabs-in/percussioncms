---
name: javadoc
description: Best practices and version-correct Javadoc comment generation for percussioncms. Use when the user asks for "Javadoc", "Java documentation", "doc comments", "doc", "documentation", or related terms.
---

# Javadoc Best Practices for percussioncms

Use this skill when generating or updating Javadoc comments in the percussioncms project.

## Quick Start

1. **Check JDK version** — Look at `pom.xml` for `<source>/<target>/<release>` (likely 21)
2. **Use the checklist** — See `reference/javadoc-checklist.md` for per-file guidance
3. **Run generation scripts** — Use `scripts/generate-javadoc-stubs.py` for initial stubs

## JDK Version Detection

| Version |     Comment Style     | HTML Allowed | Markdown Allowed |
|---------|-----------------------|--------------|------------------|
| JDK 8   | `/** ... */`          | Yes          | No               |
| JDK 21  | `/** ... */`          | Yes          | No               |
| JDK 25+ | `/** ... */` OR `///` | Deprecated   | Yes              |

**For percussioncms**: JDK 21 — use traditional `/** ... */` only.

## Key Rules

### Structure

```java
/**
 * First sentence is the summary (concise, third person, present tense).
 * 
 * <p>Additional details about behavior, side effects, or constraints.</p>
 *
 * @param paramName description of parameter (including constraints like "may not be null")
 * @return description of return value
 * @throws ExceptionType condition that causes this exception
 * @since 8.0.0
 * @see RelatedClass#method
 */
```

### Common Mistakes

- ❌ `@returns` → ✅ `@return`
- ❌ "This method does..." → ✅ "Returns..."
- ❌ Implementation details → ✅ Observable behavior
- ❌ Missing `@throws` for checked exceptions

### Inline Tags

- `{@link ClassName#method}` — Cross-reference
- `{@code expression}` — Inline code
- `{@literal text}` — Escape HTML in JDK 25+

## Scripts

- `scripts/generate-javadoc-stubs.py` — Cross-platform Python port that generates stub comments
- `scripts/generate-javadoc-stubs.py` — (superseded by the cross-platform Python entry point)

## Maven Integration

The `maven-javadoc-plugin` is configured in `ai-shared-develop/pom.xml`:

```bash
# Generate Javadoc HTML
mvn javadoc:javadoc

# Generate Javadoc JAR
mvn javadoc:jar
```

## References

- [Javadoc Spec Summary](./reference/javadoc-spec-summary.md)
- [Java API Writing Specs](./reference/java-api-writing-specs.md)
- [Javadoc Tool Comment Style](./reference/javadoc-tool-comments.md)
- [Javadoc Tool Architecture](./reference/javadoc-tool-architecture.md)
- [TechDocMonkey Checklist](./reference/javadoc-checklist.md) ← Start here!

