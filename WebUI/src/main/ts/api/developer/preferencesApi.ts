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

import { get, put, type ApiError } from "../client";
import { PATHS } from "../paths";
import {
  DEFAULT_ACL_TEMPLATE_PREF_CATEGORY,
  DEFAULT_ACL_TEMPLATE_PREF_CONTEXT,
  DEFAULT_ACL_TEMPLATE_PREF_NAME,
  parseDefaultAclTemplate,
  serializeDefaultAclTemplate,
  systemDefaultAclTemplate,
  type DefaultAclTemplate,
} from "../../developer/defaultAclTemplate";

/** REST UserPreference DTO (PreferenceResource). */
export type UserPreference = {
  name: string;
  value: string;
  category?: string;
  context?: string;
  userName?: string;
  extraParam?: string;
};

function isNotFound(err: unknown): boolean {
  const api = err as ApiError;
  return !!api && typeof api.status === "number" && api.status === 404;
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
    if (isNotFound(err)) return null;
    throw err;
  }
}

/**
 * PUT /services/preferences/ — save a single preference for the current user.
 *
 * <p>{@code userName} must be set (server requires it on the DTO).
 */
export async function saveUserPreference(
  pref: UserPreference,
): Promise<UserPreference> {
  // PreferenceResource is @Path("/preferences") with @PUT @Path("/") on save.
  return put<UserPreference>(PATHS.PREFERENCES, {
    name: pref.name,
    value: pref.value ?? "",
    category: pref.category || DEFAULT_ACL_TEMPLATE_PREF_CATEGORY,
    context: pref.context || DEFAULT_ACL_TEMPLATE_PREF_CONTEXT,
    userName: pref.userName ?? "",
    extraParam: pref.extraParam,
  });
}

export type LoadDefaultAclTemplateResult = {
  /** Effective template (system default when no valid stored pref). */
  template: DefaultAclTemplate;
  /** True when a valid preference value was loaded from the server. */
  fromPreference: boolean;
};

/**
 * Load the Developer default ACL template preference, or system default.
 */
export async function loadDefaultAclTemplate(): Promise<LoadDefaultAclTemplateResult> {
  const pref = await loadUserPreference(DEFAULT_ACL_TEMPLATE_PREF_NAME);
  if (pref?.value) {
    const parsed = parseDefaultAclTemplate(pref.value);
    if (parsed) {
      return { template: parsed, fromPreference: true };
    }
  }
  return { template: systemDefaultAclTemplate(), fromPreference: false };
}

/**
 * Persist the Developer default ACL template for the signed-in user.
 */
export async function saveDefaultAclTemplate(
  template: DefaultAclTemplate,
  userName: string,
): Promise<UserPreference> {
  return saveUserPreference({
    name: DEFAULT_ACL_TEMPLATE_PREF_NAME,
    value: serializeDefaultAclTemplate(template),
    category: DEFAULT_ACL_TEMPLATE_PREF_CATEGORY,
    context: DEFAULT_ACL_TEMPLATE_PREF_CONTEXT,
    userName: userName.trim(),
  });
}
