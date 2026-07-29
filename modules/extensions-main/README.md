# extensions-main

The **extensions-main** module is the canonical source of the built-in Java and
JavaScript extensions that ship with Percussion CMS. It bundles:

- **340+ Java extensions** registered in `Java/Extensions.xml`
- **11 JavaScript extensions** registered in `Javascript/Extensions.xml`
- Supporting resources (the top-level `Extensions.xml` handler configuration)

At install time the module's JAR is unpacked by the `updateExtensions` Ant task
(via `PSInstallExtensions`) into the `Exits/` directory under the CMS install
root, and the embedded `Extensions.xml` files are merged into the running
server's extension registry under `Extensions/Handlers/`.

> **Future direction:** The Extension Manager implementation currently lives in
> the `system` (`perc-system`) module. A planned refactoring will move
> `PSExtensionManager`, `PSExtensionHandlerConfiguration`, `PSExtensionHandler`,
> and the related interfaces into this module so that `extensions-main` becomes
> the true source of truth for both Java and JavaScript extension management.
> See [Architecture notes](#architecture-notes) below.

---

## Table of contents

1. [Module structure](#module-structure)
2. [Extension categories](#extension-categories)
3. [Extension point interfaces](#extension-point-interfaces)
4. [Architecture notes](#architecture-notes)
5. [Runtime lifecycle](#runtime-lifecycle)
6. [Adding a new extension](#adding-a-new-extension)
7. [Building and testing](#building-and-testing)
8. [Contributing](#contributing)

---

## Module structure

```
extensions-main/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/percussion/
│   │   │       ├── cas/          # Content-assembler exits
│   │   │       ├── ce/           # Content-editor exits
│   │   │       ├── cms/          # CMS-level exits and helpers
│   │   │       ├── community/    # Community and authentication exits
│   │   │       ├── cx/           # Content-explorer exits
│   │   │       ├── extensions/
│   │   │       │   ├── ca/       # Content-archive exits
│   │   │       │   ├── cms/      # Additional CMS exits
│   │   │       │   ├── components/  # Component management
│   │   │       │   ├── cx/       # Additional Content-explorer exits
│   │   │       │   ├── encoding/ # Input-sanitization transformers
│   │   │       │   ├── general/  # Generic UDFs and pre/post-processors
│   │   │       │   ├── i18n/     # Internationalization exits
│   │   │       │   ├── publishing/  # Publishing workflow exits
│   │   │       │   ├── security/ # Input-validation security extensions
│   │   │       │   ├── translations/ # Field-value translators
│   │   │       │   ├── usersearch/  # User-search cataloger
│   │   │       │   └── utils/    # Shared utilities (params, Tidy)
│   │   │       ├── filter/       # Password filter
│   │   │       ├── relationship/ # Relationship effects
│   │   │       ├── uicontext/    # UI context menu exits
│   │   │       ├── validate/     # Field validators
│   │   │       └── xmldom/       # XML DOM manipulation exits
│   │   └── resources/
│   │       ├── Extensions.xml              # Top-level handler config
│   │       ├── Java/Extensions.xml         # Java extension registry (340 entries)
│   │       └── Javascript/Extensions.xml   # JavaScript extension registry (11 entries)
│   └── test/
│       └── java/com/percussion/
│           ├── cas/              # Assembler extension tests
│           ├── cms/              # CMS exit tests
│           ├── extensions/       # General / translation / sort tests
│           ├── validate/         # Field validator tests
│           └── xmldom/           # XML DOM exit tests
```

---

## Extension categories

Each entry in `Java/Extensions.xml` carries a `categorystring` attribute that
groups extensions by functional area. The built-in categories are:

|      Category       | Count |                                Description                                 |
|---------------------|-------|----------------------------------------------------------------------------|
| `generic`           | 72    | General-purpose UDFs (string, date, link, parameter manipulation)          |
| `contentassembler`  | 25    | Assembly location and content assembler exits                              |
| `jexl`              | 20    | JEXL-expression extensions used in Velocity templates                      |
| `translation`       | 18    | Field-value translators (date formatting, form encode/decode)              |
| `psxsystem`         | 15    | Internal system exits                                                      |
| `xmldom`            | 14    | XML DOM manipulation exits                                                 |
| `relationship`      | 13    | Relationship effects and constraints                                       |
| `editiontask`       | 11    | Edition task exits (pre/post-publish events)                               |
| `contenteditor`     | 11    | Content-editor validators and transformers                                 |
| `cx`                | 9     | Content-explorer (CX) exits                                                |
| `assembly`          | 8     | Assembler and slot content-finder exits                                    |
| `encoding`          | 7     | HTML, JS, CSS, XML, URI encoding transformers                              |
| `contentlist`       | 7     | Content-list generator and template expander exits                         |
| `workflow`          | 6     | Workflow action exits                                                      |
| `validation`        | 6     | Field-value validators                                                     |
| `uicontext`         | 6     | UI context menus                                                           |
| `security`          | 6     | Input-validation security extensions                                       |
| `search`            | 6     | Search results processors                                                  |
| `itemfilter`        | 6     | Item filter rule exits                                                     |
| `cms`               | 6     | Additional CMS exits                                                       |
| `SlotContentFinder` | 5     | Slot content-finder exits                                                  |
| `scheduledTask`     | 5     | Scheduled task exits                                                       |
| `i18n`              | 5     | Internationalization date/text exits                                       |
| `publisher`         | 4     | Publishing exits                                                           |
| `communities`       | 4     | Community management exits                                                 |
| `clone`             | 3     | Content-clone exits                                                        |
| `components`        | 2     | Component management exits                                                 |
| `filter`            | 1     | Password filter (`sys_DefaultPasswordFilter`)                              |
| Others              | 8     | `usersearch`, `rule`, `report`, `filetracker`, `fastforward`, `exit`, `ca` |

---

## Extension point interfaces

Extensions implement one or more of the following interfaces defined in
`perc-system`:

|          Interface           | Count |                             Purpose                              |
|------------------------------|-------|------------------------------------------------------------------|
| `IPSResultDocumentProcessor` | 92    | Post-processes the XML result document returned to a client      |
| `IPSUdfProcessor`            | 77    | User-defined function: called from XSL or Velocity templates     |
| `IPSRequestPreProcessor`     | 55    | Pre-processes an incoming request before it hits a resource      |
| `IPSJexlExpression`          | 20    | JEXL-callable expression used in Velocity assembly               |
| `IPSFieldInputTransformer`   | 20    | Transforms a content-editor field value on input                 |
| `IPSEditionTask`             | 11    | Runs before/after a publish edition                              |
| `IPSItemInputTransformer`    | 11    | Transforms item-level data on input                              |
| `IPSEffect`                  | 10    | Relationship effect (fires on relationship create/modify/delete) |
| `IPSWorkflowAction`          | 9     | Fires on a workflow transition                                   |
| `IPSAssembler`               | 8     | Content assembler                                                |
| `IPSFieldValidator`          | 8     | Validates a single content-editor field                          |
| `IPSItemFilterRule`          | 7     | Item filter rule for visibility/publish eligibility              |
| `IPSItemValidator`           | 6     | Validates an entire content item                                 |
| `IPSFieldOutputTransformer`  | 6     | Transforms a field value on output                               |
| `IPSAssemblyLocation`        | 6     | Computes the assembly (publication) URL for an item              |
| `IPSTask`                    | 5     | Scheduled task                                                   |
| `IPSSlotContentFinder`       | 5     | Finds related items for a slot                                   |
| `IPSContentListGenerator`    | 4     | Generates the list of items for a publish edition                |
| `IPSTemplateExpander`        | 3     | Expands a content list into individual publishing items          |
| `IPSSearchResultsProcessor`  | 3     | Post-processes full-text search results                          |
| `IPSPasswordFilter`          | 1     | Hashes/validates passwords (`sys_DefaultPasswordFilter`)         |

---

## Architecture notes

### Current architecture

The extension subsystem is split across two Maven modules:

```
perc-system (system/)
  └── Extension Manager core
        ├── IPSExtensionManager               - registry facade
        ├── PSExtensionManager                - implementation
        ├── PSExtensionHandlerConfiguration   - parses Extensions.xml;
        │                                       stores defs keyed by FQN string
        ├── PSExtensionHandler                - base handler
        ├── PSJavaExtensionHandler            - Java handler (classloader)
        ├── PSExtensionRef                    - immutable identity
        │                                       (handler/context/name/category)
        └── IPSExtensionDef / PSExtensionDef  - definition model

extensions-main (modules/extensions-main/)
  └── Extension implementations + Extensions.xml registries
```

The Fully Qualified Name (FQN) for a Java extension is:

```
<handlerName>/<context><extensionName>
```

For example: `Java/global/percussion/filter/sys_DefaultPasswordFilter`

`PSExtensionRef.getFQN()` returns this string and is the stable key used
internally by `PSExtensionHandlerConfiguration` to store and look up
definitions, regardless of the `categorystring` attribute value in the XML.

### Key design constraint (FQN vs. category in equality)

`PSExtensionRef.equals()` and `hashCode()` include the `m_category` field to
enforce full object equality. Because the `categorystring` attribute in
`Extensions.xml` may differ from the (empty) category used by runtime lookup
callers that construct a `PSExtensionRef` via the three-argument constructor
`(handlerName, context, name)`, `PSExtensionHandlerConfiguration` deliberately
uses `ref.getFQN()` as the `Map` key rather than the `PSExtensionRef` object
itself. This ensures lookups always succeed regardless of category.

### Planned refactoring

The intent for a future iteration is to move the Extension Manager
implementation from `perc-system` into this module, making `extensions-main`
the single source of truth for all extension management:

- Move `IPSExtensionManager`, `PSExtensionManager`, `PSExtensionHandler`,
  `PSJavaExtensionHandler`, `PSExtensionHandlerConfiguration`, and
  supporting classes here.
- Consolidate both the built-in registry (`Extensions.xml`) and the Manager
  implementation under one Maven artifact, eliminating the runtime coupling
  to `perc-system` for this concern.
- Introduce a clean SPI so third-party extension bundles can register
  extensions without patching the XML files directly (e.g., via
  `ServiceLoader` or a Spring `@Configuration` scan).
- Scope JavaScript extension management under the same module so that JS
  exits have a first-class lifecycle matching Java exits.

---

## Runtime lifecycle

At server startup the extension subsystem is initialized as follows:

1. **`PSServer`** calls `PSExtensionManager.init(config, handlerDir)`.
2. **`PSExtensionManager`** iterates handlers declared in the root
   `Extensions/Extensions.xml`.
3. For the `Java` handler, **`PSJavaExtensionHandler`** reads
   `Extensions/Handlers/Java/30/Extensions.xml` (populated at install time
   by `updateExtensions`).
4. **`PSExtensionHandlerConfiguration`** parses the XML and builds an
   in-memory registry: `Map<context, Map<FQN, IPSExtensionDef>>`.
5. On the first use of an extension, the handler calls `prepare(ref)`, which
   looks up the `IPSExtensionDef` by FQN, instantiates the implementation
   class via a `PSExtensionClassLoader`, and caches the result.

### Install-time packaging

During a CMS installation (or upgrade), the `updateExtensions` Ant target in
`install.xml` calls `PSInstallExtensions` to:

1. Unzip this module's JAR (`extensions-main-<version>.jar`) into the
   `Exits/` directory. This extracts the compiled `.class` files and
   `Extensions.xml` resources into the install tree.
2. Merge the embedded `Java/Extensions.xml` into
   `Extensions/Handlers/Java/30/Extensions.xml` in the install directory.

The same mechanism applies to `extensions-workflow` and other extension
modules (e.g., custom customer extension bundles).

---

## Adding a new extension

### 1. Implement the extension class

Choose the appropriate interface from the table in
[Extension point interfaces](#extension-point-interfaces) and create your
implementation class under the relevant sub-package of
`src/main/java/com/percussion/`.

```java
package com.percussion.extensions.general;

import com.percussion.extension.IPSRequestContext;
import com.percussion.extension.IPSUdfProcessor;
import com.percussion.extension.PSDefaultExtension;

/** Example UDF for use in Velocity/XSL templates. */
public class PSMyNewUdf extends PSDefaultExtension implements IPSUdfProcessor {

    @Override
    public Object processUdf(Object[] params, IPSRequestContext request) {
        // implementation
        return null;
    }
}
```

### 2. Register the extension in `Java/Extensions.xml`

Add an `<Extension>` element to
`src/main/resources/Java/Extensions.xml`. Use the context convention from the
header comments at the top of that file:

```xml
<Extension categorystring="generic"
           context="global/percussion/generic/"
           deprecated="no"
           handler="Java"
           name="sys_myNewUdf">
  <initParam name="com.percussion.user.description">
    Short description of what this UDF does.
  </initParam>
  <initParam name="com.percussion.extension.version">1</initParam>
  <initParam name="className">
    com.percussion.extensions.general.PSMyNewUdf
  </initParam>
  <initParam name="com.percussion.extension.reentrant">yes</initParam>
  <interface name="com.percussion.extension.IPSUdfProcessor"/>
  <suppliedResources/>
</Extension>
```

**Context conventions:**

|          Interface           |            Context            |
|------------------------------|-------------------------------|
| `IPSUdfProcessor`            | `global/percussion/generic/`  |
| `IPSResultDocumentProcessor` | `global/percussion/exit/`     |
| `IPSRequestPreProcessor`     | `global/percussion/exit/`     |
| `IPSPasswordFilter`          | `global/percussion/filter/`   |
| `IPSFieldInputTransformer`   | `global/percussion/content/`  |
| `IPSWorkflowAction`          | `global/percussion/workflow/` |

### 3. Write a unit test

Add a JUnit 5 test class under `src/test/java/` mirroring the package of
your implementation class.

### 4. Build and verify locally

```bash
cd modules/extensions-main
../../mvnw clean install
```

Then hot-deploy and restart the local server:

```bash
cd /path/to/percussioncms
./scripts/hot-deploy-local.py \
    --install-dir /path/to/cms-install \
    --modules system \
    --restart
```

---

## Building and testing

Requires **JDK 21**. Use the wrapper scripts to ensure `JAVA_HOME` is set
correctly.

```bash
# Compile only
cd modules/extensions-main
../../mvnw clean compile

# Run unit tests
../../mvnw test

# Full install (produces JAR for deployment)
../../mvnw clean install

# Skip tests for speed
../../mvnw clean install -DskipTests

# Code style check (must pass before committing)
../../mvnw spotless:check
# Auto-fix style issues
../../mvnw spotless:apply
```

For cross-module changes that touch `perc-system` or other upstream modules,
build from the workspace root:

```bash
./mvnw clean install -DskipTests
```

---

## Contributing

- Keep public APIs backward compatible; avoid breaking changes to classes
  under `com.percussion.extensions`.
- Use the `sys_` prefix for names of built-in extensions added to
  `Extensions.xml`, matching the naming convention established across the
  existing 340+ entries.
- Add unit tests alongside any new code (JUnit 5; Mockito for mocks).
- The `categorystring` attribute in `Extensions.xml` is informational and
  does **not** affect runtime lookup. The FQN (`handler/context/name`) is
  the authoritative identity used as the map key.
- Run `../../mvnw spotless:check` before committing; apply fixes with
  `spotless:apply` if needed.
- Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

