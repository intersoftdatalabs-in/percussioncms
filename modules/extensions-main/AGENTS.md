This project follows the Universal Code v1.0.0 - read ../../docs/policies/UC-EMBED-v1.0.0.md (vendored; upstream https://github.com/monkeyking-hq/universal-code)

# AGENTS.md — extensions-main module

> **For AI agents:** Read this file first before making any changes to the extensions-main module.

## Required reading

Before writing any code in this module, **you must** read the following documentation in order:

1. **[README.md](./README.md)** — Overview of the module, its structure, and the Extension Manager architecture.
   - Key sections: "Module structure", "Extension categories", "Extension point interfaces", "Architecture notes", "Runtime lifecycle"
   - **Critical to understand:** The FQN (Fully Qualified Name) vs. category distinction and why category is correctly part of `PSExtensionRef.equals()` but map keys use FQN strings.
2. **[Extension Point Interfaces (site doc)](./src/site/markdown/extension-interfaces.md)** — All 24+ interfaces with counts, packages, and purposes.
   - Use this when choosing which interface a new extension should implement.
3. **[Extension Lookup Fix (worklog doc)](./src/site/markdown/worklog/extension-lookup-fix.md)** — Root cause analysis of the `6a1fbb402` regression.
   - **Why this matters:** Understand why the inner map in `PSExtensionHandlerConfiguration` must use FQN strings, not `PSExtensionRef` objects, as keys.
4. **[Planned Refactoring (site doc)](./src/site/markdown/planned-refactoring.md)** — The roadmap for moving Extension Manager implementation into this module.
   - Shapes all architectural decisions going forward.

## Module responsibilities

This module is **the canonical source** for:

- **340+ built-in Java extensions** (`src/main/java/com/percussion/`)
- **11 JavaScript extensions** (`src/main/resources/Javascript/Extensions.xml`)
- **Extension definitions and registry** (`src/main/resources/Java/Extensions.xml`, `Javascript/Extensions.xml`)

It **should NOT** contain (currently in `perc-system`, scheduled to move here):

- Extension Manager implementation (PSExtensionManager, PSExtensionHandlerConfiguration, etc.)
- Core extension interfaces (IPSExtensionManager, IPSExtensionDef, etc.)

These will be refactored into this module in a future phase per [Planned Refactoring](./src/site/markdown/planned-refactoring.md).

## Rules for agents

### Before adding or modifying an extension

1. **Choose the correct interface** — consult [Extension Point Interfaces](./src/site/markdown/extension-interfaces.md).
2. **Use the correct context path** — refer to the context conventions in [README.md](./README.md#extension-point-interfaces) or the site docs.
3. **Name with the `sys_` prefix** — all built-in extensions follow this convention (340+ examples in `Java/Extensions.xml`).
4. **Register in the correct categorization** — use an existing category or propose a new one with justification in a commit message or PR description.
5. **Write unit tests** — JUnit 5, using Mockito for mocks. Tests go in `src/test/java/` mirroring the package structure.
6. **Maintain backward compatibility** — do not remove or rename existing extensions. If redesigning, map old names to new implementations via deprecated entries.

### When modifying `Extensions.xml`

- The `categorystring` attribute is **informational only** — it does not affect runtime lookup.
- The `category` field in `PSExtensionRef` is **correctly** part of `equals()` and `hashCode()` (this is not a bug).
- The authoritative identity is the **FQN** (`handler/context/name`), used as the map key in `PSExtensionHandlerConfiguration`.
- **Do not** change the FQN of an existing extension — this breaks backward compatibility.

### When touching `PSExtensionHandlerConfiguration` or related classes

- Those classes currently live in `perc-system`, not in this module.
- If fixing bugs or making changes to those classes (e.g., lookup, caching, parse errors), coordinate with the refactoring plan.
- **Always validate** that lookups use `ref.getFQN()` as the key, not the `PSExtensionRef` object itself.
- Any parse error handling must log at `ERROR` level with the extension context and name for troubleshooting.

### Build and test

```bash
# From the repository root
cd modules/extensions-main

# Compile
../../mvnw clean compile

# Run unit tests
../../mvnw test

# Full install (for hot-deploy testing)
../../mvnw clean install

# Optional local formatting only (not a required process gate)
# ../../mvnw spotless:apply
# ../../mvnw spotless:check
```

## Refactoring notes

The module is being gradually evolved toward becoming the **single source of truth** for extension management:

- **Phase 1:** Extract extension APIs into a separate module or expand this module to include them.
- **Phase 2:** Move `PSExtensionManager`, `PSExtensionHandlerConfiguration`, `PSExtensionHandler`, etc. from `perc-system` into this module.
- **Phase 3:** Design and implement an SPI (Service Provider Interface) so third-party extensions can register via `ServiceLoader` or Spring configuration instead of XML patching.
- **Phase 4:** First-class lifecycle and caching for JavaScript extensions.

All changes must **preserve backward compatibility** with:

- Existing `Extensions.xml` file format and locations.
- The `IPSExtensionManager` service lookup path.
- The FQN (Fully Qualified Name) as the stable identity.

## Key documents

|       Document       |                                                  Location                                                  |             Audience              |                              Key content                              |
|----------------------|------------------------------------------------------------------------------------------------------------|-----------------------------------|-----------------------------------------------------------------------|
| README               | [./README.md](./README.md)                                                                                 | All developers, agents            | Architecture, categories, interfaces, how to add extensions, building |
| Extension Categories | [./src/site/markdown/extension-categories.md](./src/site/markdown/extension-categories.md)                 | Extension developers              | All 28+ categories, context conventions, counts                       |
| Extension Interfaces | [./src/site/markdown/extension-interfaces.md](./src/site/markdown/extension-interfaces.md)                 | Extension developers              | All 24+ interfaces, base classes, init parameters                     |
| Adding Extensions    | [./src/site/markdown/adding-extensions.md](./src/site/markdown/adding-extensions.md)                       | Extension developers              | Step-by-step guide with code examples                                 |
| Runtime Lifecycle    | [./src/site/markdown/runtime-lifecycle.md](./src/site/markdown/runtime-lifecycle.md)                       | Core developers, refactoring team | Startup sequence, caching, FQN identity, hot-deploy                   |
| Planned Refactoring  | [./src/site/markdown/planned-refactoring.md](./src/site/markdown/planned-refactoring.md)                   | Architecture team                 | Phases, constraints, backward compatibility                           |
| Extension Lookup Fix | [./src/site/markdown/worklog/extension-lookup-fix.md](./src/site/markdown/worklog/extension-lookup-fix.md) | Maintainers debugging issues      | Root cause of `6a1fbb402` regression, FQN vs. category                |

## Questions?

- **How do I add a new extension?** → Read [Adding Extensions](./src/site/markdown/adding-extensions.md) and [README.md](./README.md#adding-a-new-extension).
- **What interface should my extension implement?** → Consult [Extension Point Interfaces](./src/site/markdown/extension-interfaces.md).
- **Why is my extension not found at runtime?** → Check [Runtime Lifecycle](./src/site/markdown/runtime-lifecycle.md) and [Extension Lookup Fix](./src/site/markdown/worklog/extension-lookup-fix.md).
- **How do I refactor Extension Manager code?** → Review [Planned Refactoring](./src/site/markdown/planned-refactoring.md) and coordinate.

---

**Last updated:** 2026-03-10 (concurrent with extension lookup regression fix)
