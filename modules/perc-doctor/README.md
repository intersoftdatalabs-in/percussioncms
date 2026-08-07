# perc-doctor

Operator CLI for diagnosing and safely cleaning Percussion CMS install trees.

**Issues:** [#2213](https://github.com/intersoftdatalabs-in/percussioncms/issues/2213) (parent), [#2217](https://github.com/intersoftdatalabs-in/percussioncms/issues/2217) (`clean-install-backups`), [#2218](https://github.com/intersoftdatalabs-in/percussioncms/issues/2218) (`clean-logs`), [#2220](https://github.com/intersoftdatalabs-in/percussioncms/issues/2220) (dist packaging + install guide)  
**Package:** `com.intsof.percussioncms.doctor`  
**Shipped commands:** `clean-heap-dumps`, `clean-install-backups`, `clean-logs` with global `--dry-run` / `--install-root` / `-v`

**Operator install guide (dry-run-first examples):** [docs/operator-install-guide.md](docs/operator-install-guide.md)

Later slices (tracked on #2213 / residual issues): admin HTTP API.

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

## Safety model

1. Resolve and validate install root (must exist and be a directory).
2. Walk only under that root (and for `clean-logs`, only under allowlisted log dirs); skip / reject candidates that escape the root.
3. Match allowlisted patterns only (per command; no user-supplied globs).
4. If `--dry-run`, stop after inventory.
5. On apply, re-check containment immediately before each delete.
6. For `clean-logs`, apply `--keep-current` and `--older-than` before any delete.

## Deferred (not in this module slice)

- Admin-authenticated HTTP API mirroring CLI ([#2219](https://github.com/intersoftdatalabs-in/percussioncms/issues/2219))
