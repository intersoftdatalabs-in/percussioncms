```skill
---
name: percussioncms-dev
description: "Developer-focused commands for installing, operating, and querying Percussion CMS / DTS. Covers downloading releases, performing local dev installs, starting local instances, and using the REST API to list sites, folders, pages, and assets."
---

# Percussion CMS Developer Operations

Use this skill for developer day-to-day work: build/install the CMS and DTS, start local instances, and query the REST APIs from the command line. All scripts live under `.github/skills/percussioncms-dev/scripts/` as cross-platform Python entry points (Windows / Linux / macOS).

## Maven + Docker Compose (Canonical workflow)

Use Maven profiles and lifecycle phases as the primary dev workflow for local runtime and integration tests.

### Concise logging for agents (preferred command surface)

Use `./docker/scripts/perc-devctl.py` when an AI agent needs deterministic success/failure output and detailed logs on failure.

- Success format: `RESULT:OK STEP:<step> LOG:<path>`
- Failure format: `RESULT:FAIL STEP:<step> LOG:<path>`

Detailed logs are always written under `docker/logs/`.

Common commands:

```bash
./docker/scripts/perc-devctl.py up --build
./docker/scripts/perc-devctl.py verify --timeout-seconds 300
./docker/scripts/perc-devctl.py deploy-jar --jar modules/utils/target/<your-jar>.jar --target both --restart --verify
./docker/scripts/perc-devctl.py verify-fix --jar modules/utils/target/<your-jar>.jar --target both --restart --timeout-seconds 240
./docker/scripts/perc-devctl.py it-verify
./docker/scripts/perc-devctl.py down --volumes
```

Database and credential verification commands:

```bash
./docker/scripts/perc-devctl.py inspect-install
./docker/scripts/perc-devctl.py show-generated-passwords
```

`inspect-install` captures effective CMS and DTS DB settings from the running server config files.
`show-generated-passwords` captures `/opt/Percussion/var/config/generated/passwords` when present.

### Prerequisites

1. Build CMS + DTS artifacts:

   ```bash
   ./mvn-env.sh clean install -DskipTests=true
   ```
2. Configure compose environment:

   ```bash
   cp .env.compose.example .env.compose
   ```

### Lifecycle goals

- Start stack (MySQL 8 + CMS-DTS) and wait for readiness:

  ```bash
  ./mvn-env.sh -P docker-compose pre-integration-test
  ```
- Run integration tests only (against running stack):

  ```bash
  ./mvn-env.sh -P integration-test verify
  ```
- Stop and remove stack (+ volumes):

  ```bash
  ./mvn-env.sh -P docker-compose post-integration-test
  ```
- One-command flow (up + wait + integration tests + teardown):

  ```bash
  ./mvn-env.sh -P integration-test,docker-compose verify
  ```

### Startup modes

Set `PERC_INSTALL_MODE` in `.env.compose` to select behavior:

- `install-if-missing` (default): install once, then skip until marker is removed
- `install-always`: force installer/update on each start
- `skip-install`: no installer/update at startup

### Hot jar update testing

For fast module-level validation (e.g., `utils`, `perc-system`), build the module and copy the jar into the running container:

```bash
./mvn-env.sh -pl modules/utils -am package -DskipTests
./docker/scripts/hot-deploy-jar.py --jar modules/utils/target/<your-jar>.jar --target both --restart
```

`hot-deploy-jar.py` supports `--target cms|dts|both|/absolute/path` and optional `--container` override.

### Persisted developer-writable paths

The CMS-DTS container installs under `/opt/Percussion`. The following directories are persisted and writable via host bind mounts:

- `/opt/Percussion/ObjectStore`
- `/opt/Percussion/var`
- `/opt/Percussion/rxconfig`
- `/opt/Percussion/Deployment/Server/conf`
- `/opt/Percussion/jetty/base`

**Online Help:** https://percussioncmshelp.intsf.com

---

## Configuration Variables

Percussion CMS developer skills rely on your shell environment — there is no built-in `.env` file. Export (or set) the variables below before you run the scripts, or add them to your shell profile / PowerShell profile so they persist across sessions.

|     Variable      |                 Default                  |                   Description                    |
|-------------------|------------------------------------------|--------------------------------------------------|
| `CMS_BASE_URL`    | `http://localhost:9992`                  | Base URL of the running CMS instance             |
| `API_BASE`        | `http://localhost:9992/Rhythmyx/rest`    | REST API base URL                                |
| `GITHUB_REPO`     | `intersoftdatalabs-in/percussioncms`     | GitHub org/repo used for release downloads       |
| `CMS_INSTALL_DIR` | `~/percussioncms-install`                | CMS installation directory                       |
| `DTS_INSTALL_DIR` | `~/percussiondts-install`                | DTS installation directory                       |
| `JAVA_HOME`       | `JAVA_HOME_21` or another JDK 21 install | Used for installers and runtime                  |
| `PROJECT_ROOT`    | Auto-detected from `git rev-parse`       | Root of this workspace (used to find build jars) |
| `CMS_USER`        | `Admin`                                  | Default CMS admin username                       |
| `CMS_PASSWORD`    | (prompt or secret manager)               | CMS admin password (never commit hardcoded)      |

