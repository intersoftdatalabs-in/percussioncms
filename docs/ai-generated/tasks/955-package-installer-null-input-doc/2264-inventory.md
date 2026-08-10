# Inventory: Package Installer `NULL_INPUT_DOC` (PSDeployException error 8)

**Issue:** [#2264](https://github.com/intersoftdatalabs-in/percussioncms/issues/2264) (slice 1 of epic [#955](https://github.com/intersoftdatalabs-in/percussioncms/issues/955))  
**Related:** fix slice [#2265](https://github.com/intersoftdatalabs-in/percussioncms/issues/2265), residual install smoke [#2266](https://github.com/intersoftdatalabs-in/percussioncms/issues/2266)  
**Date:** 2026-08-07  
**Scope:** Code-path inventory + repro notes only — **no product fix** in this slice.

## Symptom (from #955)

Package Installer UI / CLI install:

1. Launch Package Installer
2. Select any package from `<CMSinstall>\Packages\Percussion\`
3. Provide server details and click **Install**
4. Error:

```text
com.percussion.error.PSDeployException: Input document expected by server, none found.
```

Message text is error code **8** from:

|                       Bundle                        |                        Key                         |
|-----------------------------------------------------|----------------------------------------------------|
| `deployer/.../PSDeployErrorStringBundle.properties` | `8=Input document expected by server, none found.` |
| `system/.../PSDeployErrorStringBundle.properties`   | same (legacy copy)                                 |

Constant: `IPSDeploymentErrors.NULL_INPUT_DOC = 8`  
(`system/src/main/java/com/percussion/error/IPSDeploymentErrors.java`)

Related job-handler constant (different code, same wording class of failure):  
`IPSJobErrors.NULL_INPUT_DOC = 5` in `PSJobHandler` (job requests, not deploy-).

---

## End-to-end call path (install)

```text
Package Installer entry
  com.percussion.packageinstaller.ui.PSPackageInstallerClient
    (binary: lib/pspackagerui.jar — sources NOT in this monorepo)
  classpath via system/installResources/install.xml PSInstallPackageExec
    → rxdeployer.jar (deployer module) + pspackagerui.jar + …

Typical install sequence (client library):
  1) PSDeploymentServerConnection.<ctor>
       → connect() POST deploy-connect + XML attach (PSXDeployConnectRequest)
  2) PSDeploymentManager.validateArchive(PSArchiveInfo, …)
       → deploy-validateArchive + XML (PSXDeployValidateArchiveRequest + PSArchiveInfo)
  3) PSDeploymentManager.copyArchiveToServer(archiveRef, .ppkg)
       → deploy-saveArchiveFile + FILE ONLY (params archiveRef; no XML input doc)
  4) PSDeploymentManager.runValidationJob(PSImportDescriptor)
       → job-runJob + descriptor XML + params sys_jobCategory=deployer, sys_jobType=validation
  5) PSDeploymentManager.loadValidationResults(desc)
       → deploy-getValidationResults + XML attr archiveRef
  6) PSDeploymentManager.runImportJob(PSImportDescriptor)
       → job-runJob + descriptor XML + sys_jobType=import

Server entry:
  HTTP POST /Rhythmyx/sys_deployerHandler  (deploy-*)
  HTTP POST /Rhythmyx/sys_jobHandler       (job-*)
    CGI header PS-Request-Type = deploy-<sub> | job-<sub>

  Multipart body:
    - optional form fields (params)
    - optional file part:
        * .xml attachment written by createAttachmentFile → intended as request input Document
        * or binary package file for saveArchiveFile

  Server parses multipart in PSFormContentParser:
    if part Content-Type is text/xml or application/xml
      → request.setInputDocument(doc)
    else
      → request parameter / File only  (getInputDocument() stays null)

  PSDeploymentHandler.processRequest:
    subReqType from deploy- prefix
    non-disconnect: checkAccessLevel + extendLock (null input doc allowed here)
    dispatch to method / PSCatalogHandler.processRequest

  PSJobHandler.processRequest:
    requires input doc for ALL job-* types (throws IPSJobErrors.NULL_INPUT_DOC = 5)
