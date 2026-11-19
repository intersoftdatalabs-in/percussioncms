<!--
  ~ Copyright (c) 2026 Intersoft Data Labs, Inc.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

# Milestone 1 Inventory: Sign-In Entry Points, `/v8/*` Allowlist & Security-Provider Ownership

This document captures the verified codebase inventory for **Milestone 1 (#48)** of [login-mfa-and-account-recovery.md](../login-mfa-and-account-recovery.md). All paths, symbol names, and line numbers are verified directly against Percussion CMS 8.2 source files.

---

## 1. Live Sign-In & Session-Grant Entry Points

Tracing [PSSecurityFilter.java](../../../../system/src/main/java/com/percussion/servlets/PSSecurityFilter.java), [PSLoginServlet.java](../../../../system/src/main/java/com/percussion/servlets/PSLoginServlet.java), [PSSecurityWs.java](../../../../system/webservices/src/com/percussion/webservices/security/impl/PSSecurityWs.java), and [system-security-conf.xml](../../../../WebUI/src/main/webapp/WEB-INF/config/security/system-security-conf.xml) identifies **five** paths capable of minting or rehydrating an authenticated CMS `Subject` / `PSUserSession`:

| # | Route / Mechanism | Code Entry Point | Current Behavior & Gap | M3 / M5 Target Policy |
|---|---|---|---|---|
| 1 | **Browser Form Login** (`POST /login`, `POST /Rhythmyx/login`) | `PSLoginServlet#login` (line 341) → `PSLoginServlet#authenticate` (line 509) → `PSSecurityFilter#authenticate(req, resp, uid, pwd)` (line 1496) | Validates JAAS credentials + `hasRhythmyxRole`, immediately binds `HttpSession.SUBJECT` (`RX_AUTHENTICATED_SUBJECT`, line 271), creates `PSUserSession`, sets `pssessionid` cookie, and redirects without calling `request.changeSessionId()`. | Split `PSSecurityFilter#authenticate` into `verifyPrimaryCredentials(...)` and `finalizeAuthenticatedSession(...)`. Intercept interactive browser accounts requiring MFA/setup (`MFA_CHALLENGE` or `ENROLL_ONLY`), defer `SUBJECT` / `pssessionid` minting until factor verification completes, and rotate `JSESSIONID` via `request.changeSessionId()`. |
| 2 | **HTTP Basic Auth & Header Override** | `PSSecurityFilter#calculateAuthType` (lines 911–934) → `PSSecurityFilter#handleBasicAuth` (line 1281) → `PSSecurityFilter#authenticate` | Applies to configured paths (`/Design*`, `/rxwebdav*`, `/sys_webServicesHandler`, `/sys_reports/*`, `/rx_reports/*`, `/content/*`) **and any URL carrying request header `RX_USEBASICAUTH` (`IPSHtmlParameters.SYS_USE_BASIC_AUTH`, line 916)** — bypassing form/MFA flow on UI and REST URLs. | Enforce `ACCOUNT_POLICY` after JAAS check: reject Basic Auth and block `RX_USEBASICAUTH` on interactive UI/REST paths with HTTP `401`/`403` for human `INTERACTIVE_MFA_REQUIRED` accounts; allow only for explicit `NON_INTERACTIVE_SERVICE` accounts (or during `optional`/`disabled` rollout). |
| 3 | **SOAP / Webservices Login** (`PSSecurityWs#login`) | `PSSecurityWs#login` (`PSSecurityWs.java:105`) → `PSSecurityFilter#authenticate(req, resp, user, pwd)` (line 123) | Authenticates via username/password only and returns `PSLogin` with `pssessionid`. | Apply the same `ACCOUNT_POLICY` gate: deny password-only SOAP logins for `INTERACTIVE_MFA_REQUIRED` human accounts when MFA is enforced; allow `NON_INTERACTIVE_SERVICE` accounts with audited service-account logging. |
| 4 | **External / WebServer SSO Headers** (`REMOTE_USER` / client cert) | `PSSecurityFilter#authorize` (line 733) → `PSSecurityFilter#handleExternalAuth` (line 1036) → `PSWebServerProvider` | Authenticates from `AUTH_TYPE` / CGI / header principal via `PSWebServerProvider` and immediately mints `SUBJECT` + `pssessionid`. | Check `ACCOUNT_POLICY`: permit direct completion for `SSO_EXTERNAL_MFA` accounts (where upstream proxy/IdP enforces MFA); otherwise require CMS-level MFA step-up before finalizing session. |
| 5 | **Existing Session Rehydration** (`HttpSession.SUBJECT` & `pssessionid`) | `PSSecurityFilter#authorize` (`sess.getAttribute(SUBJECT)` at line 700; `pssessionid` lookup via `handleExistingUserSession` at lines 704–710, 945, 1574) | `HttpSession.SUBJECT` is honored even without consulting `PSUserSessionManager`; `PSUserSessionManager` currently lacks cluster-safe `SESSION_VERSION` validation or mass per-user invalidation across sessions. | Stamp `SESSION_VERSION` from `PSX_ACCOUNT_SECURITY` onto `HttpSession` and `PSUserSession` on finalization; verify `SESSION_VERSION` in `handleAuthenticatedSubject` and `handleExistingUserSession` and add `PSUserSessionManager.releaseSessionsForUser(String)` for password reset / break-glass / admin session revocation. |

### Reverse-Proxy CSRF Gap Note
In `PSSecurityFilter#authorize` (lines 688–693), `isBehindProxy = PSServer.isRequestBehindProxy(request)` short-circuits `checkForCrossSiteRequestForgery(request)` whenever `requestBehindProxy=true`. Because production WebAuthn / HTTPS deployments sit behind a TLS-terminating reverse proxy (`requestBehindProxy=true`), all state-changing `/auth/*` and profile/admin security REST routes must validate `OWASP-CSRFTOKEN` + `Origin`/`Sec-Fetch-Site` against `auth.publicBaseUrl` / `auth.webauthn.allowedOrigins` regardless of `requestBehindProxy`.

---

## 2. Legacy `/v8/*` Anonymous Allowlist Disposition

References found in configuration and filter logic:
- [system-security-conf.xml](../../../../WebUI/src/main/webapp/WEB-INF/config/security/system-security-conf.xml#L23-L24):
  - `<path authType="anonymous">/v8/security/session</path>`
  - `<path authType="anonymous">/v8/system/sites</path>`
- [Owasp.CsrfGuard.properties](../../../../WebUI/src/main/webapp/WEB-INF/classes/Owasp.CsrfGuard.properties#L228-L257) (and mirrored copies in `WebUI/war/...` and `WebUI/src/main/webapp/cm/...`):
  - `org.owasp.csrfguard.unprotected.v8Session` / `rxv8Session`
  - `org.owasp.csrfguard.unprotected.v8Sites` / `rxv8Sites`
- [PSSecurityFilter.java](../../../../system/src/main/java/com/percussion/servlets/PSSecurityFilter.java#L873-L874) and [line 1247](../../../../system/src/main/java/com/percussion/servlets/PSSecurityFilter.java#L1247): explicitly exempts `/v8/security/session` from CSRF check and `isExternalApiRequest`.

### Disposition Decision
- No active `/v8/security/session` or `/v8/system/sites` JAX-RS resource implementations exist in `rest/` or `projects/sitemanage/` source trees, while [web.xml](../../../../WebUI/src/main/webapp/WEB-INF/web.xml#L691-L694) still maps `/v8/*` to `CXFServlet`.
- **Milestone 1 finding:** Record `/v8/security/session` and `/v8/system/sites` as **stale legacy allowlist entries** rather than active browser auth endpoints.
- **Milestone 3 action:** Remove or lock down the anonymous allowlist and CSRFGuard exemptions for `/v8/security/session` and `/v8/system/sites` (returning `401`/`404` for unauthenticated callers) unless a customer integration explicitly opts in, ensuring no unauthenticated REST authentication backdoor bypasses the MFA boundary.

---

## 3. Security-Provider Ownership Matrix

| Provider Class / Type | Backing Store | Password Ownership & Change/Reset Rules | Email & Verification Ownership | MFA Ownership & Enrollment Rules | Unattended / Service Accounts |
|---|---|---|---|---|---|
| **`BackEndTable`** ([PSBackEndTableProvider.java](../../../../system/src/main/java/com/percussion/security/PSBackEndTableProvider.java), [PSUserLogin.java](../../../../projects/sitemanage/src/main/java/com/percussion/user/data/PSUserLogin.java)) | `USERLOGIN` (`USERID VARCHAR(50)`, `PASSWORD VARCHAR(150)`) + `PSX_SUBJECTS` (`ID`, `NAME`, `TYPE`, properties including `sys_email`) + `PSX_ACCOUNT_SECURITY` | **CMS owns password.** Enforce `auth.password.minLength` (default `12`, max `150`) across create, profile change, admin set/reset, and forgot-password reset. | **CMS owns email.** Required for human accounts (created in M2); validated & confirmed via `VERIFY_EMAIL`/`CHANGE_EMAIL` tokens in `PSX_AUTH_TOKEN`. Duplicate normalized emails remain separately flagged in readiness reports and block self-service email lookup when ambiguous. | **CMS owns MFA.** Requires password + (TOTP via `java-otp` OR Passkey via `WebAuthn4J`), plus single-use recovery codes stored in `PSX_MFA_RECOVERY_CODE`. | `rxserver`, `PercussionAdmin`, publishing/automation accounts must be explicitly classified `NON_INTERACTIVE_SERVICE` (excluded from interactive MFA prompts, blocked from interactive browser login where configured). |
| **`Directory` / `JNDI`** ([PSJndiProvider.java](../../../../system/src/main/java/com/percussion/security/PSJndiProvider.java), LDAP / Active Directory) | External LDAP / AD + CMS `PSX_SUBJECTS` + `PSX_ACCOUNT_SECURITY` | **External Directory owns password.** CMS does **not** store passwords (`PSUserService.update` / reset rejects local password edits and directs users to IT/IdP). | **Synced from Directory.** Local email edits in `AccountSection` stay disabled; CMS tracks `VERIFIED_EMAIL` independently (or honors `auth.directory.trustEmailVerified` only when explicitly enabled). Changes to directory email reset verification state. | **CMS owns second factor.** After LDAP bind succeeds in `verifyPrimaryCredentials`, CMS enforces TOTP / Passkey / Recovery Code via `PSX_ACCOUNT_SECURITY` before finalizing CMS session. | Directory bind service account lives in directory config, not `USERLOGIN`; directory integration accounts marked `NON_INTERACTIVE_SERVICE` cannot sign in interactively without admin override. |
| **`WebServer`** ([PSWebServerProvider.java](../../../../system/src/main/java/com/percussion/security/PSWebServerProvider.java), reverse proxy / SSO `REMOTE_USER` / client certs) | Upstream Web Server / Reverse Proxy / IdP | **Upstream IdP/Proxy owns password.** No CMS password changes or forgot-password resets allowed. | Supplied via `sys_email` / proxy attribute sync or verified in `PSX_ACCOUNT_SECURITY`. | **Configurable by policy:** Accounts classified `SSO_EXTERNAL_MFA` accept upstream IdP MFA assertion; accounts not asserting upstream MFA must still satisfy CMS MFA step-up when enforced. | Upstream reverse-proxy machine principals classified `NON_INTERACTIVE_SERVICE` where used for backend automation. |
| **System / Internal Service Accounts** (`SYSTEM_USERS` role / internal server principals) | CMS internal (`rxserver`, `PercussionAdmin`, integration accounts) | Internal/operator managed; must use ≥12 char secrets (minLength policy). | Optional/operator contact address (`PercussionAdmin` ships with blank email in `PSUserService.createPercussionUser`). | **Exempt from interactive browser MFA** (`ACCOUNT_POLICY = NON_INTERACTIVE_SERVICE`), audited on every token/session creation. | Explicitly surfaced in the **Admin Auth & MFA Readiness Report** so no human account is mistakenly left exempt. |
