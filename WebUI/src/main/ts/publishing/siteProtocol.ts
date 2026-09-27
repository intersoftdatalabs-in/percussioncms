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

export const SITE_PROTOCOLS = ["http", "https"] as const;

export type SiteProtocol = (typeof SITE_PROTOCOLS)[number];

/** Lowercase trimmed protocol, or empty when the stored value is blank. */
export function normalizeSiteProtocol(raw: string): string {
  return raw.trim().toLowerCase();
}

export function isValidSiteProtocol(raw: string): raw is SiteProtocol {
  const value = normalizeSiteProtocol(raw);
  return value === "http" || value === "https";
}

/** True when a save would not change the stored http/https protocol. */
export function protocolsMatch(saved: string, draft: string): boolean {
  return (
    isValidSiteProtocol(saved) &&
    isValidSiteProtocol(draft) &&
    normalizeSiteProtocol(saved) === normalizeSiteProtocol(draft)
  );
}

export type SiteProtocolHttpFailure = "bad_request" | "forbidden" | "conflict";

/** HTTP statuses that must stay on the form and must not be treated as success. */
export function siteProtocolHttpFailure(
  status: number,
): SiteProtocolHttpFailure | null {
  if (status === 400) {
    return "bad_request";
  }
  if (status === 403) {
    return "forbidden";
  }
  if (status === 409) {
    return "conflict";
  }
  return null;
}
