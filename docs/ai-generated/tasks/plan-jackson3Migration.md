# Plan: Migrate to Jackson 3

**Tracking issue:** [#1706](https://github.com/intersoftdatalabs-in/percussioncms/issues/1706) — `[8.2] Migrate runtime Jackson 2.x → Jackson 3.x (LTS)`  
**Branch (WIP):** `feature/1706-jackson3-migration`

Migrate Percussion CMS from Jackson 2.x to Jackson 3.1 (LTS). This involves changing Maven groupId (`com.fasterxml.jackson` → `tools.jackson`), Java package imports, converting ObjectMapper to immutable builder pattern, updating renamed classes/methods, removing embedded Java 8 modules, and adjusting for changed defaults. An [OpenRewrite recipe](https://docs.openrewrite.org/recipes/java/jackson/upgradejackson_2_3) exists to automate much of the mechanical work.

**Status (2026-07-31):** Runtime migration to Jackson **3.1.5** implemented on branch `feature/1706-jackson3-migration` (single PR). Path C OpenAPI isolation remains the Swagger gate completed earlier.

## Current State (post-migration)

- Jackson **3.1.5** (`tools.jackson.*`) across the monorepo; `jackson-annotations` stays **2.21** on `com.fasterxml.jackson.core` by Jackson 3 design
- Java 8 modules (`jackson-datatype-jdk8` / `jsr310` / `parameter-names`) removed — built into databind
- 100+ files with Jackson imports, 150+ annotation usages
- 10 Jackson Maven artifacts declared
- 50+ direct `new ObjectMapper()` instantiations
- 3 custom serializers/deserializers extending `JsonSerializer`/`JsonDeserializer`
- 3 `ContextResolver` implementations configuring ObjectMapper
- 5 JAX-RS Application classes registering Jackson providers
- 3 Java 8 modules explicitly registered (JavaTimeModule, Jdk8Module, ParameterNamesModule)
- JDK 21 already in use (exceeds Jackson 3 minimum of JDK 17)

## Target: Jackson 3.1.x (LTS)

3.0 is transitional; 3.1 is the first LTS with ~2 year support window. Start with 3.0.x if 3.1 is not yet available and upgrade.

## Blocking Gate: Swagger Compatibility

Swagger (`io.swagger.core.v3:swagger-jaxrs2*`) currently does not have a safe Jackson 3 path for this codebase. Treat this as a hard go/no-go gate before dependency migration.

Required decision before Phase 2:

1. Confirm whether Swagger can run with Jackson 3 in this stack.
2. If not, choose one path explicitly:
   - **Path A (recommended):** Delay full Jackson 3 runtime migration until Swagger stack is replaced or upgraded.
   - **Path B:** Run Jackson 3 for application JSON serialization, but disable Swagger runtime integration (`swagger-jaxrs2*`) and keep only annotation artifacts used for source metadata.
   - **Path C:** Move OpenAPI generation to a separate build/runtime path isolated from the main app runtime.
3. Create an issue for the chosen path with acceptance criteria and rollback conditions.

Exit criteria for this gate:

1. No unresolved dependency conflicts between Swagger artifacts and Jackson 3 artifacts.
2. No mixed Jackson major versions in one runtime classloader.
3. OpenAPI/Swagger behavior is either preserved or explicitly and temporarily disabled with documented sign-off.

## Path C Blueprint: Dedicated OpenAPI Web App

Goal: isolate OpenAPI/Swagger delivery from the main `Rhythmyx` runtime so Jackson 3 migration in the main app is not blocked by Swagger runtime constraints.

Recommended design:

1. Create a new WAR module (example: `openapi-webapp`) deployed as a separate context (example: `/openapi`).
2. Keep Swagger runtime integration out of `Rhythmyx` (`/rest`) and out of `rest` runtime classpath.
3. Keep `swagger-annotations` in `rest` source only (for API model/resource metadata), but move `swagger-jaxrs2*` runtime concerns to the new module or remove them entirely.
4. Prefer build-time spec generation for Path C:
   - Generate `openapi.json` during build from `rest` module annotations.
   - Package generated `openapi.json` + Swagger UI static assets in `openapi-webapp`.
   - Serve docs from `/openapi/index.html` and spec from `/openapi/openapi.json`.
5. Update CMS UI links from `/rest/api-docs?url=/rest/openapi.json...` to `/openapi/index.html?url=/openapi/openapi.json...`.
6. Keep auth and exposure controls explicit (admin-only or internal-only) for `/openapi/*` endpoints.

Why this is safer:

1. Separate WARs can run with separate dependency sets/classloaders.
2. Main app can move to Jackson 3 without requiring Swagger runtime compatibility in the same classloader.
3. OpenAPI docs availability remains intact.

Path C constraints and checks:

1. Do not place Swagger/Jackson jars in container-shared lib locations; keep them webapp-scoped.
2. Ensure no shared Spring/CXF config in `Rhythmyx` still registers `openApiFeature` for `/rest` once cutover is complete.
3. Ensure security parity with existing `PSRestApiAuthFilter` behavior for docs access.
4. Add deployment validation that both webapps start and docs load without pulling Swagger runtime jars into `Rhythmyx`.

## Immediate Execution Checklist (Path C)

Completed:

- [x] Added `openapi-webapp` WAR module scaffold.
- [x] Added static `index.html` and placeholder `openapi.json`.
- [x] Registered `openapi-webapp` in the root reactor `pom.xml`.
- [x] Documented Path C architecture and constraints in this plan.
- [x] Removed constructor-time GUID generation side effects from filter entities to prevent Hibernate bootstrap circular initialization.
- [x] Added build-time OpenAPI spec generation to openapi-webapp pom.xml (springdoc-openapi-maven-plugin).
- [x] Removed swagger-jaxrs2* and swagger-ui WebJars from rest, system, and WebUI pom.xml.
- [x] Kept swagger-annotations in rest, system, and WebUI for source-level API documentation support.
- [x] Removed cxf-rt-rs-service-description-openapi-v3 dependency from projects/sitemanage pom.xml.
- [x] Removed openApiFeature bean definition from sitemanage-beans.xml.
- [x] Removed openApiFeature registration from rest-jax-rs server jaxrs:features in sitemanage-beans.xml.
- [x] Updated PSRestApiAuthFilter to remove /rest/openapi.json from allowlist.
- [x] Updated WebUI Swagger UI HTML files to reference /openapi/openapi.json instead of /rest/api-docs.

Pending next steps:

- [ ] Generate JAX-RS OpenAPI spec build-time (pending tool selection: CXF OpenAPI Maven plugin or custom Java annotation processor)
- [ ] Verify Maven build succeeds without dependency conflicts.
- [ ] Deploy openapi-webapp and test Swagger UI serves at /openapi.
- [ ] Verify Rhythmyx WEB-INF/lib does NOT contain swagger-jaxrs2* or swagger-ui artifacts.
- [ ] Test REST API functionality with Swagger/OpenAPI integration fully decoupled.

**Note on OpenAPI Spec Generation:**

- Currently using static `openapi.json` placeholder in `openapi-webapp/src/main/webapp/`
- Will be replaced with build-time generated spec once suitable JAX-RS-compatible tool is selected
- Candidates: CXF OpenAPI Maven plugin, custom annotation processor, or swagger-maven-plugin

---

## Phase 1: Preparation & Tooling Setup

1. Create a feature branch from `development` (e.g. `feature/ISSUE-jackson3-migration`) — create GitHub issue first
2. Evaluate OpenRewrite recipe (`upgradejackson_2_3`) — add OpenRewrite Maven plugin temporarily, run in dry-run mode to assess scope
3. Identify and fix deprecated 2.x API usage first on Jackson 2.x (cleaner migration path) — Jackson 2.20 Javadocs indicate replacements
4. Complete the Swagger compatibility gate before Phase 2 (see "Blocking Gate: Swagger Compatibility")

## Phase 2: Maven Dependency Migration

1. Update parent `pom.xml` version properties (~lines 129-135) — replace `jackson.version` with 3.x version, remove `jackson.annotation.api.version` and `jakarta.jackson.version`
2. Add `tools.jackson:jackson-bom:3.x.x` as BOM import in parent `pom.xml` dependencyManagement (~lines 1648-1710)
3. Update groupIds (`com.fasterxml.jackson.*` → `tools.jackson.*`) in all pom.xml files — **EXCEPT** `jackson-annotations` stays at `com.fasterxml.jackson.core`. *Parallel with step 4-5*
4. Remove Java 8 module dependencies: `jackson-datatype-jsr310`, `jackson-datatype-jdk8`, `jackson-module-parameter-names` (built into 3.x databind). *Parallel with step 3*
5. Update/remove JAX-RS provider artifacts to 3.x equivalents. Fix version mismatch in `rest/pom.xml` line 162. *Parallel with step 3*

## Phase 3: Java Package Import Migration

1. Bulk rename imports: `com.fasterxml.jackson.core/databind/dataformat/module` → `tools.jackson.*` — **EXCEPT** `com.fasterxml.jackson.annotation.*` stays unchanged. Annotations within `jackson-databind` (like `@JsonSerialize`) DO move to `tools.jackson.databind.annotation`. OpenRewrite automates this. *Depends on Phase 2*

## Phase 4: Class & Method Renames

1. Rename custom serializer/deserializer base classes. *Depends on Phase 3*
   - `JsonSerializer<T>` → `ValueSerializer<T>` in `PSCustomDateSerializer.java`, `LocalDateSerializer.java`
   - `JsonDeserializer<T>` → `ValueDeserializer<T>` in `LocalDateDeserializer.java`
   - `SerializerProvider` → `SerializationContext` in method signatures
2. Rename exceptions: `JsonProcessingException` → `JacksonException`, `JsonMappingException` → `DatabindException`. Update `PSJsonProcessingExceptionMapper.java` and all catch blocks. *Parallel with step 1*
3. Feature enum moves: `WRITE_DATES_AS_TIMESTAMPS` → `DateTimeFeature`, enum-related features → `EnumFeature`. *Parallel with step 1*
4. Other renames: `JsonStreamContext` → `TokenStreamContext`, `TextNode` → `StringNode`, `JsonToken.FIELD_NAME` → `JsonToken.PROPERTY_NAME`. *Parallel with step 1*

## Phase 5: ObjectMapper Immutability Migration

1. Convert 3 ContextResolver implementations to builder pattern. *Depends on Phase 4*
   - **REST JacksonContextResolver** (`rest/src/main/java/com/percussion/rest/JacksonContextResolver.java`) — `new ObjectMapper()` + `.configure()` → `JsonMapper.builder().enable/disable().build()`
   - **SiteManage JacksonContextResolver** (`projects/sitemanage/src/main/java/com/percussion/sitemanage/json/JacksonContextResolver.java`) — most complex: visibility, annotation introspectors, module registration. Remove `ParameterNamesModule`/`Jdk8Module`/`JavaTimeModule` registrations (built-in). Convert `setSerializationInclusion` → `.changeDefaultPropertyInclusion()`, `setVisibility` → `.changeDefaultVisibility()`
   - **PSJAXBContextResolver** (`projects/sitemanage/src/main/java/com/percussion/category/marshaller/PSJAXBContextResolver.java`) — remove JavaTimeModule, convert to builder
2. Convert 50+ direct `new ObjectMapper()` to `JsonMapper.builder().build()` — prioritize production code, then tests. *Parallel with step 1*
   - Key files: `PSSiteDataService.java` (4 instances), `PSSerializerUtils.java` (2 instances), `PSCloudService.java`

## Phase 6: JAX-RS Provider Migration

1. Update `JacksonXmlBindJsonProvider` references to 3.x equivalent in 5 Application classes. Update JAXB annotation introspector references for 3.x. *Depends on Phase 4*

## Phase 7: Default Configuration Alignment

1. Review and decide on changed defaults. *Depends on Phase 5*

   |             Feature              | 2.x Default | 3.x Default  |        Current Setting        |         Action          |
   |----------------------------------|-------------|--------------|-------------------------------|-------------------------|
   | `FAIL_ON_UNKNOWN_PROPERTIES`     | enabled     | **disabled** | REST: true, SiteManage: false | Keep explicit settings  |
   | `WRITE_DATES_AS_TIMESTAMPS`      | enabled     | **disabled** | Explicitly disabled           | Already aligned         |
   | `FAIL_ON_TRAILING_TOKENS`        | disabled    | **enabled**  | Not set                       | Accept 3.x default      |
   | `FAIL_ON_NULL_FOR_PRIMITIVES`    | disabled    | **enabled**  | Not set                       | Test for breakage       |
   | `SORT_PROPERTIES_ALPHABETICALLY` | disabled    | **enabled**  | Not set                       | May break brittle tests |
   | `READ_ENUMS_USING_TO_STRING`     | disabled    | **enabled**  | Not set                       | Assess impact           |

   Use `JsonMapper.builderWithJackson2Defaults()` as transitional tool if needed.

## Phase 8: Exception Handling Cleanup

1. Remove `throws JsonProcessingException`/`throws IOException` from signatures where only Jackson was the cause. `JacksonException` is now unchecked (`RuntimeException`). Jackson exceptions no longer extend `IOException` — review catch blocks catching `IOException` for Jackson. *Depends on Phase 4*

## Phase 9: Testing & Validation

1. `./mvnw clean compile -DskipTests` — fix all compilation errors. *Depends on all prior phases*
2. `./mvnw test` — run full test suite, focus on `rest`, `projects/sitemanage`, `deliverytiersuite`, `system`. *Depends on step 1*
3. `./mvnw spotless:apply` then `./mvnw spotless:check` (apply first, check second). *Parallel with step 2*
4. Manual REST API smoke test — verify JSON serialization, date formatting, root wrapping/unwrapping
5. Runtime classpath verification in deployed webapp (`WEB-INF/lib`) — ensure exactly one Jackson major line is present and no `jackson-module-jaxb-annotations` remains unless intentionally pinned for a non-migrated path

---

## Relevant Files

### Critical Configuration Files

- `pom.xml` — Parent POM with all Jackson version properties and dependencyManagement (~lines 129-135, 1648-1710)
- `rest/pom.xml` — REST module Jackson dependencies
- `system/pom.xml` — System module Jackson dependencies
- `WebUI/pom.xml` — WebUI module Jackson dependencies
- `deliverytiersuite/delivery-tier-suite/pom.xml` — DTS Jackson dependencies
- `deliverytiersuite/delivery-tier-suite/membership/pom.xml` — Membership module

### ObjectMapper Configuration (High Priority)

- `rest/src/main/java/com/percussion/rest/JacksonContextResolver.java` — REST ObjectMapper config
- `projects/sitemanage/src/main/java/com/percussion/sitemanage/json/JacksonContextResolver.java` — SiteManage ObjectMapper config (most complex)
- `projects/sitemanage/src/main/java/com/percussion/category/marshaller/PSJAXBContextResolver.java` — JAXB ObjectMapper config

### Custom Serializers (Must Update Base Class)

- `deliverytiersuite/delivery-tier-suite/common/src/main/java/com/percussion/delivery/services/PSCustomDateSerializer.java`
- `projects/sitemanage/src/main/java/com/percussion/category/data/LocalDateSerializer.java`
- `projects/sitemanage/src/main/java/com/percussion/category/data/LocalDateDeserializer.java`

### Exception Handling

- `projects/sitemanage/src/main/java/com/percussion/share/web/service/PSJsonProcessingExceptionMapper.java`

### JAX-RS Applications

- `deliverytiersuite/delivery-tier-suite/metadata/src/main/java/com/percussion/delivery/metadata/PSMetadataApplication.java`
- `deliverytiersuite/delivery-tier-suite/feeds/src/main/java/com/percussion/delivery/feeds/PSFeedsApplication.java`
- `deliverytiersuite/delivery-tier-suite/polls/src/main/java/com/percussion/delivery/polls/PSPollsApplication.java`
- `deliverytiersuite/delivery-tier-suite/comments/src/main/java/com/percussion/delivery/PSCommentsApplication.java`
- `deliverytiersuite/delivery-tier-suite/membership/src/main/java/com/percussion/PSMembershipApplication.java`

### High-Volume ObjectMapper Usage

- `projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSiteDataService.java` (4 instantiations)
- `system/business/src/com/percussion/share/dao/PSSerializerUtils.java` (2 instantiations)
- `projects/sitemanage/src/main/java/com/percussion/cloudservice/impl/PSCloudService.java`

---

## Verification

1. `./mvnw clean compile -DskipTests` — zero compilation errors
2. `./mvnw test` — all existing tests pass
3. `./mvnw spotless:check` — passes
4. `grep -r "com.fasterxml.jackson" --include="*.java" | grep -v annotation` — only annotation imports remain
5. `grep -r "new ObjectMapper()" --include="*.java"` — zero results
6. `./mvnw dependency:tree | grep com.fasterxml.jackson` — only annotations artifact
7. Deployed runtime `WEB-INF/lib` contains no mixed Jackson major versions
8. Manual REST API smoke test for JSON correctness

---

## Decisions

- **Target Jackson 3.1.x** (LTS), skip 3.0 if possible
- **Use OpenRewrite** for mechanical migration (imports, groupIds, class renames)
- **`jackson-annotations` stays at `com.fasterxml.jackson.annotation`** — by Jackson 3 design
- **Remove Java 8 modules** — built into Jackson 3 databind
- **Accept most 3.x defaults** — explicitly override only where current behavior is intentional
- **Do not run mixed Jackson major versions in one runtime classloader**
- **Swagger compatibility is a mandatory gate, not a best-effort task**
- **Scope includes:** all Java source, all pom.xml, all test files
- **Scope excludes:** JS/TS frontend code, documentation-only changes

## Further Considerations

1. **OpenRewrite recipe completeness** — May not cover all renames (e.g., `SerializerProvider` → `SerializationContext`). Plan for manual fixes after automated pass.
2. **JAX-RS Provider 3.x availability (blocking)** — Verify `jackson-jakarta-rs-json-provider` has a 3.x release under `tools.jackson` groupId and is compatible with required Swagger flow. If not, choose and document Path A/B/C from the blocking gate before proceeding.
3. **JAXB Annotation Module** — `jackson-module-jaxb-annotations` and `jackson-module-jakarta-xmlbind-annotations` need 3.x equivalents. Verify availability — affects annotation introspector pairing in SiteManage.

