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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Default landing options for Admin → Users editor (#2211 / #3537 / #3538).
 *
 * <p>Labels use nav menu i18n keys. Values are remaining top-nav homepage
 * types (or empty for clear → role resolve). The option set matches User
 * Profile Preferences (parent #3515 / slice 1 / #3537): Explorer in;
 * Editor/Design not offered as new choices after they left top nav (#3514).
 * Options are filtered to screens the assigned roles may open via shared
 * {@code isLandingAllowed} gates (#3538). Editor, Design, and Widget Builder
 * are not offered as new choices.</p>
 */

import { HOMEPAGE_TYPES, type HomepageType } from "../../api/user/userHomepageApi";
import {
  isLandingAllowed,
  landingGatesFromRoles,
} from "../../app/landing/landingPermission";

export interface LandingOption {
  /** Canonical API value, or empty string for "use role default". */
  value: HomepageType | "";
  /** TMX message key (nav-aligned). */
  labelKey: string;
}

/**
 * Product options exposed in the Users editor (#3537 / #3538 / parent #3515).
 * Empty value = no user override (role Homepage / Home fallback).
 * Matches remaining top-nav apps (Home, Explorer, Navigation, Developer,
 * Publish, Admin). Editor / Design / Widget Builder are not new choices.
 */
export const ALL_LANDING_OPTIONS: readonly LandingOption[] = [
  {
    value: "",
    labelKey: "perc.ui.users@Use role default",
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
    // Product name Navigation; SPA path /architecture (#3094 / #3217 / #3219)
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
    // Workflow admin SPA (still a valid homepage); top-nav Admin lands on tools (#2784)
    labelKey: "perc.ui.navMenu.admin@Administration",
  },
] as const;

/**
 * Stored Editor/Design/Widget Builder overrides stay visible once so the
 * admin can clear them after those items leave top nav (#3514 / #3537 / #3538).
 */
export const STALE_LANDING_OPTIONS: readonly LandingOption[] = [
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

/**
 * Whether a user with the given roles may open a landing type.
 * Aligns with SPA TopNav / shared {@code isLandingAllowed} gates (#3538).
 */
export function isLandingAllowedForRoles(
  homepageType: HomepageType | "",
  roles: readonly string[] | null | undefined,
): boolean {
  return isLandingAllowed(homepageType, landingGatesFromRoles(roles));
}

/**
 * Options for the select control: role-default + allowed product screens.
 * If the currently stored override is no longer allowed (roles changed), it is
 * still included so the admin can see and clear it.
 */
export function landingOptionsForRoles(
  roles: readonly string[] | null | undefined,
  currentValue?: string | null,
): LandingOption[] {
  const allowed = ALL_LANDING_OPTIONS.filter((opt) =>
    isLandingAllowedForRoles(opt.value, roles),
  );
  const current = currentValue == null ? "" : String(currentValue).trim();
  if (current && !allowed.some((o) => o.value === current)) {
    const extra =
      ALL_LANDING_OPTIONS.find((o) => o.value === current) ??
      STALE_LANDING_OPTIONS.find((o) => o.value === current);
    if (extra) {
      return [...allowed, extra];
    }
  }
  return allowed;
}
