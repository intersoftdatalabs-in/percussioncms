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

import {
  getAllUserPreferences,
  loadUserPreference,
  saveUserPreference,
  unwrapUserPreference,
  type UserPreference,
} from "../preferences/preferencesApi";
import {
  DEFAULT_ACL_TEMPLATE_PREF_CATEGORY,
  DEFAULT_ACL_TEMPLATE_PREF_CONTEXT,
  DEFAULT_ACL_TEMPLATE_PREF_NAME,
  parseDefaultAclTemplate,
  serializeDefaultAclTemplate,
  systemDefaultAclTemplate,
  type DefaultAclTemplate,
} from "../../developer/defaultAclTemplate";

/** Re-export shared PreferenceResource DTO + load/save for Developer callers. */
export type { UserPreference };
export { loadUserPreference, saveUserPreference };

export type LoadDefaultAclTemplateResult = {
  /** Effective template (system default when no valid stored pref). */
  template: DefaultAclTemplate;
  /** True when a valid preference value was loaded from the server. */
  fromPreference: boolean;
};

function isDefaultAclTemplatePref(pref: UserPreference): boolean {
  return (
    (pref.name || "").trim().toLowerCase() ===
    DEFAULT_ACL_TEMPLATE_PREF_NAME.toLowerCase()
  );
}

/**
 * Parse a stored preference into a template. Accepts a JSON string or an
 * already-parsed object so reload does not drop Runtime visibility (#3204).
 */
export function templateFromPreferenceValue(
  pref: UserPreference | null | undefined,
): DefaultAclTemplate | null {
  if (!pref) {
    return null;
  }
  const raw = pref.value as unknown;
  if (raw == null || raw === "") {
    return null;
  }
  if (typeof raw === "string") {
    return parseDefaultAclTemplate(raw);
  }
  if (typeof raw === "object") {
    return parseDefaultAclTemplate(raw as Record<string, unknown>);
  }
  return parseDefaultAclTemplate(String(raw));
}

/**
 * Load the Developer default ACL template preference, or system default.
 *
 * <p>GET {@code /preferences/{name}} is the primary path. When that returns
 * 404, an empty value, or a body that production unwrap rejects as flat
 * ({@code acceptFlat: false}), fall back to GET {@code /preferences/} so a
 * successful Save still reloads Design/Runtime visibility (#2948 / #3204).
 */
export async function loadDefaultAclTemplate(): Promise<LoadDefaultAclTemplateResult> {
  const named = await loadUserPreference(DEFAULT_ACL_TEMPLATE_PREF_NAME);
  const fromNamed = templateFromPreferenceValue(named);
  if (fromNamed) {
    return { template: fromNamed, fromPreference: true };
  }

  const listed = await getAllUserPreferences();
  const match = listed.find(isDefaultAclTemplatePref);
  const fromList = templateFromPreferenceValue(match);
  if (fromList) {
    return { template: fromList, fromPreference: true };
  }

  // Last resort: list item present but GET-by-name unwrap was too strict.
  if (match) {
    const loosened = unwrapUserPreference(match, { acceptFlat: true });
    const fromLoose = templateFromPreferenceValue(loosened);
    if (fromLoose) {
      return { template: fromLoose, fromPreference: true };
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
