## Overview

Globally available security utility methods and functions.

Encoding helpers use **OWASP Java Encoder** (`org.owasp.encoder`) only. The full
`org.owasp.esapi:esapi` library and its former `src/main/resources/esapi/**` config
tree were removed (issues #1675 / #1800); the distribution no longer packages
`rxconfig/esapi`.

### Tests

Unit tests live under `src/test/java` in this module.
