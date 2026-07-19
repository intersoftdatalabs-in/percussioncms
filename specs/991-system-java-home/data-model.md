# Data Model: System / Configurable Java Home

Logical entities for install-time selection and runtime resolution. No RDBMS tables.

## Entities

### JavaHome

| Field | Type | Notes |
|-------|------|--------|
| path | absolute filesystem path | Root of a JRE/JDK (`bin/java` or `bin/java.exe` present) |
| majorVersion | integer | Must be **21** for acceptance |
| vendorLabel | string (optional) | Display only (from `-version`) |
| source | enum | See ResolutionSource |

**Validation**:
- Path exists and is a directory (or symlink to directory)
- Launcher executable exists under `bin/` (platform-specific name)
- Major version parse succeeds and equals 21 for “eligible” / “resolved valid”

### ResolutionSource (enum)

| Value | Meaning | Precedence rank |
|-------|---------|-----------------|
| `PRODUCT_CONFIG` | Install-root `java.properties` | 1 (highest) |
| `PROCESS_ENV` | Process `JAVA_HOME` | 2 |
| `INSTALL_DIR_JRE` | `<InstallDir>/JRE` | 3 |
| `INSTALL_DIR_JRE64` | `<InstallDir>/JRE64` (legacy) | 3b (after JRE) |
| `PATH` | Discovered via `java` on PATH | 4 |
| `NONE` | Failure | — |

### JavaPropertiesFile

| Field | Type | Notes |
|-------|------|--------|
| location | path | `{InstallDir}/java.properties` |
| JAVA_HOME | string | Absolute home |
| JAVA | string | Absolute launcher path (recommended) |
| other keys | map | Reserved / existing keys preserved |

**Lifecycle**:
- **Created/updated** at install when a candidate is selected or unattended home is supplied  
- **Edited** by operators for post-install re-point (US5)  
- **Read** by resolve helpers on every start/stop/service install  

### JavaCandidate

| Field | Type | Notes |
|-------|------|--------|
| path | absolute path | Detected install |
| versionDisplay | string | Shown in interactive prompt |
| eligible | boolean | Major 21 + executable |

**Collections**: ordered list for interactive multi-select UI; size 0 / 1 / N drives fail / auto / prompt.

### ResolutionResult

| Field | Type | Notes |
|-------|------|--------|
| success | boolean | |
| javaHome | JavaHome or null | |
| source | ResolutionSource | |
| attempted | list of (source, path, reason) | For error messages |

### RuntimeSurface (enum)

| Value | Product root concept |
|-------|----------------------|
| `CMS_JETTY` | CMS install dir (parent of `jetty/`) |
| `DTS_PRODUCTION` | DTS production install layout |
| `DTS_STAGING` | DTS staging layout |
| `LEGACY_INSTALLER_HELPER` | `system/release/installer` scripts |

Each surface resolves relative to its **install root** for `java.properties` and install-dir JRE fallback.

## State transitions

### Install-time Java selection

```text
[Start install]
    → Discover candidates
    → 0 eligible → FAILED (guidance)
    → 1 eligible → AUTO_SELECTED → write java.properties → OK
    → N eligible + interactive → PROMPT → SELECTED → write → OK
    → N eligible + unattended without perc.java.home → FAILED or policy auto-first (prefer FAILED for safety)
    → unattended with perc.java.home → VALIDATE → write or FAILED
```

### Runtime resolve

```text
[Start/Stop/Service]
    → Try sources in precedence
    → first valid 21 home → RESOLVED (export JAVA_HOME/JAVA)
    → none → FAILED (list attempts + require 21)
```

### Operator re-point

```text
[Edit java.properties or set env]
    → Restart / reinstall service if service cached home
    → Next resolve uses new config (PRODUCT_CONFIG or PROCESS_ENV)
```

## Relationships

```text
JavaPropertiesFile ──writes──► JavaHome (selected)
JavaCandidate[] ──filter──► eligible → selection → JavaPropertiesFile
ResolutionResult ──uses──► JavaPropertiesFile, env, InstallDir JRE, PATH
RuntimeSurface ──owns──► install root paths for config + fallback
```

## Invariants
1. Product archive does not contain a JRE tree as the success path.  
2. `INSTALL_DIR_JRE*` is optional legacy, never the only documented new-install path.  
3. Major version for success is exactly **21** on 8.2.  
4. Paths in `java.properties` are absolute (or resolved to absolute at write time).  
