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
 * Default homepage options for Admin → Roles editor (#3537 / parent #3515).
 *
 * <p>Same remaining top-nav apps as User Profile Preferences and Admin → Users
 * (Explorer in; Editor/Design not new choices). Role homepage is a stored
 * default, not a "use role default" empty value.</p>
 */

import { HOMEPAGE_TYPES, type HomepageType } from "../../api/user/userHomepageApi";

export interface RoleLandingOption {
  /** Canonical API value (never empty — roles always store a homepage). */
  value: HomepageType;
  /** TMX message key (nav-aligned). */
  labelKey: string;
}

/**
 * Remaining top-nav apps an admin can assign as a role default homepage.
 */
export const ROLE_LANDING_OPTIONS: readonly RoleLandingOption[] = [
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
 * Stale stored role values remain visible once so they can be cleared
 * (Editor / Design / Dashboard were previously valid).
 */
export const STALE_ROLE_LANDING_OPTIONS: readonly RoleLandingOption[] = [
  {
    value: HOMEPAGE_TYPES.EDITOR,
    labelKey: "perc.ui.navMenu.webmgt@Editor",
  },
  {
    value: HOMEPAGE_TYPES.DESIGNER,
    labelKey: "perc.ui.navMenu.design@Design",
  },
  {
    value: HOMEPAGE_TYPES.DASHBOARD,
    labelKey: "perc.ui.navMenu.dashboard@Dashboard",
  },
] as const;

/** Canonical default when a role has no stored homepage. */
export const DEFAULT_ROLE_HOMEPAGE: HomepageType = HOMEPAGE_TYPES.HOME;

/**
 * Normalize a stored role homepage to a product type, or Home when unset.
 */
export function canonicalizeRoleHomepage(raw?: string | null): HomepageType | string {
  const current = raw == null ? "" : String(raw).trim();
  return current || DEFAULT_ROLE_HOMEPAGE;
}

/**
 * Options for the Role editor homepage select.
 * Slice 3 (#3538) will tighten by permission; this slice lists remaining apps.
 * Stale current values stay listed once so the admin can change them.
 */
export function roleLandingOptions(
  currentValue?: string | null,
): RoleLandingOption[] {
  const allowed = [...ROLE_LANDING_OPTIONS];
  const current = canonicalizeRoleHomepage(currentValue);
  if (!allowed.some((o) => o.value === current)) {
    const extra = STALE_ROLE_LANDING_OPTIONS.find((o) => o.value === current);
    if (extra) {
      return [...allowed, extra];
    }
  }
  return allowed;
}