```

### Alternate install path (in-process, not Package Installer UI)

`PSLocalDeployerClient` / `PSStartupPkgInstaller` install packages **inside** the server JVM and call `PSDeploymentHandler.validateArchive(PSArchiveInfo, …)` and `PSImportJob.install(...)` **without** HTTP multipart. That path does **not** go through `getInputDocument()` and is **not** a candidate for the UI-reported `NULL_INPUT_DOC` symptom.

---

## Error code / message mapping

|     Layer      |               Constant               | Code |                          Message                           |
|----------------|--------------------------------------|------|------------------------------------------------------------|
| Deploy handler | `IPSDeploymentErrors.NULL_INPUT_DOC` | 8    | Input document expected by server, none found.             |
| Job handler    | `IPSJobErrors.NULL_INPUT_DOC`        | 5    | (job error bundle; same failure class for missing job XML) |

Client surfaces deploy errors as `PSDeployException` after parsing the 500 XML fault body from the handler.

---

## Server throw sites — `PSDeploymentHandler`

All throws use `new PSDeployException(IPSDeploymentErrors.NULL_INPUT_DOC)` when `req.getInputDocument()` is null.

|                   Method                    | Approx. line |          How null is checked           |            Typical client request            |
|---------------------------------------------|--------------|----------------------------------------|----------------------------------------------|
| `connect`                                   | ~165         | `Optional.ofNullable(...).orElseThrow` | `deploy-connect` / `PSXDeployConnectRequest` |
| `getDeployableElements`                     | ~235         | explicit `if (doc == null)`            | `deploy-getDeployableElements`               |
| `getExportDescriptor`                       | ~342         | explicit                               | `deploy-getExportDescriptor`                 |
| `getIdTypes`                                | ~465         | explicit                               | `deploy-getIdTypes`                          |
| `saveIdTypes`                               | ~529         | explicit                               | `deploy-saveIdTypes`                         |
| `validateLocalConfig`                       | ~596         | explicit                               | `deploy-validateLocalConfig`                 |
| `validateArchive`                           | ~665         | explicit                               | `deploy-validateArchive`                     |
| `getRequiredAttrFromRequest` (private)      | ~1087        | explicit                               | **Shared** by many methods (see below)       |
| `getArchiveSummary`                         | ~1127        | explicit                               | `deploy-getArchiveSummary`                   |
| `deleteArchive`                             | ~1455        | explicit                               | `deploy-deleteArchive`                       |
| `getRequiredComponentFromRequest` (private) | ~1555        | explicit                               | saves that embed components in XML body      |
| `disconnect`                                | ~1933        | explicit                               | `deploy-disconnect`                          |
| `loadDependencies`                          | ~1979        | explicit                               | `deploy-loadDependencies`                    |
| `loadAncestors`                             | ~2024        | explicit                               | `deploy-loadAncestors`                       |
| `getFeatureSet`                             | ~2481        | explicit                               | `deploy-getFeatureSet`                       |
| `getParentTypes`                            | ~2553        | explicit                               | `deploy-getParentTypes`                      |

### Methods that throw via `getRequiredAttrFromRequest` (same error 8)

These do not restate the null check but fail if the input document is missing:

|                                Method                                 |                        Attribute(s) read                         |
|-----------------------------------------------------------------------|------------------------------------------------------------------|
| `getDependencies`                                                     | `type`, `parentId`                                               |
| `getValidationResults`                                                | `archiveRef`                                                     |
| `getDbmsMap`                                                          | `server`                                                         |
| `getIdMap`                                                            | `sourceServer`                                                   |
| `getAttrNumberFromRequest` → `getArchiveInfo`, `getLogSummary`, …     | numeric attrs                                                    |
| `deleteExportDescriptor`                                              | `descName`                                                       |
| `getArchiveFile`                                                      | `descName`                                                       |
| `createConfigDef` / `createDefaultConfig` / `createDescriptorSummary` | `descName`                                                       |
| `validateArchive` flag attrs                                          | `checkArchiveRef`, `warnOnBuidMismatch`, `warnMissingPackageDep` |

### Methods that intentionally tolerate null input document

|                           Method                           |                                                     Notes                                                     |
|------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `extendLock`                                               | Comment: *"input doc is null when copying archive to server"*. Uses session id from request when doc present. |
| `saveArchiveFile`                                          | Binary multipart file + HTML param `archiveRef` only.                                                         |
| `saveConfigFile`                                           | Same pattern as save archive (file + `configRef`).                                                            |
| `createDescriptorGuid` / `getDependencyToPackageNameIndex` | No request body required.                                                                                     |

`processRequest` always calls `extendLock` for non-disconnect traffic **before** the sub-handler; that step does **not** throw `NULL_INPUT_DOC`.

---

## Server throw site — `PSCatalogHandler`

|      Method      | Approx. line |                   Condition                   |
|------------------|--------------|-----------------------------------------------|
| `processRequest` | ~77          | `doc == null` **or** missing document element |

Dispatched from `PSDeploymentHandler.processRequest` when `subReqType.equals("catalog")`.  
Client: `PSCataloger.catalog(...)` → `deploy-catalog` with root `PSXCatalog` + request type suffix.

---

## Client request construction

### `PSDeploymentServerConnection`

|                       Piece                        |                                                                                                Role                                                                                                 |
|----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `execute(String type, Document req[, Map params])` | Writes `req` to purgable temp `dpl_*.xml`, multipart-encodes as file part, sets CGI `PS-Request-Type`, POSTs to `/sys_deployerHandler` or `/sys_jobHandler`. Sets `sessionId` attr on request root. |
| `createAttachmentFile(Document)`                   | `PSPurgableTempFile("dpl_", ".xml", …)` + `PSXmlDocumentBuilder.write`                                                                                                                              |
| `execute(String type, Map params, File body, …)`   | **No** XML document — file-only (archive/config upload).                                                                                                                                            |
| `connect` / `disconnect` / `extendLock`            | Always build non-null `Document` with expected root element.                                                                                                                                        |
| Content-Type of file part                          | `Codecs.mpFormDataEncode` → `URLConnection.guessContentTypeFromName(filename)`                                                                                                                      |

### `PSDeploymentManager` (always builds XML for document APIs)

Examples used during install:

|             Client API              |         Request type          |                        XML root                        |
|-------------------------------------|-------------------------------|--------------------------------------------------------|
| (connection) connect                | `deploy-connect`              | `PSXDeployConnectRequest`                              |
| `validateArchive`                   | `deploy-validateArchive`      | `PSXDeployValidateArchiveRequest` + archive info child |
| `copyArchiveToServer`               | `deploy-saveArchiveFile`      | **none** (file + `archiveRef` param)                   |
| `runValidationJob` / `runImportJob` | `job-runJob`                  | descriptor `toXml` root                                |
| `loadValidationResults`             | `deploy-getValidationResults` | `PSXDeployGetValidationResultsRequest` + `archiveRef`  |
| `getFeatureSet`                     | `deploy-getFeatureSet`        | `PSXDeployGetFeatureSetRequest`                        |
| catalog via `getCataloger()`        | `deploy-catalog`              | `PSXCatalog*`                                          |

**Conclusion (current `main`):** every XML deploy/job API in `PSDeploymentManager` / `PSDeploymentServerConnection` constructs a non-null `Document` before `execute`. The only intentional null-input-document stage is **archive/config file upload** (`saveArchiveFile` / `saveConfigFile`), and those handlers do **not** throw `NULL_INPUT_DOC`.

---

## Where the input document is lost (failure model)

`NULL_INPUT_DOC` means the server handler ran with `PSRequest.getInputDocument() == null`. Given the client always attaches XML for document APIs, the loss is between **multipart encode** and **handler**:

```text
Client Document
  → createAttachmentFile (dpl_*.xml)
  → Codecs.mpFormDataEncode (Content-Type from guessContentTypeFromName)
  → HTTP POST multipart
  → PSFormContentParser
       isXml only if Content-Type starts with text/xml OR application/xml
       else: stored as request parameter/File, input document stays null
  → PSDeploymentHandler / PSCatalogHandler / PSJobHandler
       throw NULL_INPUT_DOC if they require a document
