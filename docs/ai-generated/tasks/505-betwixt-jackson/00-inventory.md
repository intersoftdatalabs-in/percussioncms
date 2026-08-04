# Issue #1821 — Betwixt inventory (epic #505 slice 1)

> **Status:** Frozen inventory for parent epic **#505** (*Replace Apache Betwixt with
> Jackson XML*). **No production code changes** in this slice.
>
> **Last refreshed:** 2026-08-04 (agent pass on `main` / workspace head for PR of #1821).
>
> **Sibling slices (do not implement here):**
>
> |  Slice   | Issue |                                 Scope                                  |
> |----------|-------|------------------------------------------------------------------------|
> | 1 (this) | #1821 | Full inventory (docs only)                                             |
> | 2        | #1822 | Jackson `XmlMapper` parallel helper + golden XML snapshots (pilot DTO) |
> | 3        | #1823 | Migrate `PSXmlSerializationHelper` consumers (facade + domain batches) |
> | 4        | #1824 | Remove `commons-betwixt` from POMs/distributions + clean log4j noise   |

---

## 1. Why this doc exists

Apache Commons Betwixt **0.7** is EOL and is the sole library behind CMS design-object
XML serialize/deserialize used by package install, design export, catalog, and many
domain `fromXML` / `toXML` methods.

The **hub** is `modules/utils`:

|                            Class                             |                                                Role                                                |
|--------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| `com.percussion.services.utils.xml.PSXmlSerializationHelper` | Direct Betwixt `BeanReader` / `BeanWriter` facade; type registry; name mapper; legacy root rewrite |
| `com.percussion.services.utils.xml.PSBetwixtObjectConverter` | Extends Betwixt `DefaultObjectStringConverter` (Date ISO-8601, `Enum`, `IPSGuid`)                  |
| `com.percussion.xml.serialization.PSObjectSerializer`        | Singleton wrapper + `classregistry.xml` registration                                               |
| `com.percussion.utils.xml.IPSXmlSerialization`               | `@IPSXmlSerialization(suppress=…)` property suppression for Betwixt                                |

This inventory is the **single source of truth** for migration batching (#1823) and for
Child 4 (#1824) to prove a clean tree after Betwixt removal.

---

## 2. Maven / packaging edges (`commons-betwixt`)

Verified with repo-wide `pom.xml` search for `commons-betwixt` / `betwixt` (2026-08-04).

### 2.1 Direct / managed dependencies

|            Location            |       Edge type        |                                                              Details                                                              |
|--------------------------------|------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| Root `pom.xml`                 | `dependencyManagement` | `commons-betwixt:commons-betwixt:0.7` with exclusions of `commons-beanutils` / `commons-beanutils-core`                           |
| `modules/utils/pom.xml`        | **Direct compile**     | Runtime hub; re-excludes beanutils + junit from the Betwixt artifact; project uses managed `commons-beanutils` separately         |
| `modules/perc-toolkit/pom.xml` | **Direct `provided`**  | Declares Betwixt but **no Java sources import** `org.apache.commons.betwixt.*` (likely historical / unused; still blocks Child 4) |

### 2.2 Packaging / exclusion edges (must not reintroduce JARs)

These modules **exclude** `commons-betwixt` from `com.percussion:utils` (or related)
dependency graphs so installers/DTS distributions do not ship Betwixt transitively.
After utils migrates, exclusions become dead and should be removed in #1824 — but
**until then**, packaging must keep excluding or intentionally re-include.

|                                  Location                                  |             Edge type              |                                  Parent dependency being trimmed                                  |
|----------------------------------------------------------------------------|------------------------------------|---------------------------------------------------------------------------------------------------|
| `modules/perc-distribution-tree/pom.xml`                                   | `<exclusion>` of `commons-betwixt` | `com.percussion:utils` (preinstall / distribution tree)                                           |
| `deliverytiersuite/delivery-tier-suite/pom.xml`                            | **3×** exclusions                  | `com.percussion:utils` (main, `sources` classifier, `test-jar`) in DTS BOM / dependencyManagement |
| `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/pom.xml` | **2×** exclusions                  | `com.percussion:utils` (two separate utils dependency declarations)                               |

### 2.3 Historical / non-Maven classpath references

Not Maven coordinates, but still “Betwixt present” for Child 4 greps:

|                              Location                              |                                    Notes                                     |
|--------------------------------------------------------------------|------------------------------------------------------------------------------|
| `system/installResources/install.sh`                               | Adds `Tools/Commons/commons-betwixt-0.7RC2.jar` to `CLASSPATH`               |
| `system/installResources/launchConfig/Rhythmyx install.xml.launch` | Eclipse launch classpath entry for same JAR                                  |
| `modules/perc-ant/.../PSCheckManifestsForDuplicateFilesTest.java`  | Test fixture string mentions `commons-betwixt-0.7RC2.jar` in a fake manifest |

### 2.4 Jackson XML already on tree (migration target exists)

|           Location            |                                                    Notes                                                    |
|-------------------------------|-------------------------------------------------------------------------------------------------------------|
| Root `pom.xml`                | Manages `tools.jackson.dataformat:jackson-dataformat-xml` (`${jackson.version}`)                            |
| `projects/sitemanage/pom.xml` | Direct dependency on `jackson-dataformat-xml` (category / other marshalling — **not** the Betwixt hub path) |

**Note:** Epic #505 text still mentions `com.fasterxml.jackson.dataformat:jackson-dataformat-xml`.
Current monorepo parent BOM uses the **Jackson 3** groupId `tools.jackson.dataformat`.
Slice 2 (#1822) should align on the BOM coordinate already present.

---

## 3. Direct `org.apache.commons.betwixt.*` Java imports

**Only two production classes** import Betwixt packages (entire monorepo):

### 3.1 `PSXmlSerializationHelper`

Path: `modules/utils/src/main/java/com/percussion/services/utils/xml/PSXmlSerializationHelper.java`

|                              Import                               |
|-------------------------------------------------------------------|
| `org.apache.commons.betwixt.IntrospectionConfiguration`           |
| `org.apache.commons.betwixt.XMLIntrospector`                      |
| `org.apache.commons.betwixt.io.BeanReader`                        |
| `org.apache.commons.betwixt.io.BeanWriter`                        |
| `org.apache.commons.betwixt.io.read.BeanCreationChain`            |
| `org.apache.commons.betwixt.io.read.BeanCreationList`             |
| `org.apache.commons.betwixt.io.read.ChainedBeanCreator`           |
| `org.apache.commons.betwixt.io.read.ElementMapping`               |
| `org.apache.commons.betwixt.io.read.ReadContext`                  |
| `org.apache.commons.betwixt.strategy.HyphenatedNameMapper`        |
| `org.apache.commons.betwixt.strategy.NameMapper`                  |
| `org.apache.commons.betwixt.strategy.PropertySuppressionStrategy` |
| `org.apache.commons.betwixt.strategy.TypeBindingStrategy`         |

### 3.2 `PSBetwixtObjectConverter`

Path: `modules/utils/src/main/java/com/percussion/services/utils/xml/PSBetwixtObjectConverter.java`

|                               Import                               |
|--------------------------------------------------------------------|
| `org.apache.commons.betwixt.expression.Context`                    |
| `org.apache.commons.betwixt.strategy.DefaultObjectStringConverter` |

**Implication for migration:** all domain code goes through the facade. Replacing the
helper’s internals (#1822–#1823) can leave call sites on
`PSXmlSerializationHelper.readFromXML` / `writeToXml` until a final rename/cleanup.

---

## 4. Hub wrappers, annotation, registry

|           Artifact            |                                    Path                                     |                                              Role                                               |
|-------------------------------|-----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| `PSObjectSerializer`          | `modules/utils/.../xml/serialization/PSObjectSerializer.java`               | Singleton; `fromXml` / `fromXmlString` / `toXml` / `cloneObject` → helper                       |
| `PSObjectSerializerException` | same package                                                                | Checked exception wrapper                                                                       |
| `IPSXmlSerialization`         | `modules/utils/.../utils/xml/IPSXmlSerialization.java`                      | Annotation read by `SuppressionStrategy`                                                        |
| `classregistry.xml`           | `system/src/main/resources/com/percussion/server/classregistry.xml`         | Classes registered at server start via `PSObjectSerializer.registerBeanClasses(PSServer.class)` |
| JUnit sample beans            | `modules/utils/.../xml/serialization/junit/{Address,Book,Name,Person}.java` | Serializer test fixtures only                                                                   |

### 4.1 `classregistry.xml` registered types

|                           FQCN                            |                        Has `*.betwixt`?                         |
|-----------------------------------------------------------|-----------------------------------------------------------------|
| `com.percussion.services.guidmgr.data.PSGuid`             | Yes                                                             |
| `com.percussion.services.content.data.PSKeyword`          | Yes                                                             |
| `com.percussion.services.ui.data.PSHierarchyNode`         | No (uses `PSHierarchyNodeProperty.betwixt` for nested property) |
| `com.percussion.services.security.data.PSCommunity`       | Yes                                                             |
| `com.percussion.services.security.data.PSAclImpl`         | No                                                              |
| `com.percussion.services.security.data.PSAclEntryImpl`    | No                                                              |
| `com.percussion.services.security.PSTypedPrincipal`       | No                                                              |
| `com.percussion.services.security.data.PSAccessLevelImpl` | No                                                              |
| `com.percussion.services.security.data.PSUserAccessLevel` | No                                                              |
| `com.percussion.utils.types.PSPair`                       | No                                                              |

Commented-out registry entries: junit `Address` / `Book` / `Name` / `Person`.

### 4.2 Server bootstrap

`system/src/main/java/com/percussion/server/PSServer.java` — early init:

```text
PSObjectSerializer.getInstance().registerBeanClasses(PSServer.class);
```

Loads `classregistry.xml` from the `PSServer` package resource path and calls
`PSXmlSerializationHelper.addType(Class)` for each entry (default hyphenated element name
via `PSNameMapper`).

---

## 5. Production `*.betwixt` mapping files

Source-only (ignore `target/` copies). **10 source files total.**

### 5.1 Production (system)

|                                                  File                                                   |                                                    Custom mapping intent                                                    |
|---------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| `system/src/main/resources/com/percussion/services/assembly/data/PSAssemblyTemplate.betwixt`            | Root `assembly-template`; nested `bindings`/`binding` with `addBinding`; `template-slot-ids`                                |
| `system/src/main/resources/com/percussion/services/assembly/data/PSTemplateTypeSlotAssociation.betwixt` | Root `slot-type-association`; hyphenated `slot-id`, `template-id`, `content-type-id` (must match package-normalize rewrite) |
| `system/src/main/resources/com/percussion/services/content/data/PSKeyword.betwixt`                      | Nested `choices`/`choice` → `addChoice`                                                                                     |
| `system/src/main/resources/com/percussion/services/guidmgr/data/PSGuid.betwixt`                         | Root `guid`; **attributes** `host-id`, `type`, `uuid` (`primitiveTypes="attribute"`)                                        |
| `system/src/main/resources/com/percussion/services/security/data/PSCommunity.betwixt`                   | Root `community`; **hide** `roleAssociations`, `siteAssociations`                                                           |
| `system/src/main/resources/com/percussion/services/ui/data/PSHierarchyNodeProperty.betwixt`             | Maps `parentGuid` → `parentId` as `PSGuid` (**note:** file contains a typo `<element">` on open tag — pre-existing)         |

### 5.2 Test-only (utils junit samples)

|                                           File                                            |
|-------------------------------------------------------------------------------------------|
| `modules/utils/src/main/resources/com/percussion/xml/serialization/junit/Address.betwixt` |
| `modules/utils/src/main/resources/com/percussion/xml/serialization/junit/Book.betwixt`    |
| `modules/utils/src/main/resources/com/percussion/xml/serialization/junit/Name.betwixt`    |
| `modules/utils/src/main/resources/com/percussion/xml/serialization/junit/Person.betwixt`  |

---

## 6. Call-site inventory (grouped for #1823 batches)

**Method (2026-08-04):** files matching
`PSXmlSerializationHelper|PSObjectSerializer|PSBetwixtObjectConverter|IPSXmlSerialization`
in `*.java` (excluding `target/`) → **70 files**.

**Usage legend**

|   Code   |                               Meaning                               |
|----------|---------------------------------------------------------------------|
| **RW**   | `fromXML`/`toXML` (or equivalent) via `readFromXML` / `writeToXml`  |
| **T**    | static `addType(...)` registration only (or primary role)           |
| **A**    | `@IPSXmlSerialization` only (no direct helper call, or also has RW) |
| **G**    | `getIdFromXml` only (SAX guid extract — not full Betwixt bean IO)   |
| **S**    | `PSObjectSerializer` API                                            |
| **H**    | Hub / infrastructure                                                |
| **Test** | Unit/integration test                                               |

### 6.1 Hub — `modules/utils` (migrate first / last)

|                    File                     |                    Role                     |
|---------------------------------------------|---------------------------------------------|
| `.../PSXmlSerializationHelper.java`         | **H** — Betwixt core                        |
| `.../PSBetwixtObjectConverter.java`         | **H** — type conversions                    |
| `.../IPSXmlSerialization.java`              | **H** — annotation                          |
| `.../PSObjectSerializer.java`               | **H** / **S**                               |
| `.../PSObjectSerializerException.java`      | **H**                                       |
| `.../junit/{Address,Book,Name,Person}.java` | Test fixtures                               |
| `.../PSXmlSerializationHelperTest.java`     | **Test** — legacy `<null>` root, round-trip |

### 6.2 Domain batch: **workflow**

`system/services/src/com/percussion/services/workflow/data/`

|        Class        |          Role          |                                   Nested `addType` element → type                                   |
|---------------------|------------------------|-----------------------------------------------------------------------------------------------------|
| `PSWorkflow`        | **RW** + **A** + **T** | `state`→`PSState`, `role`→`PSWorkflowRole`, `notificationdef`→`PSNotificationDef`                   |
| `PSState`           | **RW** + **T**         | `assignedrole`→`PSAssignedRole`, `transition`→`PSTransition`, `agingTransition`→`PSAgingTransition` |
| `PSTransition`      | **RW** + **T**         | `notification`→`PSNotification`, `transitionrole`→`PSTransitionRole`                                |
| `PSTransitionHib`   | **RW** + **T**         | same as `PSTransition`                                                                              |
| `PSAgingTransition` | **RW** + **T**         | `notification`→`PSNotification`                                                                     |
| `PSNotification`    | **RW** + **T**         | `recipient`/`ccrecipient`→`String`                                                                  |
| `PSNotificationDef` | **RW**                 | —                                                                                                   |
| `PSAssignedRole`    | **RW**                 | —                                                                                                   |
| `PSTransitionRole`  | **RW**                 | —                                                                                                   |
| `PSWorkflowRole`    | **RW**                 | —                                                                                                   |

**Suggested #1823 sub-batch:** workflow graph is tightly coupled; migrate as one graph
with golden XML for a multi-state workflow export.

### 6.3 Domain batch: **assembly**

`system/services/src/com/percussion/services/assembly/`

|                Class                 |          Role          |                                             Notes                                              |
|--------------------------------------|------------------------|------------------------------------------------------------------------------------------------|
| `data/PSAssemblyTemplate`            | **RW** + **A**         | Production `.betwixt`                                                                          |
| `data/PSTemplateSlot`                | **RW** + **A** + **T** | `slot-type-association`→`PSTemplateTypeSlotAssociation`; **package tag normalize** before read |
| `data/PSTemplateTypeSlotAssociation` | **RW** + **A** + **T** | Production `.betwixt`; dual element names historically                                         |
| `data/PSTemplateBinding`             | **A** only             | Suppress binding id / version                                                                  |
| `impl/PSAssemblyService`             | **G**                  | `getIdFromXml` for TEMPLATE and SLOT                                                           |

Related deployer note (not a helper call site):  
`deployer/.../PSSlotDefDependencyHandler.java` comments on Betwixt emitting zero PKs when
association mapping fails.

Related test: `system/src/test/.../PSTemplateSlotXmlRestoreTest.java` (normalize tags).

### 6.4 Domain batch: **content / keywords**

`system/services/src/com/percussion/services/content/`

|              Class               |            Role            |                                          Notes                                          |
|----------------------------------|----------------------------|-----------------------------------------------------------------------------------------|
| `data/PSKeyword`                 | **RW** + **A** + **T**     | `.betwixt`; `choice`→`PSKeywordChoice`; package install often uses legacy `<null>` root |
| `data/PSKeywordChoice`           | **RW** (via keyword graph) | Registered as nested type                                                               |
| `data/PSAutoTranslation`         | **RW** + **A**             | —                                                                                       |
| `data/PSContentTypeSummary`      | **RW**                     | —                                                                                       |
| `data/PSContentTypeSummaryChild` | **RW**                     | —                                                                                       |
| `data/PSFieldDescription`        | **RW**                     | —                                                                                       |
| `data/PSFolderProperty`          | **RW** + **A**             | —                                                                                       |
| `data/PSItemStatus`              | **RW**                     | —                                                                                       |
| `impl/PSContentService`          | **T**                      | `auto-translation`→`PSAutoTranslation`                                                  |

### 6.5 Domain batch: **contentmgr**

|       Class        |          Role          |        `addType`        |
|--------------------|------------------------|-------------------------|
| `PSNodeDefinition` | **RW** + **A** + **T** | `variant-guid`→`PSGuid` |

### 6.6 Domain batch: **filter**

|         Class         |               Role               |          `addType` / notes           |
|-----------------------|----------------------------------|--------------------------------------|
| `PSItemFilter`        | **RW** + **A** + **T**           | `rule-def`→`PSItemFilterRuleDef`     |
| `PSItemFilterRuleDef` | **A** + **T** (+ nested RW path) | `parameters`→`PSItemFilterRuleParam` |
| `PSFilterManager`     | **G**                            | `getIdFromXml`                       |

### 6.7 Domain batch: **security**

|          Class          |            Role             |                             `addType` / notes                             |
|-------------------------|-----------------------------|---------------------------------------------------------------------------|
| `PSAclImpl`             | **RW** + **A** + **T**      | `entry`→`PSAclEntryImpl`                                                  |
| `PSAclEntryImpl`        | **A** + **T** (+ nested RW) | `ps-permission`→`PSAccessLevelImpl`, `typed-principal`→`PSTypedPrincipal` |
| `PSAccessLevelImpl`     | **A**                       | —                                                                         |
| `PSCommunity`           | **RW** + **A**              | `.betwixt` hides role/site associations                                   |
| `PSCommunityVisibility` | **RW**                      | —                                                                         |
| `PSLogin`               | **RW**                      | —                                                                         |

### 6.8 Domain batch: **sitemgr / publisher / pubserver**

|         Class          |          Role          |                          Notes                           |
|------------------------|------------------------|----------------------------------------------------------|
| `PSSite`               | **RW** + **T**         | `site-property`→`PSSiteProperty`, `template-id`→`PSGuid` |
| `PSSiteProperty`       | **RW** + **T**         | `context`→`PSPublishingContext`                          |
| `PSPublishingContext`  | **RW** + **A** + **T** | `default-scheme`→`PSLocationScheme`                      |
| `PSLocationScheme`     | **RW**                 | Fully-qualified helper calls                             |
| `PSSiteManager`        | **G**                  | SITE id extract                                          |
| `PSSiteCatalogServlet` | **S**                  | `PSObjectSerializer.getInstance().toXml(summary)`        |
| `PSContentList`        | **RW** + **A**         | publisher                                                |
| `PSDeliveryType`       | **RW**                 | publisher                                                |
| `PSEdition`            | **RW** + **A**         | publisher                                                |
| `PSPubServer`          | **RW**                 | pubserver                                                |

### 6.9 Domain batch: **catalog / system / ui / guid / i18n**

|             Class              |          Role          |                      Notes                       |
|--------------------------------|------------------------|--------------------------------------------------|
| `PSObjectSummary`              | **RW** + **A** + **T** | also `locked`→`PSObjectLockSummary`              |
| `PSAudit` / `PSAuditTrail`     | **RW**                 | system                                           |
| `PSDependency` / `PSDependent` | **RW**                 | system                                           |
| `PSMimeContentAdapter`         | **RW**                 | system                                           |
| `PSSharedProperty`             | **RW**                 | system                                           |
| `PSHierarchyNode`              | **RW** + **A**         | betwixt-oriented enum accessors in comments      |
| `PSHierarchyNodeProperty`      | **RW**                 | production `.betwixt`                            |
| `PSLegacyGuid`                 | **A** only             | suppress several properties                      |
| `PSLocale`                     | **A** only             | i18n; serialized in security tests via `addType` |

### 6.10 Server / bootstrap

|   Class    |                   Role                   |
|------------|------------------------------------------|
| `PSServer` | **S** — `registerBeanClasses` at startup |

### 6.11 Tests (system + utils)

|               Class               |                          Module                          |
|-----------------------------------|----------------------------------------------------------|
| `PSXmlSerializationHelperTest`    | utils                                                    |
| `PSObjectSerializerTest`          | system                                                   |
| `PSObjectSerializerRoundTripTest` | system                                                   |
| `PSSerializationTest`             | system                                                   |
| `PSObjectSummaryTest`             | system                                                   |
| `PSTemplateSlotXmlRestoreTest`    | system (package association normalize; Betwixt-adjacent) |

### 6.12 Production type-registry map (`addType` element names)

Complete production (non-test) registrations for Jackson element-name parity:

|           Element name           |              Class              |                    Registered from                     |
|----------------------------------|---------------------------------|--------------------------------------------------------|
| *(default via mapper)*           | `PSObjectSummary`               | `PSObjectSummary` static                               |
| `locked`                         | `PSObjectLockSummary`           | `PSObjectSummary`                                      |
| `choice`                         | `PSKeywordChoice`               | `PSKeyword`                                            |
| `auto-translation`               | `PSAutoTranslation`             | `PSContentService`                                     |
| `state`                          | `PSState`                       | `PSWorkflow`                                           |
| `role`                           | `PSWorkflowRole`                | `PSWorkflow`                                           |
| `notificationdef`                | `PSNotificationDef`             | `PSWorkflow`                                           |
| `assignedrole`                   | `PSAssignedRole`                | `PSState`                                              |
| `transition`                     | `PSTransition`                  | `PSState`                                              |
| `agingTransition`                | `PSAgingTransition`             | `PSState`                                              |
| `notification`                   | `PSNotification`                | `PSTransition`, `PSTransitionHib`, `PSAgingTransition` |
| `transitionrole`                 | `PSTransitionRole`              | `PSTransition`, `PSTransitionHib`                      |
| `recipient`                      | `String`                        | `PSNotification`                                       |
| `ccrecipient`                    | `String`                        | `PSNotification`                                       |
| `slot-type-association`          | `PSTemplateTypeSlotAssociation` | `PSTemplateSlot`                                       |
| `template-type-slot-association` | `PSTemplateTypeSlotAssociation` | `PSTemplateTypeSlotAssociation`                        |
| `variant-guid`                   | `PSGuid`                        | `PSNodeDefinition`                                     |
| `rule-def`                       | `PSItemFilterRuleDef`           | `PSItemFilter`                                         |
| `parameters`                     | `PSItemFilterRuleParam`         | `PSItemFilterRuleDef`                                  |
| `entry`                          | `PSAclEntryImpl`                | `PSAclImpl`                                            |
| `ps-permission`                  | `PSAccessLevelImpl`             | `PSAclEntryImpl`                                       |
| `typed-principal`                | `PSTypedPrincipal`              | `PSAclEntryImpl`                                       |
| `site-property`                  | `PSSiteProperty`                | `PSSite`                                               |
| `template-id`                    | `PSGuid`                        | `PSSite`                                               |
| `context`                        | `PSPublishingContext`           | `PSSiteProperty`                                       |
| `default-scheme`                 | `PSLocationScheme`              | `PSPublishingContext`                                  |
| *(classregistry defaults)*       | see §4.1                        | `PSServer` → `PSObjectSerializer`                      |

Plus any additional types registered only via `addType(Class)` from classregistry
(`guid`, `keyword`, `hierarchy-node`, `community`, `acl-impl`, … via `PSNameMapper`).

---

## 7. Logging noise (Betwixt loggers)

|                              Location                               |                                                      Config                                                      |
|---------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `modules/perc-jetty/.../log4j2.xml`                                 | `org.apache.commons.betwixt.io.BeanReader` and `...digester.ElementRule` → **error** (production Jetty defaults) |
| `system/ear/jboss-4.0/conf/log4j.xml`                               | Legacy JBoss: `BeanReader` → ERROR (comment: “turn off excessive logger”)                                        |
| `projects/sitemanage/src/main/resources/log4j.properties`           | `org.apache.commons.betwixt=DEBUG`                                                                               |
| `modules/p13n-api/src/main/resources/log4j.properties`              | `DEBUG`                                                                                                          |
| `modules/segmentation-api/src/main/resources/log4j.properties`      | `DEBUG`                                                                                                          |
| `modules/segmentation-rx/src/main/resources/log4j.properties`       | `DEBUG`                                                                                                          |
| `modules/perc-toolkit/log4j.properties`                             | `DEBUG`                                                                                                          |
| `deliverytiersuite/.../p13n-ds/src/main/resources/log4j.properties` | `DEBUG`                                                                                                          |
| `CONTRIBUTING.md`                                                   | Sample log4j2 snippet silencing `BeanReader` / `ElementRule`                                                     |

Child 4 (#1824) removes these when Betwixt is gone.

---

## 8. Known wire-contract risks (must preserve in #1822–#1823)

These are **behavioral contracts**, not optional niceties.

### 8.1 Legacy root element `<null>`

- **Where:** Package payloads under `modules/perc-packages` (especially keywords) often
  use root `<null id="…">` instead of `<keyword>`.
- **Mitigation today:** `PSXmlSerializationHelper.rewriteLegacyNullRoot(xml, clazz)`
  rewrites only case-sensitive root `<null>…</null>` to the `PSNameMapper` element name
  before `BeanReader.parse`.
- **Tests:** `PSXmlSerializationHelperTest` (utils).
- **Jackson risk:** Must keep accepting legacy roots on **read**; modern writes should
  not emit `<null>` (current write path already does not).

### 8.2 Hyphenated name mapping (`PSNameMapper`)

- Strips `PS` / `IPS` class prefixes; flattens multi-cap runs (`GUID`→`Guid`); then
  `HyphenatedNameMapper` → `assembly-template`, `content-type-id`, etc.
- Inner classes: `$` suffix segment used as name.
- **Jackson risk:** Default Jackson XML naming will **not** match unless
  `@JacksonXmlRootElement` / property names / a custom `PropertyNamingStrategy` mirror
  this mapper.

### 8.3 Type registry weakness

- Betwixt is weak on abstract/interface collection elements; product uses global
  `ms_typeMap` + `ChainedBeanCreator` (`addType`).
- **Jackson risk:** Need polymorphic type handling or explicit element→class map
  equivalent to §6.12.

### 8.4 Property suppression (`@IPSXmlSerialization(suppress=true)`)

- Applied on getters/`is` methods; also always suppresses JavaBeans `class` property.
- Many Hibernate version / identity fields suppressed so they do not appear in design XML.

### 8.5 Primitive binding strategy

- `Enum` and `IPSGuid` treated as **PRIMITIVE** bindings (`PSTypeBindingStrategy`).
- Converter: util `Date` ↔ ISO-8601 (`PSDateFormatISO8601`); `Enum` by name;
  `IPSGuid` via `PSGuid` string form.

### 8.6 Thread safety

- `writeToXml` is **`synchronized`** (historical Betwixt/beanutils race: empty
  documents under concurrent writes). Migration must retain safe concurrency or keep a
  similar gate.

### 8.7 Package association unhyphenated tags

- Archives (e.g. `perc.nav`) ship unhyphenated tags (`contenttypeid`, `templateid`,
  `slotid`).
- `PSTemplateSlot.normalizePackageAssociationElementNames` rewrites to hyphenated
  names expected by `.betwixt` / name mapper **before** `readFromXML`.
- Deployer comments: failed mapping → zero IDs → package install failures.

### 8.8 Dual association element names

- Both `slot-type-association` and `template-type-slot-association` are registered for
  `PSTemplateTypeSlotAssociation` (historical payload variance).

### 8.9 `PSGuid` attribute form

- `.betwixt` uses `primitiveTypes="attribute"` with `host-id`, `type`, `uuid` —
  different from default element-primitive style used by most beans.

### 8.10 Community hidden associations

- `.betwixt` `<hide property="roleAssociations"/>` / `siteAssociations` — must not
  reappear in XML after Jackson migration.

### 8.11 `getIdFromXml` is SAX, not Betwixt bean IO

- Scans for first `guid` attribute or `<guid>` element text.
- Can stay independent of Betwixt removal, but still lives on the helper class surface.

### 8.12 Synchronized global type map

- `addType` mutates process-wide static map; class static initializers depend on load
  order. Golden tests should force registration the same way production does.

---

## 9. Suggested migration order (feeds #1822 / #1823)

1. **#1822** — Parallel Jackson helper + golden snapshots for a **pilot DTO** (recommend
   `PSGuid` + simple keyword/choice graph: small surface, real package risk, existing tests).
2. **#1823 batches** (order by risk / coupling):
   1. Hub facade dual-path (Jackson primary, Betwixt fallback optional during transition)
   2. Guid + keyword/content package payloads (legacy `<null>` + `.betwixt`)
   3. Assembly templates/slots (normalize + dual names)
   4. Security ACL graph
   5. Workflow graph
   6. Site / publisher / filter / remaining system/ui
   7. `PSObjectSerializer` + classregistry + `PSSiteCatalogServlet`
3. **#1824** — Delete Betwixt POMs, exclusions, log4j, install classpath, `.betwixt`
   files, converter class; run §10 checklist to zero.

---

## 10. Child 4 “zero remaining refs” checklist (#1824)

Run from **repo root** after migration. Expect **zero** hits in production sources
(allowlisted docs under `docs/ai-generated/tasks/505-betwixt-jackson/` and closed epic
text may still mention Betwixt historically).

### 10.1 Dependency / packaging

```text
# Expect 0 (or only historical comments you choose to keep)
rg -n "commons-betwixt|commons-betwixt-" --glob "**/pom.xml"
rg -n "commons-betwixt" system/installResources modules/perc-ant
```

### 10.2 Java API surface

```text
rg -n "org\\.apache\\.commons\\.betwixt" --glob "**/*.{java,xml,properties}"
rg -n "PSBetwixtObjectConverter|import org\\.apache\\.commons\\.betwixt" --glob "**/*.java"
```

After facade rename/cleanup also:

```text
rg -n "PSXmlSerializationHelper|PSObjectSerializer" --glob "**/*.java"
# Expect only intentional post-migration names (or zero if fully removed/renamed)
```

### 10.3 Mapping files

```text
# Expect 0 under src/ (target/ may be dirty until clean)
rg --files -g "*.betwixt" | rg -v "[\\\\/]target[\\\\/]"
```

### 10.4 Logging

```text
rg -n "org\\.apache\\.commons\\.betwixt" --glob "**/*.{xml,properties,md}"
```

### 10.5 Functional gates (not greps)

- [ ] `modules/utils` standalone `mvnw clean install`
- [ ] `system` standalone `mvnw clean install` (or justified reactor)
- [ ] Package install smoke: keyword package with legacy `<null>` root
- [ ] Slot/template association package (e.g. nav) restores non-zero IDs
- [ ] Design export/import round-trip for workflow + ACL samples
- [ ] Jetty log defaults no longer reference Betwixt loggers
- [ ] DTS + CMS distribution trees: no `commons-betwixt` JAR on runtime classpath

### 10.6 Explicit “done when” for #1821 (this slice)

- [x] Every POM / packaging edge listed (§2)
- [x] Every direct Betwixt import listed (§3)
- [x] Every production `*.betwixt` listed (§5)
- [x] Every helper/serializer consumer grouped by domain (§6)
- [x] Log4j / log4j2 noise listed (§7)
- [x] Wire-contract risks documented (§8)
- [x] Child 4 proof checklist present (§10)
- [x] Links to parent **#505** and slices **#1822–#1824**

---

## 11. Residual gaps / non-goals

|                                      Item                                       |                            Disposition                             |
|---------------------------------------------------------------------------------|--------------------------------------------------------------------|
| Exact on-disk count of every `<null>` payload inside every `.ppkg`              | Out of scope for #1821; sample via package install tests in #1822+ |
| Implementing Jackson mappings                                                   | **#1822 / #1823**                                                  |
| Removing Betwixt from POMs                                                      | **#1824**                                                          |
| Changing domain bean XML shape                                                  | Forbidden without product approval                                 |
| `perc-toolkit` unused `provided` dependency                                     | Confirmed no Java imports; still remove in #1824                   |
| Jackson groupId mismatch in epic #505 body (`com.fasterxml` vs `tools.jackson`) | Documented here; align in #1822                                    |

No residual GitHub issues filed from this inventory pass: sibling slices already exist.

---

## 12. Verification commands used for this freeze

```text
rg -n "commons-betwixt|betwixt" --glob "**/pom.xml"
rg -n "org\\.apache\\.commons\\.betwixt" --glob "**/*.{java,xml,properties}"
rg -l "PSXmlSerializationHelper|PSObjectSerializer|PSBetwixtObjectConverter|IPSXmlSerialization" --glob "**/*.java"
# 70 files (2026-08-04)
Get-ChildItem -Recurse -Filter "*.betwixt" | Where-Object FullName -notmatch '\\target\\'
rg -n "org\\.apache\\.commons\\.betwixt|betwixt" --glob "**/*.{xml,properties,md}"
```

Re-run the above before closing #1824 to confirm the inventory is still complete if
main has moved; amend this doc if new call sites appear.
