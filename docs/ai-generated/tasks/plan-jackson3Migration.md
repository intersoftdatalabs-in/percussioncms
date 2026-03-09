# Plan: Migrate to Jackson 3

Migrate Percussion CMS from Jackson 2.21.1 to Jackson 3.1 (LTS). This involves changing Maven groupId (`com.fasterxml.jackson` → `tools.jackson`), Java package imports, converting ObjectMapper to immutable builder pattern, updating renamed classes/methods, removing embedded Java 8 modules, and adjusting for changed defaults. An [OpenRewrite recipe](https://docs.openrewrite.org/recipes/java/jackson/upgradejackson_2_3) exists to automate much of the mechanical work.

## Current State

- Jackson 2.21.1 across the codebase (annotations 2.21, Jakarta variants 2.20.1)
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

---

## Phase 1: Preparation & Tooling Setup

1. Create a feature branch from `development` (e.g. `feature/ISSUE-jackson3-migration`) — create GitHub issue first
2. Evaluate OpenRewrite recipe (`upgradejackson_2_3`) — add OpenRewrite Maven plugin temporarily, run in dry-run mode to assess scope
3. Identify and fix deprecated 2.x API usage first on Jackson 2.x (cleaner migration path) — Jackson 2.20 Javadocs indicate replacements

## Phase 2: Maven Dependency Migration

4. Update parent `pom.xml` version properties (~lines 129-135) — replace `jackson.version` with 3.x version, remove `jackson.annotation.api.version` and `jakarta.jackson.version`
5. Add `tools.jackson:jackson-bom:3.x.x` as BOM import in parent `pom.xml` dependencyManagement (~lines 1648-1710)
6. Update groupIds (`com.fasterxml.jackson.*` → `tools.jackson.*`) in all pom.xml files — **EXCEPT** `jackson-annotations` stays at `com.fasterxml.jackson.core`. *Parallel with step 7-8*
7. Remove Java 8 module dependencies: `jackson-datatype-jsr310`, `jackson-datatype-jdk8`, `jackson-module-parameter-names` (built into 3.x databind). *Parallel with step 6*
8. Update/remove JAX-RS provider artifacts to 3.x equivalents. Fix version mismatch in `rest/pom.xml` line 162. *Parallel with step 6*

## Phase 3: Java Package Import Migration

9. Bulk rename imports: `com.fasterxml.jackson.core/databind/dataformat/module` → `tools.jackson.*` — **EXCEPT** `com.fasterxml.jackson.annotation.*` stays unchanged. Annotations within `jackson-databind` (like `@JsonSerialize`) DO move to `tools.jackson.databind.annotation`. OpenRewrite automates this. *Depends on Phase 2*

## Phase 4: Class & Method Renames

10. Rename custom serializer/deserializer base classes. *Depends on step 9*
    - `JsonSerializer<T>` → `ValueSerializer<T>` in `PSCustomDateSerializer.java`, `LocalDateSerializer.java`
    - `JsonDeserializer<T>` → `ValueDeserializer<T>` in `LocalDateDeserializer.java`
    - `SerializerProvider` → `SerializationContext` in method signatures
11. Rename exceptions: `JsonProcessingException` → `JacksonException`, `JsonMappingException` → `DatabindException`. Update `PSJsonProcessingExceptionMapper.java` and all catch blocks. *Parallel with step 10*
12. Feature enum moves: `WRITE_DATES_AS_TIMESTAMPS` → `DateTimeFeature`, enum-related features → `EnumFeature`. *Parallel with step 10*
13. Other renames: `JsonStreamContext` → `TokenStreamContext`, `TextNode` → `StringNode`, `JsonToken.FIELD_NAME` → `JsonToken.PROPERTY_NAME`. *Parallel with step 10*

## Phase 5: ObjectMapper Immutability Migration

14. Convert 3 ContextResolver implementations to builder pattern. *Depends on Phase 4*
    - **REST JacksonContextResolver** (`rest/src/main/java/com/percussion/rest/JacksonContextResolver.java`) — `new ObjectMapper()` + `.configure()` → `JsonMapper.builder().enable/disable().build()`
    - **SiteManage JacksonContextResolver** (`projects/sitemanage/src/main/java/com/percussion/sitemanage/json/JacksonContextResolver.java`) — most complex: visibility, annotation introspectors, module registration. Remove `ParameterNamesModule`/`Jdk8Module`/`JavaTimeModule` registrations (built-in). Convert `setSerializationInclusion` → `.changeDefaultPropertyInclusion()`, `setVisibility` → `.changeDefaultVisibility()`
    - **PSJAXBContextResolver** (`projects/sitemanage/src/main/java/com/percussion/category/marshaller/PSJAXBContextResolver.java`) — remove JavaTimeModule, convert to builder
15. Convert 50+ direct `new ObjectMapper()` to `JsonMapper.builder().build()` — prioritize production code, then tests. *Parallel with step 14*
    - Key files: `PSSiteDataService.java` (4 instances), `PSSerializerUtils.java` (2 instances), `PSCloudService.java`

## Phase 6: JAX-RS Provider Migration

16. Update `JacksonXmlBindJsonProvider` references to 3.x equivalent in 5 Application classes. Update JAXB annotation introspector references for 3.x. *Depends on Phase 4*

## Phase 7: Default Configuration Alignment

17. Review and decide on changed defaults. *Depends on step 14*

    | Feature | 2.x Default | 3.x Default | Current Setting | Action |
    |---------|-------------|-------------|-----------------|--------|
    | `FAIL_ON_UNKNOWN_PROPERTIES` | enabled | **disabled** | REST: true, SiteManage: false | Keep explicit settings |
    | `WRITE_DATES_AS_TIMESTAMPS` | enabled | **disabled** | Explicitly disabled | Already aligned |
    | `FAIL_ON_TRAILING_TOKENS` | disabled | **enabled** | Not set | Accept 3.x default |
    | `FAIL_ON_NULL_FOR_PRIMITIVES` | disabled | **enabled** | Not set | Test for breakage |
    | `SORT_PROPERTIES_ALPHABETICALLY` | disabled | **enabled** | Not set | May break brittle tests |
    | `READ_ENUMS_USING_TO_STRING` | disabled | **enabled** | Not set | Assess impact |

    Use `JsonMapper.builderWithJackson2Defaults()` as transitional tool if needed.

## Phase 8: Exception Handling Cleanup

18. Remove `throws JsonProcessingException`/`throws IOException` from signatures where only Jackson was the cause. `JacksonException` is now unchecked (`RuntimeException`). Jackson exceptions no longer extend `IOException` — review catch blocks catching `IOException` for Jackson. *Depends on Phase 4*

## Phase 9: Testing & Validation

19. `./mvn-env.sh clean compile -DskipTests` — fix all compilation errors. *Depends on all prior phases*
20. `./mvn-env.sh test` — run full test suite, focus on `rest`, `projects/sitemanage`, `deliverytiersuite`, `system`. *Depends on step 19*
21. `./mvn-env.sh spotless:check` (and `spotless:apply` if needed). *Parallel with step 20*
22. Manual REST API smoke test — verify JSON serialization, date formatting, root wrapping/unwrapping

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

1. `./mvn-env.sh clean compile -DskipTests` — zero compilation errors
2. `./mvn-env.sh test` — all existing tests pass
3. `./mvn-env.sh spotless:check` — passes
4. `grep -r "com.fasterxml.jackson" --include="*.java" | grep -v annotation` — only annotation imports remain
5. `grep -r "new ObjectMapper()" --include="*.java"` — zero results
6. `./mvn-env.sh dependency:tree | grep com.fasterxml.jackson` — only annotations artifact
7. Manual REST API smoke test for JSON correctness

---

## Decisions

- **Target Jackson 3.1.x** (LTS), skip 3.0 if possible
- **Use OpenRewrite** for mechanical migration (imports, groupIds, class renames)
- **`jackson-annotations` stays at `com.fasterxml.jackson.annotation`** — by Jackson 3 design
- **Remove Java 8 modules** — built into Jackson 3 databind
- **Accept most 3.x defaults** — explicitly override only where current behavior is intentional
- **Scope includes:** all Java source, all pom.xml, all test files
- **Scope excludes:** JS/TS frontend code, documentation-only changes

## Further Considerations

1. **OpenRewrite recipe completeness** — May not cover all renames (e.g., `SerializerProvider` → `SerializationContext`). Plan for manual fixes after automated pass.
2. **JAX-RS Provider 3.x availability** — Verify `jackson-jakarta-rs-json-provider` has a 3.x release under `tools.jackson` groupId. If not, an alternative provider strategy is needed.
3. **JAXB Annotation Module** — `jackson-module-jaxb-annotations` and `jackson-module-jakarta-xmlbind-annotations` need 3.x equivalents. Verify availability — affects annotation introspector pairing in SiteManage.
