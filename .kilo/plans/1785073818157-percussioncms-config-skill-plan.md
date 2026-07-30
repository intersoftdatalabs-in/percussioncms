---
name: plan-percussioncms-config-skill
version: 1.0
date: 2026-07-26
status: ready-for-implementation
---

# Plan: `percussioncms.config` Skill (ai-shared-release)

## Goal

Add a new hand-curated, reference-style skill named **`percussioncms.config`** to the
released AI skills bundle so end-user/admin AI agents (Kilo, Claude, etc.) can answer
"where is `<x>` configured in Percussion CMS / DTS?" accurately, without grepping the
codebase. It also signals safe edit boundaries (hot vs. restart vs. untouched).

## Scope (confirmed)

- **Products covered**: Rhythmyx CMS server **and** DTS microservices.
- **Shape**: Reference catalog (descriptive). No runtime scripts in v1.
- **Sources of truth**: curated, version-pinned to the current released tree.
  No build-time generation.
- **Distribution**: ships with the product via the existing
  `ai-shared-release` JAR (see `modules/ai-shared-release/src/main/assembly/assembly.xml`).

## Placement

```
modules/ai-shared-release/src/main/resources/skills/percussioncms.config/
├── SKILL.md            # skill body + catalog tables
├── README.md           # curator-facing notes (generation rules, schema, link policy)
├── CHANGELOG.md        # version-pinned history (one entry per release)
└── files/              # optional: excerpts of config file headers / DTDs
    ├── cms-server.md
    ├── dts-delivery.md
    └── webapps.md
```

Verified conventions from existing skills (`modules/ai-shared-release/src/main/resources/skills/cosign-validate/`,
`.../skills/cosign-sign/`, `.../skills/cosign-invalidate/`):

- `SKILL.md` carries YAML frontmatter with `name`, `description`, optional
  `version` (cosign skills add it; some others omit).
- Distribution happens via the existing assembly; no `pom.xml` edits needed
  unless we want a checksum sidecar (the release module already signs AI resources).

## SKILL.md frontmatter (target)

```yaml
---
name: percussioncms.config
description: >
  Reference catalog of every known Percussion CMS and DTS configuration file:
  default install-tree path, purpose, subsystem owner, whether it can be edited
  live (hot) or requires a restart, related system properties / Java -D flags,
  and notes. Use when an admin or AI agent needs to locate, read, or safely
  modify a Percussion configuration value without grepping the codebase.
version: 8.2.0
---
```

The description must be trigger-rich enough to load naturally; mirror the tone
of `kilo-config`'s description and the `percussioncms-dev` skill's
`description` (developer / install / query triggers). Add the natural-language
phrases admins/agents use: "where do I configure…", "which file controls…",
"is this hot-editable…", "what restart does X need…".

## SKILL.md body — required sections

