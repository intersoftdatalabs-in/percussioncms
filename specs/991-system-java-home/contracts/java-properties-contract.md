# Contract: Install-Root `java.properties`

**Feature**: 991-system-java-home  
**Location**: `{INSTALL_ROOT}/java.properties`  
**Format**: Java `.properties` file (ISO-8859-1 or UTF-8 as already used by product loaders; prefer ASCII paths)

## Purpose

Durable, post-install configuration of which Java home CMS/DTS runtime scripts use, so operators do **not** need a manual `<InstallDir>/JRE` copy/symlink.

## Keys (normative for this feature)

| Key | Required | Example | Description |
|-----|----------|---------|-------------|
| `JAVA_HOME` | recommended | `C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.x` or `/usr/lib/jvm/java-21-openjdk` | Absolute JRE/JDK home |
| `JAVA` | recommended | `.../bin/java` or `...\bin\java.exe` | Absolute launcher |

At least one of `JAVA_HOME` or `JAVA` MUST be present for the file to count as a product-config hit. Prefer writing **both** at install time.

## Writers

| Writer | When |
|--------|------|
| Interactive install / preinstall | After candidate selection or single auto-select |
| Unattended install | After validating `-Dperc.java.home` (or documented equivalent) |
| Operator (manual edit) | Post-install re-point (US5) |

## Readers

| Reader | Behavior |
|--------|----------|
| `resolve-java-home` helpers | Precedence source #1 |
| Service installers | Seed `/etc/default` / Procrun `--JavaHome` |
| Legacy `JettyStartUtils` / install-service.sh | Align key names; update any `JAVA=`-only parsers to also honor `JAVA_HOME` |

## Write rules

1. Paths MUST be absolute when written by the installer.  
2. Selected home MUST pass major-version **21** validation before write.  
3. On validation failure, do **not** write a “success” config pointing at a non-existent install-dir JRE.  
4. Preserve unknown existing keys when updating the file (merge, do not clobber unrelated properties).  

## Unattended input mapping

| Input | Maps to |
|-------|---------|
| `-Dperc.java.home=<path>` | Validate as home → write `JAVA_HOME` + derived `JAVA` |
| Env / response file field (documented in install docs) | Same validation pipeline |

Installer JVM may continue to use `perc.java.home` / `java.home` for **running the installer**; product runtime reads **`java.properties`** after install.

## Example

```properties
# Written by Percussion install — Java for CMS/DTS runtime
JAVA_HOME=/usr/lib/jvm/java-21-openjdk
JAVA=/usr/lib/jvm/java-21-openjdk/bin/java
```

## Anti-patterns

- Writing `JAVA_HOME=./JRE` or `JAVA_HOME=<InstallDir>/JRE` when that path does not exist  
- Documenting that the product ships a JRE into this file by default  
- Storing passwords or non-path secrets in this file  
