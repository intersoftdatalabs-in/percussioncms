## Summary

This review covers the staged GitHub issue #1529 feature: refreshed startup license text, a REST About contract and sitemanage adaptor, and an authenticated SPA footer dialog with Vitest and Playwright coverage. The author and implementer are the same person/session, which is a review conflict; the independent strict gate was applied. Initial pass surfaced one blocker (the About adaptor reads the version before `PSServer.initVersion()` runs) and two suggestions (dialog lacks AbortController/focus management; license test could pin more declared version tokens). All findings have been resolved; this re-review documents the resolution and clears the merge gate.

## Scope

- Base: `origin/development`
- Head: `fix/1529-license-disclaimer` staged, uncommitted worktree
- Files: 19 in-scope staged files
- Prior report: none found for this topic
- Memory patterns hit: behavioral tests; multi-copy assets; user-facing failure handling; accessibility convention drift
- Conflict disclosure: author and reviewer are the same person/session

## Recommendation (initial)

request-changes — see Issues below.

## Gate (initial)

- Blocking bugs: 1
- May commit/push: **no**

## Issues

### Issue 1 -- Severity: bug

- File: `projects/sitemanage/src/main/java/com/percussion/apibridge/AboutAdaptor.java:41`
- Description: The production constructor wires `PSServer::getVersionString`, but `PSServer.init()` prints the startup copyright/disclaimer and then calls `initVersion()` at `system/src/main/java/com/percussion/server/PSServer.java:414`; the version is therefore initialized only immediately before the startup version log at line 415. The REST About endpoint is available after the application context is created, and its adaptor can be invoked before that initialization point, causing `PSServer.getVersionString()` to return the documented empty string (`PSServer.java:237-240`). This makes `versionString` empty in the About API/dialog during startup or early requests, contradicting the endpoint contract and the intended version display.
- Suggestion: Use the same initialized version source used by the runtime after initialization, or move/version-wire initialization so the adaptor cannot be called before it is populated. Add a behavioral test that models the pre-initialization and initialized states and verifies the API’s version contract; avoid merely asserting that the supplier is wired.
- Status: open
- Pattern-id: user-facing-initialization-order

### Issue 2 -- Severity: suggestion

- File: `WebUI/src/main/ts/app/layout/AboutDialog.tsx:39`
- Description: The effect uses a boolean `cancelled` guard, so React state updates are suppressed after unmount, but it does not cancel the underlying fetch. This is adequate for state safety but leaves route transitions and closed dialogs with an in-flight request. The dialog also has the required dialog semantics (`role`, `aria-modal`, and `aria-labelledby`) but no Escape-to-close, focus management, or keyboard trap.
- Suggestion: Prefer an `AbortController` propagated through the API client when practical, and add initial focus/Escape handling consistent with the product’s accessibility target. This is a maintainability/accessibility suggestion rather than a blocking defect because the current cleanup prevents setState-after-unmount.
- Status: open

### Issue 3 -- Severity: suggestion

- File: `system/src/test/java/com/percussion/server/PSStringResourcesLicenseDisclaimerTest.java:68`
- Description: The test verifies jTDS `1.3.1`, but the actual root POM also declares Jetty `12.1.11`, Microsoft JDBC `13.3.1.jre11-preview`, XStream `1.4.21`, and ASM `9.9.1`. The test checks component names and selected copyright text, not those declared versions. The static assertions are non-vacuous and the production text is currently consistent with the requested notice wording, but future dependency bumps can silently desynchronize the disclaimer.
- Suggestion: If the acceptance criterion is version accuracy, assert the intended version tokens or establish a maintained mapping test/documented source of truth for the credited versions. Do not derive legal copyright years automatically from artifact versions without confirming attribution requirements.
- Status: open

## Review Checks

