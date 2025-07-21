# perc-delivery-common
This module contains all the files/services that are shared by all other modules used by DTS. For example:
 * Emails
 * Exceptions
 * Security
 etc...

## Java 11 Migration (July 2025)
- Upgraded Maven configuration to require Java 11 (see pom.xml)
- Set `<maven.compiler.source>`, `<maven.compiler.target>`, and `<maven.compiler.release>` to 11
- Updated `maven-compiler-plugin` to version 3.11.0 for Java 11 compatibility
- All code is now expected to compile and run on Java 11+
- No public API changes or breaking changes introduced in this migration

## Building

```
mvn clean install
```

## Migration Notes
- Java 11 is now required to build and run this module
- If you encounter compilation issues, ensure your JAVA_HOME points to a Java 11 JDK
- For more details on migration, see the root README.md

---
< Mogambo khush hua! Java 11 migration successful! >
        \   ^__^
         \  (oo)\\_______
            (__)\\       )\/\
                ||----w |
                ||     ||
