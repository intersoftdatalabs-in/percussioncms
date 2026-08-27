# Erlang review: issue #3861 system/webservices leftover IPS*Errors typed ErrorCodes

- **Branch:** `fix/issue-3861-ws-typed-errorcodes`
- **Base:** `origin/main`
- **Date:** 2026-08-26
- **Reviewer:** Erlang (independent of implementer)
- **Recommendation:** approve
- **Gate:** May commit/push: yes
- **Memory patterns hit:** missing behavioral tests; incomplete change-class closure; non-portable path joins; tests that only grep source strings

## Summary

Converts remaining origin/main allow-list production `IPS*Errors` sites under `system/webservices` to typed `*ErrorCodes`. Dump-residuals showed four live call-sites (`PSRemoteWsRequester`, `PSWebServiceAgent`, `PSContentWs`, `PSSystemWs`); the other sixteen listed SOAP/ws files were already on `WebserviceErrorCodes` and are only dropped from the allow-list. WS 1–27 catalogs are not flattened. Tests exercise production exception types (`PSCmsException`, `PSException`, `PSServerException`, `PSUserNotMemberOfCommunityException`) and skip dual-write where `isAuditable() == false`.

## Issues

None blocking.

### Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] HttpServer binds loopback + ephemeral port (`InetAddress.getLoopbackAddress()`, port 0)
- [x] Tests do not assert Unix-only absolute path shapes
- [x] Temp files / OS temp hardcodes not used
- [x] Line-ending sensitive assertions not used

## Notes (non-blocking)

- `PSContentWs.createRelatedItem` remains private; the missing-handler path is asserted via the same `PSException(ServerErrorCodes.CE_NEEDED_APP_NOT_RUNNING, resource)` type production throws.
- `PSSystemWs.switchCommunity` still matches legacy int `PSServerException` from `PSServer.verifyCommunity` via `numericCode()`, and also typed codes when present.
- `modules/webservices` (WSDL/DTO packaging) is untouched; Java lives in `system/webservices` compiled by `perc-system`.
