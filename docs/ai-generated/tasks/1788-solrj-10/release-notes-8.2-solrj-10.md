# Release notes draft — SolrJ 10 client + supported Solr server matrix (8.2)

**Status:** Draft for product / release packaging (parent **#1788**, slice **#2000**).  
**Tracking:** [GitHub #1788](https://github.com/intersoftdatalabs-in/percussioncms/issues/1788) · [slice #2000](https://github.com/intersoftdatalabs-in/percussioncms/issues/2000)  
**Client pin on `main`:** root `pom.xml` property `solr.version` = **10.0.0** (landed with intentional code + packaging PRs **#2009** / **#2010**; **not** Dependabot-only **#1777**).

Use this text as the source for official release notes and operator upgrade guides. Engineering inventory and verification checklists live in this folder (links below).

---

## Summary

Percussion CMS optionally publishes **metadata to an external Solr** during site publish (not an embedded Solr server). Starting with the 8.2 line that ships this change:

|           Area            |                                                                                               Behavior                                                                                               |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Embedded SolrJ client** | Product ships **SolrJ 10.x** (`org.apache.solr:solr-solrj`). Standalone clients use JDK `HttpJdkSolrClient` (Apache HttpClient `HttpSolrClient` was removed upstream in Solr 10).                    |
| **Customer Solr server**  | **9.x and 10.x** remain in the supported matrix for HTTP indexing; **extract** behavior differs by major (see below).                                                                                |
| **Base URL shape**        | Configured `solrHost` must be a Solr **root** URL ending in `/solr` (e.g. `http://host:8983/solr`).                                                                                                  |
| **Cloud / ZooKeeper**     | Product packaging is **HTTP-only**. ZooKeeper client jars and `solr-solrj-zookeeper` stay **off the runtime tree** (enforcer ban, issue **#1673**).                                                  |
| **`/update/extract`**     | Still used for non-page assets (and page body extract). On **Solr 10 servers**, operators must run **Tika Server** and configure Solr’s extract backend — CMS-local Tika JARs do **not** substitute. |

Dependabot PR **#1777** (version-only `9.10.1` → `10.0.0`) was **not** the delivery vehicle and is **closed as superseded** by the intentional upgrade work.

---

## Who is affected?

|                                      Estate                                      |       Affected?       |                                                                Action                                                                |
|----------------------------------------------------------------------------------|-----------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| CMS **without** `rxconfig/DeliveryServer/solr-servers.xml` Solr delivery enabled | **No**                | No operator Solr change; product still ships SolrJ 10 on the classpath for optional use.                                             |
| CMS publishing metadata to an external **Solr 9.x** server                       | **Yes (client)**      | Upgrade CMS as usual. Keep Solr 9.x if extract without external Tika Server is required. Re-check `solrHost` ends in `/solr`.        |
| CMS publishing metadata to an external **Solr 10.x** server                      | **Yes (ops)**         | Provision **Tika Server** for extract; set Solr `tikaserver.url` (or equivalent per Solr 10 guide). Validate base URL `/solr` shape. |
| Custom **ZooKeeper-host** CloudSolrClient configs                                | **Yes (unsupported)** | Product does **not** ship ZK SolrJ modules. Migrate to **Solr base URL** list form or standalone HTTP.                               |
| Java **8** / `percussioncms-java8` line                                          | **Out of scope**      | SolrJ 10 requires Java **17+**; this product line is **Java 21**.                                                                    |

---

## Supported Solr server matrix

| Customer Solr server |           SolrJ client in CMS (8.2)            | Page metadata `add` / delete / commit |                            `/update/extract` (files + page body)                            |                                       Operator notes                                        |
|----------------------|------------------------------------------------|---------------------------------------|---------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| **9.x**              | 10.x (compatible client→server for these APIs) | Supported                             | Supported with Solr’s **local/embedded Tika** extract backend (stock Apache extract config) | Prefer remaining on 9.x only if you cannot run Tika Server yet. Still use root URL `/solr`. |
| **10.x**             | 10.x                                           | Supported                             | Requires external **Tika Server** — local Tika backend **removed** in Solr 10               | Configure `tikaserver.url` on Solr; without it, extract indexing **fails at index time**.   |
| **&lt; 9.x**         | Not a target matrix for new 8.2 installs       | Best-effort only                      | Best-effort only                                                                            | Upgrade Solr server to 9.x or 10.x.                                                         |

Official upstream notes: [Major Changes in Solr 10](https://solr.apache.org/guide/solr/latest/upgrade-notes/major-changes-in-solr-10.html).

**New installs that enable Solr metadata delivery:** prefer a **Solr 10.x** server **with** Tika Server if extract is in scope; otherwise Solr **9.x** remains valid for extract without a separate Tika process.

---

## Configuration: base URL and cloud policy

### `solrHost` must end in `/solr`

SolrJ 10 tightens base-URL rules. Operators should set `solrHost` in `rxconfig/DeliveryServer/solr-servers.xml` to the Solr **application root**, not a collection-specific path:

```text
# Good
http://solr.example.com:8983/solr
https://solr.example.com:8983/solr

# Avoid (collection-in-path / missing /solr) — may be rejected by SolrJ 10 builders
http://solr.example.com:8983/solr/mycollection
http://solr.example.com:8983/
```

Use `defaultCollection` (and product field mapping) for collection selection rather than embedding the collection in the base URL.

### HTTP-only client; no ZooKeeper packaging

|         Policy         |                                                                                                       Detail                                                                                                        |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Default path**       | Standalone `HttpJdkSolrClient` when `serverCloudType` is false/omitted.                                                                                                                                             |
| **Cloud flag**         | If `serverCloudType=true`, product builds `CloudSolrClient` from a **Solr base URL** list (same `/solr` root), with `withDefaultCollection` when configured.                                                        |
| **ZooKeeper hosts**    | **Not supported** as a product packaging path. `solr-solrj-zookeeper` and `org.apache.zookeeper:*` are excluded/enforcer-banned (#1673). Do not put ZK ensemble hosts in `solrHost` expecting product ZK discovery. |
| **Jetty SolrJ module** | Optional `solr-solrj-jetty` stays **off** the runtime tree. Multi-stream multipart client features are not productized; CMS extract posts **one file stream** per request via `ContentStreamUpdateRequest`.         |

---

## Extract path and Tika Server (Solr 10)

### What the product sends

During publish, when Solr delivery is enabled for a site:

1. **Pages** (`type == page`): `SolrClient.add(SolrInputDocument)` with `id` = delivery path and mapped fields, then `/update/extract` with the page temp file.
2. **Non-pages** (assets/files): `/update/extract` only.
3. **Deletes**: `deleteById(path)`.
4. **Job end**: `commit()` (or `rollback()` on failure paths).

Extract requests set `literal.id` and `literal.<property>` metadata, then attach the file body.

### Solr 9.x vs 10.x extract backend

|  Server  |                        Extract backend                        |                                                                        Operator action                                                                        |
|----------|---------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **9.x**  | Local/embedded Tika historically bundled with extract handler | Usually works with stock extract config; validate MIME / `dcterms:format`.                                                                                    |
| **10.x** | **Local Tika removed** — **Tika Server only**                 | Run Tika Server; configure Solr `tikaserver.url` (name per Solr 10 guide). Without it, `/update/extract` fails when indexing binaries/pages that use extract. |

### Product Tika JARs ≠ Solr Tika Server

Percussion ships **CMS-local** Tika (`tika-core` / parsers) for product-side extraction and metadata features inside the **CMS JVM**. Those libraries are **not** the backend for Solr’s ExtractingRequestHandler. Operators must provision Tika Server for **Solr 10** extract independently of CMS dependencies.

---

## Upgrade path (operators)

1. **Inventory** whether any site uses Solr delivery (`solr-servers.xml` / enabled sites).
2. **Normalize `solrHost`** to root URLs ending in `/solr`; set `defaultCollection` as needed.
3. **Choose server major:**
   - Stay on **Solr 9.x** if you need extract without operating Tika Server.
   - Move to **Solr 10.x** only with Tika Server + Solr extract backend config ready.
4. **Upgrade CMS** to the 8.2 build that includes SolrJ 10 (this change set). No separate Dependabot-only pin merge is required or recommended.
5. **Smoke** (lab only): page publish → query by `id`; non-page extract; delete; optional full-publish clean. Live checklist: [01-verification-plan.md](./01-verification-plan.md) §5 (**human-gated** — not unattended).
6. **Do not** roll Solr majors backward casually; upstream cluster rules apply (see Solr 10 upgrade notes).

---

## Intentional breaks / no longer promised

|                  Topic                  |                                                                                                                                                Detail                                                                                                                                                |
|-----------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Apache HttpClient SolrJ clients**     | Removed upstream; product no longer uses `HttpSolrClient` / `setUseMultiPartPost`.                                                                                                                                                                                                                   |
| **ZooKeeper-host CloudSolrClient**      | Not a supported product packaging path while ZK jars remain banned (#1673).                                                                                                                                                                                                                          |
| **Solr 10 extract without Tika Server** | Not supported for `/update/extract`.                                                                                                                                                                                                                                                                 |
| **Dependabot-only major bumps**         | Major Solr/SolrJ jumps require intentional refactor + verification (this epic). Dependabot is configured to **ignore `org.apache.solr` major** updates after the intentional 10.x landing so bare pin PRs do not reappear for the next major without a plan. Patch/minor within 10.x may still open. |
| **Java 8 product line**                 | SolrJ 10 is **out of scope** for `percussioncms-java8`.                                                                                                                                                                                                                                              |

---

## Engineering / verification (summary)

|            Slice            | Issue |                                Deliverable                                 |                              PR (merged on `main`)                               |
|-----------------------------|-------|----------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| 1 Inventory                 | #1996 | [00-inventory.md](./00-inventory.md)                                       | #2002                                                                            |
| 2 Client cutover            | #1997 | `HttpJdkSolrClient` + unit tests; `solr.version` **10.0.0** with real Java | via #2009 (includes #1997 commits; standalone #2008 closed as duplicate vehicle) |
| 3 Packaging                 | #1998 | Explicit modules; HTTP-only; no Jetty/ZK on tree                           | #2009                                                                            |
| 4 Verification              | #1999 | [01-verification-plan.md](./01-verification-plan.md) + mock handler tests  | #2010                                                                            |
| 5 Docs + Dependabot closure | #2000 | This release-note draft + #1777 supersede + Dependabot major ignore        | (this PR)                                                                        |

**Agent-safe verification** is mock/unit first (no live Solr required for CI). Live 9.x/10.x + Tika Server smoke is **optional and human-gated**.

---

## Document map

|                               Doc                                |                                                      Purpose                                                      |
|------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| [00-inventory.md](./00-inventory.md)                             | Call-site + POM inventory vs Solr 10 major changes                                                                |
| [01-verification-plan.md](./01-verification-plan.md)             | Structural checklist, mock tests, live smoke (optional)                                                           |
| [release-notes-8.2-solrj-10.md](./release-notes-8.2-solrj-10.md) | **This file** — operator / release packaging draft                                                                |
| Handler                                                          | `system/business/src/com/percussion/delivery/metadata/solr/impl/PSSolrDeliveryHandler.java`                       |
| Config                                                           | `rxconfig/DeliveryServer/solr-servers.xml` (install tree; not committed with secrets)                             |
| Upstream                                                         | [Major Changes in Solr 10](https://solr.apache.org/guide/solr/latest/upgrade-notes/major-changes-in-solr-10.html) |

---

## Secrets and config hygiene

- Do **not** commit Solr credentials, TLS keys, or production hostnames into the repo.
- Use local operator config / secrets managers for live smoke.
- Agent automation must not scrape or log production `solr-servers.xml` secrets.

