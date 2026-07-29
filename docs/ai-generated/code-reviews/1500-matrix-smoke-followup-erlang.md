# Erlang review — `1500-matrix-smoke-followup`

| Field | Value |
|-------|--------|
| **Date (UTC)** | 2026-07-26 |
| **Reviewer** | Erlang (strict independent pre-PR) |
| **Branch** | `1500-matrix-smoke-followup` |
| **Base** | `origin/development` |
| **Commits** | `7c85dd5c0b` (scheduler H2/PG), `376504d470` (H2 path / empty pwd / ems bean) |
| **Scope** | Branch `origin/development...HEAD` (no uncommitted dirty tree at review time) |
| **gh** | Available; no open PR for this head yet |
| **Prior report** | None under `docs/ai-generated/code-reviews/` for this slug |
| **Memory patterns hit** | Hard gate: missing behavioral tests; Installer/Ant relative path CWD; passwords in config encrypt/decrypt; Path/Files preference |

## Summary

Follow-up for #548 / #1500 matrix CMS+H2 smoke. Two commits fix:

1. Quartz `PSSchedulerBean` mapping for H2 (including `h2:file`) and PostgreSQL.
2. Install-time H2 `file:` path resolution against install root (not Ant temp `user.dir`), Jetty empty-password encrypt skip, and removal of undefined `emsAPIService` Spring ref.

Path resolution and scheduler mapping have solid unit tests. Matrix smoke was observed green (HTTP 200 / RESULT:OK) with a rebuilt distribution jar — strong runtime evidence.

**Hard gate failure:** the empty-password branch in `JettyDatasourceConfigurationAdapter` (the change that fixed Hikari “Wrong user name or password”) has **no behavioral unit assertion**. Existing `JettyDatasourceConfigurationAdapterTest` only load/saves without checking `pwd` / `pwd.encrypted`. That is a missing-test **bug** under Erlang rules.

## Recommendation

**`approve`** (after re-review — see ## Re-review)

**May commit/push / open PR: yes** (post re-review fix pack).

## Gate

| Check | Result |
|-------|--------|
| Correctness bugs in logic | No open correctness bugs found in path/scheduler resolution (smoke + unit tests support) |
| Missing behavioral tests | **Pass** after re-review (empty Jetty DS password unit tests) |
| Cross-platform path I/O | **Pass** (see below) |
| Security / secrets | **Pass** — empty password left unencrypted; no secrets committed |

## Cross-platform path review

Touched: `PSJdbcUtils.resolveEmbeddedFileServer`, `InstallUtil.ensureH2RepositoryParent`, path unit tests with `@TempDir`.

- Uses `java.nio.file.Path` / `Files.createDirectories` — good.
- JDBC `file:` fragments use `/` after normalize (`replace('\\','/')`) — correct for H2 URLs (not OS path join of filesystem segments for the product relative form).
- Tests build expected paths via `Path.resolve` then normalize separators for comparison — portable.
- No hardcoded `/tmp` / `C:\` in production code.
- **Note (non-blocking):** `Path.isAbsolute()` is OS-dependent. A Windows drive-letter absolute fragment evaluated on Linux install would not be treated as absolute. Product default is relative `file:../../Repository/CMDB`; absolute paths are produced by this resolver on the install host. Acceptable.

## Issues

### bug — Missing behavioral test for empty Jetty datasource password save

- **Status:** **Fixed** (re-review 2026-07-26)
- **Where:** `modules/utils/src/main/java/com/percussion/utils/container/adapters/JettyDatasourceConfigurationAdapter.java` (empty `plainPwd` → `pwd=` + `pwd.encrypted=N`); test gap in `modules/utils/src/test/java/com/percussion/utils/container/adapters/JettyDatasourceConfigurationAdapterTest.java`
- **Why it blocked:** New/changed non-trivial logic that already failed matrix smoke in the field. Without a unit test, a future encrypt refactor can re-break H2 default install silently.
- **Mitigation:** `save_emptyPassword_writesUnencryptedEmpty` + `save_nonEmptyPassword_encryptsWhenPossible` in `JettyDatasourceConfigurationAdapterTest` (TempDir + Path/Files). Surefire: 3 tests, 0 failures.

### suggestion — InstallUtil H2 branch has no dedicated unit test

- **Where:** `modules/utils/src/main/java/com/percussion/install/InstallUtil.java` (`createConnection` H2 resolve + `ensureH2RepositoryParent`)
- **Why:** Core resolution is covered by `PSJdbcUtilsTest`; glue is thin. Optional: package-visible helper or test that `m_rootDir` + relative server rewrites URL (may need careful static state cleanup for `m_rootDir`).
- **Not a hard gate** given pure-function tests + live smoke.

### suggestion — `emsAPIService` removal is product-facing Spring config

- **Where:** `projects/sitemanage/.../sitemanage-beans.xml`
- **Why:** Correct (bean never defined; blocked ROOT start). Confirm no out-of-tree custom EMS module expected to inject that name. Comment in XML is adequate for in-tree.
- **Not a hard gate.**

### nit — Map.of driver map size

- **Where:** `PSSchedulerBean.DRIVER_DELEGATES` — 9 entries (under `Map.of` limit of 10). Fine today; next driver should switch to `Map.ofEntries` / `Map.copyOf`.

## What looks solid

| Area | Evidence |
|------|----------|
| H2 relative → install-root absolute | `PSJdbcUtils.resolveEmbeddedFileServer` + TempDir tests (relative product default, absolute normalize, nulls/non-file) |
| Scheduler `h2:file` / postgresql | `PSSchedulerBeanDriverDelegateTest` behavioral cases |
| Driver URL map `h2:file` | `PSJdbcUtilsTest.testH2JdbcUrlAndDriverMap` |
| Runtime smoke | `docker/logs/matrix-results-20260726-080125.json` — cms-h2 **pass**, HTTP 200 |
| Full reactor | Earlier session: `clean install -DskipTests` BUILD SUCCESS |

## Required before push / PR

1. ~~Add behavioral unit test for empty (and ideally non-empty) Jetty DS password save flags.~~ **Done**
2. Run `cd modules/utils && ../../mvnw clean install` (tests on) before PR evidence.
3. ~~Erlang re-review~~ — **approve** (this section).
4. Push + open PR; cite smoke JSON + module clean install in PR body.

## Re-review (2026-07-26)

| Item | Status |
|------|--------|
| Empty Jetty pwd unit tests | **Fixed** — `JettyDatasourceConfigurationAdapterTest` (3 run / 0 fail) |
| Cross-platform path I/O | Unchanged pass |
| CMS H2 matrix smoke | Still green evidence (HTTP 200) — not re-run this re-review |
| CMS PostgreSQL matrix smoke | **Not run** on this branch session — no pass evidence |

**Recommendation:** `approve`  
**May commit/push: yes**

## Memory touch (optional)

Reinforced existing pattern: *Installer/config encrypt of empty credentials can produce non-empty ciphertext that fails decrypt under a different `rxdeploydir`/secure-dir — prefer not encrypting empty secrets and unit-test the write contract.*

---

*End of report. Gate green after re-review.*
