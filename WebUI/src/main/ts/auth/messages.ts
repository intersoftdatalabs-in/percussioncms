/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export const AUTH_MSG = {
  BACK_TO_SIGN_IN: "perc.ui.auth.modern@Back to sign in",
  VERIFY_TITLE: "perc.ui.auth.modern@Confirm your email address",
  VERIFY_SUBTITLE:
    "perc.ui.auth.modern@Click below to finish verifying your email address for Percussion CMS.",
  VERIFY_SUBMIT: "perc.ui.auth.modern@Confirm email address",
  VERIFY_SUBMITTING: "perc.ui.auth.modern@Verifying…",
  VERIFY_MISSING_TOKEN:
    "perc.ui.auth.modern@No verification token was provided in this link.",
  VERIFY_GENERIC_ERROR:
    "perc.ui.auth.modern@Unable to verify your email address. Request a new link from your profile.",
  FORGOT_USERNAME_TITLE: "perc.ui.auth.modern@Recover your username",
  FORGOT_USERNAME_SUBTITLE:
    "perc.ui.auth.modern@Enter your verified account email address and we will email your username.",
  FORGOT_USERNAME_EMAIL_LABEL: "perc.ui.auth.modern@Email address",
  FORGOT_USERNAME_SUBMIT: "perc.ui.auth.modern@Send username reminder",
  FORGOT_USERNAME_SUBMITTING: "perc.ui.auth.modern@Sending…",
  FORGOT_USERNAME_EMAIL_INVALID:
    "perc.ui.auth.modern@Enter a valid email address.",
  FORGOT_USERNAME_GENERIC_ERROR:
    "perc.ui.auth.modern@Unable to process your request right now. Try again shortly.",
  MFA_CHALLENGE_TITLE: "perc.ui.auth.modern@Two-step verification",
  MFA_CHALLENGE_SUBTITLE:
    "perc.ui.auth.modern@Enter the 6-digit code from your authenticator app to finish signing in.",
  MFA_CHALLENGE_RECOVERY_SUBTITLE:
    "perc.ui.auth.modern@Enter one of your single-use recovery codes to finish signing in.",
  MFA_CHALLENGE_TOTP_LABEL: "perc.ui.auth.modern@Verification code",
  MFA_CHALLENGE_RECOVERY_LABEL: "perc.ui.auth.modern@Recovery code",
  MFA_CHALLENGE_SUBMIT: "perc.ui.auth.modern@Verify and sign in",
  MFA_CHALLENGE_SUBMITTING: "perc.ui.auth.modern@Verifying…",
  MFA_CHALLENGE_USE_RECOVERY: "perc.ui.auth.modern@Use a recovery code instead",
  MFA_CHALLENGE_USE_TOTP:
    "perc.ui.auth.modern@Use an authenticator code instead",
  MFA_CHALLENGE_EXPIRED:
    "perc.ui.auth.modern@Your verification session has expired or is invalid. Return to sign in to try again.",
  MFA_CHALLENGE_CODE_REQUIRED:
    "perc.ui.auth.modern@Enter a verification code.",
  MFA_CHALLENGE_GENERIC_ERROR:
    "perc.ui.auth.modern@Verification failed. Check your code and try again.",
  MFA_CANCEL: "perc.ui.auth.modern@Cancel sign-in",
  MFA_SETUP_TITLE:
    "perc.ui.auth.modern@Set up two-step verification",
  MFA_SETUP_SUBTITLE:
    "perc.ui.auth.modern@Your organization requires two-step verification. Scan the QR code with your authenticator app, then enter a 6-digit code.",
  MFA_SETUP_EXPIRED:
    "perc.ui.auth.modern@Your setup session has expired or is invalid. Return to sign in to start again.",
  MFA_SETUP_LOADING: "perc.ui.auth.modern@Preparing authenticator setup…",
  MFA_SETUP_RETRY: "perc.ui.auth.modern@Try again",
  MFA_SETUP_MANUAL_LABEL: "perc.ui.auth.modern@Manual setup key",
  MFA_SETUP_COPY_KEY: "perc.ui.auth.modern@Copy setup key",
  MFA_SETUP_COPIED: "perc.ui.auth.modern@Copied",
  MFA_SETUP_CODE_LABEL: "perc.ui.auth.modern@6-digit verification code",
  MFA_SETUP_CONFIRM: "perc.ui.auth.modern@Verify and activate",
  MFA_SETUP_CONFIRMING: "perc.ui.auth.modern@Activating…",
  MFA_SETUP_RECOVERY_TITLE:
    "perc.ui.auth.modern@Save your recovery codes",
  MFA_SETUP_RECOVERY_BODY:
    "perc.ui.auth.modern@Save these single-use recovery codes in a safe place. Each code works once if you lose access to your authenticator app.",
  MFA_SETUP_COPY_CODES: "perc.ui.auth.modern@Copy recovery codes",
  MFA_SETUP_DOWNLOAD_CODES: "perc.ui.auth.modern@Download (.txt)",
  MFA_SETUP_CONTINUE: "perc.ui.auth.modern@Continue to Percussion CMS",
  MFA_PASSKEY_VERIFY_BTN: "perc.ui.auth.modern@Verify with passkey",
  MFA_PASSKEY_WAITING: "perc.ui.auth.modern@Waiting for passkey…",
  MFA_PASSKEY_OR_CODE: "perc.ui.auth.modern@or enter a code below",
  MFA_PASSKEY_ABORTED:
    "perc.ui.auth.modern@Passkey verification was cancelled or failed. Try again or use a verification code.",
  MFA_SETUP_MODE_TOTP: "perc.ui.auth.modern@Authenticator app",
  MFA_SETUP_MODE_PASSKEY: "perc.ui.auth.modern@Passkey",
  MFA_SETUP_PASSKEY_SUBTITLE:
    "perc.ui.auth.modern@Create a passkey saved on this device or your security key for faster, phishing-resistant verification.",
  MFA_SETUP_PASSKEY_LABEL: "perc.ui.auth.modern@Passkey name (optional)",
  MFA_SETUP_PASSKEY_PLACEHOLDER: "perc.ui.auth.modern@e.g. Work Laptop or YubiKey",
  MFA_SETUP_PASSKEY_BTN: "perc.ui.auth.modern@Register passkey",
  MFA_SETUP_PASSKEY_BUSY: "perc.ui.auth.modern@Follow your browser prompt…",
} as const;
