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
 * Jackson WRAP_ROOT / JAXB list envelopes used by Admin Administration tabs.
 * A non-array object stored and then {@code .map}'d is {@code TypeError}
 * ({@code e.map is not a function}) — #2959 / #3202.
 */

const MAX_UNWRAP_DEPTH = 5;

export function asJsonRecord(
  value: unknown,
): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

/**
 * Coerce a REST list field to {@code string[]}.
 * Accepts a bare array, a single string (Jackson one-item list), or null.
 */
export function asStringArray(value: unknown): string[] {
  if (value == null) {
    return [];
  }
  if (typeof value === "string") {
    const t = value.trim();
    return t ? [t] : [];
  }
  if (Array.isArray(value)) {
    return value
      .map((item) => (typeof item === "string" ? item.trim() : ""))
      .filter((s) => s.length > 0);
  }
  return [];
}

/**
 * Unwrap a named string-list envelope.
 *
 * Examples that must not throw or yield a non-array:
 * - {@code { RoleList: { roles: ["Admin"] } }}
 * - {@code { RoleList: { roles: "Admin" } }} (single-item)
 * - {@code { roles: ["Admin"] }} (already unwrapped)
 * - {@code { RoleList: { roles: { role: ["Admin"] } } }} (JAXB item wrap)
 */
export function parseWrappedStringList(
  payload: unknown,
  wrapperKeys: readonly string[],
  itemKeys: readonly string[],
): string[] {
  return unwrapStringList(payload, wrapperKeys, itemKeys, 0);
}

function unwrapStringList(
  payload: unknown,
  wrapperKeys: readonly string[],
  itemKeys: readonly string[],
  depth: number,
): string[] {
  if (depth > MAX_UNWRAP_DEPTH) {
    return [];
  }
  if (payload == null) {
    return [];
  }
  if (typeof payload === "string" || Array.isArray(payload)) {
    return asStringArray(payload);
  }
  const obj = asJsonRecord(payload);
  if (!obj) {
    return [];
  }
  for (const key of wrapperKeys) {
    if (key in obj) {
      const nested = unwrapStringList(
        obj[key],
        wrapperKeys,
        itemKeys,
        depth + 1,
      );
      if (nested.length > 0 || obj[key] == null || Array.isArray(obj[key])) {
        return nested;
      }
    }
  }
  for (const key of itemKeys) {
    if (key in obj) {
      return unwrapStringList(obj[key], wrapperKeys, itemKeys, depth + 1);
    }
  }
  return [];
}

/** User/role name lists from {@code GET user/user/roles} and {@code .../users}. */
export function parseRoleNameList(payload: unknown): string[] {
  return parseWrappedStringList(
    payload,
    ["RoleList", "roleList"],
    ["roles", "role"],
  );
}

export function parseUserNameList(payload: unknown): string[] {
  return parseWrappedStringList(
    payload,
    ["UserList", "userList"],
    ["users", "user"],
  );
}

/**
 * Coerce a list payload to an array of objects.
 * Unwraps a single known/first array property; never returns a non-array.
 */
export function asObjectArray(payload: unknown): unknown[] {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload;
  }
  const obj = asJsonRecord(payload);
  if (!obj) {
    return [];
  }
  for (const value of Object.values(obj)) {
    if (Array.isArray(value)) {
      return value;
    }
  }
  return [];
}
