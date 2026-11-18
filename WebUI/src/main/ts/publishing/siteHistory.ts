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
 * Site-scoped publishing history audit helpers (#6473, parent #4531).
 * The per-site audit view reuses the Logs request/response shape
 * (`POST …/pubstatus/logs`) with the workspace site fixed, and exports
 * the visible rows through the shared Logs CSV builder.
 */

import { message, MSG } from "../i18n/message";

export const SITE_HISTORY_CSV_PREFIX = "publish-history";

export const SITE_HISTORY_FALLBACK_SLUG = "site";

/**
 * Slug for the per-site export filename. Lowercases the site name,
 * keeps letters/digits/dash/underscore, and collapses the rest to a
 * single dash; blank or punctuation-only names fall back to "site".
 * Never returns an empty segment, so the download always has a name.
 */
export function slugifySiteName(siteName: unknown): string {
  const raw = String(siteName ?? "")
    .trim()
    .toLowerCase();
  if (!raw) {
    return SITE_HISTORY_FALLBACK_SLUG;
  }
  const slug = raw
    .replace(/[^a-z0-9_-]+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^[-_]+|[-_]+$/g, "");
  return slug || SITE_HISTORY_FALLBACK_SLUG;
}

/** `publish-history-<site-slug>.csv` for the export download. */
export function siteHistoryExportFilename(siteName: unknown): string {
  return `${SITE_HISTORY_CSV_PREFIX}-${slugifySiteName(siteName)}.csv`;
}

/**
 * Audit count line via {@code PUBLISH_SITE_HISTORY_COUNT} ({0}=visible,
 * {1}=total). Placeholders are substituted after the TMX resolve — the
 * same convention as the security audit page summary — so the English
 * key fallback and a translated template with reordered placeholders
 * both render the counts.
 */
export function describeSiteHistoryCount(
  visible: number,
  total: number,
): string {
  return message(MSG.PUBLISH_SITE_HISTORY_COUNT)
    .replace("{0}", String(visible))
    .replace("{1}", String(total));
}
