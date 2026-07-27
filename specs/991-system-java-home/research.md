# Research: System / Configurable Java Home (991)

**Issue**: https://github.com/intersoftdatalabs-in/percussioncms/issues/1340  
**Spec**: [spec.md](./spec.md)

## R1 — Current problem is operator-provided InstallDir/JRE, not a shipped JRE

**Decision**: Frame all design as removing the **mandatory post-install copy/symlink** of a JRE into `<InstallDir>/JRE` (or `JRE64`). The distribution does **not** ship a JRE today.

**Rationale**: Product scripts hard-code or prefer `${rxDir}/JRE` (e.g. `StartJetty.sh` line 9: `JAVA_HOME=${rxDir}/JRE`; Windows bat equivalents; DTS `TomcatStartup.sh` looks for `JRE/bin/java` under install). Operators must place a real runtime there manually. Spec correction (2026-07-19) makes this the problem statement.

**Alternatives considered**:
- Treat as “stop bundling JRE in archive” — incorrect; not what the product does today.
- Require symlink forever as the only supported model — fails SC-001 and FR-001.

## R2 — Existing durable config: `java.properties` (partial, underused)

**Decision**: Use install-root **`java.properties`** as the **primary durable** product configuration for resolved Java, standardizing keys and writing it at install time. Align shell/bat consumers with properties already referenced by:
- `system/release/installer/Linux/install-service.sh` (`JAVA=` from `java.properties`)
- `modules/perc-service-wrapper/.../JettyStartUtils.java` (loads root `java.properties`)

**Canonical keys** (plan contract):
- `JAVA_HOME=` absolute path to JRE/JDK home  
- `JAVA=` absolute path to `java` / `java.exe` launcher (optional but recommended for consumers that only need the binary)

**Rationale**: Already a product convention; post-install re-point is “edit file + restart”; no new invent-a-format surface. Preinstall already understands `perc.java.home` system property for the **installer JVM**, which can seed the file for the **product** runtime.

**Alternatives considered**:
- Only `/etc/default/<service>` — Linux-service-only; console `StartJetty` would still hard-code JRE.
- Only environment `JAVA_HOME` — not durable across reboots/service accounts without docs discipline.
- Symlink install-dir JRE as the “config” — keeps the bad mental model.

## R3 — Single resolution order (runtime)

**Decision**: All primary CMS/DTS start, stop, and service install paths implement this order:

1. **Persisted product config** — `java.properties` under install root (`JAVA_HOME` and/or `JAVA`) if path exists and is version-compatible
2. **Process environment `JAVA_HOME`** — if set, exists, and major version is 21
3. **Legacy install-dir layout** — `<InstallDir>/JRE` then `<InstallDir>/JRE64` if valid Java home (operator copy/symlink only)
4. **`java` on `PATH`** — resolve home via known layout / `java -XshowSettings:properties -version` (or `dirname` of realpath to binary) when major version is 21
5. **Fail** with message listing sources tried and **required major version 21**

**Rationale**: Matches FR-003 and keeps existing manual layouts working as fallback (US6) without requiring them for new installs (US1–US2).

**Alternatives considered**:
- Env before config — breaks “install chose Java X” when admin shell has a different `JAVA_HOME`.
- Install-dir before env — perpetuates manual layout as preferred.
- No PATH discovery — worse UX when only `java` is on PATH in containers.

## R4 — Shared implementation strategy (shell + bat)

**Decision**:
- Document the resolution algorithm in `contracts/java-home-resolution.md`.
- Provide **platform-native helpers** with identical precedence:
- Unix: e.g. `resolve-java-home.sh` (sourced by Jetty/DTS scripts) under a shared location per product root (CMS: next to Jetty scripts or `rxconfig/Installer/`; DTS: DTS rootFiles).
- Windows: e.g. `resolve-java-home.bat` called with `call` / `set` of `JAVA_HOME`.
- Optionally a **Java unit-testable** pure function for version parsing / precedence (in `perc-distribution-tree` preinstall helpers or a small test-focused class) to avoid untested bash-only logic for version checks.

**Rationale**: Cross-platform mandate (AGENTS); dual script forms are unavoidable for ops; shared contract + tests prevent drift. Full “generate bat from sh” generators are higher risk for this release.

**Alternatives considered**:
- Single Java wrapper for all starts — larger change to Jetty/Tomcat launch model.
- Only document and hope each script is updated consistently — high drift risk.

## R5 — Version validation (major 21)

