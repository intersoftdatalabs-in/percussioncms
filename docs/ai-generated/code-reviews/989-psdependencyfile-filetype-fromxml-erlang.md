# Erlang review — PSDependencyFile.fromXml fileType restore

| Field | Value |
|-------|--------|
| **Date** | 2026-07-17 |
| **Branch** | `989-react-cui-widget-builder` |
| **Scope** | Uncommitted local changes only (vs `HEAD`) |
| **Intent** | Restore `fileType` deserialization dropped in Java modernization; diagnose package-install well-formed / wrong-type / “missing” item-def failures; improve parse-error identity |
| **Recommendation** | **approve** |
| **Gate** | **May commit/push: yes** |
| **Memory patterns hit** | Missing behavioral tests (avoided); path portability (clean for this pack) |

## Summary

Regression fix restores required `fileType` attribute handling in `PSDependencyFile.fromXml`. Without it, `m_type` defaults to `0` (`TYPE_APPLICATION_XML`), so archive install treats DTD/PDT companions, schemas, support files, etc. as application XML — matching observed server.log symptoms (`well-formed`, `Wrong dependency file type "APPLICATION_XML"`, `could not locate the item definition file`).

Behavioral coverage is strong: parameterized round-trip of every `TYPE_ENUM` entry, explicit APPLICATION_FILE regression, missing/unknown type and empty archive path rejection. Diagnostic overload on `createXmlDocument` is small and low risk.

No hard-gate bugs found. Cross-platform path checklist: clean for this change (uses `File` + existing `getNormalizedPath`; tests avoid OS-absolute path shapes).

## Files reviewed

| Path | Role |
|------|------|
| `deployer/.../PSDependencyFile.java` | `fromXml` fileType + archive/original validation |
| `deployer/.../PSDependencyHandler.java` | `createXmlDocument(in, sourceIdentity)` |
| `deployer/.../PSApplicationDependencyHandler.java` | pass archive path into createXmlDocument |
| `deployer/.../PSDependencyFileTest.java` | new unit tests (21 cases) |

## Verification (author)

- `./mvn-env.sh -pl deployer -Dtest=PSDependencyFileTest -Dai.integrity.skip=true test` → 21 tests, 0 failures, BUILD SUCCESS

## Issues

### Bugs

_None._

### Suggestions

1. **Pre-existing: `copyFrom` omits `m_originalFile`**  
   `PSDependencyFile.copyFrom` still copies only type/file/archiveLocation. Not introduced here; not required for package install path. Optional follow-up if copyFrom is used for full clones.

2. **Optional test for diagnostic overload**  
   `createXmlDocument(..., sourceIdentity)` has no direct unit test. Acceptable for a message-append helper; only worth a tiny test if deployer test harness already mocks streams easily.

### Nits

1. `testApplicationFileNotDefaultedToApplicationXml` — `assertTrue(type != APPLICATION_XML)` is redundant after `assertEquals(APPLICATION_FILE, …)`.

2. `createXmlDocument` still uses empty `catch` on `in.close()` (pre-existing).

## Cross-platform path checklist

- [x] No new `"/" +` / `"\\" +` filesystem joins in production code  
- [x] Archive path still normalized at ZIP open via existing `getNormalizedArchivePath`  
- [x] Tests use relative `File` / child segments; no Unix-only absolute roots  
- [x] Original-file restore uses `getNormalizedPath`; test normalizes separators before compare  

## Gate

| | |
|--|--|
| **Recommendation** | approve |
| **May commit/push** | **yes** |
| **Blockers** | none |

## Handoff

Safe to commit this pack and proceed with full rebuild / redeploy of deployer into the install. Expect most type-mismatch package failures to clear on reinstall; residual true packaging gaps may remain and should be tallied after rebuild.
