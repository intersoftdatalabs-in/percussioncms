# Runtime Lifecycle

This page describes how the Extension Manager discovers, loads, and caches
extensions at server startup.

## Startup sequence

```
PSServer.init()
  └─ PSExtensionManager.init(config, handlerDir)
       └─ for each <PSXExtensionHandler> in Extensions/Extensions.xml:
            └─ PSExtensionHandlerHandler.init()
                 └─ PSExtensionHandler.initializeConfig()
                      └─ PSExtensionHandlerConfiguration(configFile)
                           └─ parses Extensions/Handlers/<handler>/<version>/Extensions.xml
                                └─ builds Map<context, Map<FQN, IPSExtensionDef>>
```

## Key classes

|                Class                 |    Module     |                                  Role                                   |
|--------------------------------------|---------------|-------------------------------------------------------------------------|
| `PSExtensionManager`                 | `perc-system` | Singleton registry facade; entry point for all extension lookups        |
| `PSExtensionHandlerHandler`          | `perc-system` | Manages the lifecycle of a single extension handler                     |
| `PSExtensionHandlerConfiguration`    | `perc-system` | Parses `Extensions.xml`; stores defs in a two-level `ConcurrentHashMap` |
| `PSJavaExtensionHandler`             | `perc-system` | Java handler; instantiates extensions via `PSExtensionClassLoader`      |
| `PSExtensionRef`                     | `perc-system` | Immutable identity: `handler`, `context`, `name`, `category`            |
| `IPSExtensionDef` / `PSExtensionDef` | `perc-system` | The parsed definition (init params, interfaces, param defs)             |

## Extension identity: FQN vs. category

A `PSExtensionRef` carries four fields:

|     Field     |           Example           |                   Source                   |
|---------------|-----------------------------|--------------------------------------------|
| `handlerName` | `Java`                      | Handler element in `Extensions.xml`        |
| `context`     | `global/percussion/filter/` | `context` attribute (always ends with `/`) |
| `extName`     | `sys_DefaultPasswordFilter` | `name` attribute                           |
| `category`    | `filter`                    | `categorystring` attribute                 |

The **FQN** (Fully Qualified Name) is `handler/context/name`:

```
Java/global/percussion/filter/sys_DefaultPasswordFilter
```

`PSExtensionRef.getFQN()` returns the FQN without the category. The internal
`ConcurrentHashMap` in `PSExtensionHandlerConfiguration` uses the FQN string
as the inner map key, so lookups succeed regardless of whether the caller
constructs a ref with or without a category.

`PSExtensionRef.equals()` and `hashCode()` include `category` for full object
equality, but map operations bypass object equality by using
`ref.getFQN()` directly.

## First-use caching

When code calls `PSExtensionManager.prepareExtension(ref, ...)`:

1. The manager looks up the handler for `ref.getHandlerName()`.
2. The handler calls `getExtensionDef(ref)` to fetch the `IPSExtensionDef`
   from the configuration map using `ref.getFQN()` as the key.
3. `PSJavaExtensionHandler` instantiates the implementation class via
   `PSExtensionClassLoader` and calls `extension.init(def, codeRoot)`.
4. The prepared instance is stored in a per-handler cache.

Subsequent calls return the cached instance immediately.

## Install-time packaging

At installation the `updateExtensions` Ant target in `install.xml`:

1. Runs `PSInstallExtensions` on `extensions-main-<version>.jar`, which
   unzips the JAR content into `{installRoot}/Exits/`.
2. Merges the embedded `Java/Extensions.xml` from the JAR into
   `{installRoot}/Extensions/Handlers/Java/30/Extensions.xml`.

The same process is applied by the `updateExtensions` task for
`extensions-workflow` and any additional extension JARs.

The merged file in `Extensions/Handlers/Java/30/Extensions.xml` is what the
server reads at startup. Changes to `src/main/resources/Java/Extensions.xml`
in this module only take effect after a re-install or hot-deploy of the JAR
followed by a server restart.

## Hot-deploying during development

```bash
# From the repository root
./scripts/hot-deploy-local.py \
    --install-dir /path/to/cms-install \
    --modules system \
    --restart
```

After restart, confirm in `server.log`:

```
INFO  [com.percussion.extension.PSExtensionManager] Initializing extension manager.
INFO  [com.percussion.extension.PSExtensionManager] Initialization successful.
```

Any extension definition parse errors now log at `ERROR` level with the
context and name of the failing extension (fixed in 2026-03 — see
[Extension Lookup Fix](./worklog/extension-lookup-fix.html)).
