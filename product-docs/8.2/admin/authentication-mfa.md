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
| `auth.mfa.issuer` | `Percussion CMS` | TOTP authenticator issuer label. |
| `auth.password.minLength` | `12` | Minimum password length (allowed range `8`–`150`). |
| `auth.publicBaseUrl` | *(empty)* | Canonical external HTTPS base URL (for example `https://cms.example.com/Rhythmyx`). |
| `auth.webauthn.rpId` | *(empty)* | WebAuthn Relying Party hostname without scheme or port (for example `example.com`). |
| `auth.webauthn.rpName` | `Percussion CMS` | Human-readable WebAuthn Relying Party name. |
| `auth.webauthn.allowedOrigins` | *(empty)* | Comma-separated HTTPS origins (for example `https://cms.example.com`). Localhost HTTP is warned as development-only. |
| `auth.token.emailVerifyTtlMinutes` | `30` | Lifetime for email verification tokens. |
| `auth.token.passwordResetTtlMinutes` | `20` | Lifetime for password reset tokens. |
| `auth.token.adminBreakGlassTtlMinutes` | `60` | Lifetime for admin break-glass recovery tokens. |
| `auth.email.required` | `true` | Require valid email addresses on internal human accounts (`PercussionAdmin` and `rxserver` service accounts are exempt). |
| `auth.email.uniqueForRecovery` | `false` | When `true`, suppress automated Forgot Username reminders if multiple verified accounts share one email address. |
| `auth.directory.trustEmailVerified` | `false` | Trust directory-synced emails as pre-verified (defaults `false`; directory email changes clear verification when false). |
| `auth.rateLimit.forgotUsernamePerIpPerHour` | `15` | Hourly Forgot Username request limit per client IP (returns HTTP `429` above threshold). |
| `auth.rateLimit.forgotUsernamePerEmailPerHour` | `5` | Hourly Forgot Username lookup limit per normalized email address (silently returns generic acknowledgment). |
| `auth.rateLimit.emailVerifyResendPerUserPerHour` | `5` | Hourly verification link issuance limit per user account. |

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

## Running the Admin Auth & MFA Readiness check

1. Sign in as an **Admin** user and open **Admin → System Tools → Auth & MFA Readiness**.
2. Review **Infrastructure prerequisites** (`AUTH_MAIL`, `PUBLIC_BASE_URL`, `WEBAUTHN_CONFIG`,
   `MFA_MODE`, `PASSWORD_POLICY`, `ENCRYPTION_KEY`, and `AUTH_SCHEMA`).
3. Use **Send test auth email** to verify synchronous SMTP delivery to an administrator inbox.
4. Filter **Accounts requiring remediation** (`MISSING_EMAIL`, `MALFORMED_EMAIL`,
   `DUPLICATE_EMAIL`, `UNVERIFIED_EMAIL`, `NO_MFA_ENROLLED`) and resolve email hygiene before
   moving `auth.mfa.mode` to `required`.

## Related

- [Users, roles & security](id:admin-users-roles)
- [Server Operations](id:admin-server-ops)
