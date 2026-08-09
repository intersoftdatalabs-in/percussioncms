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
 * Client for public PreferenceResource ({@code /services/preferences}).
 *
 * <p>Shared by Developer ACL preferences and the profile Preferences section
 * (#2396 / parent #2374). No parallel preference store — name/value prefs go
 * through this REST + sitemanage {@code IPreferenceAdaptor} stack.</p>
 *
 * <p>Default CMS landing page is <em>not</em> stored here; it uses the existing
 * user homepage REST ({@code /user/user/homepage}) already product-backed.</p>
 */

import { get, put, type ApiError } from "../client";
import { PATHS } from "../paths";

/** REST UserPreference DTO (PreferenceResource). */
export type UserPreference = {
  name: string;
  value: string;
  category?: string;
  context?: string;
  userName?: string;
  extraParam?: string;
};

/** Default category PreferenceResource applies when omitted on save. */
export const PREF_CATEGORY_SYS = "sys_preferences";

/** Default private (per-user) context PreferenceResource applies when omitted. */
export const PREF_CONTEXT_PRIVATE = "private";

function isNotFound(err: unknown): boolean {
  const api = err as ApiError;
  return !!api && typeof api.status === "number" && api.status === 404;
}

function asPreferenceArray(data: unknown): UserPreference[] {
  if (Array.isArray(data)) {
    return data.filter(isUserPreferenceShape);
  }
  if (data != null && typeof data === "object") {
    const root = data as Record<string, unknown>;
    for (const key of [
      "UserPreferenceList",
      "userPreferenceList",
      "list",
      "items",
    ]) {
      const nested = root[key];
      if (Array.isArray(nested)) {
        return nested.filter(isUserPreferenceShape);
      }
    }
  }
  return [];
}

function isUserPreferenceShape(value: unknown): value is UserPreference {
  if (value == null || typeof value !== "object") {
    return false;
  }
  const o = value as Record<string, unknown>;
  return typeof o.name === "string";
}

/**
 * GET /services/preferences/ — all stored prefs for the current user.
 *
 * @returns empty array when none stored (404) or body is empty
 */
export async function getAllUserPreferences(): Promise<UserPreference[]> {
  try {
    const data = await get<unknown>(PATHS.PREFERENCES);
    return asPreferenceArray(data);
  } catch (err: unknown) {
    if (isNotFound(err)) {
      return [];
    }
    throw err;
  }
}

/**
 * GET /services/preferences/{preference}
 *
 * @returns null when the preference is not stored (404)
 */
export async function loadUserPreference(
  preferenceName: string,
): Promise<UserPreference | null> {
  const key = encodeURIComponent(preferenceName);
  try {
    return await get<UserPreference>(`${PATHS.PREFERENCES}/${key}`);
  } catch (err: unknown) {
    if (isNotFound(err)) {
      return null;
    }
    throw err;
  }
}

/**
 * PUT /services/preferences/ — save a single preference for the current user.
 *
 * <p>{@code userName} should be set (server DTO requires it).</p>
 */
export async function saveUserPreference(
  pref: UserPreference,
): Promise<UserPreference> {
  return put<UserPreference>(PATHS.PREFERENCES, {
    name: pref.name,
    value: pref.value ?? "",
    category: pref.category || PREF_CATEGORY_SYS,
    context: pref.context || PREF_CONTEXT_PRIVATE,
    userName: pref.userName ?? "",
    extraParam: pref.extraParam,
  });
}
