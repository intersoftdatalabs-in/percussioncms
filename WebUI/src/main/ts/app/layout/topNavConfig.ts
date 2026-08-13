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
 * Primary product top-nav order and role gates (issue #2702 / #2784 / #3088 / #3201).
 *
 * Dashboard is intentionally omitted from top chrome (gadgets remain on Home
 * via deep link {@code /home/gadgets}). Administration + Admin tools collapse
 * to a single {@code admin} item labeled <strong>Admin</strong>.
 *
 * <p>Landing is the unified <strong>Admin tools</strong> shell at {@code /admin}
 * (#2784 / #3088 / #3201) — not a Workflow-only hub. Workflow / roles / users /
 * categories are Admin tabs; legacy {@code /workflow} paths redirect into
 * {@code /admin/*}.</p>
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
 * Client path for the consolidated Admin top-nav NavLink (#2784 / #3088).
 * Unified Admin shell (tasks, tools, workflow, roles, users, categories).
 */
export const ADMIN_NAV_LANDING = "/admin";

/**
 * Visible top-nav label for the consolidated Admin item (#2702 / #3201).
 *
 * TMX tuid {@code perc.ui.navMenu.admin@Administration} already ships en-us
 * {@code Admin}. When TMX is missing, the SPA message helper falls back to
 * the {@code @Administration} suffix — operators then see the old dual-entry
 * word. Normalize that English leftover so chrome always reads Admin.
 * Non-English TMX strings are left unchanged.
 */
export function adminTopNavLabel(resolved: string): string {
  const t = (resolved || "").trim();
  if (!t || t === "Administration") {
    return "Admin";
  }
  return t;
}

/**
 * Client path for Navigation top-nav NavLink and homepage SPA landing (#3094).
 * Primary Navigation entry (SPA). Legacy {@code ?view=arch} /
 * {@code siteArchitecture.jsp} hard-redirect here (#3099).
 */
export const ARCHITECTURE_NAV_LANDING = "/architecture";

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
 * Covers unified Admin shell paths and legacy {@code /workflow*} redirects (#3088).
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
