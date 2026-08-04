# perc-rxapps

This module assembles the Rhythmyx (Rx) application distribution that the CMS installer ships and
that a running Rhythmyx server imports into its ObjectStore on first start. It contains no Java
sources of its own (only `pom.xml` and one Maven assembly descriptor); the module's job is to
gather a large tree of pre-built Rhythmyx application XML, XSL, DTD, template, and content files
from across the build tree and package them into the `RxApp` + `RxFastForward` tar.gz / zip
artifacts.

## What this module produces

The output is a single distribution directory, `target/distribution/`, that the
`maven-assembly-plugin` then packages as two archive formats:

|     Archive      |                                                                                                                 Contents                                                                                                                 |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `RxApp/`         | The bundled **Rhythmyx core applications** — `ObjectStore/` (XML dataset definitions), `sysAppSupport/` (`sys_*` XSL / DTD / ApplicationFiles for the system apps), and `rxAppSupport/` (`rx_*` XSL / DTD for the legacy Rhythmyx apps). |
| `RxFastForward/` | The bundled **FastForward sample sites** — Core site, Managed Navigation site, Site Folder Publishing site, Default Template sample, plus a Sample Content dataset and the run-time `lib/` jars.                                         |

Both archives are produced from the same `target/distribution/` tree; the `maven-assembly-plugin`
descriptor (`src/main/assembly/perc-assembly.xml`) excludes Maven / OS detritus (`.DS_Store`,
`Thumbs.db`, `*.iml`, `*.bak`, `*.orig`, `*.versionsBackup`, etc.) so the installer is not forced
to ship editor and SCM files.

## How the module builds

The module wires two plugins:

|         Plugin          |        Phase        |                                                                                                                                                                                                                                                                                        Role                                                                                                                                                                                                                                                                                        |
|-------------------------|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `maven-antrun-plugin`   | `process-resources` | Runs `system/rxAppsCopy.xml` with `APPS_DIR=target/distribution/RxApp` and `FF_DIST_APPS_DIR=target/distribution/RxFastForward`. The Ant script is the same one the legacy `system` build used; it copies the per-app `ApplicationFiles/` (XSL, DTD, editors, assemblers) from `system/cms/content/applications`, `system/workflow/applications`, `system/agenthandler/applications`, `system/Designer/applications`, and `system/applications`, runs the `buildResources/fixApplicationAcls.xsl` transform over each `ObjectStore/*.xml`, and applies the FastForward ACL fixups. |
| `maven-assembly-plugin` | `package`           | Reads `src/main/assembly/perc-assembly.xml` and produces `target/perc-rxapps-<version>.{tar.gz,zip}` from the `target/distribution/` tree produced by the Antrun execution.                                                                                                                                                                                                                                                                                                                                                                                                        |

## Javadoc status

This module is intentionally Java-source-free (it is `packaging=jar` because Maven's
`maven-assembly-plugin` requires a jar packaging, but the module produces no Java classes of its
own). The `maven-javadoc-plugin` therefore reports **0 source warnings and 0 plugin warnings**
during `mvn clean install` — the `attach-javadocs` execution prints
`No Javadoc in project. Archive not created.` and exits cleanly. No code changes were required to
satisfy the zero-warnings acceptance criterion.

The `[WARNING] JAR will be empty - no content was marked for inclusion!` notice from
`maven-jar-plugin` is unrelated to this module's documentation; it is the expected behavior of
`default-jar` on a Java-source-free module whose only deliverables are the assembly
tar.gz / zip files.

## Building

```
mvn clean install
```

The artifacts produced by this module:

- `target/perc-rxapps-<version>.jar` — the empty Java jar (see note above).
- `target/perc-rxapps-<version>.tar.gz` — the `RxApp/` + `RxFastForward/` distribution.
- `target/perc-rxapps-<version>.zip` — the same distribution in zip form.

The tar.gz / zip archives are then consumed by the `perc-distribution-tree` installer module, which
lifts them into the final installer payload that the Ant installer drops into a running CMS's
`rxapp/` import directory.

## See also

- `system/rxAppsCopy.xml` — the Ant script that this module's `maven-antrun-plugin` invocation
  drives to populate `target/distribution/`. All per-application pattern sets, ACL fixup XSLs, and
  FastForward copy logic live there.
- `modules/perc-distribution-tree` — the installer module that consumes this module's tar.gz /
  zip output and stages it for the Ant installer.
- `system/cms/content/applications`, `system/workflow/applications`,
  `system/agenthandler/applications`, `system/Designer/applications`, `system/applications` — the
  source trees that `rxAppsCopy.xml` walks to populate `target/distribution/RxApp/`.
- `system/FastForward/Core`, `system/FastForward/ManagedNav`,
  `system/FastForward/SiteFolderPublishing`, `system/FastForward/DefaultTemplate`,
  `system/FastForward/SampleContent` — the source trees for `target/distribution/RxFastForward/`.

