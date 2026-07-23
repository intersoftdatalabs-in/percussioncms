# Adding a New Extension

This guide walks through adding a new Java extension to `extensions-main` and
registering it so it is available at runtime.

## Step 1: Choose the right interface

Consult the [Extension Point Interfaces](./extension-interfaces.html) reference
to select the interface that matches the behaviour you need. Common choices:

- **`IPSUdfProcessor`** — compute a value from template parameters
- **`IPSResultDocumentProcessor`** — modify the XML result document
- **`IPSRequestPreProcessor`** — modify request parameters before processing
- **`IPSFieldInputTransformer`** — transform a content-editor field on save
- **`IPSWorkflowAction`** — trigger behaviour on a workflow transition

## Step 2: Create the implementation class

Place the class in the appropriate sub-package of
`src/main/java/com/percussion/extensions/`. Use `PSDefaultExtension` as the
base:

```java
package com.percussion.extensions.general;

import com.percussion.extension.IPSRequestContext;
import com.percussion.extension.IPSUdfProcessor;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.extension.PSExtensionProcessingException;

/**
 * A UDF that returns the input string truncated to a maximum length.
 *
 * <p>Parameters:
 * <ol>
 *   <li>value (String) - the string to truncate</li>
 *   <li>maxLength (String) - maximum number of characters</li>
 * </ol>
 */
public class PSSimpleJavaUdf_truncate extends PSDefaultExtension
    implements IPSUdfProcessor {

    @Override
    public Object processUdf(Object[] params, IPSRequestContext request)
        throws PSParameterMismatchException, PSExtensionProcessingException {

        if (params == null || params.length < 2) {
            throw new PSParameterMismatchException(2, params == null ? 0 : params.length);
        }

        String value = params[0] == null ? "" : params[0].toString();
        int maxLength = Integer.parseInt(params[1].toString());
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
```

### Thread safety

Set `com.percussion.extension.reentrant` to `yes` in `Extensions.xml` only if
your extension is fully thread-safe (no shared mutable state). Most utility
extensions that operate only on their inputs are reentrant.

## Step 3: Register in `Java/Extensions.xml`

Add an `<Extension>` element to
`src/main/resources/Java/Extensions.xml`.

```xml
<Extension categorystring="generic"
           context="global/percussion/generic/"
           deprecated="no"
           handler="Java"
           name="sys_truncate">
  <initParam name="com.percussion.user.description">
    Truncates a string to a given maximum length.
  </initParam>
  <initParam name="com.percussion.extension.version">1</initParam>
  <initParam name="className">
    com.percussion.extensions.general.PSSimpleJavaUdf_truncate
  </initParam>
  <initParam name="com.percussion.extension.reentrant">yes</initParam>
  <interface name="com.percussion.extension.IPSUdfProcessor"/>
  <PSXExtensionParamDef id="0">
    <name>value</name>
    <dataType>java.lang.String</dataType>
    <description>The string to truncate.</description>
  </PSXExtensionParamDef>
  <PSXExtensionParamDef id="1">
    <name>maxLength</name>
    <dataType>java.lang.String</dataType>
    <description>Maximum number of characters to keep.</description>
  </PSXExtensionParamDef>
  <suppliedResources/>
</Extension>
```

### Naming conventions

- Use the `sys_` prefix for all built-in extensions — this is the convention
  for all 340+ existing entries.
- The name must be unique within its context path.
- The FQN of the example above is
  `Java/global/percussion/generic/sys_truncate`.

### Context path conventions

|          Interface           |              Context              |
|------------------------------|-----------------------------------|
| `IPSUdfProcessor`            | `global/percussion/generic/`      |
| `IPSResultDocumentProcessor` | `global/percussion/exit/`         |
| `IPSRequestPreProcessor`     | `global/percussion/exit/`         |
| `IPSPasswordFilter`          | `global/percussion/filter/`       |
| `IPSFieldInputTransformer`   | `global/percussion/content/`      |
| `IPSWorkflowAction`          | `global/percussion/workflow/`     |
| `IPSEffect`                  | `global/percussion/relationship/` |
| `IPSEditionTask`             | `global/percussion/task/`         |
| `IPSAssembler`               | `global/percussion/assembly/`     |

## Step 4: Write a unit test

Create a JUnit 5 test under `src/test/java/` in the same package as your
implementation:

```java
package com.percussion.extensions.general;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PSSimpleJavaUdf_truncateTest {

    private final PSSimpleJavaUdf_truncate udf = new PSSimpleJavaUdf_truncate();

    @Test
    void truncatesLongString() throws Exception {
        Object result = udf.processUdf(new Object[]{"Hello World", "5"}, null);
        assertEquals("Hello", result);
    }

    @Test
    void returnsFullStringWhenShorterThanMax() throws Exception {
        Object result = udf.processUdf(new Object[]{"Hi", "10"}, null);
        assertEquals("Hi", result);
    }
}
```

## Step 5: Build and verify

```bash
# Build and run tests
cd modules/extensions-main
../../mvn-env.sh clean install

# Hot-deploy to a local installation
cd /path/to/percussioncms
./scripts/hot-deploy-local.py \
    --install-dir /path/to/cms-install \
    --modules system \
    --restart
```

After restart, check `server.log` for:

```
INFO  [com.percussion.extension.PSExtensionManager] Initialization successful.
```

If the extension is not found at runtime, verify that:

1. The `name` attribute FQN in `Extensions.xml` exactly matches how callers
   construct the `PSExtensionRef` (handler / context / name).
2. The class is in the JAR and the fully qualified `className` is correct.
3. The interface declared in `Extensions.xml` matches what the class actually
   implements.

