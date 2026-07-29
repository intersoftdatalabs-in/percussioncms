# Windows Build Guide for Percussion CMS

This guide provides Windows-specific instructions and troubleshooting for building Percussion CMS from source, particularly for modules that use Node.js-based build tools like `perc-tinymce`.

## Prerequisites

- JDK 21 (matching the version specified in the main README)
- Maven 3.9.x
- Windows 10 or Windows 11
- Administrator privileges (for some setup steps)

## Critical: Enable Long Path Support (Windows 10/11)

**This is essential for modules with Node.js builds like `perc-tinymce`.**

Windows has a 260-character path limitation that can cause build failures when npm installs deeply nested node_modules. Node.js projects (especially with v20+) often exceed this limit.

### Enable Long Paths

1. **Open Registry Editor** (run `regedit` as Administrator)
2. **Navigate to:**

   ```
   HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\FileSystem
   ```
3. **Find or create the DWORD:** `LongPathsEnabled`
4. **Set value to:** `1`
5. **Restart your computer**

### Alternative: PowerShell Command (Requires Administrator)

```powershell
New-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" `
  -Name "LongPathsEnabled" -Value 1 -PropertyType DWORD -Force
```

**Verify the setting is applied:**

```powershell
Get-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" `
  -Name "LongPathsEnabled"
```

## Java Home Setup

Set `JAVA_HOME_21` as a **permanent** environment variable:

1. Open **System Properties > Environment Variables**
2. Click **New** (under System variables)
3. **Variable name:** `JAVA_HOME_21`
4. **Variable value:** `C:\Program Files\Java\jdk-21` (adjust path as needed)
   - If you're unsure of your JDK 21 path, run: `where javac` or check your Java installation folder
5. Click **OK** and restart any open terminals

**Verify the setup:**

```cmd
echo %JAVA_HOME_21%
mvnw.cmd -version
```

## Building on Windows

### Standard Build

Use the repo Maven wrapper (`mvnw.cmd`) instead of a random system `mvn`. Point `JAVA_HOME` at JDK 21 first (for example set `JAVA_HOME=%JAVA_HOME_21%` in the shell if you keep both side by side):

```cmd
set JAVA_HOME=%JAVA_HOME_21%
mvnw.cmd clean install
```

### Build Specific Modules

For modules with Node.js dependencies (like `perc-tinymce`):

```cmd
mvnw.cmd clean install -pl modules/perc-tinymce -am
```

Parameters:
- `-pl modules/perc-tinymce` — Build only this module
- `-am` — Auto-include upstream dependencies

## Troubleshooting Windows Builds

### Issue 1: `npm install` Fails with "Process exited with error: 1"

**Symptoms:**

```
[ERROR] Failed to execute goal com.github.eirslett:frontend-maven-plugin:1.15.1:npm (npm-install) on project perc-tinymce
[ERROR] org.apache.commons.exec.ExecuteException: Process exited with an error: 1 (Exit value: 1)
```

**Solutions (in order):**

#### Solution A: Enable Long Path Support (Most Common Fix)

Follow the [Enable Long Paths](#enable-long-paths) section above. This fixes ~90% of Windows npm issues.

#### Solution B: Clear npm Cache

```cmd
npm cache clean --force
```

Then retry the build:

```cmd
mvnw.cmd clean install -pl modules/perc-tinymce -am
```

#### Solution C: Run Build with More Verbose Output

Get detailed error information:

```cmd
mvnw.cmd clean install -pl modules/perc-tinymce -am -X > build-debug.log 2>&1
```

Search the log for the actual npm error near these markers:

```
[INFO] --- frontend-maven-plugin:1.15.1:npm (npm-install)
[ERROR] Failed to execute goal
```

#### Solution D: Disable Antivirus Temporarily (If Other Steps Fail)

Windows Defender or other antivirus software can interfere with npm file operations:

1. Temporarily disable Windows Defender real-time protection
2. Run the build
3. Re-enable Windows Defender

**This should be a last resort.** Long path support (Solution A) is the proper fix.

### Issue 2: "node: command not found" or Node.js Download Fails

**Symptoms:**

```
[ERROR] Could not download Node.js
[ERROR] Node.js is not installed
```

**Solutions:**

#### Check internet connectivity

```cmd
ping www.npm.org
```

#### Clear the frontend plugin cache

```cmd
rmdir /s %USERPROFILE%\.m2\frontend
```

Then retry:

```cmd
mvnw.cmd clean install -pl modules/perc-tinymce -am
```

#### Manual Node.js Installation (Fallback)

If the automated download fails:

1. Download Node.js v22 (or v20 for stability) from https://nodejs.org
2. Install to a standard location like `C:\Program Files\nodejs`
3. Add to PATH if not already done
4. Verify: `node --version` and `npm --version`

### Issue 3: Long Path Error After Long Path Support is Enabled

**Symptoms:**

```
Error: ENAMETOOLONG: name too long
```

**Solution:**

Verify the registry setting was applied and was successful:

1. Restart your computer
2. Open a **new** command prompt (don't reuse an old one)
3. Retry the build:

   ```cmd
   mvnw.cmd clean install -pl modules/perc-tinymce -am
   ```

### Issue 4: Port Already in Use (If Running Tests)

If you see "port 8080 already in use" or similar:

```cmd
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

Then retry the build.

## Performance Tips for Windows

1. **Disable antivirus scanning for Maven directories** (optional but speeds up builds):
   - Windows Defender Settings > Virus & threat protection > Manage settings
   - Add `%USERPROFILE%\.m2` to exclusions
2. **Use parallel builds** to speed up compilation:

   ```cmd
   mvnw.cmd clean install -T 1C
   ```

   (builds with 1 thread per CPU core)

3. **Build only what you need**:

   ```cmd
   mvnw.cmd clean install -pl modules/perc-tinymce -am -DskipTests
   ```

## Windows PowerShell vs Command Prompt

The `mvnw.cmd` script works best with **Command Prompt (cmd.exe)**. If using PowerShell:

```powershell
cmd /c mvnw.cmd clean install
```

Or enter a command prompt session first:

```powershell
cmd
mvnw.cmd clean install
exit
```

## Getting Help

If you encounter issues not covered here:

1. **Check the build log** in verbose mode (see [Solution C](#solution-c-run-build-with-more-verbose-output))
2. **Search existing GitHub issues** with your error message
3. **Post on the community forum**: https://percussioncmshelp.intsof.com
4. **Create a new GitHub issue** with:
   - Windows version (`winver`)
   - Java version (`java -version`)
   - Maven version (`mvn -version`)
   - Full build output (use verbose flag: `-X`)

## Additional Resources

- [Percussion CMS README.md](README.md) — General build setup
- [perc-tinymce README.md](modules/perc-tinymce/README.md) — Node.js-specific build info
- [Microsoft Registry Documentation](https://learn.microsoft.com/en-us/windows/win32/fileio/naming-a-file)
- [Node.js Windows Issues](https://github.com/nodejs/node/issues)
- [npm Windows Troubleshooting](https://docs.npmjs.com/cli/v10/configuring-npm/folders)

