# Planned Refactoring

## Goal

Make `extensions-main` the single source of truth for **both** the built-in
extension implementations **and** the Extension Manager that governs their
lifecycle — currently split across two modules.

## Current state

```
perc-system (system/)
  ├── Extension Manager API and implementation
  │     IPSExtensionManager, PSExtensionManager,
  │     PSExtensionHandlerConfiguration, PSExtensionHandler,
  │     PSJavaExtensionHandler, PSExtensionRef,
  │     IPSExtensionDef, PSExtensionDef, ...
  └── (all other Rhythmyx server concerns)

extensions-main (modules/extensions-main/)
  └── Extension implementations + Extensions.xml registries
```

The Extension Manager is embedded inside `perc-system`, which also contains
the server core, object store, content engine, publishing engine, and more.
This coupling complicates independent evolution of the extension subsystem.

## Target state

```
extensions-main (modules/extensions-main/)
  ├── Extension Manager API
  │     IPSExtensionManager, IPSExtensionHandler,
  │     IPSExtensionDef, IPSExtensionParamDef, ...
  ├── Extension Manager implementation
  │     PSExtensionManager, PSExtensionHandlerConfiguration,
  │     PSExtensionHandler, PSJavaExtensionHandler, ...
  ├── Extension SPI
  │     ExtensionBundle (ServiceLoader-discoverable descriptor)
  ├── Built-in Java extension implementations (current content)
  └── Extensions.xml registries + JavaScript extensions
```

`perc-system` would depend on `extensions-main` (or an extracted
`extensions-api` module) for the `IPSExtensionManager` interface, removing the
implementation from the server core.

## Proposed steps

### Phase 1: Extract the API

1. Create a new module `extensions-api` (or expand `extensions-main`) with only
   the interfaces:
   - `IPSExtensionManager`
   - `IPSExtensionHandler`
   - `IPSExtension`
   - `IPSExtensionDef` / `IPSExtensionParamDef`
   - `IPSExtensionRef` (or promote `PSExtensionRef` to the API)
   - All `IPS*Processor` / `IPS*Transformer` / `IPS*Validator` extension point
     interfaces
2. Have `perc-system` depend on `extensions-api`; move implementation classes
   to `extensions-main`.

### Phase 2: Move the implementation

3. Move `PSExtensionManager`, `PSExtensionHandlerConfiguration`,
   `PSExtensionHandler`, `PSJavaExtensionHandler`, `PSExtensionClassLoader`,
   and related classes from `system/` into `extensions-main/`.
4. Update `perc-system` to depend on `extensions-main` at runtime for the
   implementation, while retaining the compile-time dependency only on
   `extensions-api`.

### Phase 3: Introduce a SPI for third-party extensions

5. Define an `ExtensionBundle` interface discoverable via `ServiceLoader`:
   ```java
   public interface ExtensionBundle {
       /** Returns the handler name this bundle targets (e.g., "Java"). */
       String getHandlerName();

       /** Returns all extension definitions contributed by this bundle. */
       List<IPSExtensionDef> getExtensionDefs();
   }
   ```
6. Replace the XML-merge install step with a runtime discovery mechanism that
   aggregates `ExtensionBundle` implementations from all JARs on the classpath.
7. Keep backward compatibility by providing an
   `XmlExtensionBundle` adapter that reads existing `Extensions.xml` files.

### Phase 4: First-class JavaScript extensions

8. Introduce a `PSJavaScriptExtensionHandler` analogous to
   `PSJavaExtensionHandler` so that JS extensions have the same lifecycle,
   caching, and lookup semantics as Java extensions.
9. Register the JS handler in the top-level `Extensions.xml` alongside the
   Java handler.

## Backward compatibility constraints

- `PSExtensionRef.getFQN()` must remain stable — it is the authoritative
  identity string used as the registry map key.
- The `Extensions.xml` file format must remain parseable by existing
  installation tooling (`PSInstallExtensions`).
- The `IPSExtensionManager` service looked up via `PSServer.getExtensionManager()`
  must remain accessible; only the implementation location changes.

## Tracking

Create a GitHub issue to track this work before starting. Branch from
`development` and name the branch `feature/<issue-number>-extensions-main-refactor`.