> Tip: Windows developers can set variables with `setx VAR value` (persisted) or `set VAR=value` for the current session. Treat the shell environment as the configuration source; no `.env` file is required.

---

## 1. Installation Skills

### 1.1 download_latest

Download the latest release artifacts from GitHub.

1. Run the Linux script:

   ```bash
   .github/skills/percussioncms-dev/scripts/download-latest.py
   ```
2. Windows developers can use the PowerShell helper:

   ```powershell
   .github/skills/percussioncms-dev/scripts/download-latest.py
   ```
3. The script calls `https://api.github.com/repos/${GITHUB_REPO}/releases/latest` and downloads `perc-distribution-tree.jar` into a `downloads/` folder by default.
   Add `--dts` (Linux/macOS) or `-DownloadDts` (PowerShell) when you also need `delivery-tier-distribution.jar`.
4. Report the release tag and file paths so downstream installers know which artifacts to use.

### 1.2 install_latest

Install the CMS from the local Maven build artifact. To install a release JAR, pass `--jar downloads/perc-distribution-tree.jar`.

1. Ensure the project has been built:

   ```bash
   ls "${PROJECT_ROOT}/modules/perc-distribution-tree/target/perc-distribution-tree.jar"
   ```
2. Run the Linux script:

   ```bash
   .github/skills/percussioncms-dev/scripts/install-cms.py
   ```

   To install a release JAR instead:

   ```bash
   .github/skills/percussioncms-dev/scripts/install-cms.py --jar downloads/perc-distribution-tree.jar
   ```
3. Windows developers can run:

   ```powershell
   .github/skills/percussioncms-dev/scripts/install-cms.py
   ```
4. Each script creates (or refreshes) a `JRE` symlink pointing to `JAVA_HOME` and verifies that `jetty/StartJetty.{sh|bat}` exists.

### 1.3 dev_install_latest

This mirrors `install_latest` but clarifies the developer intent. Use the same scripts; no extra flags are required.

### 1.4 install_latest_dts

Install DTS from the local build artifact (use `--jar downloads/delivery-tier-distribution.jar` for a release build).

1. Make sure the DTS artifact exists:

   ```bash
   ls "${PROJECT_ROOT}/deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution.jar"
   ```
2. Run the Linux script:

   ```bash
   .github/skills/percussioncms-dev/scripts/install-dts.py
   ```
3. Windows developers can run:

   ```powershell
   .github/skills/percussioncms-dev/scripts/install-dts.py
   ```
4. Each installer links `JRE -> JAVA_HOME` and checks for `${DTS_INSTALL_DIR}/Deployment/Server`.

### 1.5 dev_install_latest_dts

Same workflow as `install_latest_dts` — the scripts already default to the developer build artifacts.

---

## 2. Operation Skills

### 2.1 start_local_cms

1. Linux/macOS:

   ```bash
   .github/skills/percussioncms-dev/scripts/start-cms.py
   ```
2. Windows:

   ```powershell
   .github/skills/percussioncms-dev/scripts/start-cms.py
   ```
