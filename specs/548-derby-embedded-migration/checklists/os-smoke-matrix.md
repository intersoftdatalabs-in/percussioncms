# OS smoke matrix — new default H2 install (T038 / SC-001)

**Feature**: #548 Derby embedded migration  
**Integration branch**: `main` (implementation stack merged; residual human QA **#2332**)  
**Purpose**: Document and capture CMS login + DTS health smoke on **Windows, Linux, and macOS** for clean installs accepting defaults (H2, no port 1527).

> **Agent note:** Full multi-OS install/login was **human QA** (#2332 — **closed 2026-08-17 by assignee with no per-OS logs attached**; administrative close, not an evidence PASS). Packaging unit evidence below is the audited PASS; per-OS rows stay `_pending_` until a real matrix run attaches logs. #548 closed 2026-08-19 citing #2332's closure.

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

None recorded. #2332 was closed 2026-08-17 without a waiver comment or per-OS evidence; #548's 2026-08-19 close comment treats that closure as the T038 disposition. Unsupported component/OS pairs still need an explicit waiver on GitHub #548 before skipping.

## Residual tracker

| Residual | Issue | Status (as of 2026-08-11) |
|----------|-------|---------------------------|
| T038 full OS install/login/health | **#2332** | closed 2026-08-17 (administrative; no per-OS logs — see note above) |
| QC-023 hard package install on H2 | **#2333** | closed — #2333 passed 2026-08-11 |

## Related

- Packaging tests: `modules/perc-distribution-tree/.../DefaultEmbeddedH2PackagingTest`
- DTS: `deliverytiersuite/.../metadata/.../DefaultH2BeansPackagingTest`
- Contracts: `contracts/repository-config.md`
- Quality gates residual table: `checklists/quality-gates.md`

