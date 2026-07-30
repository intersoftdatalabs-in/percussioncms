# System Module - Percussion CMS Core

The **system** module is the foundational core of Percussion CMS: content management, service infrastructure, business logic, configuration, and deployment resources. It is large and historically layered—prefer the layout below and root / module `AGENTS.md` over old modernization logs.

**Toolchain:** JDK **21** (`release=21`), Maven wrapper `./mvnw` / `mvnw.cmd`, Spotless under JDK 21.

## Table of Contents

- [Module Overview](#module-overview)
- [Directory Structure](#directory-structure)
- [Key Components](#key-components)
- [Java Code Organization](#java-code-organization)
- [Building the Module](#building-the-module)
- [Guidelines for Agents](#guidelines-for-agents)

## Module Overview

- **Core CMS** – Content, assembly, catalog, workflow
- **Service infrastructure** – GUID, data access, error handling, change tracking
- **Business logic** – Delivery, proxy config, auth clients, metadata
- **Configuration** – Editors, workflows, applications, install resources
- **Deployment** – EAR/WAR, Jetty release assets
- **Legacy** – Older Tools/Docs/Samples trees; touch only when required

## Directory Structure

### Active Development (Java)

|     Directory      |                       Purpose                       | Status |
|--------------------|-----------------------------------------------------|--------|
| `services/src`     | Service interfaces and implementations              | Active |
| `business/src`     | Delivery, proxy, auth client, metadata, admin beans | Active |
| `servlet/src`      | Servlet / HTTP handlers                             | Active |
| `src/main/java`    | Core CMS, object store, utilities                   | Active |
| `src/test/java`    | Unit and integration tests                          | Active |
| `beans/src`        | Bean definitions and factories                      | Active |
| `uploader/src`     | File upload handling                                | Active |
| `agenthandler/src` | Agent-related functionality                         | Active |
| `webservices/src`  | SOAP / web service endpoints (in-tree)              | Active |

### Configuration & Resources

|           Directory            |                       Contents                       |
|--------------------------------|------------------------------------------------------|
| `config/`                      | Server config, content editors, workflow, categories |
| `applications/`                | Application XML definitions                          |
| `installResources/`            | Install scripts and templates                        |
| `design/dtd`, `design/schemas` | DTDs and schemas                                     |

### Deployment & Packaging

| Directory  |         Purpose          |
|------------|--------------------------|
| `ear/`     | EAR assembly             |
| `release/` | Jetty / packaging assets |

### Legacy (minimal activity)

`Testing/`, `Docs/`, `FastForward/`, `Designer/`, `Defaults/`, `VersionControl/`, `Tools/`, `Samples/`, `lib/`, `DTD/`, `databases/`, `ReleasedDocuments/`, `configmgr/`, `dtsconfigmgr/` — treat as legacy unless the task explicitly targets them.

## Key Components

### Services (`services/src`)

Catalog, assembly, content, content manager, GUID, data, error, change tracking, security utilities, general info.

### Business (`business/src`)

Delivery / publishing, metadata (including Solr paths), SSL/client trust helpers, proxy configuration, JSF/admin beans, design-time helpers.

### Core (`src/main/java`)

Object store, content models, JDBC helpers, caching, workflow-related core types.

## Java Code Organization

```
com.percussion
├── services/          # assembly, catalog, content, guidmgr, data, …
├── business areas     # delivery, metadata, proxyconfig, rx.admin.jsf.*, …
├── cms/objectstore/   # object store implementations
├── utils/             # shared utilities
└── [legacy packages]  # backward-compatibility surfaces
```

## Building the Module

### Prerequisites

- JDK 21 (`JAVA_HOME`)
- Repo Maven wrapper from repo root or via relative path

### Commands

```bash
# From repo root
./mvnw -pl system clean install

# Format: apply first, then check (see root AGENTS.md Spotless hard gate)
./mvnw spotless:apply
./mvnw spotless:check
# (module-scoped mid-work: ./mvnw -pl system spotless:apply && ./mvnw -pl system spotless:check)
```

Windows: `mvnw.cmd` with the same goals.

### Output

- `target/perc-system-*-SNAPSHOT.jar` (version follows the reactor)

## Guidelines for Agents

1. Read **this README** and **`system/AGENTS.md`** before large changes.
2. Prefer **`src/site/markdown/`** for deeper architecture (`overview.md`, `services.md`, `building.md`).
3. Put new code in **active** trees; do not grow legacy directories without a clear reason.
4. **JDK 21** only on `development`; Google Java Style via Spotless; JUnit 5 for new tests.
5. Do **not** look for or maintain Java 11/17 package modernization logs — those were removed; work is already on the current line.

### When modifying code

- Compile with `release=21`; avoid deprecated APIs
- Run Spotless apply/check before the final PR commit (root AGENTS.md)
- Add or update unit tests for non-trivial behavior
- Update this README only when structure or agent workflow actually changes

