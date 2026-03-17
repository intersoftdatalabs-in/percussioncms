# Extension Point Interfaces

Extensions implement one or more plugin interfaces defined in `perc-system`.
The table below shows all interfaces used by built-in extensions registered in
`Java/Extensions.xml`.

## Interface reference

|          Interface           | Count |                   Package                    |                                        Purpose                                        |
|------------------------------|-------|----------------------------------------------|---------------------------------------------------------------------------------------|
| `IPSResultDocumentProcessor` | 92    | `com.percussion.extension`                   | Post-processes the XML result document returned to a client after a resource executes |
| `IPSUdfProcessor`            | 77    | `com.percussion.extension`                   | User-defined function callable from XSL stylesheets or Velocity templates             |
| `IPSRequestPreProcessor`     | 55    | `com.percussion.extension`                   | Pre-processes an incoming request before it hits a resource                           |
| `IPSJexlExpression`          | 20    | `com.percussion.extension`                   | JEXL-callable expression used in Velocity assembly templates                          |
| `IPSFieldInputTransformer`   | 20    | `com.percussion.extension`                   | Transforms a content-editor field value on input                                      |
| `IPSEditionTask`             | 11    | `com.percussion.rx.publisher`                | Runs before or after a publish edition                                                |
| `IPSItemInputTransformer`    | 11    | `com.percussion.extension`                   | Transforms item-level data on input                                                   |
| `IPSEffect`                  | 10    | `com.percussion.relationship`                | Relationship effect that fires on create, modify, or delete                           |
| `IPSWorkflowAction`          | 9     | `com.percussion.extension`                   | Fires on a workflow transition                                                        |
| `IPSAssembler`               | 8     | `com.percussion.services.assembly`           | Content assembler                                                                     |
| `IPSFieldValidator`          | 8     | `com.percussion.extension`                   | Validates a single content-editor field value                                         |
| `IPSItemFilterRule`          | 7     | `com.percussion.services.filter`             | Item filter rule for visibility or publish eligibility                                |
| `IPSItemValidator`           | 6     | `com.percussion.extension`                   | Validates an entire content item                                                      |
| `IPSFieldOutputTransformer`  | 6     | `com.percussion.extension`                   | Transforms a field value on output                                                    |
| `IPSAssemblyLocation`        | 6     | `com.percussion.extension`                   | Computes the assembly (publication) URL for an item                                   |
| `IPSTask`                    | 5     | `com.percussion.services.schedule`           | Scheduled task                                                                        |
| `IPSSlotContentFinder`       | 5     | `com.percussion.services.assembly`           | Finds related items to populate a slot                                                |
| `IPSContentListGenerator`    | 4     | `com.percussion.services.publisher`          | Generates the list of items for a publish edition                                     |
| `IPSTemplateExpander`        | 3     | `com.percussion.services.publisher`          | Expands a content list entry into individual publishing items                         |
| `IPSSearchResultsProcessor`  | 3     | `com.percussion.search`                      | Post-processes full-text search results                                               |
| `IPSLuceneTextConverter`     | 2     | `com.percussion.search.lucene.textconverter` | Converts binary content to plain text for Lucene indexing                             |
| `IPSFieldEditabilityRule`    | 2     | `com.percussion.extension`                   | Determines whether a content-editor field is editable                                 |
| `IPSFieldVisibilityRule`     | 1     | `com.percussion.extension`                   | Determines whether a content-editor field is visible                                  |
| `IPSPasswordFilter`          | 1     | `com.percussion.security`                    | Hashes or validates user passwords                                                    |

## Base classes

All Java extensions should extend `PSDefaultExtension` (or a more specific
base from `perc-system`). This base class handles lifecycle management
(`init()`, `canModifyStyleSheet()`), provides access to init parameters, and
implements the `IPSExtension` marker interface.

```java
import com.percussion.extension.PSDefaultExtension;

public class PSMyExtension extends PSDefaultExtension
    implements IPSResultDocumentProcessor {

    @Override
    public boolean canModifyStyleSheet() {
        return false;
    }

    @Override
    public Document processResultDocument(
            Object[] params,
            IPSRequestContext request,
            Document resultDoc)
        throws PSParameterMismatchException, PSExtensionProcessingException {
        // implementation
        return resultDoc;
    }
}
```

## Extension init parameters

Every extension can declare typed init parameters via `PSXExtensionParamDef`
elements in `Extensions.xml`. These are passed into the extension at
`init()` time via the `IPSExtensionDef`. Common system-defined parameters:

|            Parameter key             |                    Description                    |
|--------------------------------------|---------------------------------------------------|
| `com.percussion.user.description`    | Human-readable description shown in the Workbench |
| `com.percussion.extension.version`   | Extension schema version (always `1`)             |
| `className`                          | Fully qualified implementation class name         |
| `com.percussion.extension.reentrant` | `yes` if the extension is thread-safe             |

