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

export type AuthView =
  | "verify-email"
  | "forgot-username"
  | "mfa-challenge"
  | "mfa-setup";

export interface MfaBootstrapState {
  hasValidTx: boolean;
  txType: string;
  totpAvailable: boolean;
  passkeyAvailable: boolean;
  recoveryCodeAvailable: boolean;
}

export interface AuthBootstrap {
  view: AuthView;
  token: string;
  authCsrfToken: string;
  csrfTokenName: string;
  csrfTokenValue: string;
  contextPath: string;
  locale: string;
  textDir: "ltr" | "rtl";
  mfaState?: MfaBootstrapState;
}

export interface VerifyEmailResponse {
  verified: boolean;
  status: string;
  message: string;
}

export interface ForgotUsernameResponse {
  accepted: boolean;
  message: string;
}

export interface MfaChallengeResponse {
  authenticated: boolean;
  redirectUrl: string;
  recoveryCodesRemaining: number;
  message: string;
}

export interface TotpSetupStartResponse {
  totpId: string;
  otpauthUri: string;
  manualSecret: string;
  issuer: string;
  accountName: string;
  digits: number;
  periodSeconds: number;
}

export interface TotpSetupConfirmResponse {
  enrolled: boolean;
  recoveryCodes: string[];
  redirectUrl: string;
  message: string;
}

export interface AuthCancelResponse {
  cancelled: boolean;
  loginUrl: string;
}
