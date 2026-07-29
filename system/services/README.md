# System services (`services/`)

Active service layer for Percussion CMS: interfaces and implementations under `services/src`.

**Toolchain:** JDK 21 · part of the `system` Maven module · format with Spotless via repo `./mvnw`.

## Layout

| Area | Package (typical) | Role |
|------|-------------------|------|
| Assembly | `com.percussion.services.assembly` | Templates, binding, rendering |
| Catalog | `com.percussion.services.catalog` | Type/object discovery |
| Content | `com.percussion.services.content` | Keywords, translations, folder props |
| Content mgr | `com.percussion.services.contentmgr` | Higher-level content ops |
| GUID | `com.percussion.services.guidmgr` | ID generation |
| Data / error / security | `…data`, `…error`, `…security` | Persistence helpers, errors, ACL-related |
| Change tracking | `com.percussion.services.contentchange` | Modification notifications |

## Build & test

From repo root:

```bash
./mvnw -pl system clean test
./mvnw -pl system -Dtest=PSSomeServiceTest test
```

See parent [system/README.md](../README.md) and [system/AGENTS.md](../AGENTS.md).

## Notes for agents

- Prefer interfaces + locators / existing patterns in this tree; do not invent Spring-Boot-style wiring.
- New tests: JUnit 5. Match existing package test layout under `services` test roots as used by the module.
- There is **no** active “Java 11/17 package log” for this tree; do not recreate tracking files.