```

### Highest-probability root cause class (code evidence)

1. **Multipart part not classified as XML**
   - If `guessContentTypeFromName("dpl_….xml")` returns `null` or a non-xml type on a given JRE/platform, `PSFormContentParser` will **not** call `setInputDocument`.
   - Observed on this agent host (Java 21): `guessContentTypeFromName("dpl_foo.xml")` → `application/xml` (OK).
   - Historical / alternate JREs or custom `FileNameMap` could differ — would cause **all** XML deploy requests (including **connect** and **validateArchive**) to fail with error 8.
2. **Wrong stage expectation**
   - File-only `saveArchiveFile` correctly has null input doc. If a future or forked client routed a document API through the file-only `execute` overload, the matching handler would throw error 8. **Current** `PSDeploymentManager` does not do this for validate/import.
3. **Not the install “middle” of a successful connect**
   - If connect succeeds, the session path already proved XML multipart → input document works for that process. A later null doc would then be more likely a **specific** request that omitted the attachment (client bug outside monorepo UI) or a server parsing edge case for that request’s body.

### Stages most associated with the reported Install click

Repro steps jump straight to Install with server credentials, so first failures map to:

| Order |                Stage                 | Requires input doc? |         Throws error 8 if missing?         |
|-------|--------------------------------------|---------------------|--------------------------------------------|
| 1     | `deploy-connect`                     | yes                 | **yes** (`connect`)                        |
| 2     | catalog / feature set (if UI probes) | yes                 | yes (`PSCatalogHandler` / `getFeatureSet`) |
| 3     | `deploy-validateArchive`             | yes                 | **yes**                                    |
| 4     | `deploy-saveArchiveFile`             | no                  | **no**                                     |
| 5     | `job-runJob` (validation)            | yes                 | job error 5 (not deploy 8)                 |
| 6     | `deploy-getValidationResults`        | yes                 | **yes**                                    |
| 7     | `job-runJob` (import)                | yes                 | job error 5                                |

The user-facing message string matches **deploy** error 8, not the job-handler constant. Prefer stages **1, 2, 3, or 6** when correlating stacks.

---

## Package Installer client location

|          Artifact          |                                                     Location                                                      |
|----------------------------|-------------------------------------------------------------------------------------------------------------------|
| Main class                 | `com.percussion.packageinstaller.ui.PSPackageInstallerClient`                                                     |
| Referenced from            | `system/installResources/install.xml` macro `PSInstallPackageExec`                                                |
| Expected jar               | `lib/pspackagerui.jar` (distribution; **not** built from sources in this repo)                                    |
| Deployer client used by UI | `com.percussion.deployer.client.*` (this monorepo, module `deployer`)                                             |
| In-repo package UIs        | GWT `PCM-PkgMgtUI` (admin package mgmt — different surface); `PSPackageService` / startup installer (server-side) |

**Gap for Slice 2:** UI/CLI orchestration inside `pspackagerui.jar` cannot be grepped here. Fix candidates that live in-repo are server multipart→input-doc binding, deployer client encoding of attachments, and defensive handler messaging/tests.

---

## Historical / repro evidence

|               Source               |                                                                      Finding                                                                      |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| #955 comment (natechadwick-intsof) | “pretty sure this is resolved on the 8.1.7 and 8.2 lines.”                                                                                        |
| #955 comment (vijaya-boddipudi)    | Could not reproduce (could not find Package Installer on 8.1.7 environment).                                                                      |
| Migrated from                      | archived `percussion/percussioncms#541`                                                                                                           |
| Related login fix                  | CMS-8014 (2021): Package Builder/Installer login — `PSDeploymentHandler.connect` password handling; **not** a removal of `NULL_INPUT_DOC` checks. |
| CMS-9210 commits                   | Unrelated dependency/id-type Package Installer errors (menu action children).                                                                     |
| This agent session                 | No live CMS + Package Installer UI available. No stack capture. Code inventory only.                                                              |