**Decision**: Require major version **21**. Validation methods:
- Preferred: run `"$JAVA" -version` / `java -XshowSettings:properties -version` and parse `java.version` / version string for major 21.
- Reject 8, 11, 17, etc. with explicit error text mentioning 21.

**Rationale**: Spec and 8.2 line; issue #1340. Existing installer messaging still says “1.8” in places (`install-service.sh`) — those messages must be updated as part of this feature.

**Alternatives considered**:
- Accept any LTS ≥ 21 — out of scope (spec assumes 21 only).
- Trust folder names only — unsafe.

## R6 — Interactive multi-candidate discovery

**Decision**: Extend **preinstall / interactive installer** (CMS: `modules/perc-distribution-tree/.../preinstall/Main.java`; DTS: `MainDTSPreInstall` where applicable) to:
1. Collect candidates from: current process java home, `JAVA_HOME` env, common OS locations (Linux: `/usr/lib/jvm/*`; Windows: registry + common Program Files Java paths when practical; PATH `java`).
2. Filter to major 21 + executable launcher.
3. If 0 → fail with guidance.  
If 1 → auto-select + log.  
If >1 → prompt with path + version; write selection to `java.properties`.

Unattended: `-Dperc.java.home=...` (already known) and/or response-file / env mapped to the same writer; validate before write.

**Rationale**: Preinstall already resolves `perc.java.home` / `java.home` for running the installer itself; product needs the **persisted** outcome for post-install scripts.

**Alternatives considered**:
- Prompt only in shell start scripts — too late; service install already happened wrong.
- External OS package only — not portable to Windows corporate images.

## R7 — Surfaces inventory (evidence)

**Decision**: Treat the following as **in-scope primary** consumers of the resolver:

|           Area           |                                                                   Evidence                                                                   |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| CMS Jetty console        | `modules/perc-jetty/.../StartJetty.sh`, `StartJetty.bat`, `StopJetty.bat` — hard-code `%rxDir%\JRE` / `${rxDir}/JRE`                         |
| CMS Jetty service        | `service/install-jetty-service.sh` writes `JAVA_HOME=${rxDir}/JRE` or `JRE64` into `/etc/default/`; `.bat` normalizes `..\JRE` for Procrun   |
| DTS console              | `TomcatStartup.sh/.bat`, `TomcatShutdown.sh/.bat` — prefer `JRE` under install                                                               |
| DTS services             | `DTSProductionService` / `DTSStagingService` `.sh`/`.bat` — install-root JRE heuristics                                                      |
| Preinstall               | `perc.java.home` / `java.home` for installer JVM only today                                                                                  |
| Install XML              | `install.xml` JRE backup/`lib/ext` filesets assume install-dir JRE may exist — **gate** so missing JRE is not fatal when config Java is used |
| Legacy installer helpers | `system/release/installer/**` still `./JRE` and outdated 1.8 messaging                                                                       |

**Secondary**: util scripts still calling `../JRE/bin/java` — update if still shipped; else document out of scope in tasks.

**Out of scope**: `./mvn-env.sh` / developer `JAVA_HOME_21` (build toolchain).

## R8 — Relationship to systemd feature (988)

**Decision**: When writing `/etc/default/<service>` and unit `EnvironmentFile`, set `JAVA_HOME` / `JAVA` from the **same resolution result** used for console start (prefer values from `java.properties` after install). Do not invent a second selection path for systemd-only.

**Rationale**: FR-011 + 988 FR-009; operators expect one configured Java.

## R9 — Testing approach

**Decision**:
- **Java unit tests**: version parse, precedence pure functions, property file read/write (portable `Path` APIs).
- **Script structural tests**: assert start scripts source/call resolver (or no longer hard-code only `JRE`); assert error strings mention `21` where replaced.
- **Smoke checklist** in quickstart.md for human UAT (no InstallDir/JRE present).

**Rationale**: Constitution test discipline; no root systemd required for resolver unit tests; Windows+Linux path portability in Java tests.

## R10 — install.xml / upgrade

**Decision**: Soft-gate Ant tasks that assume `${install.dir}/JRE` exists (backup, `lib/ext` copy). If JRE dir absent and `java.properties` valid, skip JRE-specific copy/backup with log; do not fail the whole install. Keep backup path when operator still has manual JRE layout.

**Rationale**: Upgrade must not break when customers stop creating InstallDir/JRE.

## Open items deferred to tasks (not blocking plan)

- Exact shared file names/locations under Jetty vs DTS trees (implementation detail).
- Whether Windows service Procrun must re-run install after re-point (document: re-install service or update service JavaHome).
- Whether DTS and CMS share one `java.properties` when co-located (default: each product root has its own file).

