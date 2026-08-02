# Intersoft Common Utilities

Product-agnostic Java utilities for Intersoft Data Labs projects
([https://intsof.com](https://intsof.com)).

|  Coordinate  |                      Value                      |
|--------------|-------------------------------------------------|
| `groupId`    | `com.intsof.common`                             |
| `artifactId` | `utilities`                                     |
| `version`    | `0.0.1` (semver; independent of Percussion CMS) |
| License      | Apache License 2.0                              |
| Copyright    | Intersoft Data Labs                             |

## Third-party license inventory (`license.ThirdPartyLicenseInventory`)

Product-agnostic merge of a Maven-oriented inventory text file with **production**
npm packages from `package-lock.json` (lockfileVersion 2/3 `packages` map). No
Jackson or other runtime dependencies — includes a small JSON subset parser.

```java
import com.intsof.common.utilities.license.ThirdPartyLicenseInventory;
import java.nio.file.Path;

// Library API
var npm =
    ThirdPartyLicenseInventory.readProductionPackagesFromLockFile(
        Path.of("frontend/package-lock.json"), Path.of("."));
String section = ThirdPartyLicenseInventory.formatNpmSection(npm);
String merged =
    ThirdPartyLicenseInventory.mergeMavenAndNpm(mavenText, section, "My product inventory");

// Or write files (Maven half + lock list → merged THIRD-PARTY.txt)
ThirdPartyLicenseInventory.generateMergedInventory(
    projectRoot,
    outDir,
    ThirdPartyLicenseInventory.DEFAULT_MAVEN_FILE_NAME,
    ThirdPartyLicenseInventory.DEFAULT_NPM_FILE_NAME,
    ThirdPartyLicenseInventory.DEFAULT_MERGED_FILE_NAME,
    lockListFile,
    "My product inventory",
    true);
```

CLI (`main`) for Maven `exec-maven-plugin:java`:

```text
java -cp utilities-0.0.1.jar com.intsof.common.utilities.license.ThirdPartyLicenseInventory \
  --root <project-root> --require-maven [--title "..."] [--lock-list path] [--out-dir path]
```

Tests: `ThirdPartyLicenseInventoryTest`.

## User configuration (`UserConfiguration`)

Provides a portable per-user config root:

```text
${user.home}/.intsof/<application-name>/
```

Works on Windows, Linux, and macOS. Paths use `java.nio.file` only.

### Example

```java
import com.intsof.common.utilities.AppConfigurationFolder;
import com.intsof.common.utilities.UserConfiguration;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

// Default home: System.getProperty("user.home")
UserConfiguration config = UserConfiguration.openDefault();
AppConfigurationFolder app = config.createApplication("my-app");

Path settings = app.get("DefaultSettings.properties", true);
List<Path> files = app.listFiles();
app.addFile("notes.txt", "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8));
Optional<Path> existing = app.get("notes.txt");
app.removeFile("notes.txt");
```

For unit tests, inject a temporary home:

```java
UserConfiguration config = UserConfiguration.open(tempDir);
```

### API summary

|           Type           |                   Method                   |                   Behavior                   |
|--------------------------|--------------------------------------------|----------------------------------------------|
| `UserConfiguration`      | `openDefault()`                            | Uses `user.home`, ensures `~/.intsof` exists |
| `UserConfiguration`      | `open(Path userHome)`                      | Uses `userHome/.intsof` (tests / override)   |
| `UserConfiguration`      | `createApplication(name)`                  | Creates `~/.intsof/<name>/` if missing       |
| `UserConfiguration`      | `findApplication(name)`                    | Opens only if the folder already exists      |
| `AppConfigurationFolder` | `listFiles()`                              | Non-recursive regular files, sorted by name  |
| `AppConfigurationFolder` | `addFile(name)` / content overloads        | Create empty or write content                |
| `AppConfigurationFolder` | `get(name)` / `get(name, createIfMissing)` | NIO `Path` handle                            |
| `AppConfigurationFolder` | `removeFile(name)`                         | Delete if present                            |

Application and file names are validated (no path separators, `..`, or absolute paths). Resolved
paths must stay under the app directory.

### Security notes

- This library only manages **file locations**. Callers decide what content to store.
- **Do not** store passwords or secrets unless the consuming application has a deliberate,
  reviewed secret-storage design. Percussion installers intentionally omit passwords.

## Build

From this module (JDK 21, repo Maven wrapper):

```bat
..\mvnw.cmd clean install
```

```bash
../mvnw clean install
```

## Percussion installer usage

CMS and DTS installers share:

```text
~/.intsof/percussion/last-install.properties
```

with property prefixes `cms.`, `dts.prod.`, and `dts.stage.` (including `*.version`). That schema
lives in the installer modules, not in this library.
