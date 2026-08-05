# Issue #1999 — Metadata Solr verification plan (mock/unit first; live optional)

> **Status:** Verification plan for parent epic **#1788** slice 4. **Agent-safe:** structural
> checklist + mock/unit tests. **Live Solr is optional and human-gated.**
>
> **Sibling slices:**
>
> |  Slice   | Issue |                              Scope                              |
> |----------|-------|-----------------------------------------------------------------|
> | 1        | #1996 | Call-site + POM inventory vs Solr 10 (`00-inventory.md`)        |
> | 2        | #1997 | SolrJ 10 compile cutover + client construction unit tests       |
> | 3        | #1998 | Explicit Maven modules / packaging (HTTP-only policy)           |
> | 4 (this) | #1999 | Verification plan + expanded mock/unit coverage                 |
> | 5        | #2000 | Docs + Dependabot #1777 disposition + real version-bump closure |

---

## 1. Goal

Prove that CMS **metadata → external Solr** delivery remains correct after SolrJ 10
cutover **without** unattended agents talking to live clusters or storing production
secrets.

Primary code under test:

|     Piece      |                                               Path                                               |
|----------------|--------------------------------------------------------------------------------------------------|
| Handler        | `system/business/src/com/percussion/delivery/metadata/solr/impl/PSSolrDeliveryHandler.java`      |
| Config DTO     | same package `SolrServer` / `PSSolrConfig` / `SolrConfigLoader`                                  |
| Publish wiring | `system/business/src/com/percussion/rx/delivery/impl/PSMetadataDeliveryHandler.java`             |
| Unit tests     | `system/src/test/java/com/percussion/delivery/metadata/solr/impl/PSSolrDeliveryHandlerTest.java` |
| Config tests   | `…/SolrConfigLoaderTest.java`                                                                    |

**Not in scope:** DTS microservices (`deliverytiersuite/**` has no SolrJ sources),
re-architecture of indexing, production secrets, unattended live clusters.

---

## 2. Structural verification checklist

Use this as a code-review / cutover acceptance checklist. Items marked **(mock)** are
covered or coverable by unit tests with a mocked `SolrClient` (no network). Items marked
**(live)** require a human-operated Solr and are optional until operators run them.

### 2.1 Standalone HTTP client construction

| #  |                                 Check                                  |        Gate        |                                                                                            Notes                                                                                             |
|----|------------------------------------------------------------------------|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| S1 | Standalone path uses SolrJ-supported non-HttpClient client after #1997 | code review + unit | On **main today (9.10.1):** `HttpSolrClient.Builder` + `setUseMultiPartPost(true)`. **After #1997:** `HttpJdkSolrClient` (JDK `java.net.http`). Apache `HttpSolrClient` removed in SolrJ 10. |
| S2 | Base URL is Solr root ending in `/solr` (SolrJ 10 rule)                | ops config + unit  | e.g. `http://host:8983/solr` — not a collection-specific URL unless product config intentionally targets one.                                                                                |
| S3 | Client is cached on handler then closed on `commit` / `rollback`       | mock               | try-with-resources in `commit`/`rollback`; field cleared so next use rebuilds.                                                                                                               |
| S4 | Disabled handler (`serverConfig == null`) returns no client / no-ops   | mock               | `isEnabled()` false; `getClient()` null; delete/send/commit skip.                                                                                                                            |

### 2.2 Cloud client construction (secondary / classpath-impaired today)

| #  |                                   Check                                    |        Gate         |                                                                               Notes                                                                                |
|----|----------------------------------------------------------------------------|---------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| C1 | Cloud branch only when `serverCloudType=true`                              | mock                | Default config is standalone (`serverCloudType=false`).                                                                                                            |
| C2 | Builder uses **Solr base URLs**, not ZK hosts (SolrJ 10 preference)        | code review + #1997 | Today: `CloudSolrClient.Builder(List.of(solrHost))`. Product excludes `solr-solrj-zookeeper` (#1673); ZK-host cloud is **not** a supported runtime packaging path. |
| C3 | Default collection set via builder (`withDefaultCollection`) after cutover | #1997 unit          | Today: post-build `setDefaultCollection`.                                                                                                                          |
| C4 | SASL system properties only touched when `saslContextName` set             | code review         | Product default disables ZK SASL client (`zookeeper.sasl.client=false`).                                                                                           |

### 2.3 Page metadata `SolrInputDocument` path

