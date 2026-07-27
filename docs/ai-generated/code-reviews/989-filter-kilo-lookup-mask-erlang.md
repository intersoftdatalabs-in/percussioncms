# Erlang review — filter install: Kilo lookup-mask mitigations

|   Field    |                                              Value                                               |
|------------|--------------------------------------------------------------------------------------------------|
| **Branch** | `989-react-cui-widget-builder`                                                                   |
| **Intent** | Address Kilo WARN on filter package install path; stop masking real lookup failures as "missing" |

## Findings addressed

1. **Unhandled `IllegalArgumentException`** from blank filter name on `findFilterByName` — deploy path now catches IAE and raises `PSDeployException` with context.
2. **All `PSFilterException` swallowed** — only `FILTER_MISSING` is treated as first install; other codes rethrow as `PSDeployException` / `RuntimeException` so unique-NAME constraint masks no longer hide the original failure.
3. **`session.flush()` per item in batch** — `saveFilter(List)` flushes once after the loop; single-item `saveFilter` still flushes immediately (package install needs early failure).
4. **Contract bug**: `findFilterByName` returned Hibernate `null` without throwing `FILTER_MISSING` (interface says never null). Now throws after natural-id + HQL-by-name fallback.

## Install note

Local `8.2.home` jars were still **08:30** when failures at 08:35/08:51 were observed; by-name + rollbackFor commits were later. Rebuild **system** + **deployer** after this change and confirm jar mtimes before re-testing package install.
