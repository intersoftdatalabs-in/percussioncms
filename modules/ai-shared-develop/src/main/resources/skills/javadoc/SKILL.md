---
name: javadoc
description: Best practices and version-correct Javadoc comment generation. Use when the user asks for "Javadoc", "Java documentation", "doc comments", "doc", "documentation", or related terms.
---

# Javadoc Best Practices

Use this skill when generating or updating Javadoc comments.

## Steps

1. Determine the project JDK version (pom.xml, toolchain, or build config).
2. Use only the Javadoc syntax supported by that JDK version (see references).
3. Write high-quality Javadoc:
   - Summary sentence first (third person, present tense).
   - Specify contract: inputs, outputs, side effects, thread-safety, nullability, and ranges.
   - Use `@param`, `@return`, `@throws`, `@deprecated`, `@since`, `@see` as applicable.
   - Prefer observable behavior over implementation details.
   - Use `{@code ...}` and `{@link ...}` for code and references.
4. Enforce version guardrails:
   - JDK 8 and JDK 21: traditional `/** ... */` comments only; HTML + tags; no Markdown doc comments.
   - JDK 25: traditional `/** ... */` or Markdown `///` comments; `doc-files/*.md` allowed.
5. Keep comments concise, accurate, and consistent with the Java API specification guidance.

## References

- [Javadoc spec summaries](./reference/javadoc-spec-summary.md)
- [Java API writing specs](./reference/java-api-writing-specs.md)
- [Javadoc tool comment style](./reference/javadoc-tool-comments.md)
- [Javadoc tool architecture](./reference/javadoc-tool-architecture.md)

