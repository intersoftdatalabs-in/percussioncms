# Issue #667 — Jakarta EE 11 residual checklist

**Issue:** [Migrate from J2EE10 to J2EE11](https://github.com/intersoftdatalabs-in/percussioncms/issues/667)  
**Implementation branch:** `chore/667-jakarta-ee11-residual`  
**Last updated:** 2026-07-23

## Status summary

|                     Area                      |                  Status                  |
|-----------------------------------------------|------------------------------------------|
| CMS Jetty ee11 + parent Jakarta APIs          | **Done** (pre-existing on `development`) |
| DTS Tomcat packaging / naming / Cargo         | **Done** (this branch)                   |
| DTS descriptors / servlet props / Jersey 4    | **Done** (this branch)                   |
| CMS shipped web.xml + jetty-ee11 Maven plugin | **Done** (this branch)                   |
| Unit / module clean install (DTS suite)       | **Done** — `BUILD SUCCESS`               |
| Runtime smoke (deployed CMS + DTS)            | **Open** — required before closing #667  |
| Issue body / checkbox hygiene on GitHub       | **Open** — update when PR merges         |

---

## Completed residuals (this branch)

### A. DTS Tomcat packaging

| ID |               Item               |                                            Resolution                                            |
|----|----------------------------------|--------------------------------------------------------------------------------------------------|
| A1 | Cargo `tomcat10x` / plugin 1.9.0 | **`cargo-maven3-plugin` 1.10.28**, `containerId=tomcat11x`; packager → `target/package`          |
| A2 | Conf tree `tomcat10/`            | Renamed **`src/main/tomcat11/`**; conf `web.xml` → Servlet **6.1**                               |
| A3 | Dead `tomcat9/` tree             | **Removed**                                                                                      |
| A4 | Windows Procrun 9/10 mismatch    | **`tomcat11.exe` / `tomcat11w.exe`**; installDts + both service `.bat` aligned; unit tests added |
| A5 | DTS `servlet.api.version` 6.0.0  | **6.1.0**                                                                                        |
| A6 | WAR `web.xml` pre-Jakarta        | All active DTS WARs + tests → **web-app_6_1.xsd**                                                |
| A7 | Jersey 3.1 vs JAX-RS 4.0         | **Jersey 4.0.2**; dropped discontinued `jaxrs-ri`; explicit `jakarta.xml.bind-api`               |
| A8 | metadata cargo `tomcat7x`        | **`tomcat11x`** + `${tomcat.version}` zip                                                        |
| A9 | DTS distribution README          | Tomcat 11 / Cargo / Procrun table added                                                          |

### B. CMS residuals

| ID |             Item             |                                             Resolution                                              |
|----|------------------------------|-----------------------------------------------------------------------------------------------------|
| B1 | `jetty-maven-plugin` 11.0.26 | Parent pluginManagement → **`org.eclipse.jetty.ee11:jetty-ee11-maven-plugin`** @ `${jetty.version}` |
| B2 | WebUI shipped `web.xml`      | **Servlet 6.1** schemas (`WEB-INF`, `cm/WEB-INF`, `war/WEB-INF`)                                    |
| B3 | Tracker hygiene              | Status comments + this checklist; formal issue checkbox edit after PR                               |

### Tests added / updated

- `DtsTomcat11WindowsServiceAlignmentTest` (4 tests) — bats, installDts, EXEs, tree names
- `DtsInstallerJarContainsTomcatTreeTest` — expects `tomcat11.exe` / `tomcat11w.exe`

### Build evidence (2026-07-23)

```bash
cd deliverytiersuite/delivery-tier-suite && ../../mvnw clean install -DskipITs
# BUILD SUCCESS — all 12 modules
# delivery-tier-distribution: Tests run: 68, Failures: 0
# Includes DtsTomcat11WindowsServiceAlignmentTest, DtsInstallerJarContainsTomcatTreeTest
```

Shipping jar contains `distribution/tomcat11.exe`, `distribution/Deployment/Server/conf/server.xml`, DTS wars; manager apps stripped.

---

## Still open before closing #667

### C1. Runtime smoke (hard gate)

- [ ] DTS installer jar deploys and Tomcat **11.0.22** starts (Production + Staging layouts)
- [ ] Windows service install uses **`tomcat11.exe`** end-to-end (if Windows CI/lab available)
- [ ] Linux systemd path still healthy
- [ ] DTS services respond: metadata, forms, comments, feeds, membership, polls
- [ ] CMS Jetty ee11 login + publish → DTS handoff

### C2. GitHub issue close-out

- [ ] Refresh issue body “current state” and check completed phase boxes
- [ ] Link merged PR(s)
- [ ] Close only after C1 smoke sign-off

### Explicitly out of scope (not residuals for close)

- Frozen `system/release/tomcat/**` historical trees
- `p13n-ds` legacy Tomcat 6-era paths
- Full stock Tomcat 11 conf wholesale replace (overlay listeners already match stock 11.0.22 set)
- Procrun binary *re-download* from Windows ASF zip — EXEs are Commons Daemon renames; naming aligned to Tomcat 11 product line

---

## Key versions after residual work

|  Component   |                 Version                 |
|--------------|-----------------------------------------|
| Jetty (CMS)  | 12.1.7 ee11                             |
| Tomcat (DTS) | 11.0.22                                 |
| Cargo        | cargo-maven3-plugin 1.10.28 / tomcat11x |
| Servlet API  | 6.1.0                                   |
| Jersey (DTS) | 4.0.2                                   |
| JAX-RS API   | 4.0.0                                   |
| Spring       | 7.0.7                                   |
| Hibernate    | 7.2.6.Final                             |
| Java         | 21                                      |

---

## Work package map

|                     WP                     |          Status           |
|--------------------------------------------|---------------------------|
| WP-0 Issue comment + checklist             | Done (ongoing updates)    |
| WP-1 Cargo tomcat11x                       | Done                      |
| WP-2 Rename tomcat10 / drop tomcat9        | Done                      |
| WP-3 Windows Procrun alignment             | Done                      |
| WP-4 Servlet props + web.xml               | Done                      |
| WP-5 Jersey 4                              | Done                      |
| WP-6 CMS jetty-ee11 plugin + WebUI web.xml | Done                      |
| WP-7 Build / tests / PR                    | In progress               |
| WP-8 Runtime smoke + close                 | **Blocked on lab/deploy** |

