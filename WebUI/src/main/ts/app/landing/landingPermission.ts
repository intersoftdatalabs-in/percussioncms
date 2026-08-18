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
 * Shared homepage-picker permission gates (#3538 / parent #3515 slice 3).
 *
 * <p>Matches live {@code topNavItemIds} after #3514: Explorer is always
 * allowed; Navigation / Developer / Publish require Designer or Admin;
 * Admin (Workflow homepage type) requires Admin. Editor, Design, and
 * Widget Builder are not new landing choices. Stale stored values are
 * listed once by the picker helpers so they can be cleared.</p>
 */

import { HOMEPAGE_TYPES, type HomepageType } from "../../api/user/userHomepageApi";

export type LandingRoleGates = {
  isAdmin?: boolean;
  isDesigner?: boolean;
};

/**
 * Admin / Designer flags from assigned role names (Admin Users picker).
 * Role names are compared case-insensitively.
 */
export function landingGatesFromRoles(
  roles: readonly string[] | null | undefined,
): LandingRoleGates {
  const set = new Set<string>();
  if (roles) {
    for (const r of roles) {
      if (r != null && String(r).trim()) {
        set.add(String(r).trim().toLowerCase());
      }
    }
  }
  const isAdmin = set.has("admin");
  return { isAdmin, isDesigner: set.has("designer") || isAdmin };
}

/**
 * Whether a homepage type may be offered as a <strong>new</strong> landing
 * choice. Aligns with {@code topNavItemIds} (Explorer always; Navigation /
 * Developer / Publish for designer|admin; Admin for admin).
 *
 * Widget Builder is not a top-nav item after #3514 and is omitted as a
 * landing unless a stored override is kept via the stale-once picker path.
 */
export function isLandingAllowed(
  homepageType: HomepageType | "",
  gates: LandingRoleGates,
): boolean {
  if (
    homepageType === "" ||
    homepageType === HOMEPAGE_TYPES.HOME ||
    homepageType === HOMEPAGE_TYPES.EXPLORER
  ) {
    return true;
  }
  if (
    homepageType === HOMEPAGE_TYPES.EDITOR ||
    homepageType === HOMEPAGE_TYPES.DESIGNER ||
    homepageType === HOMEPAGE_TYPES.WIDGET_BUILDER
  ) {
    return false;
  }
  const isAdmin = !!gates.isAdmin;
  const isDesigner = !!gates.isDesigner || isAdmin;
  switch (homepageType) {
    case HOMEPAGE_TYPES.ARCHITECTURE:
    case HOMEPAGE_TYPES.DEVELOPER:
    case HOMEPAGE_TYPES.PUBLISH:
      return isDesigner;
    case HOMEPAGE_TYPES.WORKFLOW:
      return isAdmin;
    case HOMEPAGE_TYPES.DASHBOARD:
      return true;
    default:
      return isAdmin;
  }
}
