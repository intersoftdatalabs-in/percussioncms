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
} as const;
