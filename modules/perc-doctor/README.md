# perc-doctor

Operator CLI for diagnosing and safely cleaning Percussion CMS install trees.

**Issues:** [#2213](https://github.com/intersoftdatalabs-in/percussioncms/issues/2213) (parent), [#2217](https://github.com/intersoftdatalabs-in/percussioncms/issues/2217) (`clean-install-backups`), [#2218](https://github.com/intersoftdatalabs-in/percussioncms/issues/2218) (`clean-logs`), [#2219](https://github.com/intersoftdatalabs-in/percussioncms/issues/2219) (admin HTTP API), [#2220](https://github.com/intersoftdatalabs-in/percussioncms/issues/2220) (dist packaging + install guide), [#2232](https://github.com/intersoftdatalabs-in/percussioncms/issues/2232) (`clean-temp`)  
**Package:** `com.intsof.percussioncms.doctor` (+ `...doctor.api` for HTTP)  
**Shipped commands:** `clean-heap-dumps`, `clean-install-backups`, `clean-logs`, `clean-temp` with global `--dry-run` / `--install-root` / `-v`  
**HTTP:** Admin-only `POST .../maintenance/doctor/{command}` (wired in sitemanage)

**Operator install guide (dry-run-first examples):** [docs/operator-install-guide.md](docs/operator-install-guide.md)

## Build

From this module directory:

```bash
../../mvnw clean install
```

Windows:

```bat
..\..\mvnw.cmd clean install
```

Produces:

| Output | Description |
|--------|-------------|
| `target/perc-doctor-8.2.0-SNAPSHOT.jar` | Runnable jar (`Main-Class` = `DoctorCli`; no runtime deps) |
| `target/perc-doctor-8.2.0-SNAPSHOT-dist.zip` | Install layout: `bin/perc-doctor`, `bin/perc-doctor.bat`, `bin/perc-doctor.jar` |
| `target/perc-doctor-8.2.0-SNAPSHOT-dist/` | Exploded dist layout |

CMS distribution (`modules/perc-distribution-tree`) unpacks the `dist` classifier zip into the install assembly so operators get the launchers under `<install-root>/bin`.

## Run from a CMS install (preferred)

```bash
# Linux / macOS — dry-run first
/opt/Percussion/bin/perc-doctor --dry-run -v clean-heap-dumps

# Windows — dry-run first
C:\Percussion\bin\perc-doctor.bat --dry-run -v clean-heap-dumps
```

Wrappers default `--install-root` to the parent of `bin/` (portable; no hardcoded user home). Override with `--install-root` when needed.

## Run from a module build

```bash
java -jar target/perc-doctor-8.2.0-SNAPSHOT.jar [options] <command> [command-options]
```

Or via the exploded dist:

```bash
./target/perc-doctor-8.2.0-SNAPSHOT-dist/bin/perc-doctor --help
```

Windows:

```bat
target\perc-doctor-8.2.0-SNAPSHOT-dist\bin\perc-doctor.bat --help
```

### Global options

| Flag | Meaning |
|------|---------|
| `--install-root <path>` | CMS install root (default: current working directory for raw jar; parent of `bin/` for wrappers) |
| `--dry-run` | Report only — **never** deletes or writes |
| `-v` / `--verbose` | Path-level detail |
| `-h` / `--help` | Usage |

### Commands

#### `clean-heap-dumps`

Remove Java heap dumps under the install tree.

- **Allowlist:** files whose names end with `.hprof` (case-insensitive)
- **Scope:** only under `--install-root`; paths outside the root are never deleted
- **Dry-run:** inventories matches + sizes without deleting

Examples:

```bash
# Preview reclaimable heap dumps (safe)
java -jar target/perc-doctor-8.2.0-SNAPSHOT.jar \
  --install-root /opt/Percussion --dry-run -v clean-heap-dumps

# Apply (Windows example)
java -jar target\perc-doctor-8.2.0-SNAPSHOT.jar ^
  --install-root C:\Percussion -v clean-heap-dumps
```

#### `clean-install-backups`

Remove allowlisted installer / upgrade backup artifacts left in the install tree.

Patterns are documented from `modules/perc-distribution-tree` (`install.xml` `zip_AppServer`, assembly/install `**/*.bak` / `**/*.backup` excludes, and known upgrade copies such as `Navigation.properties.backup`). **No arbitrary user globs.**

| Pattern | Source |
|---------|--------|
| `AppServer_backup_<timestamp>.zip` | `install.xml` target `zip_AppServer` |
| `**/*.bak` | assembly / install excludes (e.g. `ResourceBundle.tmx.bak`) |
| `**/*.backup` | assembly / install excludes (includes `*.properties.backup`) |

- **Scope:** only under `--install-root`; paths outside the root are never deleted
- **Dry-run:** inventories candidates + sizes without deleting

Examples:

```bash
# Preview reclaimable install backups (safe)
java -jar target/perc-doctor-8.2.0-SNAPSHOT.jar \
  --install-root /opt/Percussion --dry-run -v clean-install-backups

# Apply (Windows example)
java -jar target\perc-doctor-8.2.0-SNAPSHOT.jar ^
  --install-root C:\Percussion -v clean-install-backups
```

#### `clean-logs`

Reclaim space from **known** log directories under the install root without nuking active server logs carelessly.

##### Target log locations (relative to `--install-root`)

| Relative path | Role |
|---------------|------|
| `jetty/base/logs` | Jetty / CMS logs (`install.xml` mkdirs; Log4j2 `server.log`, rotations, `audit/`) |
| `jetty/base/modules/perc-logging/logs` | Log4j2 default relative to perc-logging module config |
| `Deployment/Server/logs` | DTS Tomcat (`catalina.base`/logs — `catalina.log`, gzipped rotations, etc.) |

Missing directories are skipped (not an error). Only files under these roots whose names end with `.log`, `.log.gz`, or `.out` are candidates. **No user-supplied globs; no walk of the entire install for arbitrary logs.**

##### Options

| Flag | Meaning |
|------|---------|
| `--older-than <duration>` | Only files with last-modified **older** than the duration. Units: `s`, `m`, `h`, `d`, `w` (e.g. `7d`, `24h`, `30m`). Omit to ignore age. |
| `--keep-current` | **Default.** Never delete identifiable active current logs (`server.log`, `catalina.log`, `catalina.out`, … — non-dated `*.log` / `*.out`). |
| `--no-keep-current` | Allow deleting those active basenames (still subject to `--older-than` if set). |

- **Scope:** only under `--install-root` and the allowlisted log dirs
- **Dry-run:** inventories candidates + sizes without deleting
- Rotated / compressed names (embedded `yyyy-MM-dd`, `*.log.gz`, `name.log.N`) are not treated as current

Examples:

```bash
# Preview logs older than 7 days (keep active *.log)
java -jar target/perc-doctor-8.2.0-SNAPSHOT.jar \
  --install-root /opt/Percussion --dry-run -v clean-logs --older-than 7d

# Apply (Windows example)
java -jar target\perc-doctor-8.2.0-SNAPSHOT.jar ^
  --install-root C:\Percussion -v clean-logs --older-than 14d

# Delete all non-current log candidates (no age filter)
java -jar target/perc-doctor-8.2.0-SNAPSHOT.jar \
  --install-root /opt/Percussion --dry-run -v clean-logs
```

#### `clean-temp`

Reclaim space from **known** install temp / work directories under the install root. Prefer stopping CMS / DTS processes before apply so temp files are not locked.

##### Target temp / work locations (relative to `--install-root`)

| Relative path | Role |
|---------------|------|
| `temp` | CMS install temp (`install.xml` creates `${install.dir}/temp`) |
| `jetty/base/work` | Jetty work directory (assembly / install layout) |
| `Deployment/Server/temp` | DTS Tomcat `catalina.base`/temp |
| `Deployment/Server/work` | DTS Tomcat `catalina.base`/work |

Missing directories are skipped (not an error). **Only files under these roots** are candidates. The allowlisted root directories themselves are **never** deleted (empty nested subdirs may be removed best-effort after apply). **No user-supplied globs; no walk of the entire install for arbitrary temp files.**

- **Scope:** only under `--install-root` and the allowlisted temp/work dirs
- **Dry-run:** inventories candidates + sizes without deleting

Examples:

```bash
# Preview reclaimable temp/work files (safe)
java -jar target/perc-doctor-8.2.0-SNAPSHOT.jar \
  --install-root /opt/Percussion --dry-run -v clean-temp

# Apply (Windows example)
java -jar target\perc-doctor-8.2.0-SNAPSHOT.jar ^
  --install-root C:\Percussion -v clean-temp
```

## Safety model

1. Resolve and validate install root (must exist and be a directory).
2. Walk only under that root (and for `clean-logs` / `clean-temp`, only under allowlisted dirs); skip / reject candidates that escape the root.
3. Match allowlisted patterns only (per command; no user-supplied globs).
4. If `--dry-run`, stop after inventory.
5. On apply, re-check containment immediately before each delete.
6. For `clean-logs`, apply `--keep-current` and `--older-than` before any delete.
7. For `clean-temp`, never remove the allowlisted temp/work root directories themselves.

## Admin HTTP API (slice #2219)

When the CMS server is running, doctor commands are also available as an **Admin-only** REST surface next to the existing maintenance manager.

| | |
|--|--|
| **Method / path** | `POST /Rhythmyx/services/maintenance/doctor/{command}` |
| **Commands** | `clean-heap-dumps`, `clean-install-backups`, `clean-logs`, `clean-temp` (same tokens as CLI) |
| **Auth hard gate** | **Admin role only.** Anonymous and non-admin callers receive **HTTP 403**. |
| **Content-Type** | `application/json` (XML also accepted/produced by the host stack) |

### Request body (`DoctorRequest`)

| Field | Type | Default | Meaning |
|-------|------|---------|---------|
| `dryRun` | boolean | **`true` when omitted/null** | Report only — **no deletes**. Explicit apply requires `"dryRun": false`. |
| `installRoot` | string | Server RX install root (`rxdeploydir` / resolved install dir) | Optional; when set must normalize to the **same** path as the host default. Filesystem I/O always uses the host-provided root (never a client path) — arbitrary tree override is rejected (**HTTP 400**) to block path injection. |
| `olderThan` | string | unset | `clean-logs` only — e.g. `"7d"`, `"24h"` |
| `keepCurrent` | boolean | `true` when omitted/null | `clean-logs` only — retain active current logs |

**Dry-run is the default on the API** (unlike the CLI, where `--dry-run` is opt-in). This reduces risk of accidental deletes from HTTP clients. Document this to operators: inventory is safe by default; destructive apply is opt-in via `"dryRun": false`.

### Response (`DoctorReportView`)

Same structured report the CLI produces (candidate paths, sizes, status `WOULD_DELETE` / `DELETED` / `SKIPPED` / `FAILED`, totals).

Example (safe inventory):

```http
POST /Rhythmyx/services/maintenance/doctor/clean-heap-dumps
Content-Type: application/json
Authorization: (Admin session)

{"dryRun": true}
```

```json
{
  "command": "clean-heap-dumps",
  "installRoot": "/opt/Percussion",
  "dryRun": true,
  "candidateCount": 2,
  "deletedCount": 0,
  "failedCount": 0,
  "totalBytes": 12345678,
  "entries": [
    {"path": "/opt/Percussion/java_pid1.hprof", "sizeBytes": 1000, "status": "WOULD_DELETE", "detail": null}
  ]
}
```

Example apply (Admin only; deletes under install root):

```json
{"dryRun": false}
```

### Host wiring

- Resource class: `com.intsof.percussioncms.doctor.api.DoctorRestService` (`@Path("/doctor")`)
- Mounted on the **maintenance** JAX-RS server in sitemanage (`address="/maintenance"`)
- Admin gate: `com.percussion.doctor.PSDoctorAdminChecker` (`IPSUserService.isAdminUser`)
- Default install root: `com.percussion.doctor.PSDoctorInstallRootProvider` (`PathUtils.getRxDir()`)

## Deferred (not in this module slice)

`clean-temp` is delivered under [#2232](https://github.com/intersoftdatalabs-in/percussioncms/issues/2232). Further residuals (e.g. `fix-permissions` / deeper `check-config`) stay on the parent epic [#2213](https://github.com/intersoftdatalabs-in/percussioncms/issues/2213).
