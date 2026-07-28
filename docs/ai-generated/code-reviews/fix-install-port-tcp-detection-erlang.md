## Summary

`InstallUtil.portAvailable` previously required both a TCP and a UDP socket on
the same port; on systems where the kernel reports UDP ports separately, that
caused the DTS Tomcat and Derby running checks (`checkTomcatServerRunning`,
`checkServerRunning`, `isDerbyRunning`) to incorrectly report the servers as
running when no Tomcat was bound, producing 4 failing JUnit tests on Windows.
The patch narrows the check to TCP, which matches the only signal callers
care about (Tomcat HTTP/HTTPS/AJP connectors and Derby JDBC). A new test binds
a UDP socket and asserts the port still reports available.

## Scope

- Base: `origin/development` @ `9af574fa5c`
- Head: branch `fix/install-port-tcp-detection`, working tree
- Files: 2 changed (`modules/utils/src/main/java/com/percussion/install/InstallUtil.java`, `modules/utils/src/test/java/com/percussion/install/InstallUtilRunningServerTest.java`)
- Prior report: none for this branch slug
- Memory patterns hit: `tests.happy-path-only` (regression for placeholder/free connector), `installer.port-detection-false-positive`

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

None.

### Notes

- **API scope** — All known call sites of `portAvailable` are Tomcat (HTTP/HTTPS/AJP) or Derby/JDBC ports (`InstallUtil.checkTomcatServerRunning`, `checkServerRunning`, `isDerbyRunning`, `isBindableTcpPort`); UDP was never meaningful for these. Removal is safe and matches the method name and Javadoc.
- **Behavioral coverage** — Added `portAvailable_ignoresUdpBinding` so the regression cannot reappear silently. The pre-existing `portAvailable_roundTrip`, `checkTomcatServerRunning_trueWhenLiteralPortBound`, and `checkTomcatServerRunning_resolvesCatalinaPlaceholderWhenBound` cover the positive direction; the two failing "free" tests now pass with the TCP-only logic and document the original intent.
- **Build evidence** — `cd modules/utils && ../../mvn-env.bat clean install` reports **BUILD SUCCESS**, 229 tests, 0 failures/errors, 9 skipped (pre-existing skips); no new compiler or javadoc warnings introduced on the touched files.
- **Cross-platform path review** — N/A; the patch does not touch filesystem paths, installers, packaging, or path assertions.
- **Silent failure smell** — None added; the existing `catch (IOException e)` block still logs via `logError`.
