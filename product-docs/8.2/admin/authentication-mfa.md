---
id: admin-authentication-mfa
title: Authentication, MFA, and Recovery Readiness
description: Configuring transactional auth mail, WebAuthn origins, cluster key fingerprints, and running the Admin Auth & MFA Readiness report
version: "8.2"
order: 43
tags: [admin, security, operations]
---

# Authentication, MFA, and Recovery Readiness

Percussion CMS 8.2 provides infrastructure readiness diagnostics, synchronous transactional
authentication email configuration, and account email/MFA inventory under **Admin → System
Tools → Auth & MFA Readiness**.

## Rollout policy (`server.properties`)

Authentication and recovery settings live in `rxconfig/Server/server.properties`:

| Property | Default | Purpose |
|---|---|---|
| `auth.mfa.mode` | `optional` | Rollout posture: `off`/`disabled`, `optional` (QA/rollout default), `cohort`, or `required`. |
| `auth.mfa.requiredCohorts` | *(empty)* | Comma-separated role or user cohort names requiring MFA when `auth.mfa.mode=cohort`. |
| `auth.mfa.issuer` | `Percussion CMS` | TOTP authenticator issuer label. |
| `auth.mfa.totpAllowedDriftSteps` | `1` | Adjacent 30-second TOTP time steps tolerated during verification (`±1` = ±30s, single-use anti-replay tracked). |
| `auth.mfa.maxFailedAttempts` | `5` | Consecutive failed MFA verifications before temporary account MFA lockout. |
| `auth.mfa.lockoutMinutes` | `15` | Duration in minutes for MFA verification lockouts (HTTP `423`). |
| `auth.mfa.challengeTtlMinutes` | `10` | Lifetime in minutes for pending sign-in MFA challenge/setup transactions (`PERC_AUTH_TX`). |
| `auth.mfa.stepUpWindowMinutes` | `5` | Recent-MFA step-up window in minutes before sensitive profile changes prompt re-verification (HTTP `428`). |
| `auth.password.minLength` | `12` | Minimum password length (allowed range `8`–`150`). |
| `auth.publicBaseUrl` | *(empty)* | Canonical external HTTPS base URL (for example `https://cms.example.com/Rhythmyx`). |
| `auth.webauthn.rpId` | *(empty)* | WebAuthn Relying Party hostname without scheme or port (for example `example.com`). |
| `auth.webauthn.rpName` | `Percussion CMS` | Human-readable WebAuthn Relying Party name. |
| `auth.webauthn.allowedOrigins` | *(empty)* | Comma-separated HTTPS origins (for example `https://cms.example.com`). Localhost HTTP is warned as development-only. |
| `auth.webauthn.userVerification` | `preferred` | WebAuthn user-verification requirement (`required`, `preferred`, or `discouraged`) for passkey registration and challenge assertions. |
| `auth.token.emailVerifyTtlMinutes` | `30` | Lifetime for email verification tokens. |
| `auth.token.passwordResetTtlMinutes` | `20` | Lifetime for password reset tokens. |
| `auth.token.adminBreakGlassTtlMinutes` | `60` | Lifetime for admin break-glass recovery tokens. |
| `auth.email.required` | `true` | Require valid email addresses on internal human accounts (`PercussionAdmin` and `rxserver` service accounts are exempt). |
| `auth.email.uniqueForRecovery` | `false` | When `true`, suppress automated Forgot Username reminders if multiple verified accounts share one email address. |
| `auth.directory.trustEmailVerified` | `false` | Trust directory-synced emails as pre-verified (defaults `false`; directory email changes clear verification when false). |
| `auth.rateLimit.forgotUsernamePerIpPerHour` | `15` | Hourly Forgot Username request limit per client IP (returns HTTP `429` above threshold). |
| `auth.rateLimit.forgotUsernamePerEmailPerHour` | `5` | Hourly Forgot Username lookup limit per normalized email address (silently returns generic acknowledgment). |
| `auth.rateLimit.passwordResetPerIpPerHour` | `15` | Hourly Forgot Password request limit per client IP (returns HTTP `429` above threshold). |
| `auth.rateLimit.passwordResetPerAccountPerHour` | `5` | Hourly Forgot Password request limit per normalized account identifier (silently returns generic acknowledgment). |
| `auth.rateLimit.emailVerifyResendPerUserPerHour` | `5` | Hourly verification link issuance limit per user account. |
| `auth.rateLimit.mfaVerifyPerIpPerMinute` | `20` | Per-minute MFA challenge/step-up limit per client IP (returns HTTP `429`). |
| `auth.rateLimit.mfaVerifyPerUserPerMinute` | `10` | Per-minute MFA challenge/step-up limit per user account (returns HTTP `429`). |

