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
 * Deep-link builder for the chrome-less Active Assembly window.
 *
 * <p>Server entry stays on the query contract
 * {@code /cm/app/spa.jsp?entry=assembly&contentId=&templateId=}. The SPA
 * rewrites that to {@code /cm/app/assembly?…}.</p>
 */

import { withCmsContextPrefix } from "../api/paths";

export const ASSEMBLY_WINDOW_FEATURES =
  "popup=yes,width=1280,height=900,scrollbars=yes,resizable=yes";

/** Re-export so existing assembly/editor callers stay unchanged. */
export { withCmsContextPrefix };

export function parsePositiveInt(raw: string | null | undefined): number | null {
  if (raw == null || !String(raw).trim()) {
    return null;
  }
  const n = Number(String(raw).trim());
  return Number.isFinite(n) && n > 0 ? Math.trunc(n) : null;
}

/**
 * Named-window URL for Explorer Active Assembly. {@code templateId} is
 * optional — the host loads {@code isAA} page/snippet templates when omitted.
 */
export function buildAssemblyHostUrl(
  contentId: number,
  templateId?: number | null,
): string {
  const q = new URLSearchParams();
  q.set("entry", "assembly");
  q.set("contentId", String(contentId));
  if (templateId != null && templateId > 0) {
    q.set("templateId", String(templateId));
  }
  return withCmsContextPrefix(`/cm/app/spa.jsp?${q.toString()}`);
}

export function assemblyWindowName(contentId: number): string {
  return `percAssembly_${contentId}`;
}
