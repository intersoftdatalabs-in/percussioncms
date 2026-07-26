# Erlang review — #1179 jetty.home/base system properties

|   Field    |                                            Value                                            |
|------------|---------------------------------------------------------------------------------------------|
| **Date**   | 2026-07-17                                                                                  |
| **Branch** | `fix/1179-jetty-home-base-system-properties`                                                |
| **Intent** | Re-apply fix for Windows `InvalidPathException` on `basehome:lib/jdbc/` during `setup-home` |

## Scope

`modules/perc-distribution-tree/pom.xml` — `setup-home` exec: set `jetty.home` / `jetty.base` via `<systemProperties><systemProperty><key>…</key>` and remove CLI `jetty.home=` / `jetty.base=` arguments.

## Context

- Issue #1179; worklog already documents root cause.
- PR #1181 intended this fix; later commit re-added CLI args and the merged tip lost `systemProperties`.
- exec-maven-plugin `Property` uses **`key`/`value`** (confirmed via `javap` on 3.5.0/3.6.3) — not `<name>`.
- `perc-ds.mod` keeps `basehome:lib/jdbc/*.jar` in `[lib]` (correct once BaseHome is initialised).

## Cross-platform

|   OS    |                      Before                      |               After               |
|---------|--------------------------------------------------|-----------------------------------|
| Windows | `Path.of("basehome:…")` → `InvalidPathException` | `basehome:` resolves via BaseHome |
| Linux   | Silent miss of JDBC path                         | Drivers resolve under jetty.base  |

No hardcoded OS path separators introduced (Maven `${assembly-directory}` is OS-aware).

## Issues

None.

## Recommendation

**`approve`** — May commit/push: yes.
