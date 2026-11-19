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

import { get, isApiError, post } from "../client";
import { PATHS } from "../paths";
import type {
  AuthAccountReadinessItem,
  AuthAccountReadinessSummary,
  AuthCheckStatus,
  AuthInfraCheck,
  AuthMailTestResult,
  AuthReadinessReport,
} from "./types";

export const AUTH_READINESS_REPORT_ROOT = "AuthReadinessReport";
export const AUTH_INFRA_CHECK_ROOT = "AuthInfraCheck";
export const AUTH_ACCOUNT_READINESS_ITEM_ROOT = "AuthAccountReadinessItem";
export const AUTH_MAIL_TEST_RESULT_ROOT = "AuthMailTestResult";

export class AuthReadinessForbiddenError extends Error {
  readonly status = 403;
  constructor(message = "Admin role required to access auth readiness") {
    super(message);
    this.name = "AuthReadinessForbiddenError";
  }
}

function rethrowIfForbidden(err: unknown): never {
  if (isApiError(err) && err.status === 403) {
    throw new AuthReadinessForbiddenError(
      typeof err.body === "string" && err.body.trim()
        ? err.body.trim()
        : "Admin role required to access auth readiness",
    );
  }
  throw err;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function asString(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

function asNumber(value: unknown, fallback = 0): number {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === "string" && value.trim()) {
    const n = Number(value);
    if (Number.isFinite(n)) {
      return n;
    }
  }
  return fallback;
}

function asCheckStatus(value: unknown, fallback: AuthCheckStatus = "WARN"): AuthCheckStatus {
  if (value === "PASS" || value === "WARN" || value === "FAIL") {
    return value;
  }
  return fallback;
}

function unwrapList<T>(
  raw: unknown,
  rootKey: string,
  mapItem: (rec: Record<string, unknown>) => T | null,
): T[] {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    const out: T[] = [];
    for (const item of raw) {
      const rec = asRecord(item);
      if (!rec) continue;
      const inner = asRecord(rec[rootKey]) ?? rec;
      const mapped = mapItem(inner);
      if (mapped != null) out.push(mapped);
    }
    return out;
  }
  const rec = asRecord(raw);
  if (!rec) {
    return [];
  }
  const wrapped = rec[rootKey] ?? rec[rootKey.charAt(0).toLowerCase() + rootKey.slice(1)];
  if (wrapped != null && wrapped !== raw) {
    return unwrapList(wrapped, rootKey, mapItem);
  }
  const single = mapItem(rec);
  return single != null ? [single] : [];
}

function asStringArray(raw: unknown): string[] {
  if (Array.isArray(raw)) {
    return raw.filter((v): v is string => typeof v === "string");
  }
  if (typeof raw === "string" && raw.trim()) {
    return [raw.trim()];
  }
  return [];
}

function mapInfraCheck(rec: Record<string, unknown>): AuthInfraCheck | null {
  if (typeof rec.code !== "string" && typeof rec.label !== "string") {
    return null;
  }
  return {
    code: asString(rec.code),
    label: asString(rec.label),
    status: asCheckStatus(rec.status, "WARN"),
    summary: asString(rec.summary),
    remediation: asString(rec.remediation),
  };
}

function mapAccountItem(rec: Record<string, unknown>): AuthAccountReadinessItem | null {
  if (typeof rec.userName !== "string") {
    return null;
  }
  return {
    userName: rec.userName,
    providerType: asString(rec.providerType, "INTERNAL"),
    email: asString(rec.email),
    emailVerified: Boolean(rec.emailVerified),
    mfaEnrollmentState: asString(rec.mfaEnrollmentState, "NONE"),
    totpCount: asNumber(rec.totpCount),
    passkeyCount: asNumber(rec.passkeyCount),
    recoveryCodeCount: asNumber(rec.recoveryCodeCount),
    issues: asStringArray(rec.issues),
  };
}

function mapSummary(raw: unknown): AuthAccountReadinessSummary {
  const rec = asRecord(raw) ?? {};
  return {
    totalAccounts: asNumber(rec.totalAccounts),
    internalAccounts: asNumber(rec.internalAccounts),
    directoryAccounts: asNumber(rec.directoryAccounts),
    readyCount: asNumber(rec.readyCount),
    missingEmailCount: asNumber(rec.missingEmailCount),
    malformedEmailCount: asNumber(rec.malformedEmailCount),
    duplicateEmailCount: asNumber(rec.duplicateEmailCount),
    unverifiedEmailCount: asNumber(rec.unverifiedEmailCount),
    mfaEnrolledCount: asNumber(rec.mfaEnrolledCount),
    totpEnrolledCount: asNumber(rec.totpEnrolledCount),
    passkeyEnrolledCount: asNumber(rec.passkeyEnrolledCount),
  };
}

function isReportShape(o: Record<string, unknown>): boolean {
  return (
    "overallStatus" in o ||
    "infraChecks" in o ||
    "accountSummary" in o ||
    "mfaMode" in o
  );
}

export function unwrapAuthReadinessReport(payload: unknown): AuthReadinessReport {
  const root = asRecord(payload) ?? {};
  const wrapped = asRecord(
    root[AUTH_READINESS_REPORT_ROOT] ?? root.authReadinessReport,
  );
  const body = wrapped && isReportShape(wrapped) ? wrapped : isReportShape(root) ? root : {};

  return {
    generatedAt: asString(body.generatedAt),
    mfaMode: asString(body.mfaMode, "optional"),
    passwordMinLength: asNumber(body.passwordMinLength, 12),
    mailConfigured: Boolean(body.mailConfigured),
    mailSource: asString(body.mailSource, "UNCONFIGURED"),
    clusterKeyFingerprint: asString(body.clusterKeyFingerprint),
    overallStatus: asCheckStatus(body.overallStatus, "WARN"),
    infraChecks: unwrapList(body.infraChecks, AUTH_INFRA_CHECK_ROOT, mapInfraCheck),
    accountSummary: mapSummary(body.accountSummary),
    accountIssues: unwrapList(
      body.accountIssues,
      AUTH_ACCOUNT_READINESS_ITEM_ROOT,
      mapAccountItem,
    ),
  };
}

export function unwrapAuthMailTestResult(payload: unknown): AuthMailTestResult {
  const root = asRecord(payload) ?? {};
  const wrapped = asRecord(
    root[AUTH_MAIL_TEST_RESULT_ROOT] ?? root.authMailTestResult,
  );
  const body =
    wrapped && ("sent" in wrapped || "message" in wrapped) ? wrapped : root;
  return {
    sent: Boolean(body.sent),
    recipient: asString(body.recipient),
    source: asString(body.source),
    message: asString(body.message),
  };
}

export async function fetchAuthReadinessReport(): Promise<AuthReadinessReport> {
  try {
    const payload = await get<unknown>(PATHS.AUTH_ADMIN_READINESS);
    return unwrapAuthReadinessReport(payload);
  } catch (err) {
    rethrowIfForbidden(err);
  }
}

export async function sendAuthMailTest(recipient: string): Promise<AuthMailTestResult> {
  const cleanRecipient = (recipient ?? "").trim();
  if (!cleanRecipient) {
    throw new Error("Recipient email address is required");
  }
  try {
    const payload = await post<unknown>(PATHS.AUTH_ADMIN_MAIL_TEST, {
      AuthMailTestRequest: { recipient: cleanRecipient },
    });
    return unwrapAuthMailTestResult(payload);
  } catch (err) {
    rethrowIfForbidden(err);
  }
}
