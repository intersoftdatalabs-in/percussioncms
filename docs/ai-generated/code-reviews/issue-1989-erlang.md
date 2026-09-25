<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue 1989 (uncommitted)

## Scope

- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Status: mkd-code-review 0.1.18, pack percussion, `--gate advisory`, `--diff` of the uncommitted 1989 slice
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/1989
- Branch: `fix/issue-1989-linux-service-namespace-soak` (HEAD `01f7182965`, equals `origin/main`; the slice is uncommitted)
- Reviewer independence: Erlang re-review, no product-code edits this turn, no commit
- Files reviewed:
  - `scripts/linux-service-namespace-soak.sh` (untracked)
  - `scripts/linux-service-namespace-soak.bat` (untracked)
  - `modules/perc-jetty/src/test/java/com/percussion/jetty/service/LinuxServiceNamespaceSoakTest.java` (untracked)
  - `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/test/java/com/percussion/delivery/distribution/DtsLinuxServiceNamespaceSoakTest.java` (untracked)
  - `scripts/README.md`
  - `modules/perc-jetty/src/main/jetty/service/README-systemd.md`
  - `product-docs/8.2/getting-started/install.md`
- `--git-base origin/main` is empty here (HEAD has no commits beyond main) and would omit the four untracked files. Analysis used a unified diff with `a/` / `b/` prefixes so the machine pass saw all 7 files
- Memory: `~/.agents/skills/erlang/PATTERNS.md` and repo `erlang-review/patterns.md`
- Residual LLM: Ollama was up (`localhost:11434`, HTTP 200). `--models` `models.ollama-dev-coder.toml` was set. The markdown report has no `llm.error` and no extra model findings
- Prior report: `docs/ai-generated/code-reviews/issue-1989-erlang.md` (this file, previous BLOCK: missing behavioral test of host uid-map refuse)
- Author-reported Maven: `perc-jetty` standalone `clean install` — Tests run 70, Failures 0 (not re-executed this turn)

## This-diff behavior

Issue 1989 is the live Linux systemd + init.d soak leftover from #1978 / #962. This slice does **not** run `systemctl` on the host. It adds `scripts/linux-service-namespace-soak.sh`, which `unshare --user --map-root-user --mount`s, overlays tmpfs on `/etc`, `/run`, and `/var`, and puts fake `systemctl` / `chkconfig` / `service` on `PATH`. For `cms` and `dts` it runs the real installers through systemd install (unit present, `TimeoutStartSec=1800`, journal stdout, enable, no chkconfig, no `systemctl start`), uninstall with no leftover unit / init.d / `/etc/default` / wants-link / `S99`/`K99`, `--initd` only (no unit, chkconfig on), uninstall again, then init.d → uninstall → systemd reinstall.

After the outer `PERCUSSION_SOAK_NS` re-exec, the script reads `/proc/self/uid_map` and **exits 2** when the first line is `0 0 4294967295`, then requires mapped uid 0, **then** `mktemp` / copy of passwd / `mount -t tmpfs` on `/etc`, `/run`, and `/var`. Host identity map is refused before any mount.

`LinuxServiceNamespaceSoakTest.hostUidMap_refusesBeforeMount` sets `PERCUSSION_SOAK_NS=1`, invokes the soak with dummy dirs, and asserts exit 2 plus stdout/stderr containing `host uid map`. That is the rejection path. `cmsInstall_systemdInitdUninstallAndMigration` and `DtsLinuxServiceNamespaceSoakTest.productionDts_systemdInitdUninstallAndMigration` remain the mapped-root happy path (`@EnabledOnOs(OS.LINUX)`, `assumeTrue` on `unshare --help`, assert `result.txt` / unit / chkconfig log). Windows companion `linux-service-namespace-soak.bat` prints that the soak is Linux-only and exits 1 (documented in `scripts/README.md`). Product-docs `install.md` describes dual-ship install / `--initd` / uninstall / migration and points at the namespace check as **not** a host `journalctl` sign-off. CMS `README-systemd.md` matches. Init.d is not removed.

Change-class: Linux-only installer soak script + Maven tests in perc-jetty and delivery-tier-distribution + Windows `.bat` that exits 1 + `scripts/README.md` + product-docs + CMS systemd README. New Java files use the Intersoft 2026 Apache header. No agent rule files.

Cross-platform path review: the soak is Linux-only. `/etc`, `/run`, `/var`, and `/bin/java` live in that script and in `@EnabledOnOs(OS.LINUX)` tests. The `.bat` documents N/A and exits 1. Java fixtures use `Path` / `Files` / `Files.createTempDirectory`. That shape is accepted for this issue.

