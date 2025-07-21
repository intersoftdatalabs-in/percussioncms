# DTS-shared-dependencies
This module just contains all the dependency jars that are used by each of the DTS services.

## Java 11 Migration (July 2025)
- Upgraded Maven configuration to require Java 11 (see pom.xml)
- Set `<maven.compiler.source>`, `<maven.compiler.target>`, and `<maven.compiler.release>` to 11
- Updated `maven-compiler-plugin` to version 3.11.0 for Java 11 compatibility
- All dependencies are now expected to be compatible with Java 11+
- No public API changes or breaking changes introduced in this migration

## Building

```
mvn clean install
```

## Migration Notes
- Java 11 is now required to build and use this module
- If you encounter compilation issues, ensure your JAVA_HOME points to a Java 11 JDK
- For more details on migration, see the root README.md

---
< Code ka hero ban gaya tu! Java 11 migration complete! >
        \   ^__^
         \  (oo)\\_______
            (__)\\       )\/\
                ||----w |
                ||     ||
