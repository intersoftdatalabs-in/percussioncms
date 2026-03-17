# Jetty EE11 Migration

**Date:** March 12, 2026
**Branch:** development
**Target Jetty:** 12.1.7

## Summary

Migrated Percussion Jetty configuration from ee10 to ee11 and aligned module defaults with Jakarta EE 11 module names.

## Changes

1. Updated Jetty version property in the root POM:
   - `jetty.version`: 12.0.25 -> 12.1.7
2. Updated Jetty module dependencies in `perc.mod`:
   - ee10-deploy -> ee11-deploy
   - ee10-plus -> ee11-plus
   - ee10-jstl -> ee11-jstl
   - ee10-servlets -> ee11-servlets
   - ee10-annotations -> ee11-annotations
   - ee10-cdi -> ee11-cdi
3. Updated webapp context classes:
   - `org.eclipse.jetty.ee10.webapp.WebAppContext` -> `org.eclipse.jetty.ee11.webapp.WebAppContext`
4. Renamed webapp env descriptors for ee11 auto-discovery:
   - `jetty-ee10-env.xml` -> `jetty-ee11-env.xml`
   - Applied in both `WebUI` source and docker dev-data mirror.
5. Updated module documentation and agent guidance to reflect ee11 baseline.

## Verification Notes

- Jetty 12.1.7 `jetty-home` includes ee11 module descriptors and ee11 libraries.
- The previous Jetty 12.0.25 home only included ee10 modules.

## Files Updated

- `pom.xml`
- `modules/perc-jetty/src/main/jetty/defaults/modules/perc.mod`
- `modules/perc-jetty/src/main/jetty/base/webapps/Rhythmyx.xml`
- `modules/perc-jetty/src/main/jetty/base/webapps/CI_Home.xml`
- `modules/perc-jetty/src/main/jetty/base/webapps/EI_Home.xml`
- `WebUI/src/main/webapp/WEB-INF/jetty-ee11-env.xml`
- `docker/dev-data/cms-dts/jetty/base/webapps/Rhythmyx/WEB-INF/jetty-ee11-env.xml`
- `modules/perc-jetty/README.md`
- `modules/perc-jetty/AGENTS.md`

