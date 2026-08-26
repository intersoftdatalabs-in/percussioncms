# Erlang review — #3783 Developer Content Type template associations chrome (CD-12)

**Date:** 2026-08-26  
**Branch:** `feat/issue-3783-ct-template-assoc-chrome`  
**Scope:** uncommitted vs `HEAD` (WebUI SPA, rest DTO/resource, sitemanage adaptor, Vitest, Playwright, product-docs)  
**Memory patterns hit:** change-class closure (Vitest + Playwright for WebUI screen; rest DTO XmlSeeAlso peer SiteList #3090); dedicated REST consumed with live-wire fixes; no path I/O.

## Summary

Developer Content Type detail replaces allowed templates via dedicated
`PUT /services/contenttypes/{idOrName}/allowedTemplates` after a held design
lock, then `GET` lists the new set. Bulk `PUT /contenttypes/{id}` no longer
sends `allowedTemplates` and no longer wraps lock→save→unlock (held lock is
required; save must not steal/release). Unlocked editors stay disabled; 409
clears the UI lock without steal.

Live H2 required two wire/lock companions so the chrome could consume REST:
`NamedObjectRefList` JAXB `@XmlSeeAlso` + distinct root name (peer SiteList #3090);
PUT body typed as `List<NamedObjectRef>` so CXF does not cast `ArrayList`;
`replaceAllowedTemplates` uses `resolveExistingContentTypeGuid` so lock and PUT
share the packed NODEDEF id.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Issues

None blocking.

### Companions (checked)

| Artifact | Present |
|----------|---------|
| WebUI API wrap/unwrap for `NamedObjectRefList` | yes |
| ContentTypeDetailPanel lock → PUT → GET | yes |
| Vitest API + panel + DeveloperShell | yes |
| rest `NamedObjectRefList` XmlSeeAlso + List PUT param + serial test | yes |
| sitemanage lock GUID alignment + adaptor tests | yes |
| Playwright surface spec | `developer-content-type-template-associations.spec.js` |
| product-docs `8.2/admin/developer-content-types.md` + REST CD-12 | yes |
| Out of scope: workflow chrome (#3782), enable/disable (#3781) | not shipped |

### Notes (non-blocking)

- Whole-object PUT still exists for label/description/fields/workflows; templates use the CD-12 sub-resource only.
- Jackson WRAP_ROOT uses class name `NamedObjectRefList` (XmlRootElement ignored without JAXB introspector) — client wrap matches.
- Cross-platform path checklist: N/A (REST URL `/` only; no filesystem joins).
