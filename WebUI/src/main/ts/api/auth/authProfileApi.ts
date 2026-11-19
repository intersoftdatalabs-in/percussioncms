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

import { get, post } from "../client";
import { PATHS } from "../paths";
import type { ProfileEmailActionResult, ProfileEmailStatus } from "./types";

export const PROFILE_EMAIL_STATUS_ROOT = "ProfileEmailStatus";
export const PROFILE_EMAIL_ACTION_RESULT_ROOT = "ProfileEmailActionResult";

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function asString(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

export function unwrapProfileEmailStatus(payload: unknown): ProfileEmailStatus {
  const root = asRecord(payload) ?? {};
  const wrapped = asRecord(
    root[PROFILE_EMAIL_STATUS_ROOT] ?? root.profileEmailStatus,
  );
  const body =
    wrapped && ("emailVerified" in wrapped || "email" in wrapped || "userName" in wrapped)
      ? wrapped
      : root;

  return {
    userName: asString(body.userName),
    email: asString(body.email),
    emailVerified: Boolean(body.emailVerified),
    emailVerifiedAt:
      typeof body.emailVerifiedAt === "string" && body.emailVerifiedAt.trim()
        ? body.emailVerifiedAt
        : null,
    pendingEmail: asString(body.pendingEmail),
    emailEditable: body.emailEditable === undefined ? true : Boolean(body.emailEditable),
    emailRequired: body.emailRequired === undefined ? true : Boolean(body.emailRequired),
    providerType: asString(body.providerType, "INTERNAL"),
  };
}

export function unwrapProfileEmailActionResult(
  payload: unknown,
): ProfileEmailActionResult {
  const root = asRecord(payload) ?? {};
  const wrapped = asRecord(
    root[PROFILE_EMAIL_ACTION_RESULT_ROOT] ?? root.profileEmailActionResult,
  );
  const body =
    wrapped && ("verificationSent" in wrapped || "message" in wrapped)
      ? wrapped
      : root;

  return {
    verificationSent: Boolean(body.verificationSent),
    appliedImmediately: Boolean(body.appliedImmediately),
    email: asString(body.email),
    pendingEmail: asString(body.pendingEmail),
    emailVerified: Boolean(body.emailVerified),
    message: asString(body.message),
  };
}

export async function getMyEmailStatus(): Promise<ProfileEmailStatus> {
  const payload = await get<unknown>(PATHS.AUTH_PROFILE_EMAIL);
  return unwrapProfileEmailStatus(payload);
}

export async function requestMyEmailChange(
  email: string,
): Promise<ProfileEmailActionResult> {
  const payload = await post<unknown>(PATHS.AUTH_PROFILE_EMAIL_CHANGE, {
    ProfileEmailChangeRequest: { email: (email ?? "").trim() },
  });
  return unwrapProfileEmailActionResult(payload);
}

export async function resendMyEmailVerification(): Promise<ProfileEmailActionResult> {
  const payload = await post<unknown>(PATHS.AUTH_PROFILE_EMAIL_RESEND);
  return unwrapProfileEmailActionResult(payload);
}
