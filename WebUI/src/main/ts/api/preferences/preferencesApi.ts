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

/**
 * Jackson root name for {@code UserPreference} ({@code @XmlRootElement} /
 * {@code @JsonRootName}). REST {@code JacksonContextResolver} enables
 * {@code WRAP_ROOT_VALUE} / {@code UNWRAP_ROOT_VALUE}, so PUT/DELETE bodies and
 * single-pref responses use {@code { "UserPreference": { ... } }}.
 *
 * <p>A bare flat body starting with {@code name} is rejected as unexpected root
 * (JAXB/Jackson: expected {@code UserPreference}) — see #2708.</p>
 */
export const USER_PREFERENCE_ROOT = "UserPreference";

/** Default category PreferenceResource applies when omitted on save. */
export const PREF_CATEGORY_SYS = "sys_preferences";

/** Default private (per-user) context PreferenceResource applies when omitted. */
export const PREF_CONTEXT_PRIVATE = "private";

function isNotFound(err: unknown): boolean {
  const api = err as ApiError;
  return !!api && typeof api.status === "number" && api.status === 404;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function collectPreferenceShapes(value: unknown): UserPreference[] {
  if (Array.isArray(value)) {
    return value.filter(isUserPreferenceShape);
  }
  return [];
}

/**
 * Normalize GET {@code /preferences/} payloads.
 *
 * <p>Jackson {@code WRAP_ROOT_VALUE} may emit a bare array, a
 * {@code UserPreferenceList} array envelope, or a JAXB-style nested
 * {@code { UserPreferenceList: { UserPreference: [...] } }}. Missing the
 * nested form made list fallback miss {@code developer.defaultObjectAclTemplate}
 * after Save (#3204 / #2643).
 */
function asPreferenceArray(data: unknown): UserPreference[] {
  const direct = collectPreferenceShapes(data);
  if (direct.length > 0) {
    return direct;
  }
  const root = asRecord(data);
  if (!root) {
    return [];
  }
  for (const key of [
    "UserPreferenceList",
    "userPreferenceList",
    "list",
    "items",
  ]) {
    const nested = root[key];
    const fromNested = collectPreferenceShapes(nested);
    if (fromNested.length > 0) {
      return fromNested;
    }
    const nestedObj = asRecord(nested);
    if (nestedObj) {
      for (const innerKey of ["UserPreference", "userPreference", "items", "list"]) {
        const fromInner = collectPreferenceShapes(nestedObj[innerKey]);
        if (fromInner.length > 0) {
          return fromInner;
        }
      }
    }
  }
  const fromRootItems = collectPreferenceShapes(
    root.UserPreference ?? root.userPreference,
  );
  if (fromRootItems.length > 0) {
    return fromRootItems;
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

/** Options for {@link unwrapUserPreference}. */
export type UnwrapUserPreferenceOptions = {
  /**
   * When true (default), accept a flat {@code { name, value }} object if the
   * Jackson root is missing. Production GET/PUT response parsing uses
   * {@code false} so a dropped root wrap surfaces as null rather than silently
   * treating a mis-shaped body as success (#2708 review).
   */
  acceptFlat?: boolean;
};

/**
 * Normalize a PreferenceResource single-pref response or request body to a flat
 * {@link UserPreference}. Prefers Jackson-wrapped
 * {@code { UserPreference: { ... } }}. Flat objects are only accepted when
 * {@code acceptFlat} is true (tests / explicit callers).
 */
export function unwrapUserPreference(
  data: unknown,
  options?: UnwrapUserPreferenceOptions,
): UserPreference | null {
  const acceptFlat = options?.acceptFlat !== false;
  const root = asRecord(data);
  if (!root) {
    return null;
  }
  const nested = asRecord(root[USER_PREFERENCE_ROOT]);
  if (nested && isUserPreferenceShape(nested)) {
    return nested;
  }
  // Production: do not mask a missing UserPreference root with a flat fallback.
  if (!acceptFlat) {
    return null;
  }
  if (isUserPreferenceShape(root)) {
    return root;
  }
  return null;
}

/**
 * Build the wire JSON body for PreferenceResource PUT/DELETE.
 *
 * <p>Must nest fields under {@link USER_PREFERENCE_ROOT} — a flat
 * {@code { name, value, ... }} body fails server unwrap with unexpected root
 * {@code name} (#2708).</p>
 */
export function wrapUserPreferenceForWire(
  pref: UserPreference,
): Record<string, UserPreference> {
  const dto: UserPreference = {
    name: pref.name,
    value: pref.value ?? "",
    category: pref.category || PREF_CATEGORY_SYS,
    context: pref.context || PREF_CONTEXT_PRIVATE,
    userName: pref.userName ?? "",
  };
  if (pref.extraParam !== undefined) {
    dto.extraParam = pref.extraParam;
  }
  return { [USER_PREFERENCE_ROOT]: dto };
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
    const data = await get<unknown>(`${PATHS.PREFERENCES}/${key}`);
    // Prefer wrapped wire; do not accept flat as a successful production parse.
    return unwrapUserPreference(data, { acceptFlat: false });
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
 * <p>Body is Jackson root-wrapped as {@code { UserPreference: { ... } }}
 * (#2708).</p>
 */
export async function saveUserPreference(
  pref: UserPreference,
): Promise<UserPreference> {
  const data = await put<unknown>(
    PATHS.PREFERENCES,
    wrapUserPreferenceForWire(pref),
  );
  // Response must be wrapped; flat/mis-shaped bodies fall through to sent fields.
  const unwrapped = unwrapUserPreference(data, { acceptFlat: false });
  if (unwrapped) {
    return unwrapped;
  }
  // Defensive: if server returned unexpected shape, surface the fields we sent
  // so UI state stays consistent after a successful HTTP PUT (#2708).
  return {
    name: pref.name,
    value: pref.value ?? "",
    category: pref.category || PREF_CATEGORY_SYS,
    context: pref.context || PREF_CONTEXT_PRIVATE,
    userName: pref.userName ?? "",
    extraParam: pref.extraParam,
  };
}
