# FR-021 — Derby migration support window checklist

**Maps to:** FR-021, SC-012, QC-027 (US6 / T093).  
**Tracking issue:** [GitHub #548](https://github.com/intersoftdatalabs-in/percussioncms/issues/548)

## Policy (frozen)

Automatic migration from **product-managed Apache Derby** to the new default embedded engine (**H2**) remains available for:

|   Window segment   |                                                  Requirement                                                  |
|--------------------|---------------------------------------------------------------------------------------------------------------|
| **GA line**        | Product line that introduces H2 as default embedded repository — migration **must** work and be tested.       |
| **GA + 1**         | **One subsequent product line** — migration **must** still work and be tested.                                |
| **Before removal** | At least **one product-line deprecation notice** in release notes.                                            |
| **After window**   | New lines need not migrate from Derby; customers remaining on Derby must have upgraded while support existed. |

Derby jars on the product classpath are **migration/upgrade scoped** only during the window — not the live new-install default. See `specs/548-derby-embedded-migration/checklists/derby-migration-classpath.md`.

## Checklist (update dates as releases lock)

| # |                                                  Item                                                   |       Owner       |           Target            |                                 Status                                 |
|---|---------------------------------------------------------------------------------------------------------|-------------------|-----------------------------|------------------------------------------------------------------------|
| 1 | Record **GA product line name/version** that ships H2 default + migrator                                | Product / Release | When 8.2 GA ships with #548 | **Open** — pending GA tag                                              |
| 2 | Keep automated migration tests green on GA line (CMS + DTS paths)                                       | Engineering       | Continuous on GA            | **Unit/IT green on `main`** (#1494–#1499); full OS install smoke **T038** → **#2332** open |
| 3 | Identify **next product line** after GA (GA+1) that still ships migrator                                | Product           | After GA named              | **Open**                                                               |
| 4 | Keep migration tests green on GA+1                                                                      | Engineering       | GA+1                        | **Open**                                                               |
| 5 | Publish **deprecation notice** in GA+1 (or earlier) release notes: Derby migration ends after this line | Product / Docs    | Before last supporting line | **Open** — draft language below                                        |
| 6 | Remove Derby migration entry points, Derby packaging, and FR-021 capability after window                | Engineering       | First line **after** GA+1   | **Open**                                                               |
| 7 | Close QC-027 / SC-012 when removal PR merges + docs updated                                             | Engineering       | After removal               | **Open**                                                               |

**Related residual issues (not FR-021 policy itself):** US6/Phase 9 docs freeze **#3065**; QC-023 hard **#2333** closed; T038 **#2332** open.

## Deprecation notice (template)

> **Deprecation:** Automatic migration from product-managed Apache Derby to the default embedded H2 repository is supported through the **\<GA+1 product line\>**. The following product line will **remove** Derby migration and Derby migration-only libraries. Installations still on Derby must upgrade to a release that includes migration **before** that removal. New installs already use H2 and are unaffected. MySQL and SQL Server customers are unaffected.

## Related GitHub comment

Mirror this checklist on issue #548 so release managers can check boxes without opening the repo (T093).