### Agent-safe repro notes (for humans / Slice 3)

**Preferred (full):**

1. Start a local CMS (Jetty distribution) with admin user.
2. Launch Package Installer from the installed tools (`PercussionPackageInstaller` / `java … PSPackageInstallerClient`).
3. Select a small `.ppkg` under `Packages/Percussion/`.
4. Enter host/port/admin credentials; Install.
5. If error 8 appears: capture client console + server log around `PSDeploymentHandler` / `PSFormContentParser`; note first `deploy-*` / `job-*` type that fails.
6. Optional: enable client debug logging on `PSDeploymentServerConnection` (request dump on IOException path).

**Agent-safe partial (no UI):**

1. Unit/integration tests that POST multipart with/without `Content-Type: application/xml` on the XML part and assert `getInputDocument()` null vs non-null and error 8 mapping.
2. Assert `guessContentTypeFromName` for `dpl_*.xml` on CI JREs.
3. Mock `PSRequest` with null input doc into `PSDeploymentHandler.connect` / `validateArchive` / `PSCatalogHandler.processRequest` to lock throw sites (Slice 2).

### Assessment for current `main`

|                       Claim                        |                                    Evidence                                    |
|----------------------------------------------------|--------------------------------------------------------------------------------|
| Client library always sends XML for document APIs  | Grep of `PSDeploymentManager` / connection `connect`/`execute(Document)`       |
| Server still has many hard `NULL_INPUT_DOC` guards | 16+ sites in `PSDeploymentHandler` + catalog + shared helpers                  |
| File upload stage is **not** the throw site        | `saveArchiveFile` does not check input doc; `extendLock` allows null           |
| Live UI repro not confirmed on this branch         | No environment; historical “likely fixed / cannot repro” comments              |
| Residual risk remains                              | MIME classification of XML attachment; opaque `pspackagerui.jar` orchestration |

