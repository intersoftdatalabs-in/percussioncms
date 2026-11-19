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

import { del, get, isApiError, post, put } from "../client";
import { PATHS } from "../paths";
import type {
  MfaActionResult,
  MfaStepUpResponse,
  PasskeyListResponse,
  PasskeyOptionsResponse,
  PasskeyRegisterFinishResponse,
  PasskeySummary,
  ProfileMfaStatus,
  RecoveryCodesRegenerateResponse,
  TotpEnrollConfirmResponse,
  TotpEnrollStartResponse,
} from "./types";

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function unwrapRoot(payload: unknown, ...rootKeys: string[]): Record<string, unknown> {
  const root = asRecord(payload) ?? {};
  for (const key of rootKeys) {
    const nested = asRecord(root[key]);
    if (nested) {
      return nested;
    }
  }
  return root;
}

function asString(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

function asNumber(value: unknown, fallback = 0): number {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}

function asStringArray(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.filter((item): item is string => typeof item === "string");
  }
  return typeof value === "string" && value.trim() ? [value] : [];
}

/** True when an API failure indicates HTTP 428 / STEP_UP_REQUIRED. */
export function isStepUpRequiredError(err: unknown): boolean {
  if (!isApiError(err)) {
    return false;
  }
  if (err.status === 428) {
    return true;
  }
  const body = asRecord(err.body);
  return body != null && asString(body.error) === "STEP_UP_REQUIRED";
}

export function unwrapProfileMfaStatus(payload: unknown): ProfileMfaStatus {
  const body = unwrapRoot(payload, "ProfileMfaStatus", "profileMfaStatus");
  const expiresRaw = body.stepUpExpiresAt;
  return {
    userName: asString(body.userName),
    mfaMode: asString(body.mfaMode, "optional"),
    mfaRequired: Boolean(body.mfaRequired),
    totpEnabled: Boolean(body.totpEnabled),
    passkeyCount: asNumber(body.passkeyCount),
    recoveryCodesRemaining: asNumber(body.recoveryCodesRemaining),
    canDisableTotp: Boolean(body.canDisableTotp),
    stepUpActive: Boolean(body.stepUpActive),
    stepUpExpiresAt:
      typeof expiresRaw === "string" && expiresRaw.trim() ? expiresRaw : null,
  };
}

export function unwrapMfaStepUpResponse(payload: unknown): MfaStepUpResponse {
  const body = unwrapRoot(payload, "MfaStepUpResponse", "mfaStepUpResponse");
  return {
    verified: Boolean(body.verified),
    expiresAtEpochMs: asNumber(body.expiresAtEpochMs),
    message: asString(body.message),
  };
}

export function unwrapTotpEnrollStartResponse(
  payload: unknown,
): TotpEnrollStartResponse {
  const body = unwrapRoot(
    payload,
    "TotpEnrollStartResponse",
    "totpEnrollStartResponse",
  );
  return {
    totpId: asString(body.totpId),
    otpauthUri: asString(body.otpauthUri),
    manualSecret: asString(body.manualSecret),
    issuer: asString(body.issuer, "Percussion CMS"),
    accountName: asString(body.accountName),
    digits: asNumber(body.digits, 6),
    periodSeconds: asNumber(body.periodSeconds, 30),
  };
}

export function unwrapTotpEnrollConfirmResponse(
  payload: unknown,
): TotpEnrollConfirmResponse {
  const body = unwrapRoot(
    payload,
    "TotpEnrollConfirmResponse",
    "totpEnrollConfirmResponse",
  );
  return {
    enrolled: Boolean(body.enrolled),
    recoveryCodes: asStringArray(body.recoveryCodes),
    message: asString(body.message),
  };
}

export function unwrapRecoveryCodesRegenerateResponse(
  payload: unknown,
): RecoveryCodesRegenerateResponse {
  const body = unwrapRoot(
    payload,
    "RecoveryCodesRegenerateResponse",
    "recoveryCodesRegenerateResponse",
  );
  return {
    recoveryCodes: asStringArray(body.recoveryCodes),
    message: asString(body.message),
  };
}

export function unwrapMfaActionResult(payload: unknown): MfaActionResult {
  const body = unwrapRoot(payload, "MfaActionResult", "mfaActionResult");
  return {
    success: Boolean(body.success),
    message: asString(body.message),
  };
}

export async function getMyMfaStatus(): Promise<ProfileMfaStatus> {
  const payload = await get<unknown>(PATHS.AUTH_MFA_STATUS);
  return unwrapProfileMfaStatus(payload);
}

export async function verifyMfaStepUp(
  code: string,
  recoveryCode = false,
): Promise<MfaStepUpResponse> {
  const payload = await post<unknown>(PATHS.AUTH_MFA_STEP_UP, {
    MfaStepUpRequest: {
      code: (code ?? "").trim(),
      recoveryCode,
    },
  });
  return unwrapMfaStepUpResponse(payload);
}

export async function startMyTotpEnrollment(
  label = "Authenticator",
): Promise<TotpEnrollStartResponse> {
  const cleanLabel = (label ?? "").trim() || "Authenticator";
  const payload = await post<unknown>(PATHS.AUTH_MFA_TOTP_START, {
    TotpEnrollStartRequest: { label: cleanLabel },
  });
  return unwrapTotpEnrollStartResponse(payload);
}

