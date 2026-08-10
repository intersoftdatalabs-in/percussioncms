# Erlang review — issue #2347 utils test-source Xlint batch 3

**Verdict:** approve  
**Change class:** test-source generics / Xlint cleanup (no production API change)  
**Module:** `modules/utils`  
**Date:** 2026-08-07

## Scope reviewed

|          File           |                                                           Change                                                           |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `PSItemIteratorTest`    | `MultiValuedMap<String,String>`; ctor `Map<?,?>` matches production `PSItemIterator`                                       |
| `PSJexlEvaluatorTest`   | Typed `Map`/`List` locals; `createScript`/`createExpression` via type name; method-level unchecked only where binder nests |
| `PSPropertyWrapperTest` | `Long`/`Double.valueOf` (removal constructors)                                                                             |
| `PSTestUtils`           | `Class<?>` / `Constructor<?>` reflection helpers                                                                           |
| `PSTestPrinter`         | `Map<?,?>` + typed entry iteration                                                                                         |
| `PSTestResourceUtils`   | `Class<?>` (matches `PSResourceUtils`)                                                                                     |
| `TestAllHTML5Tags`      | `Map.Entry<String,String>`                                                                                                 |
| `PSFacadeMapTest`       | Documented unchecked clone cast (`HashMap.clone()` → Object)                                                               |
| `PSTestBeanConfig`      | Empty-if → explicit `return`                                                                                               |

## Hard gates

|            Gate             |                                               Result                                               |
|-----------------------------|----------------------------------------------------------------------------------------------------|
| Bugs / behavior change      | Pass — type parameters only; multi-map iteration path still exercises `asMap()`                    |
| Behavioral unit tests       | Pass — existing suite covers changed tests; no new production logic                                |
| Portable paths / file I/O   | Pass — no path changes (`PSTestResourceUtils` still uses `createTempFile` / `createTempDirectory`) |
| Companion completeness      | Pass — test-only; no Spring/rest companions                                                        |
| No new blanket suppressions | Pass — only method-level unchecked on binder cast navigation + clone cast                          |

## Residual (out of this PR)

- `PSWorkflowUtilsBaseTest` intentional raw public-API probes (~34+ under full `-Xmaxwarns`)
- Open batch 2 PR #2346: `PSReflectionHelper` + `PSMultiMapIterTest`
- Main-source `@SuppressWarnings` strip (`InstallUtil`, `PSCollection`, `PSJexlEvaluator`, …)

## Verification

- Standalone `modules/utils`: `mvnw clean install` — **BUILD SUCCESS**
- Tests: **311** run, **0** failures, **9** pre-existing skips
- Fixed files: **0** project-Xlint warnings after change

## Operator

Operator: Grok: night-issue-prs (model grok-4.5)

> Co-Authored by Grok Build using grok-4.5 with agent main.

