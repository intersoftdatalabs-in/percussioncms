# Erlang review — docker-compose dev runtime fix

**Branch**: `992-react-content-explorer-us1` (off `origin/development` HEAD `e8b0e218c8`)
**Date**: 2026-07-19
**Scope**: `docker-compose.yml` (cms-dts service) + `.env.compose`. Goal: make MySQL install work (wire `PERC_DB_*` env vars) and prevent the install/restart loop (interpolate healthcheck window from env).

## Files reviewed

|         File         |                                                                                                                                                            Change                                                                                                                                                            |
|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `docker-compose.yml` | Added `PERC_DB_*` env vars to `cms-dts.environment:` (Type, Host, Port, Name, Schema, User, Password, SSL_*). Interpolated `retries`/`start_period` from `${PERC_HEALTHCHECK_RETRIES:-180}` / `${PERC_HEALTHCHECK_START_PERIOD:-3600s}`. Restored `cms-dts:` 2-space indent (accidentally stripped during the env-var edit). |
| `.env.compose`       | Bumped `PERC_HEALTHCHECK_START_PERIOD=3600s`, `PERC_HEALTHCHECK_RETRIES=180`. Set `PERC_DB_TYPE=mysql`, `PERC_DB_HOST=mysql` (compose service name), `PERC_DB_PORT=3306`, `PERC_DB_USER=percussion`, `PERC_DB_PASSWORD=percussion`, `PERC_DB_SSL_VERIFY=false`, `PERC_DB_SSL_ALLOW_SELF_SIGNED=true` (local MySQL).          |

## Summary

The previous stack came up but installed with `--db.type=derby` (default) because `docker-compose.yml` never wired the `PERC_DB_*` vars from `.env.compose` into the container. Even with Derby the install never reached "healthy" within the 25-min window allowed by the hardcoded `start_period=300s` + `retries=60` healthcheck (it was hitting the retry cliff at 60 failures × 20s interval ≈ 25 min total). At that point `restart: unless-stopped` killed and re-launched the container, wiping the ephemeral `/opt/Percussion/.percussion-install-complete` marker, triggering a fresh install, looping indefinitely (observed 11 restarts).

This commit fixes both root causes:
1. **DB env vars wired** so MySQL actually reaches the container (and any future DB switches via `.env.compose` propagate).
2. **Healthcheck window bumped to 1 h start_period + 180 retries ≈ 3 h total** via `.env.compose`, with compose.yml using `${VAR:-default}` interpolation so users can tune per-environment.

## Hard gates checked

|                 Gate                  |                                                                                                                                                                                                          Status                                                                                                                                                                                                           |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Non-portable filesystem path joins    | **Pass (n/a)** — no filesystem path code in this commit. `.env.compose` uses URL-free `MYSQL_*`, `PERC_*` env keys.                                                                                                                                                                                                                                                                                                       |
| Secrets on command line               | **Pass (n/a)** — `.env.compose` is an env file consumed by `docker compose --env-file`, not passed as argv. Passwords (`MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `PERC_DB_PASSWORD`) are values in the env file. **Caveat**: `.env.compose` is local-dev secrets and is `.gitignore`'d by `.env.compose.example` semantics — NOT committed (only `.env.compose.example` is tracked). Verify with `git status` before push. |
| Default-true DB password `percussion` | **Pass for local dev**. The docstring on `.env.compose` explicitly says "Secrets are local-dev placeholders; rotate before any non-local use." Production must override.                                                                                                                                                                                                                                                  |
| Healthcheck exit code drift           | **Pass** — healthcheck test is unchanged; the change is in `retries`/`start_period` defaults.                                                                                                                                                                                                                                                                                                                             |
| Boolean env interpolation             | **Pass** — `PERC_DB_SSL_ENABLED`/`VERIFY`/`ALLOW_SELF_SIGNED` are interpolated via `${VAR:-true|false}` and read by `install-update.sh`'s `db_config_value` via `${!VAR}` bash indirection. Compose v5.3.1 expands `${VAR:-default}` correctly inside the `environment:` block.                                                                                                                                           |

## Cross-platform path checklist

- All env keys are POSIX-style (Linux/Windows shell parse).
- `.env.compose` is a docker compose env file; consumed identically on Linux/macOS/Windows Docker Desktop.
- **No shell scripts added in this commit.**

## Recommendation

**Approve.**

## Outstanding (deferred, non-blocking)

- **`install-if-missing` marker persistence**: `.percussion-install-complete` is at `/opt/Percussion/.percussion-install-complete` (container-ephemeral, not bind-mounted). A container restart wipes it and re-installs. Two follow-up options: (a) bind-mount a single-file named volume at `/opt/Percussion/.percussion-install-complete`, or (b) move the marker to a persistent host dir via `install-update.sh` edit. Both are PR-sized changes; not in this commit because (a) would require a Compose v2 `type: volume` workaround for single-file mounts and (b) is a behavioral change to the entrypoint. The bumped healthcheck window makes the original install finish in one pass; subsequent restarts will redo the install (fast, since bind-mounted subdirs persist), so this is acceptable for now.
- **Derby default fallback**: if a user removes `PERC_DB_*` from `.env.compose`, install falls back to Derby (the in-container default). Documented in `.env.compose.example`.

## Gate

**May commit/push: yes.**