## Verified email lifecycle and staged email changes

Percussion CMS tracks email verification independently from legacy role subject attributes in
`PSX_ACCOUNT_SECURITY`:

- **Staged internal email changes:** Internal users update their email under **My profile → Account**.
  When changing an email (or verifying via **My profile → Account** / `POST /services/auth/profile/email/change`),
  Percussion keeps the existing verified email address active in `PENDING_EMAIL` deferral until the user
  opens the verification link and clicks **Confirm email address**. When replacing an existing verified
  address, a security notification is also sent to the previous verified address.
- **Scanner-safe verification links:** `GET /auth/verify-email?token=...` renders a confirmation screen
  without consuming the token, preventing email security link scanners from invalidating links before the
  user clicks. Verification only executes on `POST /auth/verify-email` with an origin and CSRF check,
  backed by `auth.publicBaseUrl`.
- **Directory accounts:** Directory (`DIRECTORY`) account email remains read-only in **My profile → Account**.
  Whenever a directory email changes, Percussion clears prior verification unless
  `auth.directory.trustEmailVerified=true`.
- **Profile email APIs:** Signed-in users query and stage verification through
  `GET /services/auth/profile/email`, `POST /services/auth/profile/email/change`, and
  `POST /services/auth/profile/email/resend` (always scoped to the active session user).

## Forgot Username recovery

The sign-in page includes a **Forgot username?** link (`/auth/forgot-username`). Submitting an address
via `POST /auth/forgot-username`:

- Always returns the same generic acknowledgment message regardless of whether the address exists, is
  unverified, or matches a user account (protecting against account enumeration).
- Sends a username reminder email synchronously via `PSAuthMailService` **only** to **verified** email
  addresses stored in `PSX_ACCOUNT_SECURITY`.
- Enforces configurable per-IP and per-email Bucket4j rate limits.

## Dedicated synchronous auth mail (`auth.mail.*`)

Transactional authentication emails (verification links, password reset links, and admin test
messages) use a dedicated synchronous SMTP client rather than the asynchronous workflow mail queue.

- **`ENABLE_TEST_RUN_MODE` isolation:** Workflow's `ENABLE_TEST_RUN_MODE` property in
  `rxconfig/Workflow/rxworkflow.properties` is **never honored** for authentication email, ensuring
  verification/reset links and codes are never written to `notifications.log`.
- **Fallback behavior:** When `auth.mail.host` is blank and `auth.mail.fallbackWorkflowSmtp=true`
  (default), Percussion CMS reuses `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`,
  `SMTP_BOUNCEADDR`, `SMTP_TLSENABLED`, and `SMTP_SSLPORT` from
  `rxconfig/Workflow/rxworkflow.properties`. Set dedicated `auth.mail.*` keys to override or isolate
  authentication mail.

| Property | Default | Purpose |
|---|---|---|
| `auth.mail.host` | *(empty)* | SMTP server hostname. |
| `auth.mail.port` | `587` | SMTP server port (`1`–`65535`). |
| `auth.mail.username` | *(empty)* | Optional SMTP authentication user. |
| `auth.mail.password` | *(empty)* | SMTP password (supports `ENC(...)` encrypted properties via `rxconfig/secure/.key`). |
| `auth.mail.from` | *(empty)* | Sender email address (falls back to `SMTP_BOUNCEADDR` when workflow fallback is active). |
| `auth.mail.fromName` | `Percussion CMS` | Display name for the `From` header. |
| `auth.mail.startTls` | `true` | Enable STARTTLS upgrade. |
| `auth.mail.startTlsRequired` | `true` | Require STARTTLS before sending authentication emails. |
| `auth.mail.ssl` | `false` | Enable implicit SMTPS/SSL on connect. |
| `auth.mail.sslCheckServerIdentity` | `true` | Verify SMTP TLS server certificate hostname identity. |
| `auth.mail.connectTimeoutMs` | `10000` | Socket connection timeout in milliseconds. |
| `auth.mail.socketTimeoutMs` | `10000` | Socket read timeout in milliseconds. |
| `auth.mail.fallbackWorkflowSmtp` | `true` | Reuse `rxworkflow.properties` SMTP host/credentials when `auth.mail.host` is empty. |

