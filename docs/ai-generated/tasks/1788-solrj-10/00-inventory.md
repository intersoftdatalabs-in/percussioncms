# Issue #1996 — SolrJ call-site + dependency inventory vs Solr 10

> **Status:** Frozen inventory for parent epic **#1788** (*chore(deps): upgrade Apache
> Solr / SolrJ from 9.10.1 to 10.x*). **No `solr.version` bump** and **no production
> Java changes** in this slice.
>
> **Last refreshed:** 2026-08-05 (agent pass on `main` for issue **#1996** / slice 1).
>
> **Official upgrade notes:**
> [Major Changes in Solr 10](https://solr.apache.org/guide/solr/latest/upgrade-notes/major-changes-in-solr-10.html)
>
> **Sibling slices (do not implement here):**
>
> |  Slice   | Issue |                                     Scope                                      |
> |----------|-------|--------------------------------------------------------------------------------|
> | 1 (this) | #1996 | Call-site + POM inventory vs Solr 10 major notes (docs only)                   |
> | 2        | #1997 | SolrJ 10 compile cutover + unit tests (`system` / metadata primary)            |
> | 3        | #1998 | Explicit Maven modules / packaging for non-transitive SolrJ bits               |
> | 4        | #1999 | Metadata Solr verification plan (mock/unit first; live optional / human-gated) |
> | 5        | #2000 | Docs + Dependabot **#1777** disposition + real `solr.version` bump PR closure  |

---

## 1. Why this doc exists

Percussion CMS ships **SolrJ 9.10.1** (`solr.version` in root `pom.xml`) and uses it only
for **optional metadata delivery to an external Solr** during publish (not as an embedded
Solr server). Solr / SolrJ **10.x** removes Apache-HttpClient-based clients, moves Jetty
clients into `solr-solrj-jetty`, stops pulling optional modules transitively, deprecates
ZK-host `CloudSolrClient` construction, and requires **Tika Server** for
`/update/extract`.

Dependabot PR **#1777** is a version-only bump and **must not merge**. Real cutover is
slices **#1997–#2000**. This inventory is the implementer map for those slices.

### Hard bans (all 1788 children)

- No bare `solr.version` bump without slices 2–3 code/POM work
- No production config secrets / live cluster work unattended
- No Java 8 / `percussioncms-java8` work
- Prefer mock/unit over live Solr in #1999

---

## 2. Method (reproducible)

Run from repo root on the branch under review (Windows / Unix paths via tools, not shell
string concat):

1. **Property + POM edges**
   - Grep `**/pom.xml` for `solr.version`, `solr-solrj`, `solr-solrj-zookeeper`,
     `zookeeper` enforcer bans.
2. **Java call sites**
   - Grep full tree for `org.apache.solr` imports and symbols:
     `HttpSolrClient`, `CloudSolrClient`, `ContentStreamUpdateRequest`,
     `SolrInputDocument`, `setUseMultiPartPost`.
3. **Product package surface**
   - List `system/business/src/com/percussion/delivery/metadata/solr/**`.
4. **Publish wiring**
   - Grep `PSSolrDeliveryHandler` / `sendMetadataToSolr` / `solr.commit` /
     `solr.delete` from `PSMetadataDeliveryHandler`.
5. **Map** each API to Solr 10 major-change notes (see §5–§6).

**Scan date:** 2026-08-05. **Pin at scan:** `solr.version=9.10.1`.

---

## 3. Maven / packaging edges

### 3.1 Direct / managed dependencies

|               Location                |                      Edge type                       |                                                       Details                                                       |
|---------------------------------------|------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| Root `pom.xml` property               | Version pin                                          | `<solr.version>9.10.1</solr.version>`                                                                               |
| Root `pom.xml` `dependencyManagement` | Managed `org.apache.solr:solr-solrj:${solr.version}` | Excludes ZK optional stack + several Jetty ALPN/client bits + Netty buffer + `jcl-over-slf4j` (see §3.2)            |
| Root `pom.xml` properties / comments  | ZK floor (dormant)                                   | `zookeeper.version=3.9.5` managed for **if** CloudSolrClient+ZK is re-enabled; **not** on runtime tree today        |
| Root enforcer `bannedDependencies`    | Hard ban                                             | `org.apache.zookeeper:zookeeper` and `zookeeper-jute` banned with `searchTransitive=true` (issue **#1673**)         |
| `system/pom.xml`                      | Direct, **optional**                                 | `org.apache.solr:solr-solrj` (`<optional>true</optional>`) — primary runtime consumer                               |
| `WebUI/pom.xml`                       | Direct compile                                       | `org.apache.solr:solr-solrj` — **no** `org.apache.solr` Java imports under WebUI (packaging / historical edge only) |

No other child `pom.xml` declares `solr-solrj` or `org.apache.solr` artifacts.

### 3.2 Root `solr-solrj` exclusions (current)

From root `dependencyManagement` (comments reference issue **#1673**):

|            Excluded coordinate             |          Rationale today          |                                                               Solr 10 relevance                                                               |
|--------------------------------------------|-----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `org.apache.solr:solr-solrj-zookeeper`     | Optional Cloud ZK client off tree | In Solr 10 optional modules are **already non-transitive**; if Cloud+ZK path is ever productized, must **add this module explicitly** (#1998) |
| `org.apache.zookeeper:zookeeper`           | Keep ZK off tree                  | Same; also enforcer-banned                                                                                                                    |
| `org.apache.zookeeper:zookeeper-jute`      | Keep ZK off tree                  | Same                                                                                                                                          |
| `io.netty:netty-buffer`                    | Avoid Netty pull                  | Revisit if SolrJ 10 transitively needs Netty again                                                                                            |
| `org.eclipse.jetty:jetty-alpn-client`      | Shrink classpath                  | **Conflicts with Solr 10 Jetty client path** — `solr-solrj-jetty` will need Jetty client jars (#1998)                                         |
| `org.eclipse.jetty:jetty-alpn-java-client` | Same                              | Same                                                                                                                                          |
| `org.eclipse.jetty:jetty-client`           | Same                              | Same                                                                                                                                          |
| `org.eclipse.jetty:jetty-http`             | Same                              | Same                                                                                                                                          |
| `org.eclipse.jetty:jetty-io`               | Same                              | Same                                                                                                                                          |
| `org.eclipse.jetty:jetty-util`             | Same                              | Same                                                                                                                                          |
| `org.slf4j:jcl-over-slf4j`                 | Logging bridge hygiene            | Keep unless SolrJ 10 requires it                                                                                                              |

**Policy tension (document for #1997/#1998):** root comments say *“Product uses HTTP SolrJ only — no CloudSolrClient”*, yet production code **compiles and can construct** `CloudSolrClient` when `serverCloudType=true` (§4.1). With ZK excluded, a ZK-host Cloud build is **not** a supported runtime configuration on current classpath. Default config path is standalone `HttpSolrClient`.

### 3.3 Modules / trees with **zero** `org.apache.solr` sources

Verified by full-tree import grep:

- `deliverytiersuite/**` (DTS) — metadata Solr is **system-side** publish delivery, not DTS
- `rest/`, `projects/sitemanage/`, `deployer/`, `modules/**` (except none under solr packages)
- WebUI TypeScript / JSP — no Solr client usage found

### 3.4 Product Tika vs Solr extract Tika

|               Location               |                   Artifact                    |                                    Role                                    |
|--------------------------------------|-----------------------------------------------|----------------------------------------------------------------------------|
| Root `tika.version` / system + WebUI | `tika-core` / `tika-parsers-standard-package` | **CMS-local** content/metadata extraction — **not** Solr’s extract backend |
| Solr server (customer-operated)      | ExtractingRequestHandler                      | Receives `ContentStreamUpdateRequest("/update/extract")` from product      |

Solr 10 removes local Tika inside Solr; customer Solr must run **Tika Server** (`tikaserver.url`). Product’s own Tika JARs do **not** substitute for that server-side requirement.

---

## 4. Product call-site inventory

### 4.1 Sole SolrJ importer: `PSSolrDeliveryHandler`

**Path:**
`system/business/src/com/percussion/delivery/metadata/solr/impl/PSSolrDeliveryHandler.java`

**Package:** `com.percussion.delivery.metadata.solr.impl`

|              Solr type / API               |                  Import / usage site                  |                                                    Role in product                                                     |
|--------------------------------------------|-------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| `SolrClient`                               | Field `solrClient`; return of `getClient()`           | Abstraction for add/delete/commit/rollback/request                                                                     |
| `SolrServerException`                      | Catch blocks                                          | Delivery error → `PSDeliveryException`                                                                                 |
| `HttpSolrClient`                           | `getClient()` non-cloud branch                        | **Default** client: `new HttpSolrClient.Builder(solrHost).build()`                                                     |
| `HttpSolrClient#setUseMultiPartPost(true)` | Immediately after Builder.build()                     | Multipart posts for extract / file upload path                                                                         |
| `CloudSolrClient`                          | `getClient()` when `serverConfig.isServerCloudType()` | `new CloudSolrClient.Builder(List.of(solrHost)).build()` then `setDefaultCollection(...)`                              |
| `ContentStreamUpdateRequest`               | `sendFile(...)`                                       | `new ContentStreamUpdateRequest("/update/extract")` + `setParam("literal.*")` + `addFile(...)` + `client.request(req)` |
| `UpdateResponse`                           | `sendMetadata(...)`                                   | Result of `client.add(doc)`                                                                                            |
| `SolrException`                            | Catch blocks                                          | Error counting via `solrConfig.incrError()`                                                                            |
| `SolrInputDocument`                        | `sendMetadata(...)`                                   | Page metadata fields; `doc.addField("id", path)` + mapped props                                                        |
| `NamedList`                                | `sendFile(...)`                                       | Extract request result logging                                                                                         |

#### Client construction (verbatim shape)

```java
// Cloud branch (serverCloudType == true)
CloudSolrClient cloudClient =
    new CloudSolrClient.Builder(List.of(serverConfig.getSolrHost())).build();
cloudClient.setDefaultCollection(serverConfig.getDefaultCollection());

// Standalone branch (default)
HttpSolrClient httpSolrClient =
    new HttpSolrClient.Builder(serverConfig.getSolrHost()).build();
httpSolrClient.setUseMultiPartPost(true);
```

Notes for cutover:

- `List.of(solrHost)` is the **ZK-hosts / “list of strings”** Builder overload historically used for ZooKeeper ensemble or host lists — **not** the modern “Solr URL list” style preferred in Solr 10.
- `setDefaultCollection` is mutable post-build; Solr 10 prefers builder `withDefaultCollection` and root base URLs ending in `/solr`.
- SASL: cloud branch toggles system properties `zookeeper.sasl.client` and `zookeeper.sasl.clientconfig` — only meaningful if ZK client is on classpath (#1673 currently forbids that).

#### SolrJ operations used

|                Method                 |        Call path         |                            Trigger                            |
|---------------------------------------|--------------------------|---------------------------------------------------------------|
| `deleteByQuery("*:*")`                | `deleteAllSolrEntries()` | Constructor + `forceSolrClean` + `cleanAllOnFullPublish`      |
| `add(SolrInputDocument)`              | `sendMetadata(...)`      | Page-type metadata publish                                    |
| `request(ContentStreamUpdateRequest)` | `sendFile(...)`          | Non-page assets **and** after page `add` (pages also extract) |
| `deleteById(path)`                    | `delete(path)`           | Publish removal via Worker                                    |
| `commit()`                            | `commit()`               | Worker.close() end of job                                     |
| `rollback()`                          | `rollback()`             | Error paths / clean failure                                   |

`try-with-resources` on `commit()` / `rollback()` closes the client and nulls `serverConfig`.

### 4.2 Config model (no SolrJ imports)

|     Class / artifact      |                      Path                       |                                                                                         Role                                                                                         |
|---------------------------|-------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SolrServer`              | `.../solr/impl/SolrServer.java`                 | JAXB DTO: `solrHost`, `defaultCollection`, `serverCloudType` (default **false**), `saslContextName`, `maxErrors`, `cleanAllOnFullPublish`, metadata map, enabled sites               |
| `PSSolrConfig`            | `.../solr/impl/PSSolrConfig.java`               | Root JAXB: list of `SolrServer`                                                                                                                                                      |
| `SolrMetaMapEntry`        | `.../solr/impl/SolrMetaMapEntry.java`           | Map entry key/value for metadata rename                                                                                                                                              |
| `SolrConfigLoader`        | `.../solr/impl/SolrConfigLoader.java`           | Loads `rxconfig/DeliveryServer/solr-servers.xml` via `PSServer.getRxDir()` + `PSSerializerUtils`                                                                                     |
| `PSSolrConfig.xsd`        | same package                                    | Schema includes `serverCloudType`, `defaultCollection`, etc.                                                                                                                         |
| `PSSolrDeliveryException` | same package                                    | Runtime exception type (not wired as primary throw from handler — handler uses `PSDeliveryException`)                                                                                |
| `IPSSolrDeliveryService`  | `.../metadata/solr/IPSSolrDeliveryService.java` | Thin interface `sendMetadataToSolr(IPSMetadataEntry)` — **not** implemented by `PSSolrDeliveryHandler` today (handler is concrete; different method signature with path + temp file) |

Config path is portable via `File` + `PSServer.getRxDir()` (product rx dir), not hardcoded OS roots.

### 4.3 Publish integration (no SolrJ imports)

|            Class            |                                         Path                                         |                                                                               Wiring                                                                               |
|-----------------------------|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PSMetadataDeliveryHandler` | `system/business/src/com/percussion/rx/delivery/impl/PSMetadataDeliveryHandler.java` | Creates `PSSolrDeliveryHandler` in `prepareForDelivery`; `Worker.postSolr` → `sendMetadataToSolr`; `Worker.delete` → `solr.delete`; `Worker.close` → `solr.commit` |

This is the only production caller of the handler. DTS indexer HTTP path is separate (`SERVICE_INDEXER` + `PSDeliveryClient`) and does **not** use SolrJ.

### 4.4 Tests

|          Test          |                                            Path                                             |                                                                 Coverage                                                                  |
|------------------------|---------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `SolrConfigLoaderTest` | `system/src/test/java/com/percussion/delivery/metadata/solr/impl/SolrConfigLoaderTest.java` | **Config-only**: marshal/unmarshal `PSSolrConfig` XML; asserts `cleanAllOnFullPublish`. **No** `HttpSolrClient` / live Solr / mock client |

**Gap for #1997 / #1999:** no unit tests exercise `getClient()`, extract path, or commit/delete APIs. Slice 2 should introduce mock/`SolrClient` tests before version bump.

### 4.5 Full `org.apache.solr` import census

|             File             |     Count of `org.apache.solr.*` imports     |
|------------------------------|----------------------------------------------|
| `PSSolrDeliveryHandler.java` | **9** (only production importer in monorepo) |
| All other `*.java`           | **0**                                        |

POM-only references: root, `system`, `WebUI` (see §3).

---

## 5. Solr 10 major changes → product impact

Source: [Major Changes in Solr 10](https://solr.apache.org/guide/solr/latest/upgrade-notes/major-changes-in-solr-10.html) (SolrJ section + extract/Tika removals). Product JDK is **21** (SolrJ 10 requires ≥17; Solr server 10 requires ≥21) — **no JDK blocker** on this product line.

|                               Solr 10 change                                |        Hits product?         |                                     Evidence                                      |                                                          Cutover implication                                                           |
|-----------------------------------------------------------------------------|------------------------------|-----------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| Remove Apache HttpClient–based `SolrClient` implementations                 | **Yes — hard compile break** | `HttpSolrClient` import + Builder + `setUseMultiPartPost`                         | Replace with Jetty client (`HttpJettySolrClient` / `solr-solrj-jetty`) or JDK HttpClient path if available; re-home multipart behavior |
| Jetty classes → `org.apache.solr.solrj.jetty` + artifact `solr-solrj-jetty` | **Yes**                      | Current HttpClient client gone; Jetty jars **excluded** from managed `solr-solrj` | #1998 must add explicit module and reverse/narrow Jetty exclusions                                                                     |
| `Http2SolrClient` → `HttpJettySolrClient` renames                           | Indirect                     | Product never used Http2* types                                                   | Use new names if adopting Jetty path                                                                                                   |
| `CloudSolrClient` ZK-host constructor deprecated (prefer Solr URLs)         | **Yes (cloud branch)**       | `Builder(List.of(solrHost))`                                                      | Prefer Solr base URLs + `withDefaultCollection`; document config migration for operators using ZK hosts                                |
| Base URL must be root `/solr`; collection via default collection            | **Likely**                   | `solrHost` free-form string from XML                                              | Validate operator configs; builder rules may reject collection-in-path URLs                                                            |
| Optional modules not transitive from `solr-solrj` POM                       | **Yes**                      | Already partially true via our exclusions; Solr 10 hardens this                   | Explicit deps for jetty (and ZK only if cloud path kept) in #1998                                                                      |
| `SolrQuery` package move                                                    | **No**                       | No `SolrQuery` usage                                                              | —                                                                                                                                      |
| Binary* → JavaBin* renames                                                  | **No**                       | No direct Binary parser usage                                                     | —                                                                                                                                      |
| `LocalTikaExtractionBackend` removed; **Tika Server only** for extract      | **Yes — runtime/ops**        | Product posts `/update/extract`                                                   | Customer Solr 10 must configure `tikaserver.url`; product code may still send extract requests but indexing fails without Tika Server  |
| `TikaLanguageIdentifierUpdateProcessor` removed                             | **No** (server config)       | Not product Java                                                                  | Document for operators with custom solrconfig                                                                                          |
| System req: SolrJ ≥ Java 17                                                 | **OK**                       | Product Java 21                                                                   | —                                                                                                                                      |
| Rolling downgrade guard (9.10+)                                             | Ops only                     | External clusters                                                                 | Operator note in #2000                                                                                                                 |

---

## 6. Cutover matrix (API → Solr 10 replacement → risk → module)

Risk: **H** = compile/runtime break without code change; **M** = works with config/docs only or feature-flagged path; **L** = no product code change expected.

| #  |                           API / edge in use (SolrJ 9.10.1)                            |                                                                   Solr 10 replacement / action                                                                   |            Risk            |                  Owning module / slice                   |
|----|---------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|----------------------------------------------------------|
| 1  | `org.apache.solr.client.solrj.impl.HttpSolrClient`                                    | Remove; use `HttpJettySolrClient` (`org.apache.solr.solrj.jetty` / package per Solr 10 guide) **or** builder that selects JDK `HttpClient`                       | **H**                      | `system` — **#1997**                                     |
| 2  | `new HttpSolrClient.Builder(String baseUrl)`                                          | Jetty/JDK client Builder with root URL ending `/solr`; set default collection if needed                                                                          | **H**                      | `system` — **#1997**                                     |
| 3  | `httpSolrClient.setUseMultiPartPost(true)`                                            | No direct 1:1 on removed class — confirm Jetty client multipart defaults or equivalent setter/request options; extract path **requires** multipart-capable posts | **H**                      | `system` — **#1997**                                     |
| 4  | `CloudSolrClient.Builder(List.of(host))`                                              | Prefer `CloudSolrClient` Builder with **Solr URLs** (not ZK hosts); ZK constructor deprecated through Solr 11 horizon                                            | **M/H**                    | `system` — **#1997**; ops docs **#2000**                 |
| 5  | `cloudClient.setDefaultCollection(name)`                                              | Prefer `withDefaultCollection` on Builder; keep setter if still present                                                                                          | **L/M**                    | `system` — **#1997**                                     |
| 6  | Cloud + ZK SASL system properties                                                     | Only if product re-enables ZK client (#1673 reverse). Else dead code / document “unsupported while ban holds”                                                    | **M**                      | Policy + **#1998**                                       |
| 7  | `ContentStreamUpdateRequest("/update/extract")`                                       | Client API likely retained; **server** must use Tika Server backend                                                                                              | **H** (ops) / **M** (code) | Ops **#2000** + verify **#1999**; code keep in **#1997** |
| 8  | `req.addFile` + `literal.*` params                                                    | Expect retention; re-test against Solr 10 extract handler                                                                                                        | **M**                      | **#1997** / **#1999**                                    |
| 9  | `SolrInputDocument` / `addField`                                                      | Expect retention                                                                                                                                                 | **L**                      | **#1997** smoke                                          |
| 10 | `SolrClient.add` / `deleteById` / `deleteByQuery` / `commit` / `rollback` / `request` | Core API surface — expect retention                                                                                                                              | **L**                      | **#1997**                                                |
| 11 | `UpdateResponse`, `NamedList`, `SolrException`, `SolrServerException`                 | Expect retention (package moves for unrelated types only)                                                                                                        | **L**                      | **#1997**                                                |
| 12 | Managed `solr-solrj` only                                                             | Add **`solr-solrj-jetty`** (and drop conflicting Jetty exclusions)                                                                                               | **H**                      | root + `system` (+ WebUI if still needed) — **#1998**    |
| 13 | Exclude `solr-solrj-zookeeper` + enforcer ban ZK                                      | Keep for HTTP-only policy **or** document intentional Cloud+ZK re-enable                                                                                         | **M**                      | **#1998** / policy                                       |
| 14 | `system` optional + `WebUI` direct dep                                                | Confirm WebUI still needs the JAR (no imports today — candidate to drop in packaging cleanup)                                                                    | **L**                      | **#1998** / **#2000**                                    |
| 15 | `solr.version` property `9.10.1`                                                      | Bump only after #1997+#1998 green                                                                                                                                | **H** if early             | **#2000** (close #1777)                                  |
| 16 | No client unit tests                                                                  | Add mock `SolrClient` tests for add/delete/commit/extract construction                                                                                           | **H** (quality gate)       | **#1997** / **#1999**                                    |
| 17 | Customer `solr-servers.xml` `solrHost` shapes                                         | Document allowed URL forms (root `/solr`, cloud URL list vs ZK)                                                                                                  | **M**                      | **#2000**                                                |
| 18 | Customer Solr extract + local Tika                                                    | Require Tika Server URL on Solr 10                                                                                                                               | **H** (ops)                | **#2000** release notes                                  |

### Recommended implementer order (already reflected in child issues)

1. **#1997** — Replace `HttpSolrClient` construction; adjust Cloud builder; compile on a feature branch that temporarily uses Solr 10 deps **or** dual-compile helpers; add unit tests with mocked `SolrClient`.
2. **#1998** — Explicit `solr-solrj-jetty` (and any other non-transitive bits); fix Jetty exclusions; decide Cloud+ZK keep/kill (recommend **kill or URL-only Cloud** while #1673 ban remains).
3. **#1999** — Verification plan: unit/mocks first; optional human-gated live Solr 10 + Tika Server.
4. **#2000** — Bump `solr.version`, close/supersede Dependabot #1777, operator notes (extract/Tika Server, URL shapes).

---

## 7. Policy notes / discrepancies for implementers

1. **POM vs code on CloudSolrClient**  
   Root comments claim HTTP-only / no CloudSolrClient. Code still constructs Cloud clients when `serverCloudType` is true. Inventory treats **standalone HttpSolrClient as primary production path** and Cloud as **secondary / currently classpath-impaired** (ZK module excluded). Slice 2 should either:
   - migrate Cloud to **Solr URL** builder (no ZK module), or
   - remove/disable Cloud branch until ZK policy is reversed.
2. **Jetty exclusions vs Solr 10 Jetty client**  
   Current depMgmt excludes Jetty client artifacts from `solr-solrj`. Solr 10’s recommended replacement **is** Jetty-based. #1998 must reconcile exclusions with the new module rather than copy-pasting 9.x exclusion lists.
3. **WebUI dependency without imports**  
   Safe packaging cleanup candidate after #1997 proves `system` is the only runtime need — do not drop in slice 1.
4. **`IPSSolrDeliveryService`**  
   Orphan interface relative to handler signatures; not a Solr 10 concern. Out of scope unless slice 2 refactors for testability.
5. **Secrets**  
   Config may contain host URLs and SASL context **names** only in this inventory; never copy operator secrets into docs or tests.

---

## 8. Acceptance checklist (#1996)

- [x] Complete call-site inventory (`PSSolrDeliveryHandler` + config + publish wiring + tests)
- [x] Complete POM / enforcer / exclusion inventory (root, system, WebUI)
- [x] Full-tree confirmation: only one Java file imports `org.apache.solr.*`
- [x] Map each site to Solr 10 major changes (HttpClient removal, Jetty module, Cloud builder, optional modules, Tika Server extract)
- [x] Cutover matrix with risk + owning module/slice for #1997–#2000
- [x] **No** `solr.version` bump; **no** production Java change; **no** secrets

---

## 9. Links

|              Item               |                                         URL / path                                          |
|---------------------------------|---------------------------------------------------------------------------------------------|
| Parent epic                     | https://github.com/intersoftdatalabs-in/percussioncms/issues/1788                           |
| This slice                      | https://github.com/intersoftdatalabs-in/percussioncms/issues/1996                           |
| Compile cutover                 | https://github.com/intersoftdatalabs-in/percussioncms/issues/1997                           |
| Maven modules                   | https://github.com/intersoftdatalabs-in/percussioncms/issues/1998                           |
| Verification plan               | https://github.com/intersoftdatalabs-in/percussioncms/issues/1999                           |
| Version bump / docs             | https://github.com/intersoftdatalabs-in/percussioncms/issues/2000                           |
| Dependabot (do not merge alone) | https://github.com/intersoftdatalabs-in/percussioncms/pull/1777                             |
| Solr 10 major changes           | https://solr.apache.org/guide/solr/latest/upgrade-notes/major-changes-in-solr-10.html       |
| Primary handler                 | `system/business/src/com/percussion/delivery/metadata/solr/impl/PSSolrDeliveryHandler.java` |
| Root pin                        | `pom.xml` → `solr.version`                                                                  |

