# Third-party license inventory (build-generated)

## What is authoritative

The **versioned** third-party dependency / license inventory is **not** hand-edited.

It is produced at build time by
[`org.codehaus.mojo:license-maven-plugin`](https://www.mojohaus.org/license-maven-plugin/)
(`aggregate-add-third-party`) configured on the **reactor root** `pom.xml`
(issue [#1689](https://github.com/intersoftdatalabs-in/percussioncms/issues/1689)):

|           Artifact            |                                              Location                                               |
|-------------------------------|-----------------------------------------------------------------------------------------------------|
| Generated inventory           | `${repo-root}/target/generated-sources/license/THIRD-PARTY.txt`                                     |
| Shipped with installer        | `THIRD-PARTY.txt` at the root of the `perc-distribution-tree` assembly (copied from the path above) |
| Product notice (stable prose) | root `NOTICE.txt` — product copyright + pointer only                                                |
| Startup / About blurb         | `system` resource key `thirdPartyCopyright` — same pointer, **no version pins**                     |

Do **not** reintroduce hand-curated component lists or dependency version pins into
`NOTICE.txt` or `thirdPartyCopyright`. That was the drift failure mode this automation
replaces.

## Manual generation

From the repository root (JDK 21 + Maven wrapper). **Do not pass `-N` / `--non-recursive`**
— that only loads the empty root POM and produces an empty inventory:

```bash
# Loads the full reactor, then runs the aggregate goal on the root only
./mvnw license:aggregate-add-third-party
# Windows:
mvnw.cmd license:aggregate-add-third-party
```

A full reactor build also runs the goal during the root `generate-resources` phase
(before child modules package).

## `THIRD-PARTY.properties` (this directory)

This file is **not** the inventory. It is an optional **missing-license map** used when
an upstream POM does not declare a license. Entries look like:

```properties
groupId--artifactId--version=Some License Name
```

Only add rows for dependencies the plugin reports as missing a license. Never use this
file to re-list the full dependency set by hand.