export async function confirmMyTotpEnrollment(
  totpId: string,
  code: string,
): Promise<TotpEnrollConfirmResponse> {
  const payload = await post<unknown>(PATHS.AUTH_MFA_TOTP_CONFIRM, {
    TotpEnrollConfirmRequest: {
      totpId: (totpId ?? "").trim(),
      code: (code ?? "").trim(),
    },
  });
  return unwrapTotpEnrollConfirmResponse(payload);
}

export async function disableMyTotp(): Promise<MfaActionResult> {
  const payload = await del<unknown>(PATHS.AUTH_MFA_TOTP);
  return unwrapMfaActionResult(payload);
}

export async function regenerateMyRecoveryCodes(): Promise<RecoveryCodesRegenerateResponse> {
  const payload = await post<unknown>(PATHS.AUTH_MFA_RECOVERY_REGENERATE);
  return unwrapRecoveryCodesRegenerateResponse(payload);
}

function normalizePasskeySummary(raw: unknown): PasskeySummary | null {
  const rec = asRecord(raw);
  if (!rec) {
    return null;
  }
  const transportsRaw = rec.transports;
  const transports = Array.isArray(transportsRaw)
    ? transportsRaw.filter((t): t is string => typeof t === "string")
    : typeof transportsRaw === "string" && transportsRaw.trim()
      ? [transportsRaw]
      : [];
  return {
    passkeyId: asString(rec.passkeyId),
    label: asString(rec.label, "Passkey"),
    aaguid: typeof rec.aaguid === "string" ? rec.aaguid : null,
    transports,
    backupEligible: Boolean(rec.backupEligible),
    backupState: Boolean(rec.backupState),
    createdAt: typeof rec.createdAt === "string" ? rec.createdAt : null,
    lastUsedAt: typeof rec.lastUsedAt === "string" ? rec.lastUsedAt : null,
  };
}

function normalizePasskeyArray(raw: unknown): PasskeySummary[] {
  if (!raw) {
    return [];
  }
  const arr = Array.isArray(raw) ? raw : [raw];
  return arr
    .map(normalizePasskeySummary)
    .filter((item): item is PasskeySummary => item !== null && item.passkeyId.length > 0);
}

export function unwrapPasskeyListResponse(payload: unknown): PasskeyListResponse {
  const body = unwrapRoot(payload, "PasskeyListResponse", "passkeyListResponse");
  return {
    passkeys: normalizePasskeyArray(body.passkeys),
    canDeleteLastPasskey: Boolean(body.canDeleteLastPasskey),
  };
}

export function unwrapPasskeyOptionsResponse(payload: unknown): PasskeyOptionsResponse {
  const body = unwrapRoot(
    payload,
    "PasskeyOptionsResponse",
    "passkeyOptionsResponse",
  );
  return {
    challengeTxId: asString(body.challengeTxId),
    optionsJson: asString(body.optionsJson),
  };
}

export function unwrapPasskeyRegisterFinishResponse(
  payload: unknown,
): PasskeyRegisterFinishResponse {
  const body = unwrapRoot(
    payload,
    "PasskeyRegisterFinishResponse",
    "passkeyRegisterFinishResponse",
  );
  return {
    registered: Boolean(body.registered),
    passkey: normalizePasskeySummary(body.passkey),
    recoveryCodes: asStringArray(body.recoveryCodes),
    message: asString(body.message),
  };
}

export async function listMyPasskeys(): Promise<PasskeyListResponse> {
  const payload = await get<unknown>(PATHS.AUTH_MFA_PASSKEYS);
  return unwrapPasskeyListResponse(payload);
}

export async function startMyPasskeyRegistration(
  label = "",
): Promise<PasskeyOptionsResponse> {
  const payload = await post<unknown>(PATHS.AUTH_MFA_PASSKEY_REGISTER_START, {
    PasskeyOptionsRequest: { label: (label ?? "").trim() },
  });
  return unwrapPasskeyOptionsResponse(payload);
}

export async function finishMyPasskeyRegistration(
  challengeTxId: string,
  label: string,
  credentialJson: string,
): Promise<PasskeyRegisterFinishResponse> {
  const payload = await post<unknown>(PATHS.AUTH_MFA_PASSKEY_REGISTER_FINISH, {
    PasskeyRegisterFinishRequest: {
      challengeTxId: (challengeTxId ?? "").trim(),
      label: (label ?? "").trim(),
      credentialJson,
    },
  });
  return unwrapPasskeyRegisterFinishResponse(payload);
}

export async function renameMyPasskey(
  passkeyId: string,
  label: string,
): Promise<MfaActionResult> {
  const payload = await put<unknown>(PATHS.authMfaPasskey(passkeyId), {
    PasskeyRenameRequest: { label: (label ?? "").trim() },
  });
  return unwrapMfaActionResult(payload);
}

export async function deleteMyPasskey(passkeyId: string): Promise<MfaActionResult> {
  const payload = await del<unknown>(PATHS.authMfaPasskey(passkeyId));
  return unwrapMfaActionResult(payload);
}

export async function startMyPasskeyStepUp(): Promise<PasskeyOptionsResponse> {
  const payload = await post<unknown>(PATHS.AUTH_MFA_STEP_UP_PASSKEY_START);
  return unwrapPasskeyOptionsResponse(payload);
}

export async function verifyMyPasskeyStepUp(
  challengeTxId: string,
  credentialJson: string,
): Promise<MfaStepUpResponse> {
  const payload = await post<unknown>(PATHS.AUTH_MFA_STEP_UP_PASSKEY_VERIFY, {
    PasskeyStepUpVerifyRequest: {
      challengeTxId: (challengeTxId ?? "").trim(),
      credentialJson,
    },
  });
  return unwrapMfaStepUpResponse(payload);
}
