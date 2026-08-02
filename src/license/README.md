# Third-party license inventory (build-generated)

## What is authoritative

The **versioned** third-party dependency / license inventory is **not** hand-edited.

It is produced at build time and written to a **single** merged file:

| Piece | How |
|-------|-----|
| Maven / Java dependencies | [`org.codehaus.mojo:license-maven-plugin`](https://www.mojohaus.org/license-maven-plugin/) `aggregate-add-third-party` → `THIRD-PARTY-MAVEN.txt` |
| npm production dependencies | `scripts/generate-third-party-inventory.py` reads product `package-lock.json` files listed in `npm-package-locks.txt` → `THIRD-PARTY-NPM.txt` |
| **Shipped inventory** | Same script **merges** both halves → **`THIRD-PARTY.txt`** |

Issue [#1689](https://github.com/intersoftdatalabs-in/percussioncms/issues/1689).

| Artifact | Location |
|----------|----------|
| Merged inventory (authoritative) | `${repo-root}/target/generated-sources/license/THIRD-PARTY.txt` |
| Maven intermediate | `…/THIRD-PARTY-MAVEN.txt` |
| npm intermediate | `…/THIRD-PARTY-NPM.txt` |
| Shipped with installer | `THIRD-PARTY.txt` at the root of the `perc-distribution-tree` assembly |
| Product notice (stable prose) | root `NOTICE.txt` — product copyright + pointer only |
| Startup / About blurb | `system` resource key `thirdPartyCopyright` — same pointer, **no version pins** |

Do **not** reintroduce hand-curated component lists or dependency version pins into
`NOTICE.txt` or `thirdPartyCopyright`. That was the drift failure mode this automation
replaces.

## Manual generation

From the repository root (JDK 21 + Maven wrapper + Python 3.9+).

**Do not pass `-N` / `--non-recursive` to `license:aggregate-add-third-party`** — that only
loads the empty root POM and produces an empty Maven inventory.

```bash
# 1) Maven half (full reactor dependency graph)
./mvnw license:aggregate-add-third-party
# 2) npm half + merge into THIRD-PARTY.txt
python3 scripts/generate-third-party-inventory.py --require-maven

# Windows:
mvnw.cmd license:aggregate-add-third-party
scripts\generate-third-party-inventory.bat --require-maven
```

A full reactor build also runs both steps on the root `generate-resources` phase
(license plugin first, then the merge script). CLI
`license:aggregate-add-third-party` alone does **not** run the merge — always follow
with the Python script when generating by hand.

If `python` is not on `PATH`, set `-Dpython.executable=python3` on the Maven command
that runs the merge execution, or invoke `python3` on the script directly.

## npm package locks

Edit `npm-package-locks.txt` to add product-shipped frontend lockfiles. **Do not** list
QA-only trees (`modules/perc-qa-automation`) or pure build tooling.

The WebUI SPA is built from `WebUI/src/main/frontend` — that lockfile is listed once.
Do not also list `WebUI/package-lock.json` (duplicate production set).

Production packages are those in the lockfile `packages` map that are **not** marked
`dev` / `devOptional`. Licenses come from the lockfile `license` field (with optional
`node_modules/.../package.json` fallback when present).

## `THIRD-PARTY.properties` (this directory)

This file is **not** the inventory. It is an optional **missing-license map** for Maven
dependencies whose POM does not declare a license. Entries look like:

```properties
groupId--artifactId--version=License Name
```

Only add rows for dependencies the plugin reports as missing a license. Never use this
file to re-list the full dependency set by hand.
