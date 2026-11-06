/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Stored-markup helpers for the Percussion plugin rewrite slice (#5622/#5652).
 *
 * <p>The TinyMCE plugins ({@code rxinline}, {@code percadvimage},
 * {@code percadvlink}, {@code percglobalvariables}, {@code percmorelink},
 * {@code rxinserthtml}, CodeMirror source view) persist CMS-specific markup
 * the server assembly side consumes ({@code PSInlineLinkField} inline-slot
 * attributes, the global-variable marker span, the blog more-link span).
 * Their classic dialogs depend on the ContentEditor frame services
 * ({@code topFrameJQuery}, {@code PercPathService}, {@code browser.js},
 * {@code ContentBrowserDialog.jsp}) and cannot run in the React editor host,
 * so the TipTap port uses self-contained dialogs that emit the <em>same
 * stored attribute shape</em>. Content stays interchangeable between engines
 * and already-stored HTML keeps working without a migration.</p>
 *
 * <p>All builders return strings with attribute values HTML-escaped; no DOM
 * is required. Element readers take a DOM {@code Element} so both the
 * extensions and the tests can share them.</p>
 */

/** Edit- and stored-time marker class for global variables. */
export const PERC_GLOBAL_VARIABLE_CLASS = "perc-global-variable-marker";

/** Stored-time marker class for the blog "more" link. */
export const PERC_MORE_LINK_CLASS = "perc-blog-more-link";

/** Inline-slot attribute the server assembly side keys on. */
export const PERC_INLINE_SLOT_ATTR = "rxinlineslot";

/** Inline-type attribute ({@code rxhyperlink}, {@code rximage}, {@code rxvariant}). */
export const PERC_INLINE_TYPE_ATTR = "inlinetype";

/** Original selected text kept for broken-link cleanup. */
export const PERC_SELECTED_TEXT_ATTR = "rxselectedtext";

/** CMS dependent-item id for an inline link or image. */
export const PERC_DEPENDENT_ID_ATTR = "sys_dependentid";

/** CMS dependent-variant id for an inline link or image. */
export const PERC_DEPENDENT_VARIANT_ATTR = "sys_dependentvariantid";

/** Inline relationship id, empty until the item is saved. */
export const PERC_RELATIONSHIP_ATTR = "sys_relationshipid";

/** Repository path of the link/image target. */
export const PERC_JCR_PATH_ATTR = "data-jcrpath";

/** Inline-type value for inline links (matches {@code PSInlineLinkField}). */
export const PERC_INLINE_TYPE_LINK = "rxhyperlink";

/** Inline-type value for inline images. */
export const PERC_INLINE_TYPE_IMAGE = "rximage";

/** Inline-type value for inline templates (matches {@code PSSingleValueBuilder}). */
export const PERC_INLINE_TYPE_TEMPLATE = "rxvariant";

/** Slot ids the TinyMCE plugins default to when the control sets none. */
export const PERC_DEFAULT_LINK_SLOT = "103";
export const PERC_DEFAULT_IMAGE_SLOT = "104";
export const PERC_DEFAULT_VARIANT_SLOT = "105";

/**
 * Escape a value for use inside a double-quoted HTML attribute.
 *
 * @param value the raw attribute value (null/undefined become "").
 * @returns the escaped value.
 */
export function escapePercAttr(value: string | null | undefined): string {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

/**
 * Render an attribute only when its value is non-empty, so ordinary links
 * and images serialize exactly as the parity slice does (no empty CMS
 * attributes on non-inline content).
 */
function percAttr(name: string, value: string | null | undefined): string {
  const trimmed = (value ?? "").trim();
  return trimmed ? ` ${name}="${escapePercAttr(trimmed)}"` : "";
}

export interface PercInlineLink {
  href: string;
  /** Visible link text. */
  text: string;
  title?: string;
  target?: string;
  slot?: string;
  inlineType?: string;
  dependentId?: string;
  dependentVariantId?: string;
  selectedText?: string;
  jcrPath?: string;
}

/**
 * Build the stored {@code <a>} markup for an inline link or template —
 * the same attribute shape {@code percadvlink}/{@code rxinline} persist.
 */
export function buildPercInlineLinkHtml(link: PercInlineLink): string {
  const text = link.text || link.href;
  return (
    `<a href="${escapePercAttr(link.href)}"` +
    percAttr("title", link.title) +
    percAttr("target", link.target) +
    percAttr(PERC_INLINE_SLOT_ATTR, link.slot) +
    percAttr(
      PERC_INLINE_TYPE_ATTR,
      link.inlineType ?? PERC_INLINE_TYPE_LINK,
    ) +
    percAttr(PERC_DEPENDENT_ID_ATTR, link.dependentId) +
    percAttr(PERC_DEPENDENT_VARIANT_ATTR, link.dependentVariantId) +
    percAttr(PERC_RELATIONSHIP_ATTR, "") +
    percAttr(PERC_SELECTED_TEXT_ATTR, link.selectedText ?? text) +
    percAttr(PERC_JCR_PATH_ATTR, link.jcrPath) +
    `>${escapePercAttr(text)}</a>`
  );
}

export interface PercInlineImage {
  src: string;
  alt?: string;
  title?: string;
  width?: string;
  height?: string;
  align?: string;
  /** External decorative flag: forces an empty alt like the adv image dialog. */
  decorative?: boolean;
  slot?: string;
  dependentId?: string;
  dependentVariantId?: string;
  jcrPath?: string;
}

/**
 * Build the stored {@code <img>} markup for an inline or advanced image —
 * the same attribute shape {@code percadvimage}/{@code rxinline} persist.
 */
export function buildPercInlineImageHtml(image: PercInlineImage): string {
  const alt = image.decorative ? "" : (image.alt ?? "");
  const style =
    image.align === "left" || image.align === "right"
      ? `float:${image.align};`
      : image.align
        ? `vertical-align:${image.align};`
        : "";
  return (
    `<img src="${escapePercAttr(image.src)}"` +
    ` alt="${escapePercAttr(alt)}"` +
    percAttr("title", image.decorative ? "" : image.title) +
    percAttr("width", image.width) +
    percAttr("height", image.height) +
    (style ? ` style="${style}"` : "") +
    percAttr(PERC_INLINE_SLOT_ATTR, image.slot) +
    percAttr(PERC_INLINE_TYPE_ATTR, PERC_INLINE_TYPE_IMAGE) +
    percAttr(PERC_DEPENDENT_ID_ATTR, image.dependentId) +
    percAttr(PERC_DEPENDENT_VARIANT_ATTR, image.dependentVariantId) +
    percAttr(PERC_RELATIONSHIP_ATTR, "") +
    percAttr(PERC_JCR_PATH_ATTR, image.jcrPath) +
    ">"
  );
}

/**
 * Build the stored global-variable marker — the {@code <span>} form
 * {@code percglobalvariables} writes on save (edit time shows the marker;
 * never the raw name).
 */
export function buildGlobalVariableHtml(name: string, value: string): string {
  return (
    `<span class="${PERC_GLOBAL_VARIABLE_CLASS}"` +
    ` title="${escapePercAttr(name)}">` +
    `${escapePercAttr(value)}</span>`
  );
}

export interface PercGlobalVariable {
  name: string;
  value: string;
}

/**
 * Read a global-variable marker element in either form: the stored
 * {@code <span>} or the legacy TinyMCE edit-time {@code <img>}.
 *
 * @param el the element to inspect.
 * @returns the name/value pair, or null when it is not a marker.
 */
export function parseGlobalVariableMarker(
  el: Element | null | undefined,
): PercGlobalVariable | null {
  if (!el || !el.classList.contains(PERC_GLOBAL_VARIABLE_CLASS)) {
    return null;
  }
  const name = el.getAttribute("title") ?? "";
  if (!name) {
    return null;
  }
  const tag = el.tagName.toLowerCase();
  const globalData = readGlobalVariableData();
  if (tag === "img") {
    // Legacy TinyMCE edit form carries no value; resolve it like the
    // plugin's BeforeSetContent handler does.
    return { name, value: globalData?.[name] ?? "" };
  }
  return { name, value: el.textContent ?? "" };
}

/**
 * Whether an element is a blog more-link marker in either form: the stored
 * {@code <span>} or the legacy TinyMCE edit-time {@code <img>}.
 */
export function isMoreLinkElement(el: Element | null | undefined): boolean {
  return !!el && el.classList.contains(PERC_MORE_LINK_CLASS);
}

/**
 * Read the CMS global-variable table when the hosting frame provides it
 * (the classic shell injects {@code PercGlobalVariablesData}; the standalone
 * React host has none and callers fall back to manual name entry).
 */
export function readGlobalVariableData(): Record<string, string> | null {
  if (typeof window === "undefined") {
    return null;
  }
  const data = (window as unknown as Record<string, unknown>)[
    "PercGlobalVariablesData"
  ];
  if (!data || typeof data !== "object" || Array.isArray(data)) {
    return null;
  }
  const table: Record<string, string> = {};
  for (const [key, value] of Object.entries(
    data as Record<string, unknown>,
  )) {
    if (typeof value === "string") {
      table[key] = value;
    }
  }
  return table;
}

/**
 * Rewrite stored Percussion marker shorthands into explicitly-closed markup
 * before parsing — the same job TinyMCE's {@code BeforeSetContent} handlers
 * do on load. Stored more-link spans are self-closed
 * ({@code <span class="perc-blog-more-link"/>}), which HTML parsing would
 * otherwise treat as an open span swallowing the markup that follows it.
 *
 * @param html stored HTML headed for the TipTap document.
 * @returns parse-safe HTML with identical content.
 */
export function preparePercussionHtml(html: string): string {
  if (!html) {
    return html;
  }
  return html.replace(
    /<span([^>]*?)class=(["'])perc-blog-more-link\2([^>]*?)\/>/gi,
    (_match, before: string, quote: string, after: string) =>
      `<span${before}class=${quote}perc-blog-more-link${quote}${after}></span>`,
  );
}
