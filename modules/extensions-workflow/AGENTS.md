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

1. **No `PSConnectionMgr` use — the class is deleted (Phase 4d-1d, #1561).**
   - The `com.percussion.workflow.PSConnectionMgr` class no longer exists. Any
     reference to it (in imports, comments, tests, or new code) is a defect and
     must be replaced with the canonical helper below.
   - For JDBC connections in code that runs outside a Spring-managed request
     transaction, use `com.percussion.utils.jdbc.PSConnectionHelper.getDbConnection()`
     and pair it with `PSConnectionHelper.releaseDbConnection(connection)` (added
     in Phase 4d-1d). The `releaseDbConnection` helper swallows `SQLException`
     on close to match the legacy semantics; do not bypass it with a raw
     `connection.close()` in a `finally` block unless you also add the swallow.
   - For table-name resolution in any remaining raw-JDBC paths, use an inline
     uppercase constant (e.g. `private static final String TABLE_FOO = "FOO";`).
     Do not reintroduce a qualified-identifier API — the previous
     `getQualifiedIdentifier(...)` was a no-op (all catalog/schema flags were
     hardcoded `false`) and existed only to support a fragile static-init
     pattern at class-load time. The legacy context classes now use inlined
     constants (see Phase 4d-1d PR for the per-class mapping).
   - **Column identifiers must stay unqualified.** Do not reintroduce
     `schema.table.column` strings; H2 does not accept them and the original
     "Column PUBLIC not found" defect was traced to that. This rule is
     unchanged from the prior `PSConnectionMgr.getQualifiedIdentifier(...)`
     guidance.
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
../mvnw clean install
```

Plus Spotless (configured upstream):

```bash
../mvnw -pl modules/extensions-workflow spotless:apply
../mvnw -pl modules/extensions-workflow spotless:check
```

Standalone builds must succeed with **no new warnings** and all tests green
before a PR is opened.

---

## See also

- Migration epic + phased plan: `docs/ai-generated/migrations/workflow-orm/00-inventory.md`
- Root repo rules: `AGENTS.md`
- `system` module rules (services, locators, JDK 21, Spotless):
  `system/AGENTS.md`