## Cluster encryption key fingerprint

Encrypted server properties and TOTP secrets depend on `rxconfig/secure/.key`. In multi-node
clusters, open **Admin → System Tools → Auth & MFA Readiness** on each node and confirm the
**Cluster key fingerprint** (`sha256:...` prefix) matches across all nodes before enforcing MFA.

## Security boundary and session hardening

Percussion CMS separates credential verification from authenticated session creation:

- **Two-phase sign-in & MFA transactions:** Primary password or directory verification issues a short-lived opaque `PERC_AUTH_TX` transaction cookie instead of an authenticated CMS session whenever MFA verification (`/auth/challenge`) or mandatory enrollment (`/auth/setup`) is required.
- **Session ID rotation & global session invalidation (`SESSION_VERSION`):** Completing authentication calls `HttpServletRequest.changeSessionId()` to mitigate session fixation and binds the user's current `SESSION_VERSION` from `PSX_ACCOUNT_SECURITY`. Security-sensitive changes (disabling MFA, password changes, recovery revocations) increment `SESSION_VERSION` and release active sessions across the cluster.
- **Non-interactive Basic Auth & `RX_USEBASICAUTH` hardening:** Human accounts enrolled in MFA or required by policy receive HTTP `403` when attempting HTTP `Basic` authentication or `RX_USEBASICAUTH` bypass headers. Designated service accounts (`rxserver`, `PercussionAdmin`) remain non-interactive (`NON_INTERACTIVE_SERVICE`) where required by internal automation.
- **CSRF & origin validation:** State-changing `/auth/*` endpoints enforce `PSAuthCsrfGuard` origin checks (against request host, `auth.publicBaseUrl`, and `auth.webauthn.allowedOrigins`) plus double-submit/session CSRF tokens without reverse-proxy exemptions.

## Two-step verification (TOTP authenticator app and recovery codes)

Users manage authenticator enrollment under **My profile → Security**:

- **Authenticator app (TOTP):** Enrolling scans an `otpauth://` QR code or copies a Base32 setup key, encrypted at rest using AES-256-GCM derived from `rxconfig/secure/.key`. Confirming with a valid 6-digit code activates TOTP with anti-replay time-step tracking (`PSX_MFA_TOTP`).
- **Single-use recovery codes:** Initial TOTP enrollment issues 10 salted PBKDF2-HMAC-SHA256 hashed recovery codes (`XXXXX-XXXXX`) shown **once** for copying or downloading as `.txt`. Each code can only be used once during `/auth/challenge` or step-up verification; users can regenerate a fresh batch at any time (invalidating unused prior codes).
- **Mandatory enrollment on sign-in (`/auth/setup`):** When `auth.mfa.mode=required` or the user belongs to `auth.mfa.requiredCohorts` without an enrolled factor, signing in redirects through `/auth/setup` before an application session is issued. Users can complete mandatory onboarding with either an authenticator app (TOTP) or a WebAuthn passkey.
- **Last-factor protection:** When policy requires MFA for an account, Percussion prevents removing the user's last remaining MFA factor (`409 CONFLICT` in REST and disabled UI controls).

## Passkeys (WebAuthn / FIDO2)

Percussion CMS supports phishing-resistant WebAuthn / FIDO2 passkeys (platform biometrics, synced passkeys, and hardware security keys) alongside TOTP:

- **Profile enrollment and management:** Signed-in users add, rename, and remove passkeys under **My profile → Security**. Adding a passkey when no active recovery codes exist generates initial one-time recovery codes (`XXXXX-XXXXX`) once.
- **Sign-in challenge & mandatory onboarding:** During sign-in, enrolled users can verify with a passkey via `/auth/challenge` (`POST /auth/challenge/passkey/options` and `POST /auth/challenge/passkey/verify`) or fall back to TOTP/recovery codes. Unenrolled users subject to mandatory MFA can enroll a passkey directly on `/auth/setup` (`POST /auth/setup/passkey/start` and `POST /auth/setup/passkey/finish`).
- **Cryptographic and origin controls:** Server verification validates `ES256`, `RS256`, and `EdDSA` COSE public keys against configured `auth.webauthn.rpId`, `auth.webauthn.allowedOrigins` (or HTTPS canonical origin), single-use `PERC_AUTH_TX` challenge tokens, and `auth.webauthn.userVerification`. Synced/multi-device passkeys (`BE`/`BS` flags with `signCount=0`) remain compatible while non-zero counter regressions are rejected.

## Step-up authentication for sensitive self-service changes

When a signed-in user already has an enrolled MFA factor, sensitive actions (changing a verified email address, disabling TOTP, renaming or deleting a passkey where required, or regenerating recovery codes) require a recent MFA verification within `auth.mfa.stepUpWindowMinutes`. If step-up assurance has expired, REST returns HTTP `428` (`STEP_UP_REQUIRED`), prompting the user inline to verify with a passkey, TOTP code, or recovery code before automatically retrying the action.

## Profile MFA REST API (`/services/auth/mfa/*`)

Authenticated users query and manage TOTP, passkeys, and recovery codes through:

- `GET /services/auth/mfa/status` — Current user's MFA policy requirement, TOTP/passkey counts, remaining recovery codes, `canDisableTotp`, and step-up window status.
- `POST /services/auth/mfa/step-up` — Verify a TOTP or recovery code (`MfaStepUpRequest`) to refresh step-up assurance.
- `POST /services/auth/mfa/step-up/passkey/start` — Begin passkey step-up authentication by issuing WebAuthn assertion options for the signed-in user's active passkeys.
- `POST /services/auth/mfa/step-up/passkey/verify` — Complete passkey step-up verification (`PasskeyStepUpVerifyRequest`) and refresh the step-up window.
- `POST /services/auth/mfa/totp/start` — Begin pending TOTP enrollment (`TotpEnrollStartRequest`) returning setup URI and manual key.
- `POST /services/auth/mfa/totp/confirm` — Confirm pending TOTP setup (`TotpEnrollConfirmRequest`) and receive initial one-time recovery codes.
- `DELETE /services/auth/mfa/totp` — Disable TOTP authenticator (enforces step-up and last-factor policy).
- `GET /services/auth/mfa/passkeys` — List enrolled passkeys (`PasskeyListResponse`) and whether removing the last passkey is permitted.
- `POST /services/auth/mfa/passkeys/register/start` — Begin passkey registration (`PasskeyOptionsRequest`), returning `challengeTxId` and WebAuthn creation options.
- `POST /services/auth/mfa/passkeys/register/finish` — Verify passkey attestation (`PasskeyRegisterFinishRequest`), save the credential, and return one-time recovery codes when first enrolling MFA.
- `PUT /services/auth/mfa/passkeys/{passkeyId}` — Rename an enrolled passkey (`PasskeyRenameRequest`).
- `DELETE /services/auth/mfa/passkeys/{passkeyId}` — Remove an enrolled passkey (enforces step-up and last-factor policy).
- `POST /services/auth/mfa/recovery-codes/regenerate` — Invalidate prior codes and generate 10 fresh recovery codes (enforces step-up).

## Audit logging & secret redaction

Authentication and MFA security events record structured entries under `AuthenticationErrorCodes` (`1005`–`1016`, including MFA challenge success/failure, lockouts, factor enrollment/removal, recovery code consumption/regeneration, policy blocks, password resets, admin break-glass recoveries, and session version invalidations). `AuditRedactor` automatically scrubs `otpauth://` URIs, Base32 shared secrets, recovery codes, and verification query parameters before writing audit logs.

## Password policy and constant-time verification

Percussion CMS enforces a shared password policy across internal user creation, admin password updates, self-service password changes (**My profile → Security**), and password reset (`/auth/reset-password`):

