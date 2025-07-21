# metadata
This module contains all the backend support required by DTS for:
* Metadata Indexing
* Cookie Consent services
* Blog Post Visit services

Read/Write data to/from DB from published pages.
Provides REST services for above actions.

This module also contains logic to implement liquibase to connect to the database:
* In /src/main/resources/ we can see masterChangeLog.xml and changeLog.xml.
* changeLog.xml contains changes which we want to implement in any of our database tables.
* masterChangeLog.xml is the main file which should include all the changeLog files.
* In /src/main/java/webapp/WEB-INF/, we have beans.xml which contains bean id metadataLiquibase.
* In beans.xml, we are providing the path to our database connection and path to masterChangeLog.xml.

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
< Java 11 metadata: All your base are belong to us! >
        \   ^__^
         \  (oo)\\_______
            (__)\\       )\/\
                ||----w |
                ||     ||
