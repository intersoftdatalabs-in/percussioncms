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
 * Load/save Gravatar email override via PreferenceResource (#2397).
 */

import {
  getAllUserPreferences,
  saveUserPreference,
  PREF_CATEGORY_SYS,
  PREF_CONTEXT_PRIVATE,
} from "../api/preferences/preferencesApi";
import { GRAVATAR_EMAIL_PREF_NAME } from "./gravatar";

/**
 * Load stored Gravatar email override for the current session user.
 *
 * <p>Uses GET {@code /preferences/} (list) — not GET {@code /preferences/{name}}.
 * Unset named prefs 404 on live H2 and pollute Explorer when chrome or Profile
 * still probes the named path ({@code /services/…} and {@code /Rhythmyx/services/…}
 * twins, #3468). Empty string when unset (caller falls back to primary email).
 */
export async function loadGravatarEmailOverride(): Promise<string> {
  const listed = await getAllUserPreferences();
  const match = listed.find(
    (p) => (p.name || "").trim() === GRAVATAR_EMAIL_PREF_NAME,
  );
  if (match == null || match.value == null) {
    return "";
  }
  return String(match.value).trim();
}

/**
 * Persist Gravatar email override. Blank value stores empty string so primary
 * email is used at display time.
 */
export async function saveGravatarEmailOverride(
  userName: string,
  email: string,
): Promise<string> {
  const value = email == null ? "" : String(email).trim();
  const saved = await saveUserPreference({
    name: GRAVATAR_EMAIL_PREF_NAME,
    value,
    category: PREF_CATEGORY_SYS,
    context: PREF_CONTEXT_PRIVATE,
    userName: userName ?? "",
  });
  return saved.value == null ? "" : String(saved.value).trim();
}
