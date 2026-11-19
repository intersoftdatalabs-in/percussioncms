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

export type AuthCheckStatus = "PASS" | "WARN" | "FAIL";

export interface AuthInfraCheck {
  code: string;
  label: string;
  status: AuthCheckStatus;
  summary: string;
  remediation: string;
}

export interface AuthAccountReadinessSummary {
  totalAccounts: number;
  internalAccounts: number;
  directoryAccounts: number;
  readyCount: number;
  missingEmailCount: number;
  malformedEmailCount: number;
  duplicateEmailCount: number;
  unverifiedEmailCount: number;
  mfaEnrolledCount: number;
  totpEnrolledCount: number;
  passkeyEnrolledCount: number;
}

export interface AuthAccountReadinessItem {
  userName: string;
  providerType: string;
  email: string;
  emailVerified: boolean;
  mfaEnrollmentState: string;
  totpCount: number;
  passkeyCount: number;
  recoveryCodeCount: number;
  issues: string[];
}

export interface AuthReadinessReport {
  generatedAt: string;
  mfaMode: string;
  passwordMinLength: number;
  mailConfigured: boolean;
  mailSource: string;
  clusterKeyFingerprint: string;
  overallStatus: AuthCheckStatus;
  infraChecks: AuthInfraCheck[];
  accountSummary: AuthAccountReadinessSummary;
  accountIssues: AuthAccountReadinessItem[];
}

export interface AuthMailTestResult {
  sent: boolean;
  recipient: string;
  source: string;
  message: string;
}

export interface ProfileEmailStatus {
  userName: string;
  email: string;
  emailVerified: boolean;
  emailVerifiedAt: string | null;
  pendingEmail: string;
  emailEditable: boolean;
  emailRequired: boolean;
  providerType: string;
}

export interface ProfileEmailActionResult {
  verificationSent: boolean;
  appliedImmediately: boolean;
  email: string;
  pendingEmail: string;
  emailVerified: boolean;
  message: string;
}

export interface ProfileMfaStatus {
  userName: string;
  mfaMode: string;
  mfaRequired: boolean;
  totpEnabled: boolean;
  passkeyCount: number;
  recoveryCodesRemaining: number;
  canDisableTotp: boolean;
  stepUpActive: boolean;
  stepUpExpiresAt: string | null;
}

export interface MfaStepUpResponse {
  verified: boolean;
  expiresAtEpochMs: number;
  message: string;
}

export interface TotpEnrollStartResponse {
  totpId: string;
  otpauthUri: string;
  manualSecret: string;
  issuer: string;
  accountName: string;
  digits: number;
  periodSeconds: number;
}

export interface TotpEnrollConfirmResponse {
  enrolled: boolean;
  recoveryCodes: string[];
  message: string;
}

export interface RecoveryCodesRegenerateResponse {
  recoveryCodes: string[];
  message: string;
}

export interface MfaActionResult {
  success: boolean;
  message: string;
}

export interface PasskeySummary {
  passkeyId: string;
  label: string;
  aaguid: string | null;
  transports: string[];
  backupEligible: boolean;
  backupState: boolean;
  createdAt: string | null;
  lastUsedAt: string | null;
}

export interface PasskeyListResponse {
  passkeys: PasskeySummary[];
  canDeleteLastPasskey: boolean;
}

export interface PasskeyOptionsResponse {
  challengeTxId: string;
  optionsJson: string;
}

export interface PasskeyRegisterFinishResponse {
  registered: boolean;
  passkey: PasskeySummary | null;
  recoveryCodes: string[];
  message: string;
}

export interface AdminRecoveryRequest {
  targetUserName: string;
  verificationNote: string;
  resetImmediately: boolean;
}

export interface AdminRecoveryResponse {
  issued: boolean;
  targetUserName: string;
  recoveryMode: string;
  delivery: string;
  expiresAt: string;
  message: string;
}
