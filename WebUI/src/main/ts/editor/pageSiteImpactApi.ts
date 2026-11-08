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
 * Page site-impact fetch for the React Content Editor (#5739, parent
 * #5622 slice 6 batch 2).
 *
 * <p>Same backing endpoint the retired classic
 * {@code perc_page_edit_dialog} used
 * ({@code GET item/siteimpact/page/{id}} via
 * {@code PercSiteImpactService.getSiteImpactDetails}); no contract
 * changes. Logical CMS URL paths use {@code /} (not OS file
 * separators).</p>
 */

import { get } from "../api/client";
import { PATHS } from "../api/paths";
import {
  parsePageSiteImpact,
  type PageSiteImpact,
} from "./editorPageMetadata";

/**
 * Load the reverse-link / relationship owners (pages + templates) for a
 * page item. The service produces {@code TEXT_PLAIN} JSON; the body parses
 * defensively (bare or legacy {@code data}-wrapped shapes).
 */
export async function fetchPageSiteImpact(
  itemId: string,
): Promise<PageSiteImpact> {
  const payload = await get<unknown>(
    `${PATHS.PAGE_SITE_IMPACT}/${encodeURIComponent(itemId)}`,
  );
  return parsePageSiteImpact(payload);
}
