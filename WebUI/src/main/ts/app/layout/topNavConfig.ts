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
 * Primary product top-nav order and role gates (issue #2702).
 *
 * Dashboard is intentionally omitted from top chrome (gadgets remain on Home
 * via deep link {@code /home/gadgets}). Administration + Admin tools collapse
 * to a single {@code admin} item whose landing is the workflow admin hub;
 * {@code /admin} remains a deep-linked route for Admin tools.
 */

export type TopNavItemId =
  | "home"
  | "explorer"
  | "editor"
  | "architecture"
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
    items.push("architecture", "developer", "publish");
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
 * Covers both workflow administration and admin-tools SPA routes.
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
