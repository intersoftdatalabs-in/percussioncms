# Erlang review — restore Artemis / drop ActiveMQ Classic (Dependabot #79/#80)

| Field | Value |
|-------|--------|
| **Date** | 2026-07-17 |
| **Branch** | `fix/restore-artemis-drop-activemq-classic` |
| **Base** | `development` |
| **Intent** | Restore Artemis WebUI deps lost in merge 3755de7510; remove Classic 5.7.0; delete legacy Classic Jetty env and activemq.xml |
| **Reviewer** | Erlang (strict) |

## Scope

| Path | Change |
|------|--------|
| `WebUI/pom.xml` | Replace `activemq-spring`/`activemq-core` 5.7.0 with `artemis-server` + `artemis-jakarta-client` (BOM versions) |
| `WebUI/src/main/webapp/WEB-INF/jetty-env.xml` | **Delete** legacy EE10 Classic JNDI wiring |
| `modules/perc-jetty/.../etc/activemq/activemq.xml` | **Delete** leftover Classic broker XML (already removed in 88ca5716b0) |

`docker/dev-data/**` is gitignored; local docker env/web.xml were aligned but are not in this PR.

## Context verified

1. Product runtime target is Artemis (`jetty-ee11-env.xml`, `perc-mq.xml`, root BOM `artemis.version=2.50.0`).
2. No production Java imports of Classic ActiveMQ packages.
3. CVEs #79/#80 are XSS in ActiveMQ **demo** webapps; clearing `activemq-core` from the graph is the correct supply-chain fix (not bumping Classic).
4. WebUI `web.xml` already uses `jakarta.jms.*` resource types.
5. Cross-platform path checklist: N/A (XML/POM only; no new path I/O).

## Issues

None (bugs).

### Suggestions (non-blocking)

| ID | Finding |
|----|---------|
| S1 | After merge, confirm Dependabot #79/#80 close when graph refreshes. |
| S2 | Operators with local `docker/dev-data` should refresh Rhythmyx `jetty-ee11-env.xml` from WebUI Artemis template (gitignored tree). |
| S3 | Follow-up: full audit of merge `3755de7510` for other reverts of intentional removals. |

## Recommendation

**`approve`**

## Gate

**May commit/push: yes** — stage only the three intentional paths (+ this report). Exclude `shared-common*.js` CRLF noise.
