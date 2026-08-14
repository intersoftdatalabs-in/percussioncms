# Erlang review — #3391 PUT /services/acls/bulk ArrayList→AclList ClassCast

**Scope:** uncommitted branch `fix/issue-3391-acl-bulk-arraylist-cast` vs `origin/main`.
**Reviewer persona:** Erlang (independent of implementer).
**Memory patterns hit:** CXF ArrayList→typed list ClassCast; Jackson WRAP/UNWRAP_ROOT_VALUE; change-class companions (reader + deserializer + CXF pipeline test + product-docs); dual-ship beans.xml vs rest jar; behavioral tests for new deserialize path.

## Summary

Cycle Verify residual of #3387 / #3378: Playwright Display Format Object ACL Save still got HTTP 400 `ClassCastException: Cannot cast java.util.ArrayList to com.percussion.rest.acls.AclList`. `AclListJsonReader` exists and is listed ahead of `jacksonProvider`, but the live CXF pipeline still selected Jackson. `JacksonJsonProvider` constructs a collection `JavaType` whose impl is raw `ArrayList`; CXF then casts to `AclList`.

This change:

1. Adds `AclListDeserializer` (`@JsonDeserialize(using=…)`) so Jackson itself instantiates `AclList` (hot-deploy of `rest` jar is enough even if filesystem `sitemanage-beans.xml` is stale).
2. Hardens `AclListJsonReader` media-type matching (`application/json`, `text/json`, `+json`, charset).
3. Binds PUT `/acls/bulk` as `String` then `AclListJsonReader.parse` so CXF Jettison cannot reject a bare array (`JSONObject` ParseError) and Jackson cannot ClassCast.
4. `ApiUtils.convertGuid` builds `PSGuid` from `type`/`uuid`/`hostId` when `stringValue` is blank (`raw may not be blank`).
5. Adds CXF/JAX-RS pipeline tests: `JavaType` read, `JacksonJsonProvider.readFrom`, `ServerProviderFactory` selection, in-process PUT envelope + bare array.
6. Keeps persist/GET-after-save coverage from #3387.
7. Updates `product-docs/8.2/developer/rest.md`.

H2 QA Playwright `developer-object-acl-display-format-save.spec.js` passed (1) after hot-deploy.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No new bugs. Deserializer delegates to `AclListJsonReader.parseNode`. No filesystem path construction. C2: `AclList` not made `final`/`sealed`; no signature change; no `extends AclList` / anonymous subclasses. Standalone `mvnw clean install`: rest Tests run: 396, Failures: 0; sitemanage BUILD SUCCESS.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] New code is JSON/JAX-RS only (URL paths use `/` correctly)
- [x] Tests do not assert OS-specific file paths
- [x] Playwright spec already uses `TEST_CMS_URL` / `BASE_URL` (no hardcoded `:9993`)

## Issues

None (hard-gate).

## Product documentation

Updated `product-docs/8.2/developer/rest.md` (server binds `{"AclList":[…]}` as `AclList`, not raw `ArrayList`).
