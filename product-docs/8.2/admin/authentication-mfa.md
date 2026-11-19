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
