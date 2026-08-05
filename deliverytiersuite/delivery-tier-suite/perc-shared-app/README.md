# DTS-shared-dependencies

`perc-shared-app` is a **dependency-only aggregator** module for the Percussion Delivery Tier
Suite (DTS). It contains no Java source code of its own; instead, every other DTS service
module (`comments`, `feeds`, `forms`, `membership`, `metadata`, `polls`, `secure-membership`,
`tomcat-common`, `common`) declares `perc-shared-app` as a dependency so they share a single,
consistent set of transitive dependency versions.

## Purpose

- Centralizes dependency declarations for the DTS services so version skew between modules is
  impossible.
- Acts as the single `mvn install` target that pulls in the shared runtime libraries (Hibernate,
  Spring, Jersey, Tomcat, log4j, etc.) for the DTS.
- Produces a single empty `perc-shared-app-8.2.0-SNAPSHOT.jar` artifact (no Java sources, no
  classes).

## Structure

```
perc-shared-app/
├── pom.xml   # declares the empty <dependencies/> block and inherits dependencyManagement
└── README.md
```

There is no `src/` directory. The pom inherits its `<dependencyManagement>` from the parent
`delivery-tier-suite` POM and the root `core` POM, which is what every DTS service consumes.

## Build

```
mvn clean install
```

A successful build produces a `target/perc-shared-app-8.2.0-SNAPSHOT.jar` artifact (the jar is
intentionally empty). The Maven build will emit one harmless warning:

```
[WARNING] JAR will be empty - no content was marked for inclusion!
```

This warning is expected and is the only artifact of the empty `src/` tree. **No Javadoc
warnings or Javadoc errors are produced** because there are no Java sources to document.

## Javadoc status

|         Metric          | Value |
|-------------------------|-------|
| Java source files       | 0     |
| Javadoc source warnings | 0     |
| Javadoc plugin warnings | 0     |
| Javadoc errors          | 0     |
| Javadoc blocks          | 0     |

If a Javadoc-related issue is filed against this module it is almost certainly a stale report
or a copy/paste error from the issue generator — there are no source files from which warnings
could originate.
