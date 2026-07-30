## Summary

- `InstallUtil.portAvailable` now only probes TCP (`ServerSocket`). Previously it also required a `DatagramSocket` on the same port, which on Windows let a stray UDP binding make the offline-install gates (`checkTomcatServerRunning`, `checkServerRunning`, `isDerbyRunning`) incorrectly return "running".
- `@return` Javadoc tag added; wording updated so the TCP-only contract is explicit.
- New regression test `portAvailable_ignoresUdpBinding` proves a `DatagramSocket` still bound on the port does not flip the TCP availability result.

## Pre-PR clean install (HARD GATE)

Ran from the repo root with the JDK wrapper:

```text
cd modules/utils
../../mvn-env.bat clean install
```

- **BUILD SUCCESS**
- 229 tests, 0 failures, 0 errors, 9 skipped (pre-existing skips)
- No new compiler / surefire / enforcer / javadoc / Spotless warnings on the touched files

## Erlang review

Independent Erlang (strict) review: `docs/ai-generated/code-reviews/fix-install-port-tcp-detection-erlang.md` — recommendation **approve**, gate **May commit/push: yes**. No bugs, no missing behavioral tests after this change, no cross-platform path smells (diff does not touch filesystem I/O).
