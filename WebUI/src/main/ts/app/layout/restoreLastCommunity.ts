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
 * One-shot session-start restore of the last community (#3507).
 *
 * <p>Uses GET {@code /preferences/} (list) + GET current user, then POST
 * {@code /services/communities/switch/{name}} when the remember-last option
 * is on and the stored name is still in the membership list. Failures are
 * swallowed so login never fails — missing or revoked last community leaves
 * the existing login default (#3508 profile default when present).</p>
 *
 * <p>This is not UserMenu chrome: do not move a named GET
 * {@code /preferences/{name}} into the header (#3468 / #3458).</p>
 */

import {
  allowedSessionCommunities,
  switchSessionCommunity,
} from "../../api/user/communitySwitchApi";
import {
  getCurrentUserProfile,
  type CurrentUserProfile,
} from "../../api/user/userProfileApi";
import {
  loadRememberLastCommunityPrefs,
  shouldRestoreLastCommunity,
  type RememberLastCommunityPrefs,
} from "../../profile/rememberLastCommunity";
import { dispatchSessionCommunityChanged } from "./sessionCommunity";

export type RestoreLastCommunityDeps = {
  loadPrefs?: () => Promise<RememberLastCommunityPrefs>;
  loadProfile?: () => Promise<CurrentUserProfile>;
  switchCommunity?: (name: string) => Promise<void>;
  notifyChanged?: (name: string) => void;
};

/**
 * Restore last community after login when the option is on and still allowed.
 *
 * @returns switched community name, or null when no switch was performed
 */
export async function restoreLastCommunityIfNeeded(
  deps: RestoreLastCommunityDeps = {},
): Promise<string | null> {
  const loadPrefs = deps.loadPrefs ?? loadRememberLastCommunityPrefs;
  const loadProfile = deps.loadProfile ?? getCurrentUserProfile;
  const switchCommunity = deps.switchCommunity ?? switchSessionCommunity;
  const notifyChanged = deps.notifyChanged ?? dispatchSessionCommunityChanged;

  try {
    const [prefs, profile] = await Promise.all([loadPrefs(), loadProfile()]);
    const target = shouldRestoreLastCommunity({
      remember: prefs.remember,
      last: prefs.last,
      current: profile.currentCommunity,
      allowed: allowedSessionCommunities(profile.communities),
    });
    if (!target) {
      return null;
    }
    await switchCommunity(target);
    notifyChanged(target);
    return target;
  } catch {
    return null;
  }
}
