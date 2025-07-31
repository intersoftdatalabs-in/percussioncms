# integrations
This module contains all the backend support required by DTS for:
* EMS Widget
Read/Write EMS widget data to/from DB from published pages.
Provides REST services for above actions.

## Java 11 & SOAP Modernization (July 2025)
- Upgraded Maven configuration to require Java 11 (see pom.xml)
- Set `<maven.compiler.source>`, `<maven.compiler.target>`, and `<maven.compiler.release>` to 11
- Updated `maven-compiler-plugin` to version 3.11.0 for Java 11 compatibility
- All code is now expected to compile and run on Java 11+
- Refactored all Java classes and interfaces under `src/main/java/com/percussion/delivery/integrations/ems` to use Java 11 features:
  - Replaced legacy date/time with `java.time` where possible
  - Used `var`, `Optional`, and Streams for clarity and safety
  - Improved null safety, spelling, and code comments
  - Applied Google Java Style throughout
  - Marked all refactored classes with `// REFACTORED: CP-JAVA11`
- Deep modernization of SOAP server/client code:
  - Refactored SOAP server classes to use modern Java 11 and best practices
  - Marked refactored SOAP server classes with `// REFACTORED: CP-SOAP`
  - Improved XML parsing with safer, more maintainable code
  - All public APIs remain backward compatible
- No public API breaking changes introduced in this migration

## Building

```
mvn clean install
```

## Migration Notes
- Java 11 is now required to build and run this module
- If you encounter compilation issues, ensure your JAVA_HOME points to a Java 11 JDK
- All SOAP server code in `com.percussion.delivery.integrations.ems` is now modernized and marked accordingly
- For more details on migration, see the root README.md

---
< I'll be back... with cleaner code >
---------------------------------
        \   ^__^
         \  (oo)\_______
            (__)\\       )\/\
                ||----w |
                ||     ||
