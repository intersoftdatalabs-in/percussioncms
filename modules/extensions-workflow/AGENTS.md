# `modules/extensions-workflow` — Local Agent Rules

**This file scopes the root [AGENTS.md](../../../AGENTS.md) to the
`modules/extensions-workflow` module. Where this file disagrees, this file wins.**
(Matches the root's Rule Discovery Protocol: `AGENTS.local.md > AGENTS.md > root`.)

---

## Status of this module

This module is the **legacy XML-extension workflow implementation**. It predates
the Spring + Hibernate + service-locator conventions enforced elsewhere in the
codebase. Two pre-existing `@Deprecated // TODO: This class needs refactored to
use hibernate / spring` markers in `PSStateRolesContext.java:34` and
`PSContentAdhocUsersContext.java:73` already point at the direction below.

Issue **#1561** is the active epic that will replace the raw-JDBC write paths
in this module with the existing Hibernate + Spring stack that already ships in
the `perc-system` artifact.

**Phase 1 status (post-#1563):** PR #1563 fixed the H2 column-qualifier bug in
`PSContentStatusHistoryContext` and `PSContentAdhocUsersContext`. Four contexts
remain H2-vulnerable and are tracked in the inventory at
`docs/ai-generated/migrations/workflow-orm/00-inventory.md` §4.2:
`PSContentTypesContext`, `PSNotificationsContext`, `PSStateRolesContext`,
`PSTransitionNotificationsContext`.

---

## Hard rules for any change touching this module

1. **No new direct `PSConnectionMgr` use in in-product paths.**
   - `new PSConnectionMgr()` (or any constructor of
     `com.percussion.workflow.PSConnectionMgr`) is forbidden in code that runs
     inside a CMS request transaction (check-in / check-out / transition /
     site create / NavTree).
   - `PSConnectionMgr.getQualifiedIdentifier(...)` is still acceptable for
     **table-name assembly only**; **column identifiers must stay unqualified**.
     Do not reintroduce `schema.table.column` strings; H2 does not accept them
     and the original "Column PUBLIC not found" defect was traced to that.
   - Justification if absolutely required: add an audit comment with the
     specific ticket number and an explanation of why the request is not on a
     Spring transaction.
2. **New writes against workflow tables go through the ORM service.**
   - For `CONTENTSTATUSHISTORY`, use
     `com.percussion.services.system.PSSystemServiceLocator.getSystemService().saveContentStatusHistory(...)`
     rather than building an `INSERT` string.
   - The same applies once Phase 3 lands for `CONTENTADHOCUSERS`,
     `NOTIFICATIONS`, `TRANSITIONNOTIFICATIONS`, `STATEROLES`, `ROLES`,
     `CONTENTTYPES` — follow the rules in
     `docs/ai-generated/migrations/workflow-orm/00-inventory.md`.
3. **Match root AGENTS.md on cross-platform file I/O.** Any new path/file code
   in this module goes through `java.nio.file.Path` / `Files.*`; Windows,
   Linux and macOS must all be supported.
4. **Follow root AGENTS.md on Erlang pre-commit review.** String-SQL changes,
   raw JDBC, and tx-boundary changes are explicitly within scope of the
   `erlang-review` checklist. Do not commit/push without running it.
5. **Service-locator pattern, not constructor injection.** New services in or
   used by this module follow the rules in `system/AGENTS.md` §"Service
   Development" — interface + impl + `PSXxxServiceLocator`.
6. **Backwards compatibility.** Public exit classes (`PSExit*`) are invoked
   from XML applications registered in `src/main/resources/Extensions.xml`.
   Changing a method signature requires an XML-apps update **in the same PR**
   and a release-note entry.
7. **Reuse the in-tree helpers.** Connection diagnostics for any future
   in-product debug logging must use
   `com.percussion.utils.jdbc.PSJdbcConnectionDiagnostics`
   (`modules/utils/src/main/java/.../PSJdbcConnectionDiagnostics.java`,
   added by PR #1563). `CHAR(1)` `'Y'`/`'N'` flag mapping must follow the
   `com.percussion.services.sitemgr.data.BooleanToTFCharConverter` pattern
   (also added by PR #1563). Do not invent local one-off probes or converters.

---

## Pre-PR build gate for this module

Per root AGENTS.md **Pre-PR Maven verification (HARD GATE)**, this module is
small enough that a per-module standalone build is the default:

```bash
cd modules/extensions-workflow
../mvn-env.sh clean install
```

Plus Spotless (configured upstream):

```bash
../mvn-env.sh -pl modules/extensions-workflow spotless:apply
../mvn-env.sh -pl modules/extensions-workflow spotless:check
```

Standalone builds must succeed with **no new warnings** and all tests green
before a PR is opened.

---

## See also

- Migration epic + phased plan: `docs/ai-generated/migrations/workflow-orm/00-inventory.md`
- Root repo rules: `AGENTS.md`
- `system` module rules (services, locators, JDK 21, Spotless):
  `system/AGENTS.md`
