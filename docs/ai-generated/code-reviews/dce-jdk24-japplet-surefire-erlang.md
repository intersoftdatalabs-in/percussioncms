# Erlang review — DCE JDK 24+ JApplet Surefire

- **Scope:** uncommitted `perc-content-explorer` / `perc-server-ui-cmp` applet-host replacement vs `HEAD`
- **Date:** 2026-09-16
- **Recommendation:** approve
- **May commit/push:** yes
- **Gate:** approve
- **Memory patterns hit:** missing behavioral tests (checked — present); incomplete change-class closure (DCE host + AboutDialog companion + module suites); structural-only tests (AboutDialog signature check is justified — constructing `JDialog` is headless-hostile); cross-platform path checklist N/A (no filesystem I/O)

## Summary

Desktop Content Explorer still compiled with `--release 21` (ct.sym still has `JApplet`) but Surefire on JDK 24+ died in Vintage discovery with `ClassNotFoundException: javax.swing.JApplet`. The change replaces the removed JDK applet types with local `PSJApplet` / `PSAppletStub` / `PSAppletContext`, retargets the frame and stub, and drops the unused `java.applet.AppletContext` field from `PSAboutDialog`. Behavioral tests cover stub wiring, parameter round-trip, applet construction without a host, and the AboutDialog type surface. Standalone `mvnw clean install` is green for both modules.

No blocking bugs. Remaining `JApplet` subclasses (`PSHelpApplet`, `PSCheckboxTreeApplet`) are out of this change class.

## Cross-platform path checklist

N/A — no new filesystem path construction, temp files, or path assertions. Test URLs use `URI.create(...).toURL()`.

## Issues

None.

## Build evidence

- `cd modules/ServerUIComponents && ../../mvnw clean install` — BUILD SUCCESS; Tests run: 2, Failures: 0
- `cd modules/DesktopContentExplorer && ../../mvnw clean install` — BUILD SUCCESS; Tests run: 225, Failures: 0
