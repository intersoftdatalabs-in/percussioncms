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
  loadUserPreference,
  saveUserPreference,
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
