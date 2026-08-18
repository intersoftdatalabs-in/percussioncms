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
 * Default landing options for the profile Preferences section (#2396 / #3536 / #3538).
 *
 * <p>Values are product homepage types (PascalCase) already accepted by
 * {@code PSUserService} / login landing resolve. Labels reuse nav menu keys.
 * Options are filtered from SPA bootstrap admin/designer flags (self-service
 * does not load full role lists) via the shared {@code isLandingAllowed}
 * gates (#3538). Editor, Design, and Widget Builder are not offered as new
 * choices after they left top nav (#3514); a stored override still lists so
 * the user can clear it.</p>
 */

import { HOMEPAGE_TYPES, type HomepageType } from "../api/user/userHomepageApi";
import {
  isLandingAllowed,
  type LandingRoleGates,
} from "../app/landing/landingPermission";

export interface ProfileLandingOption {
  /** Canonical API value, or empty string for "use role default". */
  value: HomepageType | "";
  /** TMX message key. */
  labelKey: string;
}

/**
 * Product options for the profile landing select (#3536 / parent #3515).
 * Matches remaining top-nav apps (Home, Explorer, Navigation, Developer,
 * Publish, Admin). Editor / Design are not offered as new choices.
 */
export const PROFILE_LANDING_OPTIONS: readonly ProfileLandingOption[] = [
  {
    value: "",
    labelKey: "perc.ui.profile.modern@Use role default",
  },
  {
    value: HOMEPAGE_TYPES.HOME,
    labelKey: "perc.ui.navMenu.home@Home",
  },
  {
    value: HOMEPAGE_TYPES.EXPLORER,
    labelKey: "perc.ui.dashboard.modern@Explorer",
  },
  {
    value: HOMEPAGE_TYPES.ARCHITECTURE,
    // Product name Navigation; stored type remains Architecture; SPA path /architecture (#3094 / #3217 / #3219)
    labelKey: "perc.ui.navMenu.architecture@Navigation",
  },
  {
    value: HOMEPAGE_TYPES.DEVELOPER,
    labelKey: "perc.ui.dashboard.modern@Developer",
  },
  {
    value: HOMEPAGE_TYPES.PUBLISH,
    labelKey: "perc.ui.navMenu.publish@Publish",
  },
  {
    value: HOMEPAGE_TYPES.WORKFLOW,
    labelKey: "perc.ui.navMenu.admin@Administration",
  },
] as const;

/**
 * Stored Editor/Design/Widget Builder overrides stay visible once so the
 * user can clear them after those items leave top nav (#3514 / #3536 / #3538).
 */
export const STALE_PROFILE_LANDING_OPTIONS: readonly ProfileLandingOption[] = [
  {
    value: HOMEPAGE_TYPES.EDITOR,
    labelKey: "perc.ui.navMenu.webmgt@Editor",
  },
  {
    value: HOMEPAGE_TYPES.DESIGNER,
    labelKey: "perc.ui.navMenu.design@Design",
  },
  {
    value: HOMEPAGE_TYPES.WIDGET_BUILDER,
    labelKey: "perc.ui.navMenu.admin@Widget Builder",
  },
] as const;

export type ProfileLandingGates = LandingRoleGates;

/**
 * Whether the signed-in user may choose a landing type (peer SPA nav gates).
 */
export function isProfileLandingAllowed(
  homepageType: HomepageType | "",
  gates: ProfileLandingGates,
): boolean {
  return isLandingAllowed(homepageType, gates);
}

/**
 * Options for the select: role-default + allowed product screens.
 * If the currently stored override is no longer allowed, it is still listed.
 */
export function profileLandingOptions(
  gates: ProfileLandingGates,
  currentValue?: string | null,
): ProfileLandingOption[] {
  const allowed = PROFILE_LANDING_OPTIONS.filter((opt) =>
    isProfileLandingAllowed(opt.value, gates),
  );
  const current = currentValue == null ? "" : String(currentValue).trim();
  if (current && !allowed.some((o) => o.value === current)) {
    const extra =
      PROFILE_LANDING_OPTIONS.find((o) => o.value === current) ??
      STALE_PROFILE_LANDING_OPTIONS.find((o) => o.value === current);
    if (extra) {
      return [...allowed, extra];
    }
  }
  return allowed;
}
