# Residual smoke: Package Installer `NULL_INPUT_DOC` (Slice 3 / #2266)

Parent epic: [#955](https://github.com/intersoftdatalabs-in/percussioncms/issues/955)  
Depends on Slice 2 fix: [#2265](https://github.com/intersoftdatalabs-in/percussioncms/issues/2265) / [PR #2271](https://github.com/intersoftdatalabs-in/percussioncms/pull/2271) (merged)  
Inventory: [2264-inventory.md](./2264-inventory.md)

## Agent-safe automated residual (required overnight)

Live Package Installer + CMS was **not** assumed available for overnight agents. Residual automation lives in the `deployer` module:

| Test | Role |
|------|------|
| `PSDeploymentServerConnectionXmlMultipartTest` | Slice 2 encode + explicit `application/xml` |
| `PSNullInputDocHandlerTest` | Slice 2 null-doc → error 8 throw sites |
| `PSPackageInstallerNullInputDocResidualSmokeTest` | Slice 3 residual: install-critical request types, File encode path, MIME gate, handlers with present doc |

### Run

From repo root (or `deployer/`):

```bash
cd deployer
../mvnw clean install
# or focused:
../mvnw -Dtest=PSPackageInstallerNullInputDocResidualSmokeTest,PSDeploymentServerConnectionXmlMultipartTest,PSNullInputDocHandlerTest test
```

Windows (PowerShell), same module:

```powershell
cd deployer
..\mvnw.cmd clean install
```

**Pass criteria:** all three test classes green; multipart bodies for connect / validateArchive / catalog-style roots declare `Content-Type: application/xml`; null input doc still maps to `IPSDeploymentErrors.NULL_INPUT_DOC` (8); present document does **not** surface as error 8 from catalog / validateArchive entry.

## Live residual (optional — human / env with CMS)

Do **not** invent pass results. Run only when a running CMS, Package Installer client, and a sample package are available. **No secrets in logs or issue comments.**

### Prerequisites

1. CMS instance running (e.g. local qa-up / installed server) with deployer servlet reachable.
2. Package Installer launchable from the install tree (`system/installResources` / product docs for `PackageInstaller` / `pspackagerui` as shipped).
3. Sample package under the install tree, typically:
   - `<CMSinstall>/Packages/Percussion/*.ppkg`
   - or repo-sourced packages under `modules/perc-packages/src/main/resources` when packaged into a build.
4. Deployer/admin credentials available **only** in the operator environment (do not commit or paste passwords).

### Steps

1. Launch **Package Installer**.
2. Select any package from `<CMSinstall>/Packages/Percussion/` (or another known-good `.ppkg`).
3. Provide server host, port, protocol, and deployer-capable credentials.
4. Click **Install** (or the product’s validate-then-install flow).
5. Observe result:
   - **Pass:** install proceeds past connection/catalog/validate without  
     `PSDeployException: Input document expected by server, none found`  
     (`NULL_INPUT_DOC` / error 8).
   - **Fail:** capture exact exception text, request type if logged, CMS build/version, and JRE version; comment on #955 and re-open residual if needed.

### What this does *not* prove

- Full package matrix / all content types.
- In-process install via `PSLocalDeployerClient` (that path never uses HTTP multipart).
- Opaque Swing orchestration inside `pspackagerui.jar` beyond the HTTP document APIs.

## Env gap (overnight default)

| Item | Overnight agent |
|------|-----------------|
| Live CMS | Not available / not assumed |
| Package Installer UI | Not driven |
| Agent-safe multipart + handler smoke | **Run** (see above) |
| Live pass/fail | Document steps only; do not claim live pass |

## Slice closure criteria

- [x] Agent-safe residual smoke automated and green in `deployer`
- [x] Live residual steps documented (this file + issue comments)
- [ ] Live install executed (optional; human when env exists)
- [x] Results recorded on #2266 and parent #955 agent progress

> Co-Authored by Grok Build using grok-4.5 with agent main.
