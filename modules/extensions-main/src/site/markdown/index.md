# extensions-main

The **extensions-main** module is the canonical source of built-in Java and
JavaScript extensions that ship with Percussion CMS.

## What this module provides

- **340+ Java extensions** registered in `Java/Extensions.xml`
- **11 JavaScript extensions** registered in `Javascript/Extensions.xml`
- Supporting resources (the top-level `Extensions.xml` handler configuration)

At install time the module's JAR is unpacked by the `updateExtensions` Ant task
(via `PSInstallExtensions`) into the `Exits/` directory under the CMS install
root, and the embedded `Extensions.xml` files are merged into the running
server's extension registry under `Extensions/Handlers/`.

## Quick links

- [Extension Categories](./extension-categories.html) — functional groupings of
  all registered extensions
- [Extension Point Interfaces](./extension-interfaces.html) — the 24 plugin
  interfaces extensions can implement
- [Adding a New Extension](./adding-extensions.html) — step-by-step guide
- [Runtime Lifecycle](./runtime-lifecycle.html) — how extensions are discovered,
  loaded, and cached at startup
- [Planned Refactoring](./planned-refactoring.html) — the roadmap for making
  this module the single source of truth for extension management
- [Extension Lookup Fix (2026-03)](./worklog/extension-lookup-fix.html) — root
  cause analysis and fix for the extension loading regression in commit 6a1fbb402

## Maven coordinates

```xml
<dependency>
    <groupId>com.percussion</groupId>
    <artifactId>extensions-main</artifactId>
    <version>${project.version}</version>
</dependency>
```

## Build

```bash
cd modules/extensions-main
../../mvnw clean install
```

See the project [README](https://github.com/percussion/percussioncms/blob/development/modules/extensions-main/README.md) for full build and contribution instructions.
