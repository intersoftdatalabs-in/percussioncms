# Erlang review - issue #2984 design.objectstore this-escape PSEntry/PSDataSet

**Reviewer persona:** Erlang (strict pre-commit)  
**Change class:** design.objectstore this-escape residual (private load without parent-list registration during Element construction)  
**Module:** `system` / perc-system  
**Parent:** #2022 / #2200 / prior #2871  
**Thrash note:** open #2952 retypes parentComponents on same files — this PR only adds register flag + Application empty+fromXml; no List signature retype.

## Summary

Clear residual `-Xlint:this-escape` on non-final bases `PSEntry` / `PSDataSet` after #2871 leaf finals:

- Element constructors call private `fromXmlBase(..., registerInParentList=false)` so construction does **not** publish `this` via `updateParentList`.
- `fromXml` still registers (`true`) after full construction — product parent-list semantics preserved for post-construction load.
- `PSApplication` dataset restore switched to empty + `fromXml` so Application XML load still registers the dataset during child construction.
- `PSNullEntry` made `final` (no monorepo subclasses) for its Element/fromXml parent-list path.

## Checklist

| Gate | Result |
|------|--------|
| Bugs in new logic | None expected — flag only changes when self is pushed on parent list; ancestors still passed through |
| Behavioral unit tests | Extended `PSDesignObjectStoreThisEscapeTest` (Entry fromXml, NullEntry Element, DataSet Element + fromXml) |
| Cross-platform paths | N/A — no path I/O |
| Product docs | N/A — compiler hygiene / non-operator-facing |
| Subclass blast radius | `PSNullEntry` final: grep `extends PSNullEntry` → none. `PSEntry`/`PSDataSet` remain non-final |
| Blanket `@SuppressWarnings("this-escape")` | Not used |
| Thrash vs open PRs | Avoid List retypes (leave for #2952); local register-flag + Application call-site only |

## Risk notes

- Direct `new PSDataSet(Element,...)` no longer self-registers during child load. Sole production caller was Application — now uses empty+fromXml. Other Element callers (if any external) still restore fields; nested components still receive ancestor parents.
- `PSEntry` Element path similarly skips self-registration; children (`PSDisplayText`) do not require `PSEntry` on the parent stack.

## Build evidence

- `cd system && ..\mvnw.cmd clean install` → **BUILD SUCCESS**
- Tests run: **1756**, Failures: **0**, Errors: **0**, Skipped: 241
- `PSDesignObjectStoreThisEscapeTest`: **29** tests, 0 failures
- Compile: zero `this-escape` on `PSEntry` / `PSDataSet` Element constructors

## Downstream

- `extends PSNullEntry` / anonymous subclass: none (final OK)
- `PSEntry` / `PSDataSet` remain non-final (PSNullEntry / PSContentEditor)

## Verdict

**PASS** — module clean install green; residual this-escape on PSEntry/PSDataSet Element ctors cleared without suppress.
