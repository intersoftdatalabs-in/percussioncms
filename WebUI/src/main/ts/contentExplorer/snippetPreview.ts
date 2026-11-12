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
 * Per-row snippet preview for related-content rows (#6224 / parent #6104).
 *
 * <p>Classic {@code updaterelateditems.html} rendered a per-row eye icon from
 * a {@code previewurl} snippet. The React equivalent resolves the snippet
 * through the same assembly endpoint the template preview uses
 * ({@code GET /services/assembly/preview-location?contentId=&templateId=})
 * and opens the returned assembler URL in a new popup window — the same
 * window shape as the Explorer template preview
 * ({@code percTemplatePreview_*}) and the classic eye icon. It is not an
 * in-place iframe: related rows are predominantly snippets and the
 * assembler render is the product surface that assembles them.</p>
 *
 * <p>Rows without a known snippet template (inline local links, slot rows
 * with {@code templateId <= 0}) have no preview affordance: there is no
 * template to assemble. Non-assembler preview URLs are rejected rather
 * than opened.</p>
 */

import { fetchPreviewLocation } from "../api/contentExplorer/assemblyApi";
import { withCmsContextPrefix } from "../api/paths";

export interface SnippetPreviewRequest {
  contentId: number;
  templateId: number;
}

export interface SnippetPreviewDeps {
  fetchPreview?: typeof fetchPreviewLocation;
  openWindow?: (
    url: string,
    target?: string,
    features?: string,
  ) => Window | null;
}

function positiveInt(raw: unknown): number | null {
  const n = Number(raw);
  if (!Number.isFinite(n) || !Number.isInteger(n) || n <= 0) {
    return null;
  }
  return n;
}

/**
 * Whether a related row can offer a snippet preview. Both the snippet
 * content id and the snippet template id must be positive integers.
 */
export function canPreviewSnippetRow(
  contentId: unknown,
  templateId: unknown,
): boolean {
  return positiveInt(contentId) != null && positiveInt(templateId) != null;
}

function defaultOpenWindow(
  url: string,
  target?: string,
  features?: string,
): Window | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.open(url, target ?? "_blank", features ?? "");
}

/** Named popup window for one snippet preview (classic eye-icon shape). */
export function snippetPreviewWindowName(contentId: number): string {
  return `percSnippetPreview_${contentId}`;
}

/**
 * Open the assembled snippet preview for one related row. Throws when the
 * row has no positive content id / template id, when the preview-location
 * lookup fails, or when the returned URL is not an assembler render.
 */
export async function openSnippetPreview(
  request: SnippetPreviewRequest,
  deps: SnippetPreviewDeps = {},
): Promise<void> {
  const contentId = positiveInt(request.contentId);
  const templateId = positiveInt(request.templateId);
  if (contentId == null || templateId == null) {
    throw new Error("Snippet preview needs a content id and a template id");
  }
  const fetchPreview = deps.fetchPreview ?? fetchPreviewLocation;
  const openWindow = deps.openWindow ?? defaultOpenWindow;
  const loc = await fetchPreview(contentId, templateId);
  const previewUrl = (loc.previewUrl ?? "").trim();
  if (!previewUrl.toLowerCase().includes("/assembler/render")) {
    throw new Error("Snippet preview location is not an assembler URL");
  }
  const href = /^https?:\/\//i.test(previewUrl)
    ? previewUrl
    : withCmsContextPrefix(
        previewUrl.startsWith("/") ? previewUrl : `/${previewUrl}`,
      );
  openWindow(href, snippetPreviewWindowName(contentId));
}