- **Length requirements (`auth.password.minLength`):** Defaults to `12` characters and is clamped to the allowed range `8`–`150` with a maximum length of `150` characters. Admins see the active minimum in **Admin → System Tools → Auth & MFA Readiness**, and reset screens read the effective bounds from server configuration.
- **Constant-time password digest comparison:** Internal password verification compares PBKDF2-HMAC-SHA512 password digests using constant-time byte comparison (`MessageDigest.isEqual`) with malformed Base64 and minimum-length guards.
- **Multi-session revocation:** Self-service password changes and resets increment the account's `SESSION_VERSION` in `PSX_ACCOUNT_SECURITY` and invalidate existing sessions for that user via `PSUserSessionManager` while keeping the active self-service session current when changing a password from **My profile → Security**.

## Forgot Password and self-service password reset

The sign-in page provides a **Forgot password?** link (`/auth/forgot-password`):

- **Enumeration-safe lookup & directory guidance:** Submitting a username or verified email address via `POST /auth/forgot-password` always returns the same generic acknowledgment regardless of whether the account exists or holds a verified email. If the matched account is directory/SSO-managed with a verified email, Percussion sends identity-provider guidance instead of a local password reset token.
- **Scanner-safe confirmation flow:** `GET /auth/reset-password?token=...` renders the new-password form without consuming the token, protecting links from email security scanners. `POST /auth/reset-password` validates the submitted password against the active password policy **before** atomically consuming the single-use hashed reset token, updates the internal password, invalidates existing sessions via `SESSION_VERSION`, preserves enrolled MFA factors (so password resets never bypass MFA), and logs `PASSWORD_RESET_EVENT` (`1015`).

## Admin break-glass MFA recovery and last-administrator lockout runbook

Administrators can assist locked-out human accounts from **Admin → System Tools → Auth & MFA Readiness → Break-glass MFA recovery** or `POST /services/auth/admin/recovery` (`AdminRecoveryRequest` / `AdminRecoveryResponse`):

- **Mandatory guardrails:** Only users in the `Admin` role can initiate recovery, administrators cannot self-recover, MFA-enrolled admins must have a current step-up verification within `auth.mfa.stepUpWindowMinutes` (REST returns HTTP `428` `STEP_UP_REQUIRED` and prompts inline before retrying), and every request requires an identity verification note of at least `10` characters recorded under `ADMIN_RECOVERY_EVENT` (`1016`).
- **Recovery link vs. immediate MFA reset:**
  - **Emailed recovery link (default):** Sends a scanner-safe confirmation link (`/auth/admin-recovery?token=...`, TTL `auth.token.adminBreakGlassTtlMinutes`, default `60` minutes) to the user's verified email address. Clicking **Confirm MFA recovery** on the confirmation page (`POST /auth/admin-recovery`) clears TOTP, passkeys, and recovery codes, increments `SESSION_VERSION`, and ends existing sessions.
  - **Immediate reset (`resetImmediately=true`):** Immediately removes the target user's TOTP, passkey, and recovery code credentials and bumps `SESSION_VERSION` without waiting for email link redemption (useful when the user also lost access to their verified inbox). Users whose policy requires MFA are guided through `/auth/setup` on their next sign-in.

### Last-administrator MFA lockout runbook

If **every** account with the `Admin` role has lost access to all enrolled authenticators, passkeys, and offline recovery codes:

1. **Confirm authorization and maintenance window** on the CMS application server host before touching server configuration or database tables.
2. **Release MFA enforcement for recovery:**
   - Either set `auth.mfa.mode=optional` (or remove the locked-out administrator from `auth.mfa.requiredCohorts`) in `rxconfig/Server/server.properties`, **or**
   - Using authorized database maintenance tooling, remove only the locked-out administrator's credential rows from `PSX_MFA_TOTP`, `PSX_MFA_PASSKEY`, and `PSX_MFA_RECOVERY_CODE` and increment `SESSION_VERSION` for that user in `PSX_ACCOUNT_SECURITY`, capturing an internal change/audit ticket.
3. **Restart the CMS server** (`PercussionServer`) if `server.properties` was modified so configuration changes take effect.
4. **Sign in as the recovered administrator**, immediately enroll a fresh TOTP authenticator or passkey under **My profile → Security**, record the newly generated one-time recovery codes offline, and ensure a second break-glass administrator account also has enrolled MFA and offline recovery codes.
5. **Restore strict policy:** Revert `auth.mfa.mode` (for example `required` or `cohort`) in `rxconfig/Server/server.properties`, restart `PercussionServer` if changed, and verify readiness under **Admin → System Tools → Auth & MFA Readiness**.

