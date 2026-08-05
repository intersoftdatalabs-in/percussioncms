# Erlang review — issue #2004 compose DB freeport

**Branch:** `feat/issue-2004-compose-db-freeport`  
**Scope:** `docker/scripts/*` freeport for compose DB host ports + docs  
**Verdict:** Pass (implement)

## Change class

Host-side Docker operator scripts: extend existing freeport contract (`perc_host_ports.resolve_host_port`) to compose published DB ports (`MYSQL_PORT` / `POSTGRES_PORT` / `MSSQL_PORT`), pin process env before `docker compose up`, unit-test allocation + override.

## Checklist

|         Gate         |                                                              Result                                                               |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| Bugs / correctness   | Pass — env → preferred-when-free → freeport; pin into `os.environ` so compose shell env overrides `.env.compose`                  |
| Cross-platform paths | Pass — stdlib `socket` only; no path construction; no Unix-only tools                                                             |
| Behavioral tests     | Pass — `test_matrix_install_smoke.py` ComposeDbHostPortFreeportTests; `test_perc_devctl.py` DB pin/freeport tests; 98 tests green |
| Companions           | Pass — matrix + perc-devctl peer consumers; README freeport section; docker-compose already exposes `${*_PORT:-…}`                |
| Secrets              | Pass — no credentials in freeport path                                                                                            |
| Scope                | Pass — no live multi-DB install required; residual container_name concurrency remains parent/#2006                                |

## Notes

- Container listen ports and matrix `DB_PORT` stay fixed; cells use Docker DNS.
- Fixed `container_name` still prevents two simultaneous full stacks on one host; freeport only fixes publish port collisions.
- Host installer `PERC_DB_PORT` alignment documented in README when MYSQL freeports away from 3306.

