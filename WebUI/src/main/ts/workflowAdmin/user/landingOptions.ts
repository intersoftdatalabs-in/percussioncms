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
 * Default landing options for Admin → Users editor (#2211 / parent #959).
 *
 * <p>Labels use nav menu i18n keys. Values are slice-2 canonical homepage
 * types (or empty for clear → role resolve). Options are filtered to screens
 * the assigned roles may open (peer SPA TopNav gates).</p>
 */

import { HOMEPAGE_TYPES, type HomepageType } from "../../api/user/userHomepageApi";

export interface LandingOption {
  /** Canonical API value, or empty string for "use role default". */
  value: HomepageType | "";
  /** TMX message key (nav-aligned). */
  labelKey: string;
}

/**
 * Product options exposed in the Users editor.
 * Empty value = no user override (role Homepage / Home fallback).
 * Role Homepage field is intentionally left alone — this is a user-level override.
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
    value: HOMEPAGE_TYPES.EDITOR,
    labelKey: "perc.ui.navMenu.webmgt@Editor",
  },
  {
    value: HOMEPAGE_TYPES.DESIGNER,
    labelKey: "perc.ui.navMenu.design@Design",
  },
  {
    value: HOMEPAGE_TYPES.ARCHITECTURE,
    // Product "Navigation" / Architecture SPA (#3094 → /architecture)
    labelKey: "perc.ui.navMenu.architecture@Architecture",
  },
  {
    value: HOMEPAGE_TYPES.WORKFLOW,
    // Workflow admin SPA (still a valid homepage); top-nav Admin lands on tools (#2784)
    labelKey: "perc.ui.navMenu.admin@Administration",
  },
] as const;

function roleSet(roles: readonly string[] | null | undefined): Set<string> {
  const set = new Set<string>();
  if (!roles) {
    return set;
  }
  for (const r of roles) {
    if (r != null && String(r).trim()) {
      set.add(String(r).trim().toLowerCase());
    }
  }
  return set;
}

/**
 * Whether a user with the given roles may open a landing type.
 * Aligns with SPA TopNav / RequireRole gates (Admin, Admin|Designer).
 */
export function isLandingAllowedForRoles(
  homepageType: HomepageType | "",
  roles: readonly string[] | null | undefined,
): boolean {
  if (homepageType === "" || homepageType === HOMEPAGE_TYPES.HOME) {
    return true;
  }
  if (homepageType === HOMEPAGE_TYPES.EDITOR) {
    return true;
  }
  const rs = roleSet(roles);
  const isAdmin = rs.has("admin");
  const isDesigner = rs.has("designer") || isAdmin;
  switch (homepageType) {
    case HOMEPAGE_TYPES.DESIGNER:
    case HOMEPAGE_TYPES.ARCHITECTURE:
    case HOMEPAGE_TYPES.PUBLISH:
    case HOMEPAGE_TYPES.WIDGET_BUILDER:
      return isDesigner;
    case HOMEPAGE_TYPES.WORKFLOW:
    case HOMEPAGE_TYPES.DASHBOARD:
      // Administration tools / dashboard chrome: Admin (dashboard also open to all historically)
      return homepageType === HOMEPAGE_TYPES.DASHBOARD ? true : isAdmin;
    default:
      return isAdmin;
  }
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
  if (
    current &&
    !allowed.some((o) => o.value === current) &&
    ALL_LANDING_OPTIONS.some((o) => o.value === current)
  ) {
    const extra = ALL_LANDING_OPTIONS.find((o) => o.value === current);
    if (extra) {
      return [...allowed, extra];
    }
  }
  return allowed;
}
