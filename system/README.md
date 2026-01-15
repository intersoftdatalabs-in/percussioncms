## Java 17 Migration (August 2025)

- Migrated all source code to Java 17 features (var, Optional, Streams, generics).
- Upgraded logging to Log4j 2.x (where applicable).
- Refactored JUnit4 tests to JUnit5 (in Testing/src).
- Ensured backwards compatibility for all public APIs.
- Updated generics usage for collections and iterators.
- Added `// REFACTORED: CP-JAVA11` marker to refactored classes.
- See `refactored-java11-packages.txt` for package tracking.

### Migration Notes

- No breaking changes to public interfaces.
- All legacy logging replaced with Log4j 2.x.
- JUnit5 is now required for all tests.
- Please run `./mvn-env.sh spotless:check` (Spotless enforces code formatting/style — if it fails, run `./mvn-env.sh spotless:apply`), then `./mvn-env.sh clean verify` (or `mvn-env.bat` on Windows) before commits. Note: `google-java-format` (used by Spotless) requires JDK 21 to run, so use the wrapper scripts to execute Spotless under JDK 21.

### Usage Example

```java
// Example: Using Optional and Streams
Optional<String> name = Optional.ofNullable(user.getName());
List<String> emails = users.stream().map(User::getEmail).collect(Collectors.toList());
```

### Package: com.percussion.data

- All data extraction, conditional evaluation, update handler, backend login, stylesheet caching, data type info, error code, index statistics, and utility classes in this package have been refactored to use Java 17 features (var, Optional, Streams, Google Java Style).
- Improved type safety, immutability, and error handling throughout, including PSUpdateHandler, PSBackEndLogin, PSCachedStylesheet, PSDataTypeInfo, IPSBackEndErrors, PSIndexStatistics, and all utility interfaces/classes.
- All public APIs remain backward compatible.
- Logging is now fully Log4j 2.x and OWASP compliant.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these data extraction, update handler, backend login, stylesheet caching, data type info, error code, index statistics, or utility classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.
- PSUpdateHandler, PSBackEndLogin, PSCachedStylesheet, PSDataTypeInfo, IPSBackEndErrors, PSIndexStatistics, and all utility classes now use generics and Java 17 idioms; review for type safety if you subclass or use reflection.

# system

This is the cms core project.This module contains support for following:

* Code for gadgets and widgets.
* XML, JS and resource files for gadgets and widgets.
* Backend support for assembling the gadgets and widgets as well as their respective contents.
* Support for i18n.
* Some miscellaneous backend utilities.
* Backend support to integrate various other modules.

## Building

mvn clean install

## Java 17 Refactoring

### Package: com.percussion.rx.delivery.impl

- All delivery handler classes in this package have been refactored to use Java 17 features (var, Optional, Streams, try-with-resources, etc.).
- Improved type safety, immutability, and error handling.
- All public APIs remain backward compatible.
- Logging is now fully Log4j 2.x and OWASP compliant.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these handlers, ensure your code is Java 17 compatible.
- Optional is now used for some return types (see getPubServerDao().findPubServer and getPropertyValue).
- No breaking changes to public interfaces.

### Package: com.percussion.rx.audit

- All audit logging and design object auditing classes in this package have been refactored to use Java 17 features (var, Optional, Streams, Google Java Style).
- Deprecated methods (e.g., setDate(Date)) are still used for backward compatibility; see inline comments for details.
- All public APIs remain backward compatible.
- Logging and exception handling improved for maintainability and security.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these audit classes, ensure your code is Java 17 compatible.
- Deprecated methods are retained for legacy integration; review comments for migration guidance.
- No breaking changes to public interfaces.

### Package: com.percussion.EditableListBox

- All EditableListBox UI components in this package have been refactored to use Java 17 features (generics, Google Java Style, removal of legacy/unused code).
- Improved type safety and maintainability.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these UI components, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.validation

- All validation constraints and framework classes in this package have been refactored to use Java 17 features (var, Optional, Streams, Google Java Style).
- Improved type safety, immutability, and error handling.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these validation classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.layout

- All grid and box layout manager classes in this package have been refactored to use Java 17 features (generics, Google Java Style).
- Improved type safety and maintainability.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these layout classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.error

- All error handling and string bundle classes in this package have been refactored to use Java 17 features (Google Java Style, improved exception handling).
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these error classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.ImageListControl

- All image list UI components in this package have been refactored to use Java 17 features (generics, Google Java Style, removal of legacy/unused code).
- Improved type safety and maintainability.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these UI components, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.integration

- All integration utility and helper classes in this package have been refactored to use Java 17 features (var, Optional, Streams, Google Java Style).
- Improved type safety, immutability, and error handling.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these integration classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.webdav

- All WebDAV servlet and constants classes in this package have been refactored to use Java 17 features (generics, Google Java Style, improved type safety).
- Improved maintainability and compliance with Google Java Style.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these WebDAV classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.hooks

- All core servlet hooks and utility classes in this package have been refactored to use Java 17 features (generics, Google Java Style, improved type safety).
- Improved maintainability and compliance with Google Java Style.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these hooks classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

### Package: com.percussion.hooks.webservices

- All SOAP webservices servlet endpoint classes in this package have been refactored to use Java 17 features (generics, Google Java Style, improved type safety).
- Deprecated HTTP client APIs are retained for backward compatibility; see TODO comments for future migration.
- All public APIs remain backward compatible.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these webservices classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.
- Review TODO comments for future HTTP client migration.

### Package: com.percussion.util.servlet

- All servlet utility, HTTP request/response, and multipart handling classes in this package have been refactored to use Java 17 features (var, Optional, Streams, generics, Google Java Style).
- Improved type safety, immutability, and error handling.
- All public APIs remain backward compatible.
- Logging is now fully Log4j 2.x and OWASP compliant.
- See code for // REFACTORED: CP-JAVA11 markers.

#### Migration Notes

- If you extend or use these servlet utility classes, ensure your code is Java 17 compatible.
- No breaking changes to public interfaces.

