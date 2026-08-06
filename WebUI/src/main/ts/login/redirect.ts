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
 * Allowlisted SPA post-login entry paths (server entry contract — query only).
 *
 * @see docs/ai-generated/tasks/#000-pure-react-spa/design.md
 */
export const DEFAULT_SPA_ENTRY_REDIRECT = "/cm/app/spa.jsp?entry=home";

const ALLOWED_ENTRIES = new Set([
  "home",
  "publish",
  "workflow",
  "admin",
  "widget-builder",
  "explorer",
  "unavailable",
]);

/**
 * Builds a path-absolute SPA entry URL. Rejects open redirects and fragments.
 *
 * @param entry - allowlisted entry id
 * @param extra - optional allowlisted query pairs (section, tab, path, siteId, serverId)
 * @returns safe path + query, or default home entry
 */
export function buildSpaEntryRedirect(
  entry: string = "home",
  extra: Record<string, string> = {},
): string {
  const e = (entry || "home").trim().toLowerCase();
  const safeEntry = ALLOWED_ENTRIES.has(e) ? e : "home";
  const params = new URLSearchParams();
  params.set("entry", safeEntry);

  const allowExtra = new Set(["section", "tab", "path", "siteId", "serverId"]);
  for (const [k, v] of Object.entries(extra)) {
    if (!allowExtra.has(k) || v == null) continue;
    const trimmed = String(v).trim();
    if (!trimmed || trimmed.length > 2048) continue;
    // path must be relative absolute path without schemes
    if (k === "path") {
      if (!trimmed.startsWith("/") || trimmed.includes("://") || trimmed.includes("..")) {
        continue;
      }
    }
    params.set(k, trimmed);
  }

  return `/cm/app/spa.jsp?${params.toString()}`;
}

/**
 * Validates a candidate post-login redirect for use as the form {@code sys_redirect} value.
 * Only path-absolute same-app URLs to the SPA (or known CMS app roots) are accepted.
 *
 * @param candidate - raw redirect from bootstrap or query
 * @param fallback - used when candidate is invalid
 */
export function sanitizeLoginRedirect(
  candidate: string | null | undefined,
  fallback: string = DEFAULT_SPA_ENTRY_REDIRECT,
): string {
  if (candidate == null || !String(candidate).trim()) {
    return fallback;
  }
  const raw = String(candidate).trim();
  // Never allow fragments or schemes (open redirect / fragment redirect bugs)
  if (raw.includes("#") || raw.includes("://") || raw.startsWith("//")) {
    return fallback;
  }
  if (!raw.startsWith("/") || raw.includes("..")) {
    return fallback;
  }
  // Prefer SPA entry; also allow /cm/app for transitional landings
  if (raw.startsWith("/cm/app/spa.jsp") || raw === "/cm/app" || raw.startsWith("/cm/app?")) {
    return raw.length > 2048 ? fallback : raw;
  }
  return fallback;
}
