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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Read-only client for GET {@code /user/user/current}.
 *
 * <p>Used by profile avatar (#2397 / parent #2374 slice 5) for primary email
 * without depending on account-editor PUT from #2395.</p>
 */

import { get } from "../client";
import { PATHS } from "../paths";

export type CurrentUserBasic = {
  name: string;
  email: string;
};

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

/**
 * Unwrap Jackson {@code @JsonRootName} envelopes ({@code CurrentUser}, {@code User})
 * or accept a flat body (any remaining object root).
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
  return root;
}

export function normalizeCurrentUserBasic(data: unknown): CurrentUserBasic {
  const body = unwrapNamed(data, "CurrentUser", "User") ?? {};
  return {
    name: asString(body.name, "").trim(),
    email: asString(body.email, "").trim(),
  };
}

/** GET signed-in user name + email for avatar / Gravatar primary fallback. */
export async function getCurrentUserBasic(): Promise<CurrentUserBasic> {
  const data = await get<unknown>(PATHS.USER_CURRENT);
  return normalizeCurrentUserBasic(data);
}

/**
 * Client-side email shape check for Gravatar override fields.
 * Blank is allowed (clear override / use primary).
 */
export function isValidEmailAddress(email: string): boolean {
  const value = (email ?? "").trim();
  if (!value) {
    return true;
  }
  if (value.length > 254) {
    return false;
  }
  return /^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,}$/.test(
    value,
  );
}
