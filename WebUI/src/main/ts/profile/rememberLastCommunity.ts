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
 * Remember-last-community prefs (#3507 / parent #3505 slice 3).
 *
 * <p>Uses GET {@code /preferences/} (list) — never GET {@code /preferences/{name}}.
 * Unset named prefs 404 on live H2 and pollute Explorer when chrome probes
 * the named path (#3468 / #3458).</p>
 */

import {
  getAllUserPreferences,
  saveUserPreference,
  PREF_CATEGORY_SYS,
  PREF_CONTEXT_PRIVATE,
  type UserPreference,
} from "../api/preferences/preferencesApi";

/** Opt-in flag: restore last community on the next login. */
export const REMEMBER_LAST_COMMUNITY_PREF_NAME =
  "perc_profile_rememberLastCommunity";

/** Last community the user switched to (name). */
export const LAST_COMMUNITY_PREF_NAME = "perc_profile_lastCommunity";

export type RememberLastCommunityPrefs = {
  remember: boolean;
  last: string;
};

export type RestoreLastCommunityInput = {
  remember: boolean;
  last: string;
  current: string;
  allowed: readonly string[];
};

/** True for stored "true" / "1" / "yes" (case-insensitive) or boolean true. */
export function parseRememberLastCommunityFlag(
  value: string | boolean | number | null | undefined,
): boolean {
  if (value === true || value === 1) {
    return true;
  }
  if (value === false || value === 0 || value == null) {
    return false;
  }
  const raw = String(value).trim().toLowerCase();
  return raw === "true" || raw === "1" || raw === "yes";
}

export function prefValueByName(
  listed: readonly UserPreference[],
  name: string,
): string {
  const match = listed.find((p) => (p.name || "").trim() === name);
  if (match == null || match.value == null) {
    return "";
  }
  return String(match.value).trim();
}

/**
 * When remember-last is on and {@code last} is still in the membership list
 * and differs from the current session community, return the name to switch
 * to. Otherwise null — caller leaves the login default (profile default from
 * #3508 or the existing product default) and must not fail login.
 */
export function shouldRestoreLastCommunity(
  input: RestoreLastCommunityInput,
): string | null {
  if (!input.remember) {
    return null;
  }
  const last = (input.last ?? "").trim();
  if (!last) {
    return null;
  }
  const current = (input.current ?? "").trim();
  if (last === current) {
    return null;
  }
  const allowed = new Set(
    (input.allowed ?? []).map((n) => String(n ?? "").trim()).filter(Boolean),
  );
  if (!allowed.has(last)) {
    return null;
  }
  return last;
}

export function prefsFromList(
  listed: readonly UserPreference[],
): RememberLastCommunityPrefs {
  return {
    remember: parseRememberLastCommunityFlag(
      prefValueByName(listed, REMEMBER_LAST_COMMUNITY_PREF_NAME),
    ),
    last: prefValueByName(listed, LAST_COMMUNITY_PREF_NAME),
  };
}

/** Load remember-last prefs via the list endpoint only. */
export async function loadRememberLastCommunityPrefs(): Promise<RememberLastCommunityPrefs> {
  const listed = await getAllUserPreferences();
  return prefsFromList(listed);
}

export async function saveRememberLastCommunityFlag(
  userName: string,
  remember: boolean,
): Promise<boolean> {
  const saved = await saveUserPreference({
    name: REMEMBER_LAST_COMMUNITY_PREF_NAME,
    value: remember ? "true" : "false",
    category: PREF_CATEGORY_SYS,
    context: PREF_CONTEXT_PRIVATE,
    userName: userName ?? "",
  });
  return parseRememberLastCommunityFlag(saved.value);
}

/**
 * Persist the last switched community. Write-only — chrome may call this
 * after a successful switch without reading preferences.
 */
export async function saveLastCommunity(
  userName: string,
  community: string,
): Promise<string> {
  const value = (community ?? "").trim();
  const saved = await saveUserPreference({
    name: LAST_COMMUNITY_PREF_NAME,
    value,
    category: PREF_CATEGORY_SYS,
    context: PREF_CONTEXT_PRIVATE,
    userName: userName ?? "",
  });
  return saved.value == null ? value : String(saved.value).trim();
}
