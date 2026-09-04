# Erlang review — #4271 segmentation-rx ExtensionErrorCodes (orphan build)

## Scope
- modules/segmentation-api (standalone dep repair + JUnit5 assertion/jmock test repair)
- modules/segmentation-rx (IPSExtensionErrors → ExtensionErrorCodes; orphan dep repair; dual-write skip tests)
- scripts/ipserrors-residual-allowlist.txt + gate pytest

## Findings
- No bugs in typed ErrorCodes conversion (EXT_INIT_FAILED parity + isAuditable==false).
- Paths: no new filesystem path construction; portable.
- Companions: allowlist shrink, gate pytest, slice unit test, standalone clean install both modules.
- Pre-existing orphan test debt repaired only as needed for clean install (jmock Mockery per-test, JUnit5 asserts, XMLUnit/DOM workarounds).

## Verdict
PASS — ready to commit/PR.