| #  |                            Check                            | Gate |                                                                Notes                                                                 |
|----|-------------------------------------------------------------|------|--------------------------------------------------------------------------------------------------------------------------------------|
| P1 | Entry type `"page"` → `client.add(SolrInputDocument)`       | mock | Branch in `sendMetadataToSolr`.                                                                                                      |
| P2 | Document `id` field = delivery path argument                | mock | `doc.addField("id", path)`.                                                                                                          |
| P3 | Transformed properties become document fields               | mock | Base fields: `name`, `linktext`, `type`, `site`, `folder`, `pagepath`; plus entry properties with optional rename via `metadataMap`. |
| P4 | Page path also calls file extract after `add`               | mock | `sendMetadata` → `add` then `sendFile` (both paths for pages).                                                                       |
| P5 | Solr / I/O failures → `PSDeliveryException` + `incrError()` | mock | Max errors deactivate server (`isActive()`).                                                                                         |

### 2.4 File extract path (`ContentStreamUpdateRequest` / `/update/extract`)

| #  |                            Check                             |       Gate        |                                                                                                       Notes                                                                                                        |
|----|--------------------------------------------------------------|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| E1 | Non-page types use extract only (no `SolrInputDocument` add) | mock              |                                                                                                                                                                                                                    |
| E2 | Request path is `/update/extract`                            | mock              | `new ContentStreamUpdateRequest("/update/extract")`.                                                                                                                                                               |
| E3 | `literal.id` = delivery path                                 | mock              |                                                                                                                                                                                                                    |
| E4 | Metadata properties posted as `literal.<name>`               | mock              | `dcterms:format` also used as content type for `addFile`.                                                                                                                                                          |
| E5 | File attached via `addFile`                                  | mock + #1997 note | SolrJ 9: `File` overload; SolrJ 10: `Path` overload (`psPurgableTempFile.toPath()`).                                                                                                                               |
| E6 | Multipart multi-stream posts                                 | product policy    | SolrJ 9: `setUseMultiPartPost(true)`. SolrJ 10 JDK client: single-stream content-writer path only; multi-stream would need `solr-solrj-jetty` (#1998 defers Jetty). Product sends **one file stream** per request. |
| E7 | **Solr 9.x server** extract uses embedded/local Tika backend | **(live)** ops    | Customer Solr 9 extract works without external Tika Server (default Apache config).                                                                                                                                |
| E8 | **Solr 10.x server** extract requires **Tika Server**        | **(live)** ops    | See §4. Product JARs of Tika **do not** substitute for Solr’s extract backend.                                                                                                                                     |

### 2.5 deleteById / deleteByQuery / commit / rollback

| #  |                                    Check                                    | Gate |                            Notes                             |
|----|-----------------------------------------------------------------------------|------|--------------------------------------------------------------|
| D1 | `delete(path)` → `client.deleteById(path)` when enabled + active            | mock | Marks delivered.                                             |
| D2 | Full-publish clean → `deleteByQuery("*:*")` when `cleanAllOnFullPublish`    | mock | `forceSolrClean` constructor path + `deleteAllSolrEntries`.  |
| D3 | `commit()` when delivered → `client.commit()` then drop config/client       | mock | No-op when nothing delivered.                                |
| D4 | `rollback()` when delivered → `client.rollback()`; swallows rollback errors | mock | No-op when nothing delivered; null-safe when config cleared. |
| D5 | Max errors / inactive → `PSDeliveryException` on send/delete/commit         | mock | Message includes max-error / fatal skip text.                |

---

## 3. Mock / unit verification (agent-safe)

### 3.1 How to run

From repo root, JDK 21, Maven wrapper. Prefer standalone module install:

```bat
cd system
..\mvnw.cmd -Dtest=PSSolrDeliveryHandlerTest,SolrConfigLoaderTest test
```

Or full module gate (required before PR):

```bat
cd system
..\mvnw.cmd clean install
```

**Hard rule:** no test may open a socket to a real Solr host. Construction of
`HttpSolrClient` / `HttpJdkSolrClient` / `CloudSolrClient` is allowed **only** when no
request is issued (client built offline). Update paths **must** inject a mock
`SolrClient` via package-private test hooks.

### 3.2 Coverage matrix (this slice)

|                    Behavior                    |                               Test (class method)                               |            Network?            |
|------------------------------------------------|---------------------------------------------------------------------------------|--------------------------------|
| Disabled handler no-ops                        | `isEnabled_falseWhenNoServerConfig`, `delete_disabledHandler_doesNotCallClient` | none                           |
| Enabled with config                            | `isEnabled_trueWithServerConfig`                                                | none                           |
| deleteById                                     | `delete_invokesDeleteByIdOnInjectedClient`                                      | mock only                      |
| delete error mapping                           | `delete_propagatesSolrExceptionAsDeliveryException`                             | mock only                      |
| commit when delivered                          | `commit_invokesCommitWhenDelivered`                                             | mock only                      |
| commit skip                                    | `commit_skipsWhenNothingDelivered`                                              | mock only                      |
| rollback when delivered                        | `rollback_invokesRollbackWhenDelivered`                                         | mock only                      |
| rollback skip / null config                    | `rollback_skipsWhenNothingDelivered`, `rollback_nullSafeWhenConfigCleared`      | mock only                      |
| page `SolrInputDocument` + extract             | `sendMetadata_page_addsDocumentAndExtractRequest`                               | mock only                      |
| non-page extract only                          | `sendMetadata_file_extractOnly_noDocumentAdd`                                   | mock only                      |
| metadata map rename                            | `sendMetadata_page_appliesMetaMapping`                                          | mock only                      |
| deleteByQuery full clean                       | `deleteAll_invokesDeleteByQueryWhenCleanAllOnFullPublish`                       | mock only                      |
| deleteByQuery skip flag                        | `deleteAll_skipsWhenCleanAllDisabled`                                           | mock only                      |
| inactive after max errors                      | `delete_throwsWhenServerInactive`                                               | mock only                      |
| Standalone construction type                   | `getClient_standaloneBranch_constructsHttpClient`                               | construct only                 |
| Cloud classpath gap (SolrJ 9 + Jetty excluded) | `getClient_cloudBranch_classpathImpairedWithoutJettyOnSolrJ9`                   | construct only (expects NCDFE) |
| Cloud with injected mock                       | `getClient_returnsInjectedMockEvenWhenCloudConfigured`                          | mock only                      |

### 3.3 Gaps / deferred (explicit)

|                            Gap                             |                             Why                             |         Owner slice          |
|------------------------------------------------------------|-------------------------------------------------------------|------------------------------|
| Live extract against real Tika Server                      | Needs operator Solr + secrets/network                       | Optional live §5             |
| Jetty multipart multi-stream equivalence                   | Packaging excludes Jetty client (#1998)                     | #1998 / product decision     |
| `SolrConfigLoader` filesystem load of install `rxconfig`   | Existing config marshal tests only                          | Config already covered       |
| End-to-end publish job through `PSMetadataDeliveryHandler` | Heavy Spring/job fixtures; Worker posts Solr after metadata | Future integration if needed |

---

## 4. Operator notes — extract / Tika Server

### 4.1 What the product sends

During publish, when Solr delivery is configured in `rxconfig/DeliveryServer/solr-servers.xml`:

1. **Pages** (`entry.type == "page"`):
   - `SolrClient.add(SolrInputDocument)` with `id` = path + mapped fields
   - Then `ContentStreamUpdateRequest("/update/extract")` with the page temp file
2. **Non-pages** (assets/files): extract request only
3. **Deletes**: `deleteById(path)`
4. **Job end**: `commit()` (or `rollback()` on failure paths)

Extract requests set:

- `literal.id` = CMS delivery path
- `literal.<property>` for each transformed metadata property
- File body via SolrJ `addFile`

### 4.2 Solr server version differences

| Customer Solr server |                          Extract backend                           |                                                          Operator action for extract indexing                                                          |
|----------------------|--------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| **9.x**              | Local/embedded Tika historically bundled with Solr extract handler | Usually works with stock extract config; still validate MIME/`dcterms:format`                                                                          |
| **10.x**             | **Local Tika removed** — extract requires external **Tika Server** | Configure Solr `tikaserver.url` (or equivalent per Solr 10 guide) pointing at a running Tika Server; without it, `/update/extract` fails at index time |

Official reference: [Major Changes in Solr 10](https://solr.apache.org/guide/solr/latest/upgrade-notes/major-changes-in-solr-10.html)
(extract / Tika sections).

### 4.3 Product Tika JARs vs Solr Tika Server

Percussion ships **CMS-local** Tika (`tika-core` / parsers) for product-side extraction
and metadata features. Those libraries run **inside the CMS JVM**. They are **not** the
backend for Solr’s ExtractingRequestHandler. Operators must provision Tika Server for
**Solr 10** extract independently of CMS dependencies.

### 4.4 Multipart / client packaging

- Product extract attaches **one** content stream per request.
- SolrJ 10 recommended multi-stream multipart path is Jetty-based (`solr-solrj-jetty`).
- Current product packaging policy (#1998): **HTTP-only** `solr-solrj` core; optional
  Jetty/ZK modules stay off the runtime tree unless a later decision productizes them.

### 4.5 Secrets and config hygiene

- Do **not** commit Solr credentials, TLS keys, or production hostnames into the repo.
- Use local operator config / secrets managers for live smoke.
- Agent automation must not scrape or log production `solr-servers.xml` secrets.

---

## 5. Live Solr smoke (optional, human-gated)

**Do not run unattended overnight.** Operators only, after #1997 cutover is on the branch
under test (or against Solr 9.x baseline for regression).

### 5.1 Preconditions

- [ ] Human present; lab/dev Solr only (not production)
- [ ] Solr **9.x** *or* **10.x** instance reachable from CMS host
- [ ] If Solr **10.x** and extract is in scope: Tika Server up; Solr `tikaserver.url` set
- [ ] Collection/core created matching `defaultCollection` / path layout
- [ ] `solr-servers.xml` points at lab host; **no production secrets** in git
- [ ] CMS build includes the SolrJ cutover under test (#1997 / packaging #1998 as needed)

### 5.2 Smoke steps (checklist only)

1. Enable one site in `solr-servers.xml` (`enabledSites`, `solrHost`, `serverType`).
2. Publish a **page** to that site (full or incremental).
3. Query Solr for document `id` = published path; confirm mapped fields.
4. Publish a **non-page asset** (or page with extract body); confirm extract content fields
   when extract handler is configured.
5. Unpublish/delete content; confirm `deleteById` removed the doc (or soft-delete per core
   config).
6. Full publish with `cleanAllOnFullPublish=true`; confirm wipe (`*:*`) then re-index.
7. Force a failure (bad host); confirm CMS logs delivery error and does not hang the job
   beyond configured retries / max errors.
8. **Solr 10 only:** stop Tika Server, republish extract path, confirm clear extract failure
   (proves dependency is external Tika, not CMS Tika JARs).

### 5.3 Pass / fail recording

Record results on the parent epic **#1788** or this issue with:

- Solr server version (9.x / 10.x + patch)
- Tika Server version (if used)
- SolrJ client version from CMS build
- Date + operator identity
- Pass/fail per step 1–8

---

## 6. Sequencing with sibling slices

|        When        |                                                                    What this plan expects                                                                    |
|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Before #1997 merge | Mock tests validate **behavior** against `SolrClient` API; construction asserts **9.x** `HttpSolrClient` on main. Checklist rows call out 10.x target types. |
| With / after #1997 | Construction tests switch expected type to `HttpJdkSolrClient`; `addFile(Path)` path covered. Re-run §3 tests on cutover branch.                             |
| After #1998        | Packaging policy tests (optional modules absent) remain green.                                                                                               |
| Optional live      | §5 only after human readiness; does not block mock/unit PR for #1999.                                                                                        |

---

## 7. Acceptance mapping (#1999)

|                  Acceptance criterion                  |                     Deliverable                     |
|--------------------------------------------------------|-----------------------------------------------------|
| Written smoke/verification checklist linked from #1788 | This file + inventory cross-link + issue/PR comment |
| Unit/mock coverage improved for critical handler paths | `PSSolrDeliveryHandlerTest` expansions (§3.2)       |
| Extract/Tika Server compatibility documented           | §4                                                  |
| Live Solr steps marked optional / human-gated          | §5                                                  |

---

## 8. Links

|       Resource        |                                         URL / path                                          |
|-----------------------|---------------------------------------------------------------------------------------------|
| Parent epic           | https://github.com/intersoftdatalabs-in/percussioncms/issues/1788                           |
| This issue            | https://github.com/intersoftdatalabs-in/percussioncms/issues/1999                           |
| Inventory             | `docs/ai-generated/tasks/1788-solrj-10/00-inventory.md`                                     |
| Solr 10 major changes | https://solr.apache.org/guide/solr/latest/upgrade-notes/major-changes-in-solr-10.html       |
| Handler               | `system/business/src/com/percussion/delivery/metadata/solr/impl/PSSolrDeliveryHandler.java` |

