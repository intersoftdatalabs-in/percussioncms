# deployer

The **deployer** module is the heart of Percussion's packaging and deployment
infrastructure. It provides the code used by both the CMS server and the
packager tools to create, read and apply package archives, maintain dependency
maps, and manage the life‑cycle of deployed objects.

Key responsibilities include:

* Managing the `PSPackageConfiguration` and `PSDependencyMap` used during
  import/export operations.
* Implementing `PSDependencyHandler` classes for every deployable object type
  (templates, content editors, workflows, etc.).
* Providing services such as `PSArchiveHandler`, `PSDependencyManager`,
  `PSPackageLockManager`, and related utilities used by the deployer CLI and
  installer.
* Supporting configuration parsing (rx config) and conversion utilities used
  by packaging scripts.

The module is **not** a standalone application; it is packaged as a library and
included in the CMS server or the `perc-deployer` commandline tool.

## Building

Build from the workspace root or from the `deployer/` directory using the
wrapper script to ensure JDK‑21 is used:

```bash
# compile & run unit tests only
./mvnw -pl deployer test -DskipITs

# compile & package the module
./mvnw clean install -pl deployer -DskipITs
```

A full top‑level `./mvnw clean install` will build all modules including
`deployer`.

### Running

No executable artifact is produced.  To exercise functionality you can invoke
individual `main` classes via Maven or run the packaged library inside the
full CMS server.  Example:

```bash
cd deployer
./mvnw exec:java \
    -Dexec.mainClass=com.percussion.deployer.Packager \
    -Dexec.args="-pack -src <src> -out <archive>.zip"
```

Adjust the class and arguments for other utilities as needed.

## Developer Notes

During the recent migration to JUnit 5 the test suite was expanded, hardened,
and in some cases temporarily disabled.  Attention areas:

* **Spring‑dependent beans** (`PSIdNameServiceTest`, `PSPkgInfoServiceTest`,
  `PSIdNameHelperTest`) are currently marked `@Disabled`; they require a
  lightweight application context or should be reworked with mocks.
* **File‑based configuration** (`PSDependencyManagerTest`,
  `PSPackageConfigurationTest`) now copy `sys_PackageConfiguration.xml` into
  both the classpath and the repository root `config/Deployer` directory.  The
  former ensures the tests run on any platform, the latter satisfies relative
  path lookups.  These tests still assume a large configuration; consider
  simplifying them or injecting a small stub.
* **Resource‑heavy tests** (`PSPackageLockManagerTest` etc.) continue to be
  disabled because they expect pre‑existing package archives under
  `Packages/Percussion`.  Either add minimal ZIPs or refactor to generate
  content on the fly.
* **General cleanup**: many tests still contain raw types, deprecated APIs or
  Windows‑style paths; feel free to modernize them as you work on related
  functionality.  Search for `@Disabled` and `// TODO` comments to find
  outstanding issues.

Re‑run the full deployer test suite regularly with:

```bash
./mvnw -pl deployer test -DskipITs
```

This will catch regressions early.  When re‑enabling disabled tests, make sure
any required config files are checked into `deployer/src/test/resources` so
platform case‑sensitivity no longer causes failures.

Happy hacking!

