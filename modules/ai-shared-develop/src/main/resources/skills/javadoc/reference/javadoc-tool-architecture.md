# Javadoc Tool Architecture (Summary)

Source: https://openjdk.org/groups/compiler/javadoc-architecture.html

## High-Level Flow

- `javadoc` is a JDK tool that reads source code + doc comments and emits output.
- It uses a doclet plugin architecture; the standard doclet generates HTML.
- Taglets provide custom tag handling for the standard doclet.

## Key Components

- **javadoc tool**: orchestrates parsing and doclet execution.
- **Doclet API** (`jdk.javadoc.doclet`): public plugin API for doclets.
- **Standard doclet**: wraps internal HTML doclet implementation.
- **Toolkit builders/writers**: build page models and render output.

## External APIs Used

- `javax.tools.DocumentationTool` (tool entry point).
- `javax.lang.model.*` (language model for elements).
- `com.sun.source.*` (compiler tree API for source structure).

## Guidance

- Prefer public Doclet API; avoid internal packages in `jdk.javadoc.internal.*`.

