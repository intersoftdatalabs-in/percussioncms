# Erlang review — issue #4143 (parent #2616 leftover)

**Branch:** `fix/issue-4143-job-error-codes`  
**Scope:** uncommitted `system` job-handler retype vs `origin/main`  
**Recommendation:** approve  
**Gate:** pass  
**May commit/push:** yes  
**Memory patterns hit:** leftover `IPS*Errors` int throw sites → typed `*ErrorCodes`; dual-write skip via `isAuditable()==false`; keep numeric bridge interface.

## Summary

Production `PSJobException` throws in `com.percussion.server.job` (`PSJobHandler`, `PSJobHandlerConfiguration`, `PSJobRunnerFactory`) now use `JobErrorCodes` / typed `PSJobException` constructors. `IPSJobErrors` remains as the int bridge. Behavioral tests cover missing job definition, factory class-not-found, missing runJob params, malformed/unknown job id, already-running lock, and catalog dual-write skip (`DefaultAuditLogService.log` → `SKIPPED`, empty repository). Numeric codes match `IPSJobErrors`. No public signature change; no path I/O.

## Cross-platform path checklist

N/A — no filesystem path construction. Config XML is an in-memory string.

## Issues

None.

## Tests / build

- `cd system && ../mvnw.cmd "-Dtest=PSJobHandlerTypedErrorCodeSliceTest" test` — Tests run: 7, Failures: 0
- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 2696, Failures: 0, Errors: 0, Skipped: 245