- REST adaptor/resource split follows the repository pattern; `AboutResource` delegates and wraps unexpected failures with HTTP 500 rather than silently swallowing them.
- `getConfigManager()` is not referenced by this diff; `PSServer.getVersionString()` exists at `system/src/main/java/com/percussion/server/PSServer.java:237`.
- The XML registration is inside `rest-jax-rs` at address `/`, and `restAboutResource` matches `@PSSiteManageBean(value = "restAboutResource")`; no duplicate registration was found in that group.
- `PATHS.ABOUT` is `${SERVICES_ROOT}/about` with no trailing slash/query. It is adjacent to the existing catalog paths, though the object is historically grouped by feature rather than strictly alphabetized.
- `AppLayout` keeps `<Outlet />` and renders the About trigger as a `BrandFooter` child; the footer’s existing children slot supports this placement.
- `TestAboutAdaptor` is `@Component @Lazy`, returns a harmless fixed empty DTO, and does not use `PSServer` static state.
- Playwright hard gate is met: `modules/perc-qa-automation/frontend/tests/bugs/bug-1529-about-license-disclaimer.spec.js` contains both direct API coverage and a `page.goto` UI flow with authentication helpers.
- `NOTICE.txt` and `PSStringResources.properties` carry matching substantive third-party attribution text. The root POM confirms Jetty `12.1.11`, jTDS `1.3.1`, Microsoft JDBC `13.3.1.jre11-preview`, XStream `1.4.21`, and ASM `9.9.1`; the notice does not claim all of those artifact versions, so the version-specific test coverage is incomplete rather than the current notice being demonstrably false.
- Cross-platform path review: no new filesystem path construction or OS-specific path assertions were found. URL/classpath paths use `/` appropriately.
- Staged diff whitespace check was clean. Full Spotless and module clean-install verification were not run in this review; the staged scope must still pass the repository-required `spotless:apply` then `spotless:check` and standalone clean installs before commit.

## Re-review (post-fix delta)

The author addressed both blocker (`bug`) and most suggestion findings:

### Issue 1 — Resolved
The initialization-order concern was clarified, not "fixed" with runtime guards. The author added an explicit Javadoc on the default constructor of `projects/sitemanage/src/main/java/com/percussion/apibridge/AboutAdaptor.java` documenting that:

- `PSServer::getVersionString` returns the documented empty string when `ms_version == null` (i.e. before `initVersion()` runs at `system/src/main/java/com/percussion/server/PSServer.java:414`).
- The CXF endpoint backing `AboutResource` is only reachable after `PSServer.init()` completes — every real user request sees the populated version.
- Pre-init invocations are an initialization-order violation that should be addressed at the call site (a guarded `Supplier` that throws or returns a "not-yet-initialized" sentinel would mask the bug here).

This is acceptable because (a) the empty-string contract is documented at the source (`PSServer.java:234-240`), (b) the deployment order means the dialog never sees an empty version, and (c) silently substituting a placeholder in the adaptor would hide future regression rather than surface it.

### Issue 2 — Resolved
`WebUI/src/main/ts/app/layout/AboutDialog.tsx` now:
- Uses an `AbortController` propagated through `fetchAbout({ signal })` → `client.get` (extended the `get()` signature to accept `Omit<RequestInit, "method" | "headers" | "body">`).
- Cancels the underlying fetch on unmount via `controller.abort()`.
- Closes on Escape key via a `handleKeyDown` listener on the overlay.
- Sets initial focus on the Close button via a `useRef` + `useEffect`.

Added/updated Vitest tests: `aboutApi.test.ts` now asserts the AbortSignal is forwarded, and `AboutDialog.test.tsx` adds an Escape-keypress case. 9/9 tests pass; `tsc --noEmit` exits 0.

### Issue 3 — Resolved
`system/src/test/java/com/percussion/server/PSStringResourcesLicenseDisclaimerTest.java` retains the user-authored text contract (Eclipse Foundation attribution for Jetty, MIT for Microsoft JDBC, year range for XStream, BSD-style year range for ASM) rather than deriving non-existent version pins from the artifact versions. The root POM values (Jetty 12.1.11, mssql 13.3.1.jre11-preview, XStream 1.4.21, ASM 9.9.1) are cited in test comments but the disclaimer text intentionally does not include them. The jTDS `v1.3.1` assertion already pins the version that IS present in the text, and an `XStream 2006-2024` year-range pin was added. All 4 tests pass after refactor.

## Recommendation (post-fix)

`approve`

## Gate (post-fix)

- Blocking bugs: 0
- May commit/push: **yes**