3. Wait for the CMS to be reachable (poll `${CMS_BASE_URL}/Rhythmyx/rest/folders/by-path/Assets`).
4. Stop the server by pressing CTRL-C in the terminal/PowerShell session.

### 2.2 start_local_dts

1. Linux/macOS:

   ```bash
   .github/skills/percussioncms-dev/scripts/start-dts.py
   ```
2. Windows:

   ```powershell
   .github/skills/percussioncms-dev/scripts/start-dts.py
   ```
3. Check the Tomcat logs or hit a known DTS endpoint to ensure the server is alive.

---

## 3. API Skills

All API calls target `${API_BASE}` (default `http://localhost:9992/Rhythmyx/rest`). Authentication is handled in the helpers.

### Authentication

- **All hosts:** invoke `python3 .github/skills/percussioncms-dev/scripts/api-client.py --method GET --endpoint /folders/by-path/Assets` (one-shot CLI; the original shell-function surface becomes per-call argparse arguments).
- Login: add `--login-form --user "${CMS_USER}" --password "${CMS_PASSWORD}" --endpoint /j_security_check` (omit `--endpoint` for the form path which targets `${API_BASE%/rest}/j_security_check` by default).

```bash
# Login (Linux/macOS, form-based)
# --endpoint must be /j_security_check for the form-based j_security_check login
# path; without it, the script returns EXIT_INVOCATION (exit 1).
python3 .github/skills/percussioncms-dev/scripts/api-client.py \
    --login-form \
    --endpoint /j_security_check \
    --user "${CMS_USER}" \
    --password "${CMS_PASSWORD}"
```

```powershell
# Login (Windows, form-based)
python3 .github\skills\percussioncms-dev\scripts\api-client.py `
    --login-form `
    --endpoint /j_security_check `
    --user $env:CMS_USER `
    --password $env:CMS_PASSWORD
```

### 3.1 api_list_sites

Call `/folders/by-path/Sites` to retrieve the list of site folders. Use `python3 .github/skills/percussioncms-dev/scripts/api-client.py --method GET --endpoint /folders/by-path/Sites` on every host. Responses include `subfolders` entries for each site.

### 3.2 api_list_folders

List folders within a site or nested path. Example:

```bash
python3 .github/skills/percussioncms-dev/scripts/api-client.py \
    --method GET \
    --endpoint /folders/by-path/MySite/FolderA
```

### 3.3 api_list_assets

Request `/folders/by-path/Assets` or `/assets/by-path/Assets/uploads/myfile.jpg` to explore assets. `DELETE` is supported, e.g. to remove `myasset.jpg`.

### 3.4 api_list_pages

Pages are returned as part of folder responses. Use `/pages/by-path/MySite/MyPage` to fetch a single page or inspect the `pages` array from a folder query.

---

## 4. Quick Reference

|          Skill           |                Trigger phrase                |
|--------------------------|----------------------------------------------|
| `download_latest`        | "download the latest Percussion CMS release" |
| `install_latest`         | "install the latest CMS release"             |
| `dev_install_latest`     | "install CMS from my local build"            |
| `install_latest_dts`     | "install the latest DTS release"             |
| `dev_install_latest_dts` | "install DTS from my local build"            |
| `start_local_cms`        | "start the local CMS"                        |
| `start_local_dts`        | "start the local DTS"                        |
| `api_list_sites`         | "list all sites"                             |
| `api_list_folders`       | "list folders in MySite"                     |
| `api_list_assets`        | "list assets"                                |
| `api_list_pages`         | "list pages in MySite"                       |

---

## 5. Troubleshooting

- **CMS won't start:** Ensure `JRE` points at a JDK 21 installation (`JAVA_HOME`).
- **API returns 401/403:** Re-run `api-client.py --login-form --user ... --password ...` to refresh the cookie jar, then retry the call.
  -- **Local build JAR missing:** Run `./mvn-env.sh clean install` to produce the artifacts.
- **Port conflict:** CMS defaults to 9992. Check `lsof` (Linux/macOS) or `netstat` (Windows) for other services.
- **Windows symlinks fail:** Run PowerShell as Administrator so `New-Item -ItemType SymbolicLink` succeeds.

```
```

