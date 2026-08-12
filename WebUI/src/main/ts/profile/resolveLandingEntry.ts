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
 * Map stored homepage / landing tokens to SPA entry + client path (#3219).
 *
 * <p>Canonical stored type remains {@code Architecture}. Product name
 * Navigation and view-key aliases ({@code arch}, {@code navigation}) must
 * resolve to the Architecture SPA, not Home.</p>
 */

import { HOMEPAGE_TYPES } from "../api/user/userHomepageApi";
import type { SpaEntry } from "../app/deepLinks/allowlists";

/** Unauthenticated login posts here so index.jsp can apply getUserHomepage(). */
export const POST_LOGIN_DISPATCHER = "/cm/app/";

/**
 * Normalize a homepage type, view key, or product label to an SPA entry.
 * Blank / unknown → {@code home} (same fail-closed as server mapping).
 */
export function resolveHomepageToSpaEntry(
  raw: string | null | undefined,
): SpaEntry {
  const trimmed = raw == null ? "" : String(raw).trim();
  if (!trimmed) {
    return "home";
  }
  switch (trimmed) {
    case HOMEPAGE_TYPES.HOME:
      return "home";
    case HOMEPAGE_TYPES.DASHBOARD:
      return "home";
    case HOMEPAGE_TYPES.EDITOR:
      return "home";
    case HOMEPAGE_TYPES.DESIGNER:
      return "design";
    case HOMEPAGE_TYPES.ARCHITECTURE:
      return "architecture";
    case HOMEPAGE_TYPES.PUBLISH:
      return "publish";
    case HOMEPAGE_TYPES.WORKFLOW:
      return "admin";
    case HOMEPAGE_TYPES.WIDGET_BUILDER:
      return "widget-builder";
    default:
      break;
  }
  const lower = trimmed.toLowerCase();
  switch (lower) {
    case "home":
      return "home";
    case "dash":
    case "dashboard":
      return "home";
    case "editor":
    case "pageeditor":
    case "webmgt":
      return "home";
    case "design":
    case "designer":
    case "siteadmin":
      return "design";
    case "arch":
    case "architecture":
    case "navigation":
    case "site_arch":
    case "sitearch":
      return "architecture";
    case "publish":
      return "publish";
    case "workflow":
    case "admin":
      return "admin";
    case "widgetbuilder":
    case "widget-builder":
    case "widget_builder":
      return "widget-builder";
    case "explorer":
      return "explorer";
    case "profile":
      return "profile";
    case "developer":
      return "developer";
    default:
      return "home";
  }
}

/** Client route under the SPA basename for a stored homepage token. */
export function resolveHomepageToClientPath(
  raw: string | null | undefined,
): string {
  const entry = resolveHomepageToSpaEntry(raw);
  switch (entry) {
    case "home":
      return "/home";
    case "widget-builder":
      return "/widget-builder";
    default:
      return `/${entry}`;
  }
}

/** True when a stored or typed landing token means Navigation / Architecture. */
export function isNavigationHomepageToken(
  raw: string | null | undefined,
): boolean {
  return resolveHomepageToSpaEntry(raw) === "architecture";
}
