# docker/ — Percussion CMS Dev/Test Stack

This directory holds the docker-compose stack, operator-facing control scripts, and container entrypoints for the cms-dts dev/test environment.

Per spec 994 (`specs/994-python-build-scripts/spec.md`): the original `.sh` wrappers around these scripts have been removed (FR-004). Windows, Linux, and macOS operators now invoke the Python entry points identically (`python3 scripts/<name>.py`).

## Layout

|     Path      |                                 Purpose                                 |
|---------------|-------------------------------------------------------------------------|
| `cms/`        | Dockerfile + image for the cms-dts container                            |
| `dev-data/`   | Persistent bind-mount volume (CMS install + DB)                         |
| `entrypoint/` | Container-side startup scripts (run inside the cms-dts image)           |
| `scripts/`    | Host-side operator control (CLI for `docker compose` + auxiliary tools) |
| `logs/`       | Timestamped log files written by `perc-devctl.py`                       |

## Host-side scripts

### `docker/scripts/perc-devctl.py`

Cross-platform replacement for the legacy `perc-devctl.sh`. The Python entry point exposes every subcommand the original `.sh` provided:

```
perc-devctl.py install [--reset] [--no-bootstrap] [--install-root <path>]
perc-devctl.py up [--build]
perc-devctl.py down [--volumes]
perc-devctl.py status
perc-devctl.py verify [--timeout-seconds N] [--interval-seconds N]
perc-devctl.py it-verify
perc-devctl.py deploy-jar --jar <path> [--target cms|dts|both|/abs/path] [--restart] [--verify]
perc-devctl.py verify-fix --jar <path> [--target ...] [--restart|--no-restart] [--timeout-seconds N]
perc-devctl.py logs-path
perc-devctl.py inspect-install
perc-devctl.py show-generated-passwords
```

Each subcommand writes full output to a timestamped file under `docker/logs/<label>-<ts>.log` and emits a single `RESULT:OK STEP:<label> LOG:<path>` (or `RESULT:FAIL`) line on stdout so agent workflows can parse the result without parsing free-form output.

### `docker/scripts/hot-deploy-jar.py`

Cross-platform replacement for the legacy `hot-deploy-jar.sh`. Copies a built module jar into a running cms-dts container:

```
python3 docker/scripts/hot-deploy-jar.py --jar modules/utils/target/utils-8.2.0.jar --target both --restart
```

Default container: `percussion-cms-dts`. Default target: `both` (CMS + DTS lib dirs). `--target` accepts `cms`, `dts`, `both`, or an absolute container path.

## Container entrypoint

### `docker/entrypoint/install-update.py`

Cross-platform replacement for `install-update.sh`. Runs inside the cms-dts container at startup. **Service-only mode**: the install lives entirely on the host (see `scripts/install-cms-dev.py`); this script only starts the service.

Container-side invocation (set in `docker/cms/Dockerfile`):

```dockerfile
ENTRYPOINT ["/usr/local/bin/python3", "/usr/local/bin/install-update.py"]
```

`--service-mode` defaults to `cms-dts` (start both). Other values: `cms`, `dts`.

## Tests

```sh
# Linux / macOS
bash scripts/run-python-tests.sh --skip-install --pytest-args "-q"
# Windows
scripts\run-python-tests.cmd --skip-install --pytest-args "-q"
```

The pytest collection covers all in-scope script dirs per spec 994; the docker scripts above are included automatically.

## When to add a new script here

Per root `AGENTS.md`, scripts that CI or an operator runs must be cross-platform. For docker tooling, that means: implement the logic in Python, invoke `docker` / `docker compose` via `subprocess.run([...], shell=False, ...)` (FR-008), use `pathlib.Path` (FR-007), and add a pytest module under the same directory. Avoid introducing new `.sh` files — every shell script in this directory is a candidate for spec 994 migration.
