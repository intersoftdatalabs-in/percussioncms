/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import type { PublishSiteSummary, SiteListViewMode } from "./types";

/** Filter sites by name substring (case-insensitive). */
export function filterSitesByName(
  sites: PublishSiteSummary[],
  filter: string,
): PublishSiteSummary[] {
  const q = filter.trim().toLowerCase();
  if (!q) {
    return sites;
  }
  return sites.filter((s) => (s.name ?? "").toLowerCase().includes(q));
}

/** Toggle card/list view mode. */
export function nextViewMode(mode: SiteListViewMode): SiteListViewMode {
  return mode === "card" ? "list" : "card";
}

/** Stable site key for selection (prefer id/siteId, fall back to name). */
export function siteKey(site: PublishSiteSummary): string {
  if (site.siteId != null && String(site.siteId) !== "") {
    return String(site.siteId);
  }
  if (site.id != null && String(site.id) !== "") {
    return String(site.id);
  }
  return site.name ?? "";
}
