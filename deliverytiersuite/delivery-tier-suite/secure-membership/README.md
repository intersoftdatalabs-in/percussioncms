# secure-membership
This module contains all the backend support required by DTS for secure published pages which need authentication. It provides AuthenticationProviders that can be LDAP or Internal DB Users.

If a page is set to be secure, then this service takes care of authentication/authorization before DTS presents the page to the user using LDAP Authentication or internal user Authentication.

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
< Java 11 secure-membership: Authentication ka baap! >
        \   ^__^
         \  (oo)\\_______
            (__)\\       )\/\
                ||----w |
                ||     ||
