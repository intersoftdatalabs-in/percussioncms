# Issue #1994 — PSMimeContentAdapter Jackson wire deviations

Parent: #1920 · Grandparent: #1892 / #1823 · Epic: #505

## Scope delivered

|         Class          |      Root element      |                                  Nested / notes                                  |
|------------------------|------------------------|----------------------------------------------------------------------------------|
| `PSMimeContentAdapter` | `mime-content-adapter` | Inline base64 `content`; scalar `attachment-id` / encodings / guid / mime / name |

Golden fixtures + round-trip + legacy `<null>` root tests live in
`system/src/test/java/com/percussion/services/system/data/PSSystemDataXmlSerializationTest`
(alongside #1920 / #1993 system data coverage). Existing interface tests remain in
`PSMimeContentAdapterTest`.

## Approved / intentional deviations vs historical Betwixt

|                      Deviation                       |                                                                       Notes                                                                       |
|------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| No Betwixt graph-identity `id="…"` attributes        | Same as #1887 facade / other domain batches.                                                                                                      |
| No XML declaration                                   | `PSJacksonXmlSerializationHelper` default.                                                                                                        |
| `InputStream` content is **not** a wire type         | API keeps `getContent()` / `setContent(InputStream)`; design XML uses base64 string via `getContentBase64` / `setContentBase64`.                  |
| Wire element remains `content`                       | Matches historical property name; value is standard Base64 (no MIME line wrapping), same intent as SOAP `PSMimeContentAdapterConverter`.          |
| Content buffered as `byte[]`                         | `setContent` reads the stream into a buffer; `getContent` always returns a fresh `ByteArrayInputStream`. Avoids BeanUtils/toXML stream EOF races. |
| `setContent(null)` clears (null-safe)                | BeanUtils may copy null `content` before `attachment-id` during `fromXML`; same pattern as `PSSharedProperty#setVersion(null)`.                   |
| Attachment mode omits inline `content`               | When `attachment-id >= 0`, `content` serializes as null / omitted; buffer cleared by `setAttachmentId`.                                           |
| Derived / alias properties suppressed                | `label` (name alias), `content-attached` (href ≥ 0), `description` (always null API) are `@JsonIgnore`.                                           |
| `guid` is `CONFIGURATION` type only                  | Existing `setGUID` validation unchanged; wire form is `PSGuid` string (e.g. `0-34-100`).                                                          |
| `transfer-encoding` field not auto-cleared on attach | Historical bean field retained as-is; javadoc claimed null when attached but code never enforced that.                                            |

## Residual

Not in this PR:

1. Betwixt POM removal (#1824)
2. Types already done in #1920 / #1993 (`PSAudit*`, `PSSharedProperty`, `PSHierarchyNode*`, `PSDependency` / `PSDependent`)
3. SOAP / Axis-generated `com.percussion.webservices.system.PSMimeContentAdapter` (separate converter; not design-object XML)

## Out of scope

- filter (#1915), sitemgr (#1918), publisher/pubserver (#1919)
- content leftovers (#1921)
- Live CMS configuration file I/O (`PSSystemService.loadConfiguration` / `saveConfiguration`)

## Tests

|               Suite                |                                    Coverage                                    |
|------------------------------------|--------------------------------------------------------------------------------|
| `PSSystemDataXmlSerializationTest` | mime-content-adapter golden + inline RT + attachment RT + legacy null root     |
| `PSMimeContentAdapterTest`         | Existing programming-interface coverage (defaults, setters, attachment toggle) |

