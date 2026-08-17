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
 * Client for user default CMS landing-page override (#2209 / #2211).
 *
 * <p>REST (text/plain): GET/PUT/DELETE {@code /user/user/homepage/{userName}}.
 * Empty string means no override (role homepage resolve → Home).</p>
 */

import { del, get, isApiError, putPlainText } from "../client";
import { PATHS } from "../paths";

/** Canonical product types accepted by the server (PascalCase). */
export const HOMEPAGE_TYPES = {
  HOME: "Home",
  DASHBOARD: "Dashboard",
  EDITOR: "Editor",
  DESIGNER: "Designer",
  ARCHITECTURE: "Architecture",
  PUBLISH: "Publish",
  WORKFLOW: "Workflow",
  WIDGET_BUILDER: "WidgetBuilder",
  EXPLORER: "Explorer",
  DEVELOPER: "Developer",
} as const;

export type HomepageType =
  (typeof HOMEPAGE_TYPES)[keyof typeof HOMEPAGE_TYPES];

function homepageUrl(userName?: string): string {
  const base = PATHS.USER_HOMEPAGE;
  if (userName == null || !userName.trim()) {
    return base;
  }
  return `${base}/${encodeURIComponent(userName.trim())}`;
}

/** Prefer TEXT_PLAIN so CXF does not JSON-quote the product type string. */
const HOMEPAGE_ACCEPT: HeadersInit = { Accept: "text/plain, */*" };

function isNotFound(err: unknown): boolean {
  return isApiError(err) && err.status === 404;
}

/**
 * Unwrap a homepage GET/PUT body to a trimmed string.
 *
 * <p>Handles raw {@code text/plain}, leftover JSON-quoted {@code "Editor"},
 * and small object wrappers ({@code homepage}/{@code value}/{@code data}).</p>
 */
export function asPlainHomepageString(data: unknown): string {
  if (data == null) {
    return "";
  }
  if (typeof data === "string") {
    let s = data.trim();
    if (s.length >= 2 && s.startsWith('"') && s.endsWith('"')) {
      try {
        const parsed: unknown = JSON.parse(s);
        if (typeof parsed === "string") {
          s = parsed.trim();
        }
      } catch {
        /* keep trimmed original */
      }
    }
    return s;
  }
  if (typeof data === "object" && !Array.isArray(data)) {
    const o = data as Record<string, unknown>;
    for (const key of ["homepage", "value", "data", "homepageType"]) {
      if (typeof o[key] === "string") {
        return asPlainHomepageString(o[key]);
      }
    }
  }
  return String(data).trim();
}

const HOMEPAGE_ALIAS: Record<string, HomepageType> = {
  home: HOMEPAGE_TYPES.HOME,
  dash: HOMEPAGE_TYPES.DASHBOARD,
  dashboard: HOMEPAGE_TYPES.DASHBOARD,
  editor: HOMEPAGE_TYPES.EDITOR,
  pageeditor: HOMEPAGE_TYPES.EDITOR,
  webmgt: HOMEPAGE_TYPES.EDITOR,
  design: HOMEPAGE_TYPES.DESIGNER,
  designer: HOMEPAGE_TYPES.DESIGNER,
  siteadmin: HOMEPAGE_TYPES.DESIGNER,
  admin: HOMEPAGE_TYPES.DESIGNER,
  arch: HOMEPAGE_TYPES.ARCHITECTURE,
  architecture: HOMEPAGE_TYPES.ARCHITECTURE,
  navigation: HOMEPAGE_TYPES.ARCHITECTURE,
  site_arch: HOMEPAGE_TYPES.ARCHITECTURE,
  sitearch: HOMEPAGE_TYPES.ARCHITECTURE,
  publish: HOMEPAGE_TYPES.PUBLISH,
  workflow: HOMEPAGE_TYPES.WORKFLOW,
  widgetbuilder: HOMEPAGE_TYPES.WIDGET_BUILDER,
  "widget-builder": HOMEPAGE_TYPES.WIDGET_BUILDER,
  widget_builder: HOMEPAGE_TYPES.WIDGET_BUILDER,
  explorer: HOMEPAGE_TYPES.EXPLORER,
  developer: HOMEPAGE_TYPES.DEVELOPER,
};

/**
 * Canonical product homepage type (PascalCase) or {@code ""} when unset.
 * Unknown non-empty values are returned trimmed (server validates on PUT).
 */
export function canonicalizeHomepageType(raw: unknown): string {
  const s = asPlainHomepageString(raw);
  if (!s) {
    return "";
  }
  if ((Object.values(HOMEPAGE_TYPES) as string[]).includes(s)) {
    return s;
  }
  return HOMEPAGE_ALIAS[s.toLowerCase()] ?? s;
}

/**
 * GET persisted override for a user. Returns {@code ""} when unset/invalid.
 * Pass blank {@code userName} for the signed-in self endpoint.
 */
export async function getUserHomepageOverride(
  userName?: string,
): Promise<string> {
  try {
    const data = await get<unknown>(homepageUrl(userName), HOMEPAGE_ACCEPT);
    return canonicalizeHomepageType(data);
  } catch (err) {
    // Unset override is empty, not a hard failure for the Preferences select.
    if (isNotFound(err)) {
      return "";
    }
    throw err;
  }
}

/**
 * PUT override. Blank {@code homepage} clears (role/Home fallback).
 * Returns the stored canonical type or {@code ""}.
 * Pass blank {@code userName} for the signed-in self endpoint.
 */
export async function setUserHomepageOverride(
  userName: string | undefined,
  homepage: string,
): Promise<string> {
  const body = homepage == null ? "" : String(homepage).trim();
  if (!body) {
    try {
      await clearUserHomepageOverride(userName);
    } catch (err) {
      if (!isNotFound(err)) {
        throw err;
      }
    }
    return "";
  }
  const canonical = canonicalizeHomepageType(body) || body;
  const data = await putPlainText<unknown>(
    homepageUrl(userName),
    canonical,
    HOMEPAGE_ACCEPT,
  );
  return canonicalizeHomepageType(data) || canonical;
}

/** DELETE override for a named user (or self when name omitted). */
export async function clearUserHomepageOverride(
  userName?: string,
): Promise<void> {
  try {
    await del(homepageUrl(userName), HOMEPAGE_ACCEPT);
  } catch (err) {
    if (!isNotFound(err)) {
      throw err;
    }
  }
}

/** GET self default landing override (no user name on path — no IDOR). */
export async function getMyHomepageOverride(): Promise<string> {
  return getUserHomepageOverride();
}

/**
 * PUT self default landing override. Blank clears to role/Home fallback.
 * No user name on path — always the signed-in session user.
 */
export async function setMyHomepageOverride(
  homepage: string,
): Promise<string> {
  return setUserHomepageOverride(undefined, homepage);
}
