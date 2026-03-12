# perc-jetty

This Module creates Jetty install/deployment jar including dependencies, configurations and script files.

## Runtime Baseline

- Jetty: 12.1.7
- Jakarta Servlet API: 6.1.0
- Servlet environment modules: ee11
- Embedded JMS broker: Apache Artemis (in-vm `vm://0`)

The module descriptor at src/main/jetty/defaults/modules/perc.mod is the source of truth for enabled Jetty ee11 modules.

## Embedded Messaging Configuration

- Artemis broker XML: `src/main/jetty/defaults/etc/artemis/broker.xml`
- Jetty JNDI resources: `src/main/jetty/defaults/etc/perc-mq.xml`
- Artemis config folder is exposed on classpath via `src/main/jetty/defaults/modules/perc-mq.mod`

## Building

Run: ../../mvn-env.sh -pl modules/perc-jetty clean install -DskipTests
