# Erlang review: issue #4456 commons-email2 WAR / hot-deploy

**Date:** 2026-09-11  
**Branch:** `fix/issue-4456-commons-email2-war-lib`  
**Base:** `origin/main`  
**Reviewer:** Erlang (pre-commit, implementer session)

## Summary

Skip-image-build QA cells copied perc-system / rest / sitemanage SNAPSHOTs into `WEB-INF/lib` without Commons Email 2 transitives. New perc-system references `org.apache.commons.mail2.core.EmailException`; Jetty then logged `Failed startup of context` on `sys_emailQueueListener`.

This change:

1. Copies `commons-email2-core`, `commons-email2-jakarta`, `com.sun.mail:jakarta.mail` 2.x, and `extensions-workflow` SNAPSHOT in `hot-deploy-rhythmyx-war-jars.py`.
2. Removes stale `commons-email-*.jar` (1.x) and `jakarta.mail-1.x` (not `jakarta.mail-api`) from the cell WAR lib.
3. Truncates `server.log` after StopJetty so `qa-health` does not match a prior failed boot.
4. Keeps perc-system mail2 + `com.sun.mail:jakarta.mail` compile-scoped.
5. Adds Python behavioral tests and a perc-system POM contract test.

## Scope

- Uncommitted + untracked vs `HEAD` on `fix/issue-4456-commons-email2-war-lib`.
- `git fetch origin main` already done; branch is from `83420a03ee` (`origin/main`).
- Files: `docker/scripts/hot-deploy-rhythmyx-war-jars.py`, `test_hot_deploy_rhythmyx_war_jars.py`, `perc-devctl.py`, `docker/README.md`, `system/pom.xml`, `system/.../CommonsEmail2WarLibContractTest.java`, `WebUI/pom.xml`, `WebUI/.../CommonsEmail2WarLibPackagingTest.java`.
- Memory patterns hit: skip-image-build WAR classpath vs SNAPSHOT-only copy (`NoClassDefFoundError` after `--then-qa-deploy-war-jars`); compile vs provided for WEB-INF/lib (servlet-utils peer).
- Cross-platform path review: local jar resolve uses `Path` / `Path.joinpath` / `os.path.expanduser("~")` (not `Path("~")`). Docker dest remains POSIX `/` (container path, not host FS). Tests normalize dest strings with `replace("\\", "/")` only for docker target assertions.

## Recommendation

**approve**

## Gate

- Bugs: none found
- Missing behavioral tests: no (Python deploy/copy/rm + missing-mail2 refusal; Java POM/compile contracts)
- Non-portable path I/O: none found
- **May commit/push: yes** (after module `clean install` and C5 qa-health / Playwright)

## Issues

None at `bug` severity.

### suggestion

- `resolve_mail2_jars` requires `<commons-email.version>` even when `system/target` already has both jars. Acceptable in this monorepo (parent POM is always present). If this script is ever reused outside the tree, parse version only for the m2 fallback.

### nit

- `perc-devctl.py` argparse help for `qa-deploy-war-jars` now mentions mail2; keep docker/README and the module docstring in lockstep if artifacts are added later (same SNAPSHOT-only trap).

## Tests observed (pre-commit)

- `python docker/scripts/test_hot_deploy_rhythmyx_war_jars.py` — 20 tests, OK
- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS; `CommonsEmail2WarLibContractTest` Tests run: 2, Failures: 0; suite Tests run: 3075, Failures: 0, Errors: 0, Skipped: 254
- WebUI standalone `clean install` in progress at review time