1. **Purpose & when to load** — short paragraph, load-trigger list.
2. **Install-tree anchors (portable)** — show the universal anchors and how they
   resolve per platform. Example:

   ```text
   ${PERCUSSION_HOME}       → CMS install root  (default: /opt/Percussion or C:\Percussion)
   ${PERCUSSION_HOME}/rxconfig
   ${PERCUSSION_HOME}/Deployment/Server/conf        (Tomcat side, also used by Jetty bridge)
   ${PERCUSSION_HOME}/Deployment/Server/rxconfig    (legacy)
   ${PERCUSSION_HOME}/ObjectStore                   (server-managed; do not hand-edit)
   ${PERCUSSION_HOME}/var/config                    (installer-generated)
   ${PERCUSSION_HOME}/jetty/base/etc                (Jetty datasource props)
   ${DTS_HOME}/Delivery/Server/conf                 → catalogued separately (DTS section)
   ```

   - **All paths must use `${...}` placeholders + portable relative joins**, per
     root `AGENTS.md` → *Cross-Platform File I/O & Paths*. No hardcoded `/` or
     `\` separators in examples.
3. **CMS Catalog** — table per install subtree:

   | File (relative to anchor) | Purpose | Subsystem | Hot-editable? | Restart required? | Overridable via system property | Notes |

   Subtrees to cover at minimum:

   - `rxconfig/Server.xml`, `rxconfig/perc-*.xml` (workflow, security, search).
   - `Deployment/Server/conf/perc/perc-catalog-services.properties`,
     `perc-catalina.properties`, `perc-email.properties`,
     `perc-secured-sections.properties`, `perc-encryption.properties`,
     `perc-system.properties`.
   - `Deployment/Server/conf/server.xml` (Jetty/Tomcat bridge), `web.xml`.
   - `jetty/base/etc/perc-ds.properties`, `perc-ds-derby.properties`,
     `perc-ds-mysql.properties`, `perc-ds-mssql.properties`,
     `start.ini`, `start.d/*.ini`.
   - `var/config/generated/*` (passwords, SSL); mark as **installer-only**, do
     not hand-edit.
   - `ObjectStore/.../*.xml` — **mark explicitly "do not edit; managed by CMS"**
     and direct agents to the REST API / Designer instead.
   - Designer / webapp config under `Rhythmyx/sys_resources/webapps/*/web.xml`
     and `WEB-INF/perc-*.xml` — flag as **not customer-editable; copy into
     override layer**.
   - `system/config/*.xml` and `*.properties` — install/build-time only,
     surfaced via `install.xml`; explicitly out of scope for live edits.

   For every row, cite the **source-of-truth** Java constants or installer
   template (e.g. `com.percussion.cx.PSPropertiesLoader`,
   `com.percussion.utils.container.PSContainerUtils`) so curators can refresh
   the row when upstream changes.

4. **DTS Catalog** — same table shape, anchored at `${DTS_HOME}`. Cover:

   - `Delivery/Server/conf/catalina.properties`, `server.xml`,
     `web.xml`, `tomcat-users.xml`.
   - `webapps/<svc>/WEB-INF/perc-context.properties`,
     `perc-security.properties`, `perc-datasources.properties`,
     `perc-form-processor.properties`, `perc-polls-services.properties`,
     `perc-comments-services.properties`, `perc-membership-services.properties`,
     `perc-metadata-services.properties`, `perc-encryption.properties`,
     `perc-datatype-mappings.properties`, `perc-email.properties`.
   - Mark microservice-specific files with `service: <name>` (forms / polls /
     comments / metadata / membership / feeds / secure-membership).
5. **Resolution & override order** — fixed precedence table:
   1. JVM `-D` system property (`-Drhythmyx.<key>=…`, `-Dperc.<svc>.<key>=…`).
   2. `<file>.local` or `<file>.properties` override on the classpath /
      filesystem if supported by the loader.
   3. The shipped default file under `rxconfig/`, `Delivery/Server/conf/perc/`,
      or `WEB-INF/`.

   Note that the loader class is config-specific; link the relevant Java class
   per row when the override hook is non-obvious.

6. **Hot-edit / restart policy** — short canonical table:

   |          Change kind          |      Restart needed      |                     Notes                     |
   |-------------------------------|--------------------------|-----------------------------------------------|
   | Email / SMTP                  | No for most loaders      | Some require `perc-system.properties` reload. |
   | Datasource (JDBC URL / creds) | Yes (CMS + Jetty/Tomcat) | Re-encrypt secret with install tool.          |
   | Workflow / security XML       | Yes                      | `rxconfig` loaders cache at boot.             |
   | Hibernate / search index      | Yes                      | …                                             |

   This is the section AI agents actually quote to admins.

7. **Security & secrets** — callouts:

   - Never commit plain-text passwords. Use `var/config/generated/passwords`.
   - Log redaction toggles live in `perc-*.properties` and `rxlogger.properties`.
   - TLS / keystore locations: point at `keystore.*` and `perc-encryption.properties`.
8. **Cross-platform path reminders** — link to root `AGENTS.md` → *Cross-Platform
   File I/O & Paths* and repeat the portable-join rule. This must be present
   because the skill ships into customer installs.
9. **Help site & version pins** — at the bottom, link to
   <https://percussioncmshelp.intsf.com> and the matching installer /
   system-admin guides; declare the exact `version:` the catalog was
   generated against (e.g. `8.2.0`).
10. **How to extend** — point curators at `README.md` and the source-of-truth
    Java constants.

## `README.md` — curator handbook (target outline)

- **Who maintains this**: Percussion CMS team, branch owner `development`.
- **Source-of-truth policy**:
  - Every catalog row must cite either (a) the installer template that ships
    the file (e.g. `system/installResources/installServer.xml`,
    `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/assembly/...`),
    or (b) the Java loader class that parses it (e.g.
    `com.percussion.cx.PSPropertiesLoader`,
    `com.percussion.utils.container.jetty.ds.PercDataSourceFactory`).
- **Adding a row**: schema above; required columns.
- **Removing a row**: only when the underlying file/template is deleted in
  `development`; do not silently edit.
- **Versioning**: bump `version:` in frontmatter per release; one entry per
  release in `CHANGELOG.md`.
- **Coexistence with cosign signing**: catalog files are read-only AI
  resources; rely on existing `SkillVerificationService` and the
  `modules/ai-shared-develop/scripts/sign-ai-resources.py` pipeline — no new
  signing code.

## `CHANGELOG.md` — seed entry

```markdown
## 8.2.0 — 2026-07-26
- Initial catalog: CMS `rxconfig/`, `Deployment/Server/conf/`, `ObjectStore/`,
  `var/config/`, `jetty/base/etc/`, plus DTS `Delivery/Server/conf/` and
  per-service `WEB-INF/perc-*.properties`.
- Resolution-order and hot-edit/restart policy sections added.
- Cross-platform path guidance linked to root AGENTS.md.
```

## Coexistence / wiring notes

- **No `pom.xml` change required** — the assembly descriptor already picks up
  `skills/**`. Verified by reading
  `modules/ai-shared-release/src/main/assembly/assembly.xml`.
- **Signing pipeline**: cosign-signed resources use `<name>.sha256` and
  `<name>.sigstore.json` sidecars. `sign-ai-resources.py` already globs the
  skills directory, so new skill files are auto-signed. No script edits
  required.
- **Distribution verification**: a single `mvn -pl modules/ai-shared-release
  clean install` should place the skill under
  `target/distribution/ai/skills/percussioncms.config/SKILL.md`.
  Add this command to the PR description as evidence (matches the
  cross-platform guidance for this module).

## Risks & mitigations

|                                Risk                                |                                                         Mitigation                                                          |
|--------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| Catalog drifts from installed reality                              | Each row cites a Java class or installer template — curators can grep the repo to verify.                                   |
| Portable path bug sneaks in                                        | Catalog has a hard "no hardcoded `/` or `\\` in file paths" review checklist; Erlang pre-commit review catches regressions. |
| Conflict with existing `kilo-config` description frontmatter style | Mirror the existing `cosign-validate` frontmatter (`name`, `description`, `version`); keep prose terse.                     |
| Help-site links rot                                                | Pin to product version in the frontmatter; CHANGELOG records URL + retrieval date.                                          |
| Bundle size growth                                                 | Catalog is markdown only; <50 KB. Acceptable.                                                                               |

## Validation (executed before merge)

1. **Per-module standalone build** (root `AGENTS.md` *Pre-PR Maven
   verification — HARD GATE*): `cd modules/ai-shared-release && ../mvnw
   clean install`. BUILD SUCCESS required. Record in PR body.
2. **Distribution check**: unpack `target/distribution/ai/` and confirm
   `skills/percussioncms.config/SKILL.md` is present alongside the existing
   cosign skills.
3. **Frontmatter check**: the SKILL.md frontmatter parses; `name` matches the
   directory name (`percussioncms.config`); description includes trigger
   phrases.
4. **Path-portability spot-check**: grep `SKILL.md` for hardcoded separators
   in `Filesystem`-context lines; URL/classpath `/` only is allowed.
5. **Link check**: webfetch each external URL listed once (help site,
   github repo).
6. **Erlang review** (root `AGENTS.md`): required for any change to
   `ai-shared-release` modules. Run `/erlang-review uncommitted` and only
   commit when Gate says `approve`.

## Out of scope (intentionally deferred)

- Build-time catalog generator (per Q3 answer — hand-curated for v1).
- Operational scripts (per Q2 — descriptive only for v1).
- Coverage of designer-only / not-customer-editable trees beyond a
  one-line "do not edit" row each.
- Per-service Helm / Kubernetes overlays (not part of the on-disk install
  tree in this repo).
- Translations of the catalog itself (English-only v1).

## Open questions

None. All design decisions were answered by the user:

1. **Scope** = CMS + DTS.
2. **Persona** = reference catalog.
3. **Source** = hand-curated, version-pinned.

## Ordered implementation tasks (hand-off to implementer)

1. Create the skill directory
   `modules/ai-shared-release/src/main/resources/skills/percussioncms.config/`.
2. Author `SKILL.md` with frontmatter and the 10 sections above, using
   portable paths and citing source-of-truth Java classes / installer
   templates per row.
3. Author `README.md` (curator handbook) and `CHANGELOG.md` (seed v8.2.0
   entry).
4. Self-review: frontmatter parses, links resolve, no hardcoded `/`/`\` in
   filesystem-path examples.
5. Per-module build: `cd modules/ai-shared-release && ../mvnw
   clean install`. Confirm BUILD SUCCESS and that
   `target/distribution/ai/skills/percussioncms.config/SKILL.md` exists.
6. Open PR on `development` with command + BUILD SUCCESS evidence in the
   body. Do not commit/push until Erlang review passes.