**Working conclusion for Slice 2:** Treat as **not proven fixed by absence of repro alone**. Prefer a **defensive, test-backed** fix on the encode/parse boundary (ensure XML parts always set content-type `application/xml` / `text/xml`) and/or clearer diagnostics, plus unit tests on throw sites and multipart parsing — rather than removing null checks. Do not assume a single “install stage” in product code omits the document on current `main` client library paths.

---

## Slice 2 guidance (minimal fix candidates)

1. **Client (`PSDeploymentServerConnection.createAttachmentFile` / encode path):** Force multipart file part Content-Type to `text/xml` or `application/xml` (do not rely solely on `guessContentTypeFromName`).
2. **Server (`PSFormContentParser`):** Optionally treat `.xml` filename as XML when Content-Type missing (careful with security/XXE existing validate flags).
3. **Tests:** Multipart round-trip + direct null-doc handler tests for `connect`, `validateArchive`, `PSCatalogHandler.processRequest`.
4. **Do not** remove `NULL_INPUT_DOC` guards without replacement validation.
5. **Out of scope without jar sources:** Changing Package Installer Swing UI flow inside `pspackagerui.jar`.

---

## Files touched by inventory (reference)

|                           Path                            |                 Role                  |
|-----------------------------------------------------------|---------------------------------------|
| `deployer/.../server/PSDeploymentHandler.java`            | Dispatch + majority of throw sites    |
| `deployer/.../catalog/server/PSCatalogHandler.java`       | Catalog throw site                    |
| `deployer/.../client/PSDeploymentManager.java`            | Client install orchestration          |
| `deployer/.../client/PSDeploymentServerConnection.java`   | HTTP multipart execute                |
| `system/.../content/PSFormContentParser.java`             | Multipart → `setInputDocument`        |
| `system/.../HTTPClient/Codecs.java`                       | Multipart encode + content-type guess |
| `system/.../error/IPSDeploymentErrors.java`               | Error code 8                          |
| `deployer/.../error/PSDeployErrorStringBundle.properties` | User message                          |
| `system/installResources/install.xml`                     | Package Installer launch classpath    |
| `deployer/.../server/PSLocalDeployerClient.java`          | In-process install (not HTTP)         |
| `system/.../server/job/PSJobHandler.java`                 | Job null-doc (code 5)                 |

---

## Acceptance checklist (Slice 1)

- [x] Named code paths for client request construction and server `NULL_INPUT_DOC` throw sites
- [x] Documented which install stages require / omit input documents (upload omits; document APIs require)
- [x] Repro notes + “cannot live-repro here / historical likely-fixed comments” with code evidence
- [x] Findings under `docs/ai-generated/tasks/955-package-installer-null-input-doc/` for Child 2

> Co-Authored by Grok Build using grok-4.5 with agent main.

