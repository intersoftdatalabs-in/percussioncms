# System web services (`webservices/`)

SOAP / web-service endpoints and related types for Percussion CMS (in-tree under `system`).

**Toolchain:** JDK 21 · build with the `system` module · `./mvnw` / `mvnw.cmd`.

## Architecture (high level)

- **Base** – shared SOAP base types / helpers  
- **Services** – security, content, and other SOAP implementations  
- **Wire / fault types** – request/response and fault handling  
- **Clients / stubs** – generated or supporting client utilities where present  

Stack has moved off legacy Axis toward modern JAX-WS-style endpoints where migrated; follow existing classes in-tree rather than reintroducing Axis 1 patterns.

## Build

```bash
./mvnw -pl system clean install
```

See [system/README.md](../README.md) and [system/AGENTS.md](../AGENTS.md).

## Notes for agents

- Preserve public SOAP contracts unless the task explicitly includes a breaking wire change.
- No package-by-package modernization log is maintained here.
