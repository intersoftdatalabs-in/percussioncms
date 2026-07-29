# `percussioncms.config` — Curator Handbook

This file is the **curator-facing** companion to `SKILL.md`. End users
read `SKILL.md`; contributors read this file. Anything that helps future
maintainers keep the catalog honest lives here.

## Who maintains this

The Percussion CMS team, branch owner `development`. The skill ships in
the `ai-shared-release` module so it is bundled with the product JAR
(via `modules/ai-shared-release/src/main/assembly/assembly.xml`).

## Source-of-truth policy

Every catalog row must cite **either**:

1. The installer template that ships the file (e.g.
   `system/installResources/installServer.xml`, line 589 for
   `rxconfig/DeliveryServer/delivery-servers.xml`); **or**
2. The Java loader / installer class that parses it (e.g.
   `com.percussion.cx.PSPropertiesLoader`,
   `com.percussion.ant.install.PSConfigureDatasource`,
   `com.percussion.ant.install.PSUpdateDTSConfiguration`).

If neither exists, the row is **not** added. The handover doc for v1
explicitly says: "Do not invent row entries." Grep the cited class
when in doubt.

## Adding a row

1. Identify the installer template path **or** the Java loader FQN.
2. Confirm the file ships in the on-disk install tree (run the
   installer or grep the bundled JAR).
3. Pick the correct subsystem table (3.1 – 3.8 for CMS, 4.1 – 4.3 for
   DTS).
4. Fill the columns: **File (relative)**, **Purpose**, **Subsystem**,
   **Hot?**, **Restart?**, **Overridable via**, **Source-of-truth**.
5. If the file supports an override hook, name the loader class in the
   "Overridable via" column (e.g. `-Dperc.ds.*`).
6. Update the `version:` field in the frontmatter.
7. Add a `CHANGELOG.md` entry.

## Removing a row

Remove a row only when the underlying file/template is deleted in
`development`. Never silently edit a row out of the catalog — the
catalog is supposed to mirror the installed reality.

## Versioning

- One entry per release in `CHANGELOG.md`.
- Bump `version:` in the frontmatter to match the release tag.
- The catalog is regenerated (curated) for each release; do not
  generate the catalog at build time.

## Coexistence with cosign signing

Catalog files are AI resources like every other skill. They are signed
by the existing pipeline:

```bash
python3 modules/ai-shared-develop/scripts/sign-ai-resources.py
```

The script globs `modules/ai-shared-release/src/main/resources/skills/...`
and emits `<name>.sha256` + `<name>.sigstore.json` sidecars. Commit
those sidecars alongside the markdown files.

## Build & distribute

The assembly descriptor in
`modules/ai-shared-release/src/main/assembly/assembly.xml` already
globs `**/*` under the resources directory, so the new skill is
picked up automatically. No `pom.xml` change is required.

Verify the catalog ships by running the per-module build:

```bash
cd modules/ai-shared-release
../mvnw clean install
```

After a successful build, the catalog should be present at:

```
modules/ai-shared-release/target/distribution/ai/skills/percussioncms.config/SKILL.md
```

Record the `BUILD SUCCESS` line in the PR body as evidence (per root
`AGENTS.md` → *Pre-PR Maven verification — HARD GATE*).

## Schema reference

The catalog uses a fixed schema:

| Column | Allowed values |
| --- | --- |
| File (relative) | Path relative to `${PERCUSSION_HOME}` or `${DTS_HOME}`; portable joins only. |
| Purpose | One sentence. |
| Subsystem | One of: Server, Core, Repository, Security, Workflow, Mail, Logging, Scheduler, Jetty, Jetty JDBC, Messaging, Search, Content Editor, I18n, Search, Proxy, Content Connector, Package Installer, UI, Audit, Installer, ObjectStore, Runtime, Catalog, Catalog. |
| Hot? | `Yes`, `No`, `Partial`. |
| Restart? | `Yes`, `No`, `n/a`, `Rebuild required`. |
| Overridable via | JVM `-D` flag, `<file>.local` sidecar, JNDI, or `n/a`. |
| Source-of-truth | Fully-qualified Java class or installer template path. |

## Link policy

- Internal code references: `path/to/file.ext:line` (the catalog
  itself uses `path:line` where possible).
- Internal docs: relative paths only.
- External docs: only the official help site
  (<https://percussioncmshelp.intsf.com>) and the GitHub repo
  (<https://github.com/intersoftdatalabs-in/percussioncms>).

## Out of scope (deferred)

- Build-time catalog generator (hand-curated for v1).
- Operational scripts (descriptive only for v1).
- Designer-only / not-customer-editable trees beyond a one-line
  "do not edit" row each.
- Per-service Helm / Kubernetes overlays.
- Translations of the catalog itself (English-only v1).
