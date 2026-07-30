# Windows service / start scripts — H2 home after upgrade (T097 / QC-024)

**Date:** 2026-07-24  
**Scope:** Document expected service/JVM behavior for default embedded H2; verify paths operators touch after upgrade.

## Policy

After a successful Derby → H2 cutover (or on a new H2 default install):

- Live repository is **H2** (file-mode multiuser under the product repository base).
- Windows Procrun / service JVM options and Unix start scripts must **not** rely solely on `derby.system.home` / `derby.drda.*` for the **live** store.
- Migration-window Derby properties may remain in docs for residual trees until FR-021 ends.

## CMS (Jetty)

|        Item         |                                                  Notes                                                   |
|---------------------|----------------------------------------------------------------------------------------------------------|
| Datasource defaults | `modules/perc-jetty/.../defaults/etc/perc-ds.properties` — `perc.ds.1.driver.name=h2`                    |
| Start/stop          | `StartJetty.sh` / `StartJetty.bat`, `StopJetty.*`, `resolve-java-home.*`                                 |
| After upgrade       | Cutover rewrites `rxrepository.properties` and Jetty `perc-ds` labels to H2; restart CMS service/process |

Operators should confirm service **restart** after upgrade so the JVM reloads cutover files. If a custom service definition hardcodes Derby system properties, update or remove those entries.

## DTS (Tomcat / Windows service)

|          Item           |                                        Notes                                         |
|-------------------------|--------------------------------------------------------------------------------------|
| Service install scripts | `DTSProductionService.bat`, `DTSStagingService.bat` under delivery-tier-distribution |
| Start/stop              | `TomcatStartup.*`, `TomcatShutdown.*`                                                |
| Per-service DS          | Service-level `perc-datasources` / props after DTS migrator cutover                  |

US1/US2 work rewired DTS packaging toward H2 home properties. After upgrade:

1. Confirm each migrated service’s datasource points at H2.
2. Reinstall or update Windows service only if JVM options still inject Derby-only homes for the live path.
3. Smoke-test service start without port **1527**.

Automated alignment test (when present): `DtsTomcat11WindowsServiceAlignmentTest` (Tomcat 11 service naming); pair with H2 packaging tests for driver identity.

## Operator checklist

- [ ] CMS service/process restarted after SUCCESS cutover
- [ ] DTS services restarted after per-service SUCCESS
- [ ] No hard dependency on listening **1527** for new default or post-migration live path
- [ ] Custom Procrun `++JvmOptions` reviewed for obsolete `derby.*` live settings

## Related

- [operator-upgrade-sequence.md](../../../docs/ai-generated/tasks/548-derby-embedded-migration/operator-upgrade-sequence.md)
- [derby-migration-classpath.md](./derby-migration-classpath.md)
- [packaging-audit.md](./packaging-audit.md)

