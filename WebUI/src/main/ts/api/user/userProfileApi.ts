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

/**
 * Client for self-service account profile (issue #2395 / parent #2374 slice 2).
 *
 * <p>GET {@code /user/user/current} and PUT {@code /user/user/profile}.
 * Mutations are always applied to the signed-in session user (no user name
 * on the path — no IDOR).</p>
 */

import { get, put } from "../client";
import { PATHS } from "../paths";

export type UserProviderType = "INTERNAL" | "DIRECTORY";

export interface CurrentUserProfile {
  name: string;
  email: string;
  providerType: UserProviderType;
  roles: string[];
  communities: string[];
  currentCommunity: string;
  adminUser: boolean;
  designerUser: boolean;
  accessibilityUser: boolean;
  /** True when product may persist email for this user (internal only). */
  emailEditable: boolean;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function asString(value: unknown, fallback = ""): string {
  if (typeof value === "string") {
    return value;
  }
  if (value == null) {
    return fallback;
  }
  return String(value);
}

function asStringList(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((v) => (typeof v === "string" ? v.trim() : String(v ?? "").trim()))
    .filter((s) => s.length > 0);
}

function asBoolean(value: unknown): boolean {
  return value === true || value === "true" || value === 1 || value === "1";
}

/**
 * Unwrap Jackson {@code @JsonRootName} envelopes ({@code CurrentUser},
 * {@code UserAccountUpdate}) or accept a flat body.
 */
function unwrapNamed(
  data: unknown,
  ...rootNames: string[]
): Record<string, unknown> | null {
  const root = asRecord(data);
  if (!root) {
    return null;
  }
  for (const name of rootNames) {
    const nested = asRecord(root[name]);
    if (nested) {
      return nested;
    }
  }
  // Flat body (no wrap) or already unwrapped
  if ("name" in root || "email" in root || "providerType" in root) {
    return root;
  }
  return root;
}

export function normalizeCurrentUser(data: unknown): CurrentUserProfile {
  const body = unwrapNamed(data, "CurrentUser", "User") ?? {};
  const providerRaw = asString(body.providerType, "INTERNAL").toUpperCase();
  const providerType: UserProviderType =
    providerRaw === "DIRECTORY" ? "DIRECTORY" : "INTERNAL";
  const email = asString(body.email, "").trim();
  return {
    name: asString(body.name, "").trim(),
    email,
    providerType,
    roles: asStringList(body.roles),
    communities: asStringList(body.communities),
    currentCommunity: asString(body.currentCommunity, "").trim(),
    adminUser: asBoolean(body.adminUser),
    designerUser: asBoolean(body.designerUser),
    accessibilityUser: asBoolean(body.accessibilityUser),
    emailEditable: providerType === "INTERNAL",
  };
}

/** GET signed-in user identity for the profile Account section. */
export async function getCurrentUserProfile(): Promise<CurrentUserProfile> {
  const data = await get<unknown>(PATHS.USER_CURRENT);
  return normalizeCurrentUser(data);
}

/**
 * PUT self-service email update. Server always targets the session user.
 * Directory-managed accounts are rejected by the server.
 */
export async function updateMyAccountEmail(
  email: string,
): Promise<CurrentUserProfile> {
  const body = {
    UserAccountUpdate: {
      email: email == null ? "" : String(email).trim(),
    },
  };
  const data = await put<unknown>(PATHS.USER_PROFILE, body);
  return normalizeCurrentUser(data);
}

/**
 * Client-side email shape check (mirrors server practical regex).
 * Domain labels may not start/end with hyphen or contain consecutive dots.
 */
export function isValidEmailAddress(email: string): boolean {
  const value = (email ?? "").trim();
  if (!value) {
    // Blank is allowed (clear stored email).
    return true;
  }
  if (value.length > 254) {
    return false;
  }
  return /^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,}$/.test(
    value,
  );
}
