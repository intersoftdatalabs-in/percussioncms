# extensions-main

The **extensions-main** module provides a collection of server-side extension
classes and utilities that ship with Percussion CMS.  It houses all of the
standard Java exits, UDFs, and other extension points that are used by the
core application and by customers when they implement custom behavior.  The
code here is intended to be lightweight, backward‑compatible and safe to run
in both production and development environments.  In addition to Java
implementations, the module also contains supporting JavaScript resources used
by the Rhythmyx engine.

## Key responsibilities

* Common extension base classes (`PSGenericAssembly`, `PSSimpleJavaUdf_…`,
  etc.)
* CMS-specific business logic exits (search handlers, community ACL helpers)
* Utility helpers for string manipulation, date conversion, etc.
* Preprocessors and result document processors used by the Content
  Management clients.

## Developer guide

Before building, make sure you're using **JDK 21** (see project README for
wrapper scripts).  The module is built with Maven and is part of the parent
multi‑module project, so its lifecycle is managed by the top‑level POM.

From the workspace root you can compile just this module with:

```bash
# use wrapper so JAVA_HOME is set correctly
cd modules/extensions-main
../../mvn-env.sh clean compile
```

Run the unit tests with:

```bash
../../mvn-env.sh test
```

Because the module depends on other components of the system, a full
`clean install` from the root is usually required when making cross‑module
changes:

```bash
./mvn-env.sh clean install -DskipTests
```

### Notes for contributors

* Keep public APIs backward compatible; avoid breaking changes in classes
  under `com.percussion.extensions`.
* Add new tests alongside any new code; the module uses JUnit 5.
* Run `./mvn-env.sh spotless:check` before committing and apply fixes with
  `spotless:apply` if needed.

---

