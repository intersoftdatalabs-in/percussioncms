# delivery-tier-suite

This module contains submodules for each of the DTS Services.
* Caching Service
* Comments Service
* Feeds Service
* Forms Service
* Membership Service
* Metadata Service
* Polls Service

And all other supporting services modules.

## Building

```
Use `./mvnw clean install` (or `mvnw.cmd clean install` on Windows) so Maven runs with JDK 21.
```

## Test logging

Unit tests must stay quiet on the Maven reactor console. Each service module ships
`src/test/resources/log4j2-test.xml` (Log4j2’s auto-load name — not `log4j2-tester.xml`)
with Hibernate/Spring at WARN. Test Hibernate configs set `hibernate.show_sql=false`.
Surefire also redirects leftover `System.out` to `target/surefire-reports` so SQL dumps
do not flood CI.