## Running the Admin Auth & MFA Readiness check

1. Sign in as an **Admin** user and open **Admin → System Tools → Auth & MFA Readiness**.
2. Review **Infrastructure prerequisites** (`AUTH_MAIL`, `PUBLIC_BASE_URL`, `WEBAUTHN_CONFIG`,
   `MFA_MODE`, `PASSWORD_POLICY`, `ENCRYPTION_KEY`, and `AUTH_SCHEMA`).
3. Use **Send test auth email** to verify synchronous SMTP delivery to an administrator inbox.
4. Filter **Accounts requiring remediation** (`MISSING_EMAIL`, `MALFORMED_EMAIL`,
   `DUPLICATE_EMAIL`, `UNVERIFIED_EMAIL`, `NO_MFA_ENROLLED`) and resolve email hygiene before
   moving `auth.mfa.mode` to `required`.

## Pilot cohort rollout and audited rollback procedure

Move deployments from optional mode to universal enforcement in controlled stages so existing automation and un-onboarded editors are never locked out unexpectedly:

1. **Stage 0 — Baseline & remediation (`auth.mfa.mode=optional`):**
   - Verify synchronous SMTP test delivery, canonical HTTPS `auth.publicBaseUrl`, valid `auth.webauthn.rpId` / `allowedOrigins`, and identical **Cluster key fingerprint** values across all CMS cluster nodes.
   - Remediate `MISSING_EMAIL`, `MALFORMED_EMAIL`, `DUPLICATE_EMAIL`, and `UNVERIFIED_EMAIL` accounts in **Admin → System Tools → Auth & MFA Readiness**.
   - Tag unattended publishing/integration accounts under `NON_INTERACTIVE_SERVICE` (`PSX_ACCOUNT_SECURITY.ACCOUNT_POLICY`), keeping human operators on `INTERACTIVE_MFA_REQUIRED` (or `SSO_EXTERNAL_MFA` only where upstream IdP MFA is guaranteed).
   - Ensure **at least two** administrator accounts have enrolled MFA and stored offline recovery codes before turning on enforcement.
2. **Stage 1 — Pilot cohort enforcement (`auth.mfa.mode=cohort`):**
   - Set `auth.mfa.mode=cohort` and `auth.mfa.requiredCohorts=<cohort-or-role-list>` in `rxconfig/Server/server.properties` (matches case-insensitively against `PSX_ACCOUNT_SECURITY.MFA_ENFORCEMENT_COHORT`, CMS role names such as `Admin`, or explicit usernames).
   - Restart `PercussionServer` and validate interactive browser sign-in (`/auth/setup` mandatory enrollment for unenrolled pilot users, `/auth/challenge` TOTP + passkey + recovery code sign-in for enrolled users), inline step-up (`428`), `HTTP 403` denial of Basic Auth / `sys_use_basic_auth` and SOAP password-only logins for enforced human accounts, and **Admin break-glass recovery**.
3. **Stage 2 — Global interactive enforcement (`auth.mfa.mode=required`):**
   - Once pilot cohorts reach `PASS` readiness coverage and support procedures are validated, set `auth.mfa.mode=required` in `rxconfig/Server/server.properties` and restart `PercussionServer`.
4. **Non-destructive rollback (`optional`, `disabled`, or `off`):**
   - Setting `auth.mfa.mode=optional` lets unenrolled users sign in normally while **keeping MFA active for users who already enrolled**.
   - Setting `auth.mfa.mode=disabled` (or `off`) suspends interactive MFA challenges and re-allows non-interactive Basic/SOAP channels temporarily during an emergency **without deleting** `PSX_MFA_TOTP`, `PSX_MFA_PASSKEY`, or `PSX_MFA_RECOVERY_CODE` rows.
   - Restart `PercussionServer` after changing `server.properties`, review `PSX_SYSTEM_AUDIT_LOG` (`AuthenticationErrorCodes` `1005`–`1016`) to investigate any incident, and restore `cohort` or `required` once remediated.

## Related

- [Users, roles & security](id:admin-users-roles)
- [Server Operations](id:admin-server-ops)
