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
 * Self-service password change client (issue #2394 / parent #2374 slice 3).
 *
 * <p>PUT {@code /user/user/changepw} — server enforces self-only and only
 * updates passwords for INTERNAL provider users. Prefer this over any
 * parallel password store.</p>
 */

import { put } from "../client";
import { PATHS } from "../paths";

/** Legacy UI min length; server also rejects blank via parameter validation. */
export const MIN_PASSWORD_LENGTH = 6;

export type PasswordChangeInput = {
  /** Signed-in user name (must match session; server rejects others). */
  name: string;
  password: string;
  email?: string;
  roles?: string[];
};

export type PasswordFieldErrors = {
  newPassword?: string;
  confirmPassword?: string;
};

export type PasswordValidationResult =
  | { ok: true }
  | { ok: false; fields: PasswordFieldErrors; formMessage?: string };

/**
 * Client-side password form checks (length + confirm match).
 * Messages are key-agnostic — callers pass localized strings.
 */
export function validatePasswordChange(
  newPassword: string,
  confirmPassword: string,
  labels: {
    required: string;
    tooShort: string;
    mismatch: string;
  },
): PasswordValidationResult {
  const fields: PasswordFieldErrors = {};
  const next = newPassword ?? "";
  const confirm = confirmPassword ?? "";

  if (!next.trim()) {
    fields.newPassword = labels.required;
  } else if (next.length < MIN_PASSWORD_LENGTH) {
    fields.newPassword = labels.tooShort;
  }

  if (!confirm) {
    fields.confirmPassword = labels.required;
  } else if (next && confirm !== next) {
    fields.confirmPassword = labels.mismatch;
  }

  if (fields.newPassword || fields.confirmPassword) {
    return { ok: false, fields };
  }
  return { ok: true };
}

/**
 * PUT change password for the signed-in user only.
 * Body shape matches legacy PercUserService / PSUser JAXB root.
 */
export async function changeMyPassword(
  input: PasswordChangeInput,
): Promise<void> {
  const name = (input.name ?? "").trim();
  const password = input.password ?? "";
  if (!name) {
    throw new Error("User name is required for password change");
  }
  if (!password) {
    throw new Error("Password is required");
  }

  const body = {
    User: {
      name,
      password,
      email: input.email ?? "",
      roles: Array.isArray(input.roles) ? input.roles : [],
    },
  };

  await put<unknown>(PATHS.USER_CHANGE_PW, body);
}
