# Erlang review — #3288 DCE Xlint this-escape/serial cluster

**Branch:** `fix/issue-3288-dce-xlint-this-escape`  
**Date:** 2026-08-13  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** this-escape remediations prefer `final` + transient collaborators (PSFolderDialog / status dialog peers); do not strip applet `removal` suppressions.

## Summary

Re-probed `perc-content-explorer` with `javac -Xlint -Xmaxwarns 10000` (289 warning lines). Cleared the next this-escape/serial cluster: real-fixed `PSNode` / `PSNavigationTree` (removed ctor suppressions via `final` + static folder-type helpers + lazy accessible context) and applied the same `final` + `serialVersionUID` + `transient` pattern to explorer chrome and option beans. `PSContentExplorerFrame` still implements `java.applet.AppletStub`/`AppletContext` (removal warnings left in place per issue).

## Issues

None (no bugs, no missing behavioral tests for new static helpers, no path I/O).

## Cross-platform path checklist

N/A — no file I/O or path construction in this diff.

## Evidence

- `cd modules/DesktopContentExplorer && ../../mvnw.cmd clean install` → BUILD SUCCESS, Tests run: 184, Failures: 0
- C2: grepped monorepo for `extends` / anonymous subclasses of finalized types — only `? extends PSNode` iterator bounds
