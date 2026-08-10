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
 * Primary product top-nav order and role gates (issue #2702 / #2784).
 *
 * Dashboard is intentionally omitted from top chrome (gadgets remain on Home
 * via deep link {@code /home/gadgets}). Administration + Admin tools collapse
 * to a single {@code admin} item.
 *
 * <p>Landing is the working <strong>Admin tools</strong> shell at {@code /admin}
 * (#2784). Workflow administration remains at {@code /workflow} via deep link
 * and the Admin tools sibling cross-link.</p>
 */

export type TopNavItemId =
  | "home"
  | "explorer"
  | "editor"
  | "architecture"
  | "design"
  | "developer"
  | "publish"
  | "admin"
  | "widget-builder";

export interface TopNavGates {
  isAdmin?: boolean;
  isDesigner?: boolean;
  isWidgetBuilderActive?: boolean;
}

/**
 * Client path for the consolidated Admin top-nav NavLink (#2784).
 * Prefer Admin tools over Workflow administration as the primary landing.
 */
export const ADMIN_NAV_LANDING = "/admin";

/**
 * Ordered top-nav item ids for the given role / feature gates.
 * Explorer is always immediately after Home.
 */
export function topNavItemIds(gates: TopNavGates = {}): TopNavItemId[] {
  const isAdmin = !!gates.isAdmin;
  const isDesigner = !!gates.isDesigner || isAdmin;
  const canPublish = isAdmin || isDesigner;
  const canWb =
    !!gates.isWidgetBuilderActive && (isAdmin || isDesigner);

  const items: TopNavItemId[] = ["home", "explorer", "editor"];
  if (canPublish) {
    items.push("architecture", "design", "developer", "publish");
  }
  if (isAdmin) {
    items.push("admin");
  }
  if (canWb) {
    items.push("widget-builder");
  }
  return items;
}

/**
 * Whether a client pathname should mark the consolidated Admin top-nav active.
 * Covers both admin-tools and workflow administration SPA routes.
 */
export function isAdminNavPath(pathname: string): boolean {
  const p = pathname || "";
  return (
    p === "/admin" ||
    p.startsWith("/admin/") ||
    p === "/workflow" ||
    p.startsWith("/workflow/")
  );
}
