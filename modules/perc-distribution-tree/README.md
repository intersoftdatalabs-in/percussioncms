# perc-distribution-tree

This Module contains all the Percussion CMS Install, Upgrade related classes, configurations and scripts.
It uses ANT for Install and Upgrades.

## Building

```bash
mvn clean install
```

## Developer Guide

### Build Process Overview

The build process follows these phases:

1. **generate-resources**: Unpacks dependencies (perc-jetty, perc-rxapps, perc-packages, etc.) to `target/` directories
2. **process-resources**: Runs Ant script (`installDistributionFiles.xml`) to copy files to `target/classes/distribution/`
3. **process-classes**: Runs Jetty setup via exec-maven-plugin to configure Jetty modules
4. **package**: Assembles the final installer JAR

### Important Build Configurations

#### Maven Execution IDs

- **NEVER** use `default-cli` as an execution ID in this module's pom.xml
- `default-cli` is a special Maven ID that only runs when explicitly invoked from command line, not during normal build lifecycle
- Use descriptive IDs like `setup-distribution-files` instead

#### Ant Task Execution

The Ant task in `maven-antrun-plugin` must run in the `process-resources` phase to ensure:

- Dependencies are unpacked first (from `generate-resources` phase)
- Files are copied to assembly directory before Jetty setup runs (in `process-classes` phase)

### File Permissions

When adding or modifying shell scripts in the installer:

#### Ant Installer (install.xml)

- **Always use `perm="ugo+x"`** for shell scripts, NOT `perm="o+x"`
- `ugo+x` = user, group, other execute permissions (correct: `-rwxrwxr-x`)
- `o+x` = only "other" execute permissions (incorrect: `-rw-rw-r-x`)

Example:

```xml
<chmod perm="ugo+x" failifexecutionfails="false" failonerror="false" 
       includes="**/*.sh" dir="${install.dir}"/>
```

#### File Locations

- Main installer: `src/main/resources/distribution/rxconfig/Installer/install.xml`
- DTS installer: `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/rxconfig/Installer/installDts.xml`

### Jetty Distribution

The Jetty distribution is managed through:

- Source: `perc-jetty` module (builds a ZIP distribution)
- Unpacked to: `${jetty-directory}/perc-jetty-${project.version}/`
- Copied to: `${assembly-directory}/jetty/` by Ant script
- Structure: `upstream/`, `base/`, `defaults/` directories

#### Adding Jetty Modules

1. Add `.mod` file to `system/Tools/jetty/defaults/modules/`
2. Update module dependencies in `perc.mod` if needed
3. Ensure module is listed in Jetty base `start.ini` or added via `--add-to-start`

### Testing the Installer

Build and run locally:

```bash
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-amazon-corretto
export PATH=$JAVA_HOME/bin:$PATH
mvn clean install -DskipTests=true
java -jar ./modules/perc-distribution-tree/target/perc-distribution-tree.jar /path/to/install/dir
```

Verify:

- Shell scripts have correct execute permissions (`ls -la /path/to/install/dir/jetty/*.sh`)
- Jetty starts successfully
- Log files are created in expected locations

### Common Issues

#### "Unknown module='perc'" Error

- Caused by missing or incorrect Jetty module files
- Check that `perc.mod` exists in `${assembly-directory}/jetty/defaults/modules/`
- Verify Ant task ran successfully (check build logs for "Setting up distribution files" messages)

#### Files Not Executable After Install

- Check `chmod` commands in `install.xml` use `perm="ugo+x"`
- Verify Ant task runs on Linux/Unix systems (check OS detection logic)

#### Ant Task Not Running

- Verify execution ID is NOT `default-cli`
- Check phase is set to `process-resources`
- Ensure task is inside `<executions>` block

