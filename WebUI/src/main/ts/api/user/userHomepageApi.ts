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

import { del, get, putPlainText } from "../client";
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

function asPlainString(data: unknown): string {
  if (data == null) {
    return "";
  }
  if (typeof data === "string") {
    return data.trim();
  }
  return String(data).trim();
}

/**
 * GET persisted override for a user. Returns {@code ""} when unset/invalid.
 * Pass blank {@code userName} for the signed-in self endpoint.
 */
export async function getUserHomepageOverride(
  userName?: string,
): Promise<string> {
  const data = await get<unknown>(homepageUrl(userName));
  return asPlainString(data);
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
    await clearUserHomepageOverride(userName);
    return "";
  }
  const data = await putPlainText<unknown>(homepageUrl(userName), body);
  return asPlainString(data) || body;
}

/** DELETE override for a named user (or self when name omitted). */
export async function clearUserHomepageOverride(
  userName?: string,
): Promise<void> {
  await del(homepageUrl(userName));
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
