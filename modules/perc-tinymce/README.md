# perc-tinymce

This module contains the support for tinymce editor plugin.

## Javadoc status

This module contains **no Java sources** — it is a packaging-only module that bundles the
TinyMCE and CodeMirror WebJARs and produces a resource-only JAR under
`META-INF/resources/sys_resources/tinymce/`. As a result, the Maven Javadoc plugin skips
the `javadoc:javadoc` goal for this module and the module's `JavadocSrcWarnings` count is
**structurally zero** by design.

* Configuration files (json files : default, customer overridden etc.) for configuring the tinymce editor.
* Javascript files to initialize and default editor functionality.
* Javascript files for customising the functionality.
* Plugins and supporting resource files (js, css etc.) for the tinymce editor.

## Building

### Linux/macOS

```bash
./mvnw clean install
```

### Windows

⚠️ **Important:** See [WINDOWS-BUILD-GUIDE.md](../../WINDOWS-BUILD-GUIDE.md) for Windows-specific setup, especially the critical "Enable Long Path Support" section.

```cmd
mvnw.cmd clean install
```

## How It Works

This module uses the `frontend-maven-plugin` to:

1. **Automatically download and install Node.js v22** to `%USERPROFILE%\.m2\frontend`
2. **Run `npm install`** to install esbuild and dependencies
3. **Minify TinyMCE plugins** using esbuild during the `prepare-package` phase

The built resources are packaged into `META-INF/resources/sys_resources/tinymce/` in the JAR.

### Node.js Version

* **Version:** v22.22.0 (latest stable)
* **Install Location:** `%USERPROFILE%\.m2\frontend` (Windows) or `~/.m2\frontend` (Linux/macOS)
* **Note:** The plugin downloads and caches Node.js, so `mvn clean` will NOT trigger a re-download

## Troubleshooting

### Windows: `npm install` Fails

If you see `[ERROR] Failed to execute goal com.github.eirslett:frontend-maven-plugin:1.15.1:npm (npm-install)`:

1. **Enable long path support** (critical for Windows):
   * See [WINDOWS-BUILD-GUIDE.md](../../WINDOWS-BUILD-GUIDE.md#enable-long-paths)
2. **Clear npm cache:**

```cmd
npm cache clean --force
mvnw.cmd clean install
```

3. **Get more details** with verbose logging:

```cmd
mvnw.cmd clean install -X > build-debug.log 2>&1
```

### Node.js Download Fails

If Node.js fails to download:

```cmd
REM Clear the cached Node.js
rmdir /s %USERPROFILE%\.m2\frontend

REM Retry the build
mvnw.cmd clean install
```

## Building Without Node.js Download

If you have npm already installed and want to use it:

Set the npm executable path in the pom.xml `<npmInheritsProxyConfigFromMaven>false</npmInheritsProxyConfigFromMaven>` configuration, or install Node.js globally and the plugin will detect it.
