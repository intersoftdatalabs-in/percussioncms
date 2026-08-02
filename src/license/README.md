# Third-party license inventory (build-generated)

## What is authoritative

The **versioned** third-party dependency / license inventory is **not** hand-edited.

It is produced at build time and written to a **single** merged file:

|                  Piece                  |                                                                                                                                 How                                                                                                                                  |
|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Maven / Java dependencies               | [`org.codehaus.mojo:license-maven-plugin`](https://www.mojohaus.org/license-maven-plugin/) `aggregate-add-third-party` on the reactor root → `THIRD-PARTY-MAVEN.txt`                                                                                                 |
| npm production dependencies + **merge** | [`ThirdPartyLicenseInventory`](../../modules/intsof-common-utilities/src/main/java/com/intsof/common/utilities/license/ThirdPartyLicenseInventory.java) in `com.intsof.common:utilities` (generic, product-agnostic) → `THIRD-PARTY-NPM.txt` + **`THIRD-PARTY.txt`** |
| Ship                                    | `perc-distribution-tree` copies merged `THIRD-PARTY.txt` into the installer assembly root                                                                                                                                                                            |

Issue [#1689](https://github.com/intersoftdatalabs-in/percussioncms/issues/1689).

|             Artifact             |                                    Location                                     |
|----------------------------------|---------------------------------------------------------------------------------|
| Merged inventory (authoritative) | `${repo-root}/target/generated-sources/license/THIRD-PARTY.txt`                 |
| Maven intermediate               | `…/THIRD-PARTY-MAVEN.txt`                                                       |
| npm intermediate                 | `…/THIRD-PARTY-NPM.txt`                                                         |
| Shipped with installer           | `THIRD-PARTY.txt` at the root of the `perc-distribution-tree` assembly          |
| Product notice (stable prose)    | root `NOTICE.txt` — product copyright + pointer only                            |
| Startup / About blurb            | `system` resource key `thirdPartyCopyright` — same pointer, **no version pins** |

Do **not** reintroduce hand-curated component lists or dependency version pins into
`NOTICE.txt` or `thirdPartyCopyright`.

## Why Java (not a Python merge)

The merge lives in **`com.intsof.common:utilities`** so the monorepo build stays on **JDK + Maven** only. The API is product-agnostic (paths and titles are caller-supplied) and covered by JUnit in that module.

## Manual generation

From the repository root (JDK 21 + Maven wrapper).

**Do not pass `-N` to `license:aggregate-add-third-party`** — that only loads the empty root POM.

```bash
# 1) Maven half (full reactor dependency graph)
./mvnw license:aggregate-add-third-party

# 2) npm half + merge (uses the installed utilities jar)
./mvnw -pl modules/intsof-common-utilities install -DskipTests
./mvnw -pl modules/perc-distribution-tree process-resources \
  -Dmaven.antrun.skip=true -Dexec.skip=false

# Or invoke the main class directly after utilities is installed:
java -cp modules/intsof-common-utilities/target/utilities-0.0.1.jar \
  com.intsof.common.utilities.license.ThirdPartyLicenseInventory \
  --root . --require-maven \
  --title "Percussion CMS third-party dependency license inventory"
```

Windows: use `mvnw.cmd` and `;` / path separators as appropriate.

A full reactor build runs the Maven aggregate on the root `generate-resources` phase and
the Java merge on `perc-distribution-tree` `generate-resources` (after `utilities` is built).

## npm package locks

Edit `npm-package-locks.txt` to add product-shipped frontend lockfiles. **Do not** list
QA-only trees (`modules/perc-qa-automation`) or pure build tooling.

The WebUI SPA is built from `WebUI/src/main/frontend` — that lockfile is listed once.

Production packages are those in the lockfile `packages` map that are **not** marked
`dev` / `devOptional`.

## `THIRD-PARTY.properties` (this directory)

Optional **missing-license map** for Maven dependencies whose POM does not declare a
license (format: `groupId--artifactId--version=License Name`). Not a hand-maintained
full inventory.
