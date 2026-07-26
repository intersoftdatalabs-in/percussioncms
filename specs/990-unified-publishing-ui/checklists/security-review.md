# Security review notes (US polish / T113)

**Feature**: `990-unified-publishing-ui`  
**Date**: 2026-07-19

|        Check        |                                       Result                                       |
|---------------------|------------------------------------------------------------------------------------|
| CSRF on mutations   | Ops/design/runtime clients use `api/client.ts` (CSRFGuard header)                  |
| Secrets in logs     | `serverSecrets.redactSecretsForLog`; password fields type=password; FR-016 tests   |
| Query XSS           | `publishModern.jsp` allowlists section; id params charset-restricted; `mapIdParam` |
| Design façade AuthZ | Relies on session + existing design/admin gates on services; additive REST only    |
| AuthZ errors        | Publish maps FORBIDDEN/403 to user messaging                                       |
| No Spring Boot      | Confirmed Track B React + existing sitemanage JAX-RS                               |

Cross-platform (T114): no new filesystem path string construction in Java façade; GUID/service only.
