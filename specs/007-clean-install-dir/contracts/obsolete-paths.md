# Contract: MVP Obsolete Install-Root Paths

Relative to the CMS **install root**. Only delete if the path exists and is eligible.

|       Relative path        |                       Eligibility                       |                                                  Purpose                                                   |
|----------------------------|---------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| `PreInstall`               | Always if present                                       | Legacy preinstall/backup tree unused by 8.x (issue #1157)                                                  |
| `_Percussion_Installation` | Always if present (also try `_Percussion_installation`) | Legacy install-metadata folder; historical cleanup stub never fully implemented                            |
| `JBossServerXML_BAK`       | Conditional — see below                                 | JBoss-era `server.xml` backup used only for very old (5.3-era) port/SSL migration in preinstall post-steps |

## JBossServerXML_BAK eligibility

**Do not offer/delete** when both:

1. Existing product version is `majorVersion == 5` and `minorVersion < 4`, **and**
2. `AppServer` is not present (cannot recreate the bak from JBoss layout)

Otherwise, if the directory exists, it may be cleaned.

## Hard exclusions (never candidates)

Examples (not exhaustive): `jetty/`, `rxconfig/`, `ObjectStore/`, `Repository/`, `rx_resources/`, product `bin/` / service scripts, live content and database files.

## Extension

Adding paths requires product review that 8.x upgrade/runtime never requires them **after** early cleanup, plus doc update in module README.
