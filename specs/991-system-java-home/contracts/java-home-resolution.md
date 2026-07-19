# Contract: Runtime Java Home Resolution

**Feature**: 991-system-java-home  
**Consumers**: CMS Jetty start/stop/service; DTS Tomcat start/stop/service; service env writers (`/etc/default`, Windows Procrun JavaHome)

## Purpose

Define the **behavioral contract** every production start/stop/service path must implement so operators get one mental model across platforms.

## Inputs

| Input | Required | Description |
|-------|----------|-------------|
| `INSTALL_ROOT` | yes | Absolute product install directory for this surface |
| Process environment | no | Especially `JAVA_HOME` |
| `{INSTALL_ROOT}/java.properties` | no | Product config if present |
| `{INSTALL_ROOT}/JRE` / `JRE64` | no | Legacy operator copy/symlink |
| `PATH` | no | For launcher discovery |

## Algorithm (normative)

```text
function resolve_java_home(INSTALL_ROOT):
  attempts = []

  # 1. Product config
  props = load_optional(INSTALL_ROOT + "/java.properties")
  if props.JAVA_HOME:
    if is_valid_java21_home(props.JAVA_HOME):
      return success(props.JAVA_HOME, PRODUCT_CONFIG)
    attempts += (PRODUCT_CONFIG, props.JAVA_HOME, "invalid or not Java 21")
  if props.JAVA:
    home = infer_home_from_launcher(props.JAVA)
    if home and is_valid_java21_home(home):
      return success(home, PRODUCT_CONFIG)
    attempts += (PRODUCT_CONFIG, props.JAVA, "launcher invalid or not Java 21")

  # 2. Process environment
  if env.JAVA_HOME:
    if is_valid_java21_home(env.JAVA_HOME):
      return success(env.JAVA_HOME, PROCESS_ENV)
    attempts += (PROCESS_ENV, env.JAVA_HOME, "invalid or not Java 21")

  # 3. Legacy install-dir layout (operator-provided only)
  for rel in ["JRE", "JRE64"]:
    candidate = INSTALL_ROOT + "/" + rel   # platform join
    if is_valid_java21_home(candidate):
      return success(candidate, INSTALL_DIR_JRE*)
    if path_exists(candidate):
      attempts += (INSTALL_DIR_JRE*, candidate, "present but not valid Java 21")

  # 4. PATH
  launcher = find_java_on_path()
  if launcher:
    home = infer_home_from_launcher(launcher)
    if home and is_valid_java21_home(home):
      return success(home, PATH)
    attempts += (PATH, launcher, "not Java 21 or home unresolved")

  # 5. Fail
  fail(message including required major version 21 and attempts)
```

## `is_valid_java21_home(path)`

Must all hold:
1. Path exists as a directory (follow symlinks for validity).  
2. Launcher exists: Unix `bin/java` (executable); Windows `bin\java.exe`.  
3. Running the launcher reports major version **21** (parse `-version` or showSettings).

## Outputs (on success)

| Output | Description |
|--------|-------------|
| `JAVA_HOME` | Absolute validated home |
| `JAVA` | Absolute launcher path (`$JAVA_HOME/bin/java` or `.exe`) |
| optional log line | Source used (config/env/install-dir/PATH) for diagnostics |

## Failure message (minimum content)

- Statement that no compatible Java was found  
- Required major version: **21**  
- At least a summary of sources tried (config / env / install-dir / PATH)  
- Non-zero exit (scripts) / non-success return for installers  

## Non-goals

- Accepting Java 8/11/17 as success on 8.2  
- Creating `<InstallDir>/JRE` automatically by copying a system JRE (optional symlink is **not** required for success)  
- Changing application classpath layout  

## Platform notes

- Path joining must use OS-native separators (scripts) or NIO `Path` (Java).  
- Windows services must store **absolute** `JavaHome` / `jvm.dll` parent home consistent with this result.  
- Unix `/etc/default/<service>` `JAVA_HOME=` must match this result at service install time.  
