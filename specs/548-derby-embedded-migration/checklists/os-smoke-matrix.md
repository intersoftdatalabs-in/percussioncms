# OS smoke matrix — new default H2 install (T038 / SC-001)

**Feature**: #548 Derby embedded migration  
**Integration branch**: `main` (implementation stack merged; residual human QA **#2332**)  
**Purpose**: Document and capture CMS login + DTS health smoke on **Windows, Linux, and macOS** for clean installs accepting defaults (H2, no port 1527).

> **Agent note:** Full multi-OS install/login is **human QA** (#2332). Overnight agents re-verify packaging unit evidence only; they do not claim T038 closed without host installs.

## Commands (cross-platform outline)

|              Step              |                               Linux / macOS                               |           Windows            |
|--------------------------------|---------------------------------------------------------------------------|------------------------------|
| Build installer artifacts      | Per product release pipeline / local `Maven wrapper` distribution modules | Same via `mvnw.cmd`          |
| Install CMS accepting defaults | Installer CLI with no `--db.type` (defaults `h2`)                         | Same                         |
| Start CMS                      | Product start script under install root                                   | Product `.bat` service/start |
| CMS smoke                      | Browser/curl login to `/Rhythmyx/login`                                   | Same URL                     |
| Install DTS defaults           | DTS installer; `perc.db.type=h2` default                                  | Same                         |
| DTS health                     | Service health endpoints / Tomcat start without DRDA 1527                 | Same                         |

**Do not** require Derby NetworkServer / port **1527** for new default path.

## Evidence log

|   OS    |       Component        |    Date    |     Result      |                                                  Notes / waiver                                                   |
|---------|------------------------|------------|-----------------|-------------------------------------------------------------------------------------------------------------------|
| Linux   | CMS packaging defaults | 2026-07-24 | **PASS** (unit) | `DefaultEmbeddedH2PackagingTest`, `DefaultH2BeansPackagingTest`; full install smoke pending distribution artifact |
| Linux   | DTS packaging defaults | 2026-07-24 | **PASS** (unit) | metadata/comments/forms/feeds/membership/polls H2 props; Liquibase `dbms="h2"`                                    |
| Linux   | CMS full login smoke   | _pending_  |                 | Needs installed distribution from this branch                                                                     |
| Linux   | DTS health smoke       | _pending_  |                 | Needs installed DTS from this branch                                                                              |
| Windows | CMS + DTS              | _pending_  |                 | CI agent or desktop run; no product-owner waiver yet                                                              |
| macOS   | CMS + DTS              | _pending_  |                 | Same                                                                                                              |

## Product-owner waivers

None recorded. Unsupported component/OS pairs must be waived on GitHub #548 before skipping.

## Residual tracker

| Residual | Issue | Status (as of 2026-08-11) |
|----------|-------|---------------------------|
| T038 full OS install/login/health | **#2332** | open — assignee @vijaya-boddipudi |
| QC-023 hard package install on H2 | **#2333** | closed — QA Passed |

## Related

- Packaging tests: `modules/perc-distribution-tree/.../DefaultEmbeddedH2PackagingTest`
- DTS: `deliverytiersuite/.../metadata/.../DefaultH2BeansPackagingTest`
- Contracts: `contracts/repository-config.md`
- Quality gates residual table: `checklists/quality-gates.md`

