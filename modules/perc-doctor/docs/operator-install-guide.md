# perc-doctor operator install guide

**Module:** `modules/perc-doctor`  
**Issues:** [#2220](https://github.com/intersoftdatalabs-in/percussioncms/issues/2220) (packaging), [#2213](https://github.com/intersoftdatalabs-in/percussioncms/issues/2213) (parent), [#2232](https://github.com/intersoftdatalabs-in/percussioncms/issues/2232) (`clean-temp`)

This guide is for operators running **perc-doctor** against a CMS install on Windows or Linux. Prefer **dry-run first** for every command; only apply after you review the candidate list and sizes.

## Install layout

After a CMS distribution build that includes this module, the install tree ships:

```text
<install-root>/
  bin/
    perc-doctor          # Unix launcher (executable)
    perc-doctor.bat      # Windows launcher
    perc-doctor.jar      # Runnable jar (Main-Class = DoctorCli)
```

The launchers set `--install-root` to the **parent of `bin/`** (the install root) when you do not pass `--install-root` yourself. Paths are resolved from the script location — never from a hardcoded user home.

### Cross-platform path rules

| Do | Do not |
|----|--------|
| Use `<install-root>` (e.g. `/opt/Percussion` or `C:\Percussion`) | Embed a personal profile directory path |
| Prefer `bin/perc-doctor` / `bin\perc-doctor.bat` from the install | Assume a fixed drive letter or username |
| Pass `--install-root` only when the tree is not the parent of `bin/` | Embed machine-specific absolute paths in scripts or runbooks |
| Use JDK 21+ via `JAVA_HOME` or `PATH` | Rely on an optional bundled JRE path that may not exist |

### Alternate: module build / java -jar

From a developer or support workstation after `mvnw clean install` in this module:

```bash
# Exploded dist (created by assembly)
java -jar target/perc-doctor-*-dist/bin/perc-doctor.jar --help

# Or the versioned module jar
java -jar target/perc-doctor-8.2.0-SNAPSHOT.jar --help
```

Windows:

```bat
java -jar target\perc-doctor-8.2.0-SNAPSHOT.jar --help
```

## Global options

| Flag | Meaning |
|------|---------|
| `--install-root <path>` | CMS install root (wrapper default: parent of `bin/`; jar default: current working directory) |
| `--dry-run` | **Report only** — list what would change; **no deletes / writes** |
| `-v` / `--verbose` | Path-level detail (recommended with dry-run) |
| `-h` / `--help` | Usage |

## Safety first: dry-run then apply

For every command:

1. Run with `--dry-run -v` and read `candidates=` / `bytes=` / path list.
2. Confirm only intended allowlisted paths appear under the install root.
3. Re-run **without** `--dry-run` to apply.
4. Prefer off-hours for apply when reclaiming large log trees.

## Commands

### `diagnose` / `health` (read-only)

Run a non-mutating install checklist (layout dirs, free disk, key config presence, Java version, known log dirs). **Never deletes.** Alias: `health` is the same command. Safe to run anytime; prefer `-v` for checklist detail.

**Linux / macOS:**

```bash
cd /opt/Percussion
./bin/perc-doctor -v diagnose
# or
./bin/perc-doctor --dry-run -v health
```

**Windows:**

```bat
cd /d C:\Percussion
bin\perc-doctor.bat -v diagnose
```

Exit code is non-zero when any check is `FAIL` (e.g. missing `jetty/base` or `rxconfig`). WARN-only outcomes still exit 0.

### `clean-heap-dumps`

Removes recursive `*.hprof` under the install root (case-insensitive). Scope is hard-limited to `--install-root`.

**Dry-run first (Linux / macOS install):**

```bash
cd /opt/Percussion
./bin/perc-doctor --dry-run -v clean-heap-dumps
```

**Dry-run first (Windows install):**

```bat
cd /d C:\Percussion
bin\perc-doctor.bat --dry-run -v clean-heap-dumps
```

**Apply (only after review):**

```bash
./bin/perc-doctor -v clean-heap-dumps
```

```bat
bin\perc-doctor.bat -v clean-heap-dumps
```

### `clean-install-backups`

Removes **allowlisted** installer/upgrade backup artifacts only (`AppServer_backup_*.zip`, `*.bak`, `*.backup`). No user-supplied globs.

**Dry-run first:**

```bash
./bin/perc-doctor --install-root /opt/Percussion --dry-run -v clean-install-backups
```

```bat
bin\perc-doctor.bat --install-root C:\Percussion --dry-run -v clean-install-backups
```

**Apply:**

```bash
./bin/perc-doctor --install-root /opt/Percussion -v clean-install-backups
```

```bat
bin\perc-doctor.bat --install-root C:\Percussion -v clean-install-backups
```

### `clean-logs`

Reclaims space under known log directories only:

| Relative path | Role |
|---------------|------|
| `jetty/base/logs` | Jetty / CMS logs |
| `jetty/base/modules/perc-logging/logs` | perc-logging module logs |
| `Deployment/Server/logs` | DTS Tomcat logs |

| Flag | Meaning |
|------|---------|
| `--older-than <duration>` | Only files older than duration (`7d`, `24h`, `30m`, …) |
| `--keep-current` | Default: never delete active current `*.log` / `*.out` basenames |
| `--no-keep-current` | Allow deleting those basenames (still subject to age filter if set) |

**Dry-run first (keep current logs, older than 7 days):**

```bash
./bin/perc-doctor --install-root /opt/Percussion --dry-run -v clean-logs --older-than 7d
```

```bat
bin\perc-doctor.bat --install-root C:\Percussion --dry-run -v clean-logs --older-than 7d
```

**Apply:**

```bash
./bin/perc-doctor --install-root /opt/Percussion -v clean-logs --older-than 14d
```

```bat
bin\perc-doctor.bat --install-root C:\Percussion -v clean-logs --older-than 14d
```

#### OS log rotation (Linux logrotate + Windows schedule)

`clean-logs` is an **age-based purge** of allowlisted files. It does **not** replace OS-level rotation of continuous files such as `catalina.out`. Default samples ship with the product (issue [#2348](https://github.com/intersoftdatalabs-in/percussioncms/issues/2348)):

| Path | Contents |
|------|----------|
| `<install-root>/rxconfig/Installer/logrotate/` | `percussion-cms`, `percussion-dts`, `schedule-clean-logs.ps1`, `README.md` |
| Standalone DTS: `<dts-root>/logrotate/` | `percussion-dts`, short README |

**Not auto-enabled.** Operators copy the Linux fragments into `/etc/logrotate.d/` only with consent after path substitution and `logrotate -d` dry-run. Prefer **`copytruncate`** so Jetty/Tomcat keep writing without a restart.

Coexistence:

| Layer | Role |
|-------|------|
| Log4j2 RollingFile | Size-based app log rollover (CMS perc-logging, DTS service configs) |
| OS logrotate samples | Daily / 14 compressed archives for `*.log` / `*.out` under known roots |
| `perc-doctor clean-logs` | Manual or scheduled purge of aged allowlisted files |

**Windows:** schedule `perc-doctor clean-logs --older-than 14d` (Task Scheduler) or use `rxconfig/Installer/logrotate/schedule-clean-logs.ps1` (defaults to dry-run). Full steps: `rxconfig/Installer/logrotate/README.md`.

### `clean-temp`

Removes **files** under known install temp / work directories only. Prefer stopping CMS / DTS first so files are not locked. Allowlisted roots themselves are retained.

| Relative path | Role |
|---------------|------|
| `temp` | CMS install temp |
| `jetty/base/work` | Jetty work directory |
| `Deployment/Server/temp` | DTS Tomcat temp |
| `Deployment/Server/work` | DTS Tomcat work |

**Dry-run first:**

```bash
./bin/perc-doctor --install-root /opt/Percussion --dry-run -v clean-temp
```

```bat
bin\perc-doctor.bat --install-root C:\Percussion --dry-run -v clean-temp
```

**Apply (only after review; prefer CMS/DTS stopped):**

```bash
./bin/perc-doctor --install-root /opt/Percussion -v clean-temp
```

```bat
bin\perc-doctor.bat --install-root C:\Percussion -v clean-temp
```

### `check-config`

Read-only **value / misconfig** checklist on documented config files (beyond presence-only checks). Always non-mutating; `--dry-run` is accepted for flag parity.

Scoped files:

- `rxconfig/Server/server.properties` — e.g. `enableDebugTools`, CSRF disable, `bindPort`, `requireHTTPS`
- `rxconfig/Installer/rxrepository.properties` — required JDBC keys, driver/backend consistency, weak `PWD`, plaintext `PWD_ENCRYPTED`

Exit code is non-zero when any check is **FAIL**. Password values are never printed.

**Safe inventory (Linux / macOS):**

```bash
./bin/perc-doctor --install-root /opt/Percussion --dry-run -v check-config
```

**Safe inventory (Windows):**

```bat
bin\perc-doctor.bat --install-root C:\Percussion -v check-config
```

### `fix-permissions`

Reports (and on POSIX can fix) **allowlisted** mode / access issues only — no shell, no user globs. Prefer dry-run first.

| Target | Fix behavior |
|--------|----------------|
| `bin/perc-doctor` (+ `.bat` / `.jar` report) | POSIX: add owner-execute on the Unix script when missing |
| Known log directories | POSIX: ensure owner rwx when the directory exists |
| Key `rxconfig` property files | POSIX: ensure owner-read when present |

On Windows the command reports readable/writable and does **not** rewrite ACLs.

**Dry-run first:**

```bash
./bin/perc-doctor --install-root /opt/Percussion --dry-run -v fix-permissions
```

```bat
bin\perc-doctor.bat --install-root C:\Percussion --dry-run -v fix-permissions
```

**Apply (POSIX mode bits only):**

```bash
./bin/perc-doctor --install-root /opt/Percussion -v fix-permissions
```


## Distribution packaging (developers)

Module `mvnw clean install` attaches:

| Artifact | Role |
|----------|------|
| `perc-doctor-<version>.jar` | Runnable Main-Class jar (no runtime deps) |
| `perc-doctor-<version>-dist.zip` | `bin/perc-doctor`, `bin/perc-doctor.bat`, `bin/perc-doctor.jar` |
| `target/perc-doctor-<version>-dist/` | Exploded copy of the same layout |

`modules/perc-distribution-tree` unpacks the `dist` zip into the CMS assembly so operators receive the launchers under `<install-root>/bin`.

## Related

- Module README: [../README.md](../README.md)
- Parent epic: [#2213](https://github.com/intersoftdatalabs-in/percussioncms/issues/2213)
- `clean-temp` slice: [#2232](https://github.com/intersoftdatalabs-in/percussioncms/issues/2232)
- Admin HTTP API: shipped with MVP cluster ([#2219](https://github.com/intersoftdatalabs-in/percussioncms/issues/2219) / [#2230](https://github.com/intersoftdatalabs-in/percussioncms/pull/2230))
