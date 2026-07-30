# Changelog — `percussioncms.config`

All notable changes to this catalog are recorded here. The catalog is
version-pinned to the product release; bump the `version:` field in
`SKILL.md` frontmatter on each release.

## 8.2.0 — 2026-07-26

- Initial catalog. CMS coverage:
  - `rxconfig/` (Server, Workflow, Security, LDAP, Proxy, I18n,
    ContentConnector, Packages, DeliveryServer, Installer, Logs).
  - `Deployment/Server/conf/` (Tomcat / Jetty-bridge) and
    `conf/perc/perc-*.properties`.
  - `jetty/base/etc/` (datasource, SSL, MQ, JAAS, artemis) and
    `start.d/*.ini`.
  - `var/config/` and `var/config/generated/` (installer-only).
  - `ObjectStore/IdTypes/` (server-managed; do not edit).
  - `Rhythmyx/sys_resources/webapps/...` (designer-only; do not edit).
  - `system/installResources/` and `system/config/` (build-time).
- DTS coverage:
  - `Deployment/Server/conf/` and `conf/perc/perc-*.properties`.
  - Per-service `webapps/<svc>/WEB-INF/perc-*.properties` for
    `forms`, `polls`, `comments`, `membership`, `metadata`, `feeds`,
    `secure-membership`.
  - Per-service log4j2 files under `conf/perc/`.
- Added the resolution & override order section (JVM `-D` →
  `<file>.local` → shipped default).
- Added the hot-edit / restart policy canonical table.
- Added security & secrets section (installer-generated passwords,
  keystore, encryption, audit log).
- Added cross-platform path reminders linked to root `AGENTS.md`.
- Help site & version pin section linked to
  <https://percussioncmshelp.intsf.com>.
- Created `README.md` curator handbook.