## Re-review of prior BLOCK

Prior Issue 1 (bug): `PERCUSSION_SOAK_NS=1` skipped the host-uid-0 refuse, so host root could mount tmpfs over real `/etc`. Product code was already fixed (`linux-service-namespace-soak.sh:29-33`). The remaining hole was no Surefire case that sets `PERCUSSION_SOAK_NS=1` and asserts exit 2 / `host uid map`. **Closed.** `LinuxServiceNamespaceSoakTest.hostUidMap_refusesBeforeMount` (lines 41–59) puts `PERCUSSION_SOAK_NS=1` on the `ProcessBuilder` environment, runs `bash scripts/linux-service-namespace-soak.sh cms …` against dummy dirs, merges stderr, and asserts exit 2 plus `out.contains("host uid map")`. That is a real process, not a source grep. Exit 2 alone would also match the later `id -u != 0` abort; the message locks the identity-map refuse. On this host `/proc/self/uid_map` is `0 0 4294967295` (uid 1000). Author reports perc-jetty Tests run 70, Failures 0.

Prior Issue 2 (suggestion): DTS test has the same `unshare --help` `assumeTrue` as CMS. Residual: `unshare --help` still does not prove `--user --map-root-user --mount` is allowed; missing binary still throws `IOException` instead of skip. **Open (suggestion).**

Prior Issue 3 (suggestion): DTS `README-systemd.md` still has no namespace-soak paragraph. **Open (suggestion).**

Prior Issue 4 (nit, later suggestion): stray `6. Open the Web UI…` under the Linux services subsection is gone. **Closed.** Empty `uid_map` from `awk … || true` still fail-opens to the mapped-root `id -u` check. **Open (suggestion).**

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 7 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Erlang interpretation

The machine pass is clean on the seven-file slice. The residual model added nothing. The install / `--initd` / uninstall / migration assertions in the soak script are behavioral. The two Maven happy-path tests run that script through `unshare`. The prior hard-gate gap is closed: `hostUidMap_refusesBeforeMount` exercises the identity-map refuse with `PERCUSSION_SOAK_NS=1`, exit 2, and the `host uid map` text. A later edit that restores an env-flag bypass of the uid_map check fails that assertion.

No open `bug`. Residual items below stay suggestions.

### Issue 1 -- Severity: bug

- File: `modules/perc-jetty/src/test/java/com/percussion/jetty/service/LinuxServiceNamespaceSoakTest.java:41` (in-diff)
- Description: Host uid-map refuse had no behavioral test. `hostUidMap_refusesBeforeMount` now sets `PERCUSSION_SOAK_NS=1` and asserts exit 2 plus `host uid map`.
- Suggestion: (done) Keep this case when editing the soak guard.
- Status: closed

### Issue 2 -- Severity: suggestion

- File: `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/test/java/com/percussion/delivery/distribution/DtsLinuxServiceNamespaceSoakTest.java:43`
- Description: Both happy-path tests skip only when `unshare --help` is missing or non-zero. `ProcessBuilder("unshare", "--help")` still throws `IOException` if the binary is absent (error, not skip), and `--help` succeeding does not mean `--user --map-root-user --mount` is allowed. On a Linux CI image without user namespaces both module suites fail.
- Suggestion: Share one skip helper: missing `unshare` (catch `IOException`) and EPERM on `--user --map-root-user --mount` → `assumeTrue` false.
- Status: open

### Issue 3 -- Severity: suggestion

- File: `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/README-systemd.md:100`
- Description: CMS `README-systemd.md` documents the user-namespace soak. The DTS sibling still stops at “offline review of the template + scripts” and does not mention `linux-service-namespace-soak.sh`.
- Suggestion: Add the same “namespace soak, not journalctl sign-off” paragraph used on the CMS README.
- Status: open

### Issue 4 -- Severity: suggestion

- File: `scripts/linux-service-namespace-soak.sh:29`
- Description: `uid_map="$(awk … || true)"` treats a missing `awk` or unreadable `uid_map` as empty, which is not equal to `0 0 4294967295`, so the identity-map refuse does not fire. Mapped-root then still requires `id -u` = 0. Host root plus `PERCUSSION_SOAK_NS=1` plus no `awk` would reach `mount`. Unlikely on a real Linux image.
- Suggestion: If `uid_map` is empty, refuse (fail closed) the same as the identity map.
- Status: open

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

Gate: PASS
May commit: yes

> Co-Authored by Grok Build 1.0.41 using grok-4.6 with agent Erlang Shen.
