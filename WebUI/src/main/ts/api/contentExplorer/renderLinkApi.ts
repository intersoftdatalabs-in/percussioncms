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
 * Typed client for the sitemanage inline render-link preview used by the
 * React content-picker lookup (#5900 / parent #5622).
 *
 * <p>Provider: sitemanage
 * {@code com.percussion.pagemanagement.service.impl.PSRenderLinkService} at
 * {@code GET …/pagemanagement/renderlink/preview/{id}/default} — the same
 * endpoint the classic {@code PercPathService.getInlineRenderLink} calls
 * before the TinyMCE {@code percadvlink} / {@code percadvimage} dialogs
 * persist a managed link or image. Server DTO:
 * {@code com.percussion.pagemanagement.data.PSInlineRenderLink}
 * ({@code @XmlRootElement(name = "InlineRenderLink")}).</p>
 *
 * <p>Wire shape (verified against the classic callers, which read
 * {@code retData.InlineRenderLink.sys_dependentid},
 * {@code .sys_dependentvariantid}, {@code .url}, {@code .title},
 * {@code .stateClass}, {@code .altText}):
 * {@code {"InlineRenderLink": {…}}}. {@link fetchInlineRenderLink} also
 * accepts a bare (already-unwrapped) body for tests.</p>
 */

import { get } from "../client";
import { PATHS } from "../paths";

/**
 * Wire form of {@code PSInlineRenderLink}. Key names mirror the classic
 * JSON contract (JAXB {@code @XmlElement} names), not the Java getter
 * names — see the classic {@code percadvlink} / {@code percadvimage}
 * plugins. All fields optional: the server omits what it cannot resolve.
 */
export interface PSInlineRenderLinkWire {
  sys_dependentid?: number | string | null;
  sys_dependentvariantid?: number | string | null;
  rxinlineslot?: string | null;
  inlinetype?: string | null;
  url?: string | null;
  title?: string | null;
  altText?: string | null;
  stateClass?: string | null;
  targetId?: string | null;
  thumbUrl?: string | null;
}

interface PSInlineRenderLinkResponse {
  InlineRenderLink?: PSInlineRenderLinkWire | null;
  inlineRenderLink?: PSInlineRenderLinkWire | null;
}

/**
 * Unwrap a render-link preview body to the {@code PSInlineRenderLink} wire
 * object. Accepts the server {@code {"InlineRenderLink": …}} envelope and a
 * bare wire object; returns {@code null} when neither is present.
 */
export function unwrapInlineRenderLink(
  body: unknown,
): PSInlineRenderLinkWire | null {
  if (body == null || typeof body !== "object" || Array.isArray(body)) {
    return null;
  }
  const rec = body as Record<string, unknown> & PSInlineRenderLinkResponse;
  const nested = rec.InlineRenderLink ?? rec.inlineRenderLink ?? null;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    return nested as PSInlineRenderLinkWire;
  }
  if (isInlineRenderLinkLike(rec)) {
    return rec as unknown as PSInlineRenderLinkWire;
  }
  return null;
}

function isInlineRenderLinkLike(
  rec: Record<string, unknown>,
): boolean {
  return (
    rec.url !== undefined ||
    rec.title !== undefined ||
    rec.sys_dependentid !== undefined ||
    rec.sys_dependentvariantid !== undefined ||
    rec.altText !== undefined ||
    rec.stateClass !== undefined
  );
}

/**
 * Fetch the inline render-link preview for a content id — the managed-link
 * reference the adv dialogs persist (URL, title, dependent ids).
 *
 * @param contentId CMS content id (numeric or composite wire id).
 * @param titleField optional control-configured title field name, passed as
 *   {@code ?titleField=} for the server resolve (peer of classic
 *   {@code getInlineLinkTitleField}, #2242).
 * @returns the unwrapped wire object, or {@code null} when the server
 *   returned no link (callers keep manual-URL entry).
 */
export async function fetchInlineRenderLink(
  contentId: string | number,
  titleField?: string,
): Promise<PSInlineRenderLinkWire | null> {
  const id = encodeURIComponent(String(contentId ?? "").trim());
  if (!id) {
    throw new Error("fetchInlineRenderLink requires contentId");
  }
  let url = `${PATHS.RENDER_LINK_PREVIEW}/${id}/default`;
  const field = String(titleField ?? "").trim();
  if (field) {
    url += `?titleField=${encodeURIComponent(field)}`;
  }
  const body = await get<unknown>(url);
  return unwrapInlineRenderLink(body);
}
