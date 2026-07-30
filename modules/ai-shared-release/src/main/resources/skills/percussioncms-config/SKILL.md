---
name: percussioncms.config
description: >
  Reference catalog of every known Percussion CMS and DTS configuration file:
  default install-tree path, purpose, subsystem owner, whether it can be edited
  live (hot) or requires a restart, related system properties / Java -D flags,
  and source-of-truth citations. Use when an admin or AI agent needs to locate,
  read, or safely modify a Percussion configuration value without grepping the
  codebase. Triggers: "where do I configure X", "which file controls Y",
  "is this hot-editable", "what restart does Z need", "perc-*.properties",
  "rxconfig", "jetty/base/etc", "DTS Delivery/Server/conf".
version: 8.2.0
---

# Percussion CMS & DTS Configuration Catalog

## 1. Purpose & when to load

This skill is a **reference catalog** of the configuration files that ship
with a Percussion CMS and DTS installation. It is hand-curated and
version-pinned (currently `8.2.0`). Load it whenever an admin, operator, or
AI agent needs to:

- Locate the file that controls a specific setting ("where do I configure
  email?" / "where do I change the JDBC URL?").
- Decide whether an edit is safe to apply at runtime (hot) or whether a
  service restart is required.
- Find the Java loader class or installer template that backs a config
  file (when the loader supports overrides).
- Distinguish customer-editable files from server-managed state
  (e.g. `ObjectStore/*.xml` files).
- Avoid hardcoded OS-specific paths in scripts and tests (the catalog uses
  portable `${...}` placeholders throughout).

**It does NOT** generate configs at runtime, run installers, or perform
edits. It is descriptive.

## 2. Install-tree anchors (portable)

All paths in this catalog are written as portable joins against the
universal anchors below. Substitute the actual install root for the
placeholder on the running host. **Never** hardcode `/` or `\` separators
in production code or tests (see Cross-Platform File I/O & Paths in root
`AGENTS.md`).

|                        Anchor                         |                  Resolves to (per platform)                  |                                                    Purpose                                                    |
|-------------------------------------------------------|--------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `${PERCUSSION_HOME}`                                  | `C:\Percussion` (Windows) or `/opt/Percussion` (Linux/macOS) | CMS install root                                                                                              |
| `${PERCUSSION_HOME}/rxconfig`                         | —                                                            | CMS subsystem configs (workflow, security, search, etc.)                                                      |
| `${PERCUSSION_HOME}/Deployment/Server/conf`           | —                                                            | Tomcat / Jetty bridge configs and `perc-*.properties`                                                         |
| `${PERCUSSION_HOME}/Deployment/Server/rxconfig`       | —                                                            | Legacy alias; new edits use `rxconfig/` at the install root                                                   |
| `${PERCUSSION_HOME}/ObjectStore`                      | —                                                            | **Server-managed; do not hand-edit** (community-managed XML)                                                  |
| `${PERCUSSION_HOME}/var/config`                       | —                                                            | Installer-generated; passwords, SSL, runtime secrets                                                          |
| `${PERCUSSION_HOME}/var/config/generated`             | —                                                            | Installer-generated secrets; treat as opaque                                                                  |
| `${PERCUSSION_HOME}/jetty/base/etc`                   | —                                                            | Jetty datasource / SSL / messaging XML                                                                        |
| `${PERCUSSION_HOME}/jetty/base/start.d`               | —                                                            | Jetty module `.ini` enablement                                                                                |
| `${DTS_HOME}`                                         | `C:\DTS` or `/opt/DTS` (varies)                              | DTS install root                                                                                              |
| `${DTS_HOME}/Delivery/Server/conf`                    | —                                                            | DTS Tomcat / Jetty-bridge configs                                                                             |
| `${DTS_HOME}/Deployment/Server/conf/perc`             | —                                                            | DTS `perc-*.properties` (See `PSUpdateDTSConfiguration`)                                                      |
| `${DTS_HOME}/Deployment/Server/webapps/<svc>/WEB-INF` | —                                                            | Per-microservice layer (`forms`, `polls`, `comments`, `metadata`, `membership`, `feeds`, `secure-membership`) |

## 3. CMS Catalog

All paths are relative to `${PERCUSSION_HOME}` unless stated otherwise.
"Purpose" and "Restart?" columns assume normal CMS operations; consult
section 5 (Resolution & override order) before turning a loader's
override into a production change.

### 3.1 `${PERCUSSION_HOME}/rxconfig/` (CMS subsystem XML)

|                File (relative)                 |                               Purpose                               |     Subsystem     |  Hot?   |     Restart?     |                    Overridable via                     |                              Source-of-truth                               |
|------------------------------------------------|---------------------------------------------------------------------|-------------------|---------|------------------|--------------------------------------------------------|----------------------------------------------------------------------------|
| `rxconfig/Server/config.xml`                   | Master CMS config (datasources, repository, mail, encryption, etc.) | Core              | No      | Yes              | `<file>.local` classpath / `<file>.properties` sidecar | `PSConfigureDatasource` (perc-ant); `com.percussion.cx.PSPropertiesLoader` |
| `rxconfig/Server/server.properties`            | Server runtime properties (port, locale, JNDI bindings)             | Server            | No      | Yes              | JVM `-D` system properties                             | `system/installResources/install.xml`                                      |
| `rxconfig/Server/rxrepository.properties`      | Repository DB connection (type, url, user, encrypted password)      | Repository        | No      | Yes              | `-Drhythmyx.repo.*`                                    | `system/installResources/installRepository.xml`                            |
| `rxconfig/Server/cmserver.properties`          | Internal CM server flags                                            | Server            | No      | Yes              | JVM `-D`                                               | `system/installResources/install.xml`                                      |
| `rxconfig/Server/encryption.properties`        | Symmetric keys for at-rest encryption                               | Security          | No      | Yes              | Re-run installer tool                                  | `PSUpgradeEncryption` (perc-ant)                                           |
| `rxconfig/Server/system.properties`            | System-wide CMS settings (logging, caches)                          | Core              | Partial | Yes              | JVM `-D`                                               | `system/installResources/install.xml`                                      |
| `rxconfig/Server/scheduledTasks.xml`           | Schedule for background jobs                                        | Scheduler         | No      | Yes              | n/a                                                    | `system/installResources/install.xml`                                      |
| `rxconfig/Server/sysActionMenu.xml`            | Action menu definitions                                             | UI                | No      | Yes              | n/a                                                    | `system/installResources/install.xml`                                      |
| `rxconfig/Server/sys_SearchEditor.xml`         | Search editor config                                                | Search            | No      | Yes              | n/a                                                    | `system/installResources/install.xml`                                      |
| `rxconfig/Server/sys_ceResources.xml`          | Content editor resources                                            | Content Editor    | No      | Yes              | n/a                                                    | `system/installResources/install.xml`                                      |
| `rxconfig/Server/sys_EditorVariables.xml`      | Editor variables                                                    | Content Editor    | No      | Yes              | n/a                                                    | `system/installResources/install.xml`                                      |
| `rxconfig/Workflow/rxworkflow.properties`      | Workflow engine properties                                          | Workflow          | No      | Yes              | JVM `-D`                                               | `system/installResources/install.xml` (line 1302)                          |
| `rxconfig/Workflow/rxcmsconfig.xml`            | Workflow definitions and transitions                                | Workflow          | No      | Yes              | n/a                                                    | `system/workflow/config/`                                                  |
| `rxconfig/Workflow/rxwftabledata.xml`          | Workflow table data                                                 | Workflow          | No      | Yes              | n/a                                                    | `system/workflow/config/`                                                  |
| `rxconfig/Workflow/rxwfdatatypemaps.xml`       | Workflow data-type mappings                                         | Workflow          | No      | Yes              | n/a                                                    | `system/workflow/config/`                                                  |
| `rxconfig/Workflow/publish.xml`                | Publish action definitions                                          | Workflow          | No      | Yes              | n/a                                                    | `system/workflow/config/`                                                  |
| `rxconfig/Security/*.xml`                      | Security providers, role mappings                                   | Security          | No      | Yes              | n/a                                                    | `system/installResources/install.xml`                                      |
| `rxconfig/LDAP/*.xml`                          | LDAP directory configuration                                        | Security          | No      | Yes              | n/a                                                    | `system/installResources/install.xml`                                      |
| `rxconfig/Proxy/proxy-config.xml`              | Inbound / outbound proxy rules                                      | Proxy             | No      | Yes              | n/a                                                    | `system/config/proxy-config.xml`                                           |
| `rxconfig/I18n/*.xml`                          | Locale and resource bundle wiring                                   | I18n              | No      | Yes              | n/a                                                    | `system/installResources/install.xml`                                      |
| `rxconfig/ContentConnector/*.xml`              | Content connector mappings                                          | Content Connector | No      | Yes              | n/a                                                    | `system/installResources/install.xml`                                      |
| `rxconfig/Packages/DefaultConfigs/*`           | Default package configs (read-only templates)                       | Package Installer | No      | Rebuild required | n/a                                                    | `system/installResources/installServer.xml`                                |
| `rxconfig/Packages/LocalConfigs/*`             | Customer-local package configs                                      | Package Installer | No      | Rebuild required | n/a                                                    | `system/installResources/installServer.xml`                                |
| `rxconfig/DeliveryServer/delivery-servers.xml` | Declares registered DTS servers                                     | Publisher         | No      | Restart CMS      | n/a                                                    | `system/installResources/installServer.xml` (line 589)                     |
| `rxconfig/Installer/installation.properties`   | Installer-written stamp (irreversible)                              | Installer         | n/a     | n/a              | Do not edit                                            | `system/installResources/install.xml`                                      |
| `rxconfig/Installer/rxrepository.properties`   | Installer-written repository stamp                                  | Installer         | n/a     | n/a              | Do not edit                                            | `system/installResources/installRepository.xml`                            |

### 3.2 `${PERCUSSION_HOME}/Deployment/Server/conf/` (Tomcat / Jetty bridge)

|                        File (relative)                         |                   Purpose                   | Subsystem  |  Hot?   | Restart? |  Overridable via   |                               Source-of-truth                               |
|----------------------------------------------------------------|---------------------------------------------|------------|---------|----------|--------------------|-----------------------------------------------------------------------------|
| `Deployment/Server/conf/server.xml`                            | Tomcat / Jetty server connector, HTTPS, AJP | Server     | No      | Yes      | n/a                | `modules/utils/src/test/resources/.../Deployment/Server/conf/server.xml`    |
| `Deployment/Server/conf/web.xml`                               | Default servlet webapp descriptor           | Server     | No      | Yes      | n/a                | `system/installResources/installServer.xml`                                 |
| `Deployment/Server/conf/perc/perc-catalina.properties`         | Tomcat connector / heap / GC settings       | Server     | No      | Yes      | JVM `-D`           | `PSUpdateDTSConfiguration` (perc-ant)                                       |
| `Deployment/Server/conf/perc/perc-datasources.properties`      | JDBC URL/user/password for server DB        | Repository | No      | Yes      | JVM `-D`           | `PSExecDTSSqlStmt`, `PSExecStagingDTSSqlStmt` (perc-ant)                    |
| `Deployment/Server/conf/perc/perc-datasources.xml`             | Datasource pool definitions (JNDI)          | Repository | No      | Yes      | `-Dperc.ds.*`      | `commerce/perc-ant`                                                         |
| `Deployment/Server/conf/perc/perc-security.properties`         | Webapp security flags (CSRF, headers)       | Security   | No      | Yes      | n/a                | `system/installResources/installServer.xml`                                 |
| `Deployment/Server/conf/perc/perc-email.properties`            | SMTP / outbound mail                        | Mail       | Partial | Yes      | JVM `-D`           | `projects/sitemanage/.../secure/conf/perc/perc-email.properties`            |
| `Deployment/Server/conf/perc/perc-encryption.properties`       | At-rest encryption keys                     | Security   | No      | Yes      | JVM `-D`           | `projects/sitemanage/.../secure/conf/perc/perc-encryption.properties`       |
| `Deployment/Server/conf/perc/perc-secured-sections.properties` | URL allow / deny rules for secured sections | Security   | No      | Yes      | n/a                | `projects/sitemanage/.../secure/conf/perc/perc-secured-sections.properties` |
| `Deployment/Server/conf/perc/perc-system.properties`           | System-wide CSS / JS / log redaction        | Core       | Partial | Yes      | JVM `-D`           | `system/installResources/installServer.xml`                                 |
| `Deployment/Server/conf/perc/perc-catalog-services.properties` | Catalog-service cache TTL                   | Catalog    | Partial | Yes      | `-Dperc.catalog.*` | `system/installResources/installServer.xml`                                 |

### 3.3 `${PERCUSSION_HOME}/jetty/base/etc/` (Jetty datasource / SSL / MQ)

|                   File (relative)                   |                   Purpose                    |  Subsystem  | Hot? | Restart? | Overridable via |                                              Source-of-truth                                              |
|-----------------------------------------------------|----------------------------------------------|-------------|------|----------|-----------------|-----------------------------------------------------------------------------------------------------------|
| `jetty/base/etc/perc-ds.properties`                 | Default datasource driver list, login module | Jetty JDBC  | No   | Yes      | `-Dperc.ds.*`   | `modules/perc-jetty/src/main/jetty/defaults/etc/perc-ds.properties`                                       |
| `jetty/base/etc/perc-ds-derby.properties`           | Derby profile (default)                      | Jetty JDBC  | No   | Yes      | JVM `-D`        | `modules/utils/src/test/resources/com/percussion/utils/container/jetty/base/etc/perc-ds-derby.properties` |
| `jetty/base/etc/perc-ds-mysql.properties`           | MySQL profile                                | Jetty JDBC  | No   | Yes      | JVM `-D`        | `modules/utils/src/test/resources/.../perc-ds-mysql.properties`                                           |
| `jetty/base/etc/perc-ds-mssql.properties`           | MSSQL profile                                | Jetty JDBC  | No   | Yes      | JVM `-D`        | `modules/utils/src/test/resources/.../perc-ds-mssql.properties`                                           |
| `jetty/base/etc/perc-ds.xml`                        | JNDI datasource pool definitions             | Jetty JDBC  | No   | Yes      | n/a             | `modules/perc-jetty/src/main/jetty/defaults/etc/perc-ds.xml`                                              |
| `jetty/base/etc/perc-ssl.xml`                       | SSL / TLS connector config                   | Jetty HTTPS | No   | Yes      | n/a             | `modules/perc-jetty/src/main/jetty/defaults/etc/perc-ssl.xml`                                             |
| `jetty/base/etc/perc-mq.xml`                        | Embedded JMS / Artemis JNDI                  | Messaging   | No   | Yes      | n/a             | `modules/perc-jetty/src/main/jetty/defaults/etc/perc-mq.xml`                                              |
| `jetty/base/etc/login.conf`                         | JAAS login modules                           | Security    | No   | Yes      | n/a             | `modules/utils/src/test/resources/.../login.conf`                                                         |
| `jetty/base/etc/installation.properties`            | Installer-written install stamp              | Installer   | n/a  | n/a      | Do not edit     | `modules/utils/src/test/resources/.../installation.properties`                                            |
| `jetty/base/etc/artemis/broker.xml`                 | Embedded Artemis broker config               | Messaging   | No   | Yes      | n/a             | `modules/perc-jetty/src/main/jetty/defaults/etc/artemis/broker.xml`                                       |
| `jetty/base/etc/perc-webdefault.xml`                | Jetty webdefault overrides                   | Jetty       | No   | Yes      | n/a             | `modules/perc-jetty/src/main/jetty/defaults/etc/perc-webdefault.xml`                                      |
| `jetty/base/start.d/jvm.ini`                        | JVM args for Jetty                           | Jetty       | No   | Yes      | n/a             | `modules/perc-jetty/src/main/jetty/defaults/start.d/jvm.ini`                                              |
| `jetty/base/start.d/perc.ini`                       | Module enablement (perc, ee11, deploy)       | Jetty       | No   | Yes      | n/a             | `modules/perc-jetty/src/main/jetty/defaults/start.d/perc.ini`                                             |
| `jetty/base/start.d/perc-logging.ini`               | Log4j2 module enablement                     | Logging     | No   | Yes      | n/a             | `modules/perc-jetty/src/main/jetty/defaults/start.d/perc-logging.ini`                                     |
| `jetty/base/modules/perc-ds/etc/perc-ds.properties` | Module-scoped datasource defaults            | Jetty JDBC  | No   | Yes      | `-Dperc.ds.*`   | `modules/perc-jetty/.../modules/perc-ds/etc/perc-ds.properties`                                           |

### 3.4 `${PERCUSSION_HOME}/var/config/` (installer-generated)

|          File (relative)          |                        Purpose                        | Subsystem | Hot? | Restart? |          Overridable via          |           Source-of-truth           |
|-----------------------------------|-------------------------------------------------------|-----------|------|----------|-----------------------------------|-------------------------------------|
| `var/config/generated/passwords`  | Installer-generated DB / admin passwords              | Installer | n/a  | n/a      | **Do not edit; re-run installer** | `perc-ant` install tools            |
| `var/config/generated/keystore.*` | TLS / self-signed keystores                           | Security  | No   | Yes      | n/a                               | `PSUpdateDTSCertificate` (perc-ant) |
| `var/config/runtime/*`            | Runtime overrides (created by product on first start) | Runtime   | No   | Yes      | n/a                               | First-boot scripts                  |

### 3.5 `${PERCUSSION_HOME}/ObjectStore/...` (server-managed)

|             File (relative)             |                Purpose                |  Subsystem  | Hot? | Restart? |          Overridable via           |                                 Source-of-truth                                  |
|-----------------------------------------|---------------------------------------|-------------|------|----------|------------------------------------|----------------------------------------------------------------------------------|
| `ObjectStore/IdTypes/ViewDef-*.xml`     | View definitions (server-managed)     | ObjectStore | n/a  | n/a      | **Do not edit; use REST/Designer** | `system/FastForward/Core/MSM/sys_MultiServerManager/server/objectstore/IdTypes/` |
| `ObjectStore/IdTypes/TemplateDef-*.xml` | Template definitions (server-managed) | ObjectStore | n/a  | n/a      | **Do not edit; use REST/Designer** | same as above                                                                    |
| `ObjectStore/IdTypes/SlotDef-*.xml`     | Slot definitions (server-managed)     | ObjectStore | n/a  | n/a      | **Do not edit; use REST/Designer** | same as above                                                                    |
| `ObjectStore/...` (other types)         | All other server-managed XML          | ObjectStore | n/a  | n/a      | **Do not edit; use REST/Designer** | n/a                                                                              |

### 3.6 `${PERCUSSION_HOME}/rxconfig/Logs/` (logs)

|          File (relative)           |          Purpose          | Subsystem | Hot? | Restart? | Overridable via |                          Source-of-truth                           |
|------------------------------------|---------------------------|-----------|------|----------|-----------------|--------------------------------------------------------------------|
| `rxconfig/Logs/rxlog4j2.xml`       | Log4j2 main configuration | Logging   | No   | Yes      | n/a             | `modules/perc-jetty/.../modules/perc-logging/resources/log4j2.xml` |
| `rxconfig/Logs/perc-auditlog.conf` | Audit log writer          | Audit     | No   | Yes      | n/a             | `system/config/perc-auditlog.conf`                                 |

### 3.7 Designer / webapp config (not customer-editable)

These files live under `projects/sitemanage/src/main/resources/Rhythmyx/sys_resources/webapps/`
in the source tree and are **bundled at build time** into the deliverable
webapps. They are **not** customer-editable in place — create an override
copy in the customer's webapp overlay when a change is needed.

|                                  File (relative)                                   |               Purpose                | Subsystem | Hot? | Restart? |           Overridable via           |                   Source-of-truth                   |
|------------------------------------------------------------------------------------|--------------------------------------|-----------|------|----------|-------------------------------------|-----------------------------------------------------|
| `Rhythmyx/sys_resources/webapps/secure/conf/perc/perc-secured-sections.properties` | Secured-section URL rules (template) | Security  | No   | Yes      | Copy into customer's webapp overlay | `projects/sitemanage/.../webapps/secure/conf/perc/` |
| `Rhythmyx/sys_resources/webapps/secure/conf/perc/perc-encryption.properties`       | Encryption keys (template)           | Security  | No   | Yes      | Copy into customer's webapp overlay | `projects/sitemanage/.../webapps/secure/conf/perc/` |
| `Rhythmyx/sys_resources/webapps/secure/conf/perc/perc-email.properties`            | SMTP/outbound mail (template)        | Mail      | No   | Yes      | Copy into customer's webapp overlay | `projects/sitemanage/.../webapps/secure/conf/perc/` |
| `Rhythmyx/sys_resources/webapps/secure/WEB-INF/web.xml`                            | Webapp descriptor (template)         | UI        | No   | Yes      | Copy into customer's webapp overlay | `projects/sitemanage/.../webapps/secure/WEB-INF/`   |

### 3.8 Install-time / build-time files (out of live-editing scope)

These files are surfaced via `system/installResources/install.xml` and its
siblings. They are **not** customer-editable at runtime — they exist in
the source tree to drive the installer / build. A single pointer row is
cataloged here so agents don't go searching randomly.

|                                        File (relative)                                         |                   Purpose                   |                   Source-of-truth                    |
|------------------------------------------------------------------------------------------------|---------------------------------------------|------------------------------------------------------|
| `system/installResources/install.xml`                                                          | Top-level installer                         | `system/installResources/`                           |
| `system/installResources/installServer.xml`                                                    | Server install steps                        | `system/installResources/`                           |
| `system/installResources/installRepository.xml`                                                | Repository install steps                    | `system/installResources/`                           |
| `system/installResources/installFastForward.xml`                                               | FastForward / MSM schema install            | `system/installResources/`                           |
| `system/installResources/installDevTools.xml`                                                  | DevTools install                            | `system/installResources/`                           |
| `system/installResources/installSymlinks.xml`                                                  | Symlink generation                          | `system/installResources/`                           |
| `system/installResources/install.properties`                                                   | Installer properties                        | `system/installResources/`                           |
| `system/installResources/symlinks.properties`                                                  | Symlink mapping                             | `system/installResources/`                           |
| `system/config/*.xml` and `system/config/*.properties`                                         | Build-time defaults                         | `system/config/`                                     |
| `modules/perc-packages/src/main/resources/Packages/perc.<name>/perc.<name>.mapping.properties` | Package install mapping (installer-managed) | `modules/perc-packages/src/main/resources/Packages/` |

## 4. DTS Catalog

All paths are relative to `${DTS_HOME}` unless stated otherwise.

### 4.1 `${DTS_HOME}/Deployment/Server/conf/` (DTS Tomcat / Jetty bridge)

|                           File (relative)                            |                  Purpose                   | Subsystem  |  Hot?   | Restart? | Overridable via |                                        Source-of-truth                                         |
|----------------------------------------------------------------------|--------------------------------------------|------------|---------|----------|-----------------|------------------------------------------------------------------------------------------------|
| `Deployment/Server/conf/server.xml`                                  | Tomcat / Jetty connector, HTTPS, AJP       | Server     | No      | Yes      | n/a             | `modules/perc-ant/src/test/.../Deployment/Server/conf/server.xml`                              |
| `Deployment/Server/conf/web.xml`                                     | Default servlet descriptor                 | Server     | No      | Yes      | n/a             | `system/installResources/install.xml`                                                          |
| `Deployment/Server/conf/catalina.properties`                         | Tomcat class-loader / cache                | Server     | No      | Yes      | JVM `-D`        | `system/installResources/install.xml`                                                          |
| `Deployment/Server/conf/tomcat-users.xml`                            | Tomcat user database (basic auth fallback) | Security   | No      | Yes      | n/a             | `system/installResources/install.xml`                                                          |
| `Deployment/Server/conf/perc/perc-catalina.properties`               | Tomcat connector / heap / GC               | Server     | No      | Yes      | JVM `-D`        | `PSUpdateDTSConfiguration` (perc-ant)                                                          |
| `Deployment/Server/conf/perc/perc-security.properties`               | DTS security flags (CSRF, headers)         | Security   | No      | Yes      | n/a             | `deliverytiersuite/.../delivery-tier-distribution/src/main/conf/perc/perc-security.properties` |
| `Deployment/Server/conf/perc/perc-datasources.properties`            | DTS DB connection                          | Repository | No      | Yes      | JVM `-D`        | `PSExecDTSSqlStmt` (perc-ant)                                                                  |
| `Deployment/Server/conf/perc/perc-datasources.xml`                   | Datasource pool definitions                | Repository | No      | Yes      | `-Dperc.ds.*`   | `deliverytiersuite/.../perc-datasources.xml.sample-MYSQL-MARIADB` (sample)                     |
| `Deployment/Server/conf/perc/perc-datasources.postgresql.properties` | PostgreSQL datasource sample               | Repository | No      | Yes      | JVM `-D`        | `deliverytiersuite/.../perc-datasources.postgresql.properties.sample`                          |
| `Deployment/Server/conf/perc/perc-encryption.properties`             | At-rest encryption                         | Security   | No      | Yes      | JVM `-D`        | `deliverytiersuite/.../perc-encryption.properties.sample`                                      |
| `Deployment/Server/conf/perc/perc-email.properties`                  | SMTP / outbound mail                       | Mail       | Partial | Yes      | JVM `-D`        | `deliverytiersuite/.../perc-email.properties.sample`                                           |
| `Deployment/Server/conf/perc/perc-secured-sections.properties`       | Secured-section URL rules                  | Security   | No      | Yes      | n/a             | `deliverytiersuite/.../perc-secured-sections.properties.sample`                                |

### 4.2 `${DTS_HOME}/Deployment/Server/webapps/<svc>/WEB-INF/` (per-service)

Mark each row with `service: <name>` so tenants can find the right file.

|                               File (relative)                               |       Service       |                   Purpose                   |    Hot?    | Restart? | Overridable via |                                       Source-of-truth                                       |
|-----------------------------------------------------------------------------|---------------------|---------------------------------------------|------------|----------|-----------------|---------------------------------------------------------------------------------------------|
| `webapps/<svc>/WEB-INF/perc-context.properties`                             | all                 | Context-scoped properties                   | Partial    | Yes      | JVM `-D`        | per-service `WEB-INF/perc-context.properties`                                               |
| `webapps/<svc>/WEB-INF/perc-security.properties`                            | all                 | Service-tier security flags                 | No         | Yes      | n/a             | per-service `WEB-INF/perc-security.properties`                                              |
| `webapps/<svc>/WEB-INF/perc-datasources.properties`                         | all                 | Service DB connection                       | No         | Yes      | JVM `-D`        | per-service `WEB-INF/perc-datasources.properties`                                           |
| `webapps/<svc>/WEB-INF/perc-datasources.xml`                                | all                 | Service JNDI pool                           | No         | Yes      | `-Dperc.ds.*`   | `forms/.../WEB-INF/perc-datasources.xml`                                                    |
| `webapps/forms/WEB-INF/perc-form-processor.properties`                      | `forms`             | Form processor config (validation, payload) | Yes (most) | Yes      | JVM `-D`        | `forms/src/main/java/webapp/WEB-INF/perc-form-processor.properties`                         |
| `webapps/forms/WEB-INF/perc-datatype-mappings.properties`                   | `forms`             | Form field → Java type mapping              | Yes        | Yes      | JVM `-D`        | `forms/src/main/java/webapp/WEB-INF/perc-datatype-mappings.properties`                      |
| `webapps/polls/WEB-INF/perc-polls-services.properties`                      | `polls`             | Polls service config                        | Partial    | Yes      | JVM `-D`        | `polls/src/main/java/webapp/WEB-INF/perc-polls-services.properties`                         |
| `webapps/polls/WEB-INF/perc-datasources.xml`                                | `polls`             | Polls JNDI pool                             | No         | Yes      | `-Dperc.ds.*`   | `polls/src/main/java/webapp/WEB-INF/perc-datasources.xml`                                   |
| `webapps/comments/WEB-INF/perc-comments-services.properties`                | `comments`          | Comments service config                     | Partial    | Yes      | JVM `-D`        | `comments/src/main/java/webapp/WEB-INF/perc-comments-services.properties`                   |
| `webapps/membership/WEB-INF/perc-membership-services.properties`            | `membership`        | Membership service config                   | Partial    | Yes      | JVM `-D`        | `membership/src/main/java/webapp/WEB-INF/perc-membership-services.properties`               |
| `webapps/metadata/WEB-INF/perc-metadata-services.properties`                | `metadata`          | Metadata indexing / search config           | Partial    | Yes      | JVM `-D`        | `metadata/src/main/java/webapp/WEB-INF/perc-metadata-services.properties`                   |
| `webapps/feeds/WEB-INF/perc-feeds-services.properties`                      | `feeds`             | Feeds service config                        | Partial    | Yes      | JVM `-D`        | `feeds/src/main/java/webapp/WEB-INF/perc-feeds-services.properties`                         |
| `webapps/secure-membership/WEB-INF/config/perc-secured-sections.properties` | `secure-membership` | Secured-section overrides (test)            | No         | Yes      | n/a             | `secure-membership/src/test/resources/test/WEB-INF/config/perc-secured-sections.properties` |
| `webapps/<svc>/WEB-INF/web.xml`                                             | all                 | Webapp descriptor                           | No         | Yes      | n/a             | per-service `WEB-INF/web.xml`                                                               |
| `webapps/<svc>/WEB-INF/beans.xml`                                           | all                 | Spring beans wiring                         | No         | Yes      | n/a             | per-service `WEB-INF/beans.xml`                                                             |
| `webapps/<svc>/WEB-INF/hibernate.cfg.xml`                                   | `polls`             | Hibernate session-factory                   | No         | Yes      | n/a             | `polls/src/main/java/webapp/WEB-INF/hibernate.cfg.xml`                                      |
| `webapps/<svc>/WEB-INF/security.xml`                                        | `polls`             | Spring security wiring                      | No         | Yes      | n/a             | `polls/src/main/java/webapp/WEB-INF/security.xml`                                           |

### 4.3 `${DTS_HOME}/Deployment/Server/conf/perc/` (logging)

|                   File (relative)                   |          Purpose          | Subsystem | Hot? | Restart? | Overridable via |                                     Source-of-truth                                      |
|-----------------------------------------------------|---------------------------|-----------|------|----------|-----------------|------------------------------------------------------------------------------------------|
| `Deployment/Server/conf/perc/caching-log4j2.xml`    | Caching service log4j2    | Logging   | No   | Yes      | n/a             | `deliverytiersuite/.../delivery-tier-distribution/src/main/conf/perc/caching-log4j2.xml` |
| `Deployment/Server/conf/perc/comments-log4j2.xml`   | Comments service log4j2   | Logging   | No   | Yes      | n/a             | `deliverytiersuite/.../comments-log4j2.xml`                                              |
| `Deployment/Server/conf/perc/feeds-log4j2.xml`      | Feeds service log4j2      | Logging   | No   | Yes      | n/a             | `deliverytiersuite/.../feeds-log4j2.xml`                                                 |
| `Deployment/Server/conf/perc/forms-log4j2.xml`      | Forms service log4j2      | Logging   | No   | Yes      | n/a             | `deliverytiersuite/.../forms-log4j2.xml`                                                 |
| `Deployment/Server/conf/perc/membership-log4j2.xml` | Membership service log4j2 | Logging   | No   | Yes      | n/a             | `deliverytiersuite/.../membership-log4j2.xml`                                            |
| `Deployment/Server/conf/perc/metadata-log4j2.xml`   | Metadata service log4j2   | Logging   | No   | Yes      | n/a             | `deliverytiersuite/.../metadata-log4j2.xml`                                              |
| `Deployment/Server/conf/perc/polls-log4j2.xml`      | Polls service log4j2      | Logging   | No   | Yes      | n/a             | `deliverytiersuite/.../polls-log4j2.xml`                                                 |

## 5. Resolution & override order

For every property loaded through `PSPropertiesLoader` and its variants,
the runtime value is resolved in this fixed precedence (highest first):

1. **JVM `-D` system property** — e.g. `-Drhythmyx.serverName=…`,
   `-Dperc.db.url=…`, `-Dlog4j2.configurationFile=…`. Highest priority and
   survives container restarts.
2. **`<file>.local` / `<file>.properties` override** — the loader first
   looks for a classpath / filesystem sibling named `*.local` or
   `*.properties` before the packaged default. Loader-specific; see the
   "Overridable via" column in the tables above.
3. **Shipped default file** — the file under `rxconfig/`,
   `Deployment/Server/conf/perc/`, or `WEB-INF/perc-*.properties` as
   bundled in the CMS or DTS JAR.

> Each loader class is config-specific. The "Source-of-truth" column in
> sections 3–4 names the Java class or installer template that performs
> the load — grep that class to confirm the override hook before
> turning a `-D` override into a production change.

## 6. Hot-edit / restart policy

The table below is the canonical answer to "does this need a restart?".
Use it before making a live edit; cross-reference the source-of-truth
class for confirmation.

|                            Change kind                             |       Restart needed?        |                                         Notes                                          |
|--------------------------------------------------------------------|------------------------------|----------------------------------------------------------------------------------------|
| Email / SMTP (`perc-email.properties`)                             | No for most loaders          | Some require reloading `perc-system.properties` (e.g. via a CMS admin console action). |
| Outbound HTTP proxy / timeouts                                     | Hot-reloadable               | Spring runtime picks up `perc-system.properties` refresh.                              |
| Datasource (JDBC URL / credentials)                                | **Yes (CMS + Jetty/Tomcat)** | Re-encrypt the secret with the installer tool; never edit in place.                    |
| Datasource pool sizing                                             | **Yes**                      | Hibernate / JNDI pool init is read-once.                                               |
| Workflow / security XML (`rxcmsconfig.xml`, `Security/*.xml`)      | **Yes**                      | `rxconfig` loaders cache at boot.                                                      |
| LDAP config (`rxconfig/LDAP/*.xml`)                                | **Yes**                      | Loaded once at startup.                                                                |
| Hibernate / search index                                           | **Yes**                      | Lucene / Hibernate create caches on first use.                                         |
| Logging (`log4j2.xml`, `perc-logging.ini`)                         | Partial                      | File watcher can pick up `log4j2.xml`; module init requires restart.                   |
| Jetty modules (`start.d/*.ini`)                                    | **Yes**                      | Modules activate at startup.                                                           |
| Keystore / TLS (`perc-ssl.xml`, `var/config/generated/keystore.*`) | **Yes**                      | SSL connectors cache at startup.                                                       |
| Scheduler (`scheduledTasks.xml`)                                   | **Yes**                      | Quartz schedule built at startup.                                                      |
| ObjectStore XML (`ViewDef-*.xml`, `TemplateDef-*.xml`)             | n/a                          | **Server-managed; use REST/Designer**, not file edits.                                 |
| Designer / webapp config (`sys_resources/webapps/...`)             | n/a                          | **Build-time only; create an override copy** in the customer webapp overlay.           |

## 7. Security & secrets

- **Never commit plain-text passwords.** Use the installer-generated
  `var/config/generated/passwords` file and reference it via the
  installer; never edit by hand. The installer re-encrypts secrets
  with the configured master key.
- **Log redaction toggles** live in `perc-*.properties` and
  `rxlogger.properties` (`system/config/rxlogger.properties`).
- **TLS / keystore locations** are configured in
  `Deployment/Server/conf/perc/perc-encryption.properties` and
  `jetty/base/etc/perc-ssl.xml`; the certificate update flow is
  implemented by `PSUpdateDTSCertificate` (perc-ant).
- **At-rest encryption** is configured in `var/config/generated/...` and
  `rxconfig/Server/encryption.properties`. The `PSUpgradeEncryption`
  utility (perc-ant) is the supported way to rotate keys.
- **Audit log writer** is configured in `system/config/perc-auditlog.conf`
  and the runtime override `rxconfig/Logs/perc-auditlog.conf` (see
  section 3.6).

## 8. Cross-platform path reminders

This skill ships to customer installs on Windows, Linux, and macOS.
Agents and scripts quoting paths from this catalog MUST follow the
portable rules in root `AGENTS.md` → **Cross-Platform File I/O & Paths**:

- Use `${PERCUSSION_HOME}` / `${DTS_HOME}` placeholders, not
  `C:\Percussion` or `/opt/Percussion`, in **any** code, test, or
  script citation that is not the OS-specific resolution row in
  section 2.
- Build OS paths with `java.nio.file.Path` / `Path.of` /
  `Paths.get`, never with `"\\"` or `"/"` concatenation.
- Use `File.separator` / `File.pathSeparator` only when a single
  `char` is required. Use `Path.resolve` whenever possible.
- For URL/URI/classpath/zip-entry paths, `/` is correct (always).
- For tests, normalize multi-line file content with
  `replace("\r\n", "\n")` before string-equality assertions.
- Treat the catalog rows as portable joins; the cited source-of-truth
  class is the place to verify any path resolution.

## 9. Help site & version pins

- **Help site:** <https://percussioncmshelp.intsf.com>
- **Git repository:** <https://github.com/intersoftdatalabs-in/percussioncms>
- **Catalog pinned to product version:** `8.2.0`
- **Last verified:** 2026-07-26

When a new release branch is cut, bump `version:` in the frontmatter
and add a `CHANGELOG.md` entry. The catalog is regenerated (curated)
for each release; do not invent rows without a source-of-truth class
or installer template.

## 10. How to extend

Curators (Percussion CMS team, branch owner `development`) extend this
catalog by:

1. Adding or updating a row in the relevant subsystem table in
   section 3 or 4.
2. Citing the source-of-truth Java class or installer template in the
   "Source-of-truth" column.
3. Bumping the `version:` field in the frontmatter.
4. Adding a `CHANGELOG.md` entry that names the new row(s) and the
   underlying file/template change.
5. Running the per-module build (see `README.md` § Build & distribute)
   so the catalog ends up in
   `modules/ai-shared-release/target/distribution/ai/skills/percussioncms.config/`.
6. Letting the existing cosign pipeline
   (`modules/ai-shared-develop/scripts/sign-ai-resources.py`) sign the
   new files before commit.

Do not invent rows in v1 of the catalog. If a file is not grep-able
from a Java loader or an installer template, omit it.
