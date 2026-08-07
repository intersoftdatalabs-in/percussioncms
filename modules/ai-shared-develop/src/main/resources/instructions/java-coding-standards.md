# Agent Java Coding Standards

## Compiler Warnings

Resolve compiler warnings when making code changes.

### Java Serialization Warning Policy

When encountering "serial" warnings in Serializable classes:

1. Always add `private static final long serialVersionUID = 1L;`.
2. Place it as the first field in the class definition.
3. Do not use `@SuppressWarnings("serial")` unless the class is strictly for internal, short-lived memory use.

### JDK 21 Compatibility: this-escape

When addressing `[this-escape]` warnings:

- Priority 1: Make called methods `private` or `final`.
- Priority 2: Refactor to a `static` factory method if the object must be registered globally.
- Priority 3: Use `@SuppressWarnings("this-escape")` ONLY if refactoring creates high regression risk in legacy code.

### JDK 21 Deprecation Standards

#### 1. Security Manager / AccessController

- If the code is purely for internal file access or system properties, **remove** the `AccessController.doPrivileged` wrapper and execute the code directly.
- If the logic is critical for a sandbox, use `@SuppressWarnings("removal")` and flag for architectural review.

#### 2. Reflection

- **Never use** `clazz.newInstance()`.
- **Always use** `clazz.getDeclaredConstructor().newInstance()`.
- Ensure appropriate exception handling for `NoSuchMethodException` and `InvocationTargetException` is added when making this change.

### IO Standards & Deprecation

- **Internal Utility:** Do not use `com.percussion.util.IOTools`.
- **Stream Handling:** Always prioritize `try-with-resources` over manual close calls or utility "close" methods.
- **File Operations:** Use `java.nio.file.Files` for copying, moving, or deleting files.
- **Dependency:** If complex IO is required, use `org.apache.commons.io.FileUtils` (ensure it is defined in the module's `pom.xml`).
- **Cross-platform (mandatory):** Percussion CMS builds and deploys on Windows, Linux, and macOS. Never hardcode filesystem separators (`"/"` or `"\\"`) when joining paths. Prefer `java.nio.file.Path` / `Path.of` / `path.resolve(...)` and `Files.*`. Use `File.separator` / `File.pathSeparator` only when a separator character is truly required. Full rules: root `AGENTS.md` → **Cross-Platform File I/O & Paths**. Erlang reviews treat violations as **bugs**.

### Type Safety & Generics

- **Redundant Casts:** Always remove casts where the compiler can infer the type (e.g., from Generic collections).
- **Generics:** If you encounter a raw `List` or `Map`, attempt to parameterize it (e.g., `List<String>`) to eliminate the need for casts throughout the module.
- **No Raw Types:** Avoid using raw collections (e.g., `List`, `Map`, `Set`). Always provide type parameters.
- **Inference:** Use the diamond operator `<>` for object instantiation to keep code concise.
- **Legacy Boundaries:** If a method must return a raw type for compatibility with other modules, use `@SuppressWarnings("unchecked")` only at the narrowest possible scope (the variable assignment) and document why.

#### Use of 'var' (Local Variable Type Inference)

- **Use `var` only when the type is clearly obvious** from the right-hand side of the assignment (e.g., `var list = new ArrayList<String>();`).
- **Avoid `var` for method return values** where the type is not immediately apparent (e.g., `var data = getMetadata();`).
- **Toolchain:** On `main` (formerly `development`), modules inherit **JDK 21** (`release=21`). Use `var` freely where the type is obvious. JDK 8 maintenance is the separate `percussioncms-java8` repo (formerly `development-8.1.x` on this remote) — do **not** use post-8 language features if targeting that line.
- **Consistency:** Do not mix `var` and explicit types within the same block of code; choose the most readable option for that specific context.

### Control Flow Standards

- **Switch Statements:** - Prefer **Switch Expressions** (`case ->`) for all new code or when refactoring logic that doesn't require complex fall-through.
  - If using traditional `switch` blocks, every `case` must end with `break`, `return`, `throw`, or an explicit `// fall through` comment.
  - Use `@SuppressWarnings("fallthrough")` only for complex legacy logic where refactoring is high-risk.

