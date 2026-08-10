# Erlang review — issue #1915 filter Jackson migration

**Change class:** design-object XML domain batch (Jackson annotations + golden/round-trip tests)

## Checklist

|                Gate                 |                                                     Result                                                     |
|-------------------------------------|----------------------------------------------------------------------------------------------------------------|
| Bugs / behavioral regressions       | Pass — suppress parentFilter/version/rule/GUID/circular filter; live rule set mutation via field in mergeRules |
| Unit tests for new/changed logic    | Pass — `PSItemFilterXmlSerializationTest` (6 tests)                                                            |
| Cross-platform paths                | Pass — classpath resources only; no filesystem path joins                                                      |
| Companions from peers (#1888–#1891) | Pass — annotations, addType, golden, package smoke, deviations doc                                             |
| Spotless in-scope only              | Pass — out-of-scope apply hits discarded                                                                       |
| Module clean install                | Required before PR                                                                                             |

## Notes

- `getGUID()` uses `new PSGuid(...)` (no GuidManager) for offline XML parity with peers.
- `setParam` constructs `PSItemFilterRuleParam(true)` to avoid GuidManager during XML restore.
- No `.betwixt` files for filter domain.

