# perc-doctor

Operator CLI for diagnosing and safely cleaning Percussion CMS install trees.

**Issues:** [#2213](https://github.com/intersoftdatalabs-in/percussioncms/issues/2213) (parent), [#2217](https://github.com/intersoftdatalabs-in/percussioncms/issues/2217) (`clean-install-backups`)  
**Package:** `com.intsof.percussioncms.doctor`  
**Shipped commands:** `clean-heap-dumps`, `clean-install-backups` with global `--dry-run` / `--install-root` / `-v`

Later slices (tracked on #2213 / residual issues): `clean-logs`, admin HTTP API, distribution `bin` packaging.

## Build

From this module directory:

```bash
../../mvnw clean install
```

Windows:

```bat
..\..\mvnw.cmd clean install
```

## Run

```bash
java -jar target/perc-doctor-8.2.0-SNAPSHOT.jar [options] <command>
```

Or with the compiled classes on the classpath (after install).

### Global options

| Flag | Meaning |
|------|---------|
| `--install-root <path>` | CMS install root (default: current working directory) |
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

## Safety model

1. Resolve and validate install root (must exist and be a directory).
2. Walk only under that root; skip / reject candidates that escape the root.
3. Match allowlisted patterns only (per command; no user-supplied globs).
4. If `--dry-run`, stop after inventory.
5. On apply, re-check containment immediately before each delete.

## Deferred (not in this module slice)

- `clean-logs` — age / keep-current log cleanup
- Admin-authenticated HTTP API mirroring CLI
- Distribution packaging (`bin/perc-doctor` / fat jar in install tree)
