/**
 * Pure helpers for custom inline-link title field residual (#2243 / parent #946).
 *
 * Peers of WebUI PercPathService.getInlineRenderLink titleField pass-through and
 * server PSInlineLinkTitleResolver fallback chain. Used by Playwright residual
 * specs and Node unit tests (no live CMS required for unit).
 *
 * @see tests/bugs/bug-2243-inline-link-title-field.spec.js
 * @see projects/sitemanage/.../PSInlineLinkTitleResolver.java
 */

"use strict";

const PARENT_ISSUE = 946;
const SLICE_ISSUE = 2243;
const REPO_ISSUES =
  "https://github.com/intersoftdatalabs-in/percussioncms/issues";

/** Default page title field when control setting is empty (BC). */
const PAGE_DEFAULT_TITLE_FIELD = "resource_link_title";

/** Shared/system fallback after missing custom field. */
const DISPLAYTITLE_FIELD = "displaytitle";

/**
 * Skip reason when stock H2 / QA image has no page/asset fixture to exercise
 * renderlink preview with titleField. Not a silent flake — durable BUG URL.
 *
 * @returns {string}
 */
function inlineLinkTitleFixturesSkipReason() {
  return (
    `BUG: stock H2/qa image has no page/asset fixture for inline link title ` +
    `preview (#${SLICE_ISSUE} residual of #${PARENT_ISSUE}). ` +
    `Need at least one Sites page (or asset) to call renderlink/preview with ` +
    `titleField custom + displaytitle/resource_link_title fallback. ` +
    `See ${REPO_ISSUES}/${SLICE_ISSUE}`
  );
}

/**
 * Normalize TinyMCE / control option value (peer of getInlineLinkTitleField).
 *
 * @param {unknown} raw
 * @returns {string}
 */
function normalizeInlineLinkTitleField(raw) {
  if (raw == null) {
    return "";
  }
  return String(raw).trim();
}

/**
 * Build renderlink preview URL with optional titleField query.
 *
 * @param {string} baseUrl CMS base (no trailing slash)
 * @param {string} itemId content id
 * @param {unknown} [titleField]
 * @returns {string}
 */
function buildRenderLinkPreviewUrl(baseUrl, itemId, titleField) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  let url = `${root}/Rhythmyx/services/pagemanagement/renderlink/preview/${itemId}/default`;
  const field = normalizeInlineLinkTitleField(titleField);
  if (field !== "") {
    url += `?titleField=${encodeURIComponent(field)}`;
  }
  return url;
}

/**
 * Client peer of PSInlineLinkTitleResolver.resolve.
 *
 * @param {string|null|undefined} configuredFieldName
 * @param {Record<string, unknown>|null|undefined} fields
 * @param {string|null|undefined} typeDefault
 * @returns {string}
 */
function resolveInlineLinkTitle(configuredFieldName, fields, typeDefault) {
  const map = fields && typeof fields === "object" ? fields : {};

  /**
   * @param {string|null|undefined} name
   * @returns {string|null}
   */
  function fieldAsString(name) {
    if (name == null || String(name).trim() === "") {
      return null;
    }
    const key = String(name).trim();
    let value = map[key];
    if (value == null) {
      const lower = key.toLowerCase();
      const hit = Object.keys(map).find((k) => k.toLowerCase() === lower);
      value = hit != null ? map[hit] : null;
    }
    if (value == null) {
      return null;
    }
    const s = String(value).trim();
    return s === "" ? null : s;
  }

  if (
    configuredFieldName != null &&
    String(configuredFieldName).trim() !== ""
  ) {
    const configured = fieldAsString(configuredFieldName);
    if (configured != null) {
      return configured;
    }
    const cfg = String(configuredFieldName).trim();
    if (cfg.toLowerCase() !== DISPLAYTITLE_FIELD) {
      const displayTitle = fieldAsString(DISPLAYTITLE_FIELD);
      if (displayTitle != null) {
        return displayTitle;
      }
    }
  }
  return typeDefault == null ? "" : String(typeDefault);
}

/**
 * Extract a usable content id from path folder JSON (PathItem wrappers).
 *
 * @param {unknown} body
 * @returns {{ id: string, name: string, type?: string }[]}
 */
function pathItemsWithIds(body) {
  if (body == null) {
    return [];
  }
  const items = Array.isArray(body.PathItem)
    ? body.PathItem
    : Array.isArray(body)
      ? body
      : [];
  return items
    .map((it) => {
      if (!it || typeof it !== "object") {
        return null;
      }
      const id =
        it.id != null
          ? String(it.id)
          : it.guid != null
            ? String(it.guid)
            : null;
      const name = it.name != null ? String(it.name) : "";
      if (!id || !String(id).trim()) {
        return null;
      }
      return {
        id: String(id).trim(),
        name,
        type: it.type != null ? String(it.type) : undefined,
      };
    })
    .filter(Boolean);
}

/**
 * Title from InlineRenderLink REST payload (Jackson / client shapes).
 *
 * @param {unknown} body
 * @returns {string|null}
 */
function extractTitleFromPreviewBody(body) {
  if (body == null || typeof body !== "object") {
    return null;
  }
  const anyBody = /** @type {Record<string, any>} */ (body);
  const nested =
    anyBody.InlineRenderLink ||
    anyBody.inlineRenderLink ||
    anyBody.PSInlineRenderLink ||
    null;
  if (nested && typeof nested === "object" && nested.title != null) {
    const t = String(nested.title).trim();
    return t === "" ? null : t;
  }
  if (anyBody.title != null) {
    const t = String(anyBody.title).trim();
    return t === "" ? null : t;
  }
  return null;
}

module.exports = {
  PARENT_ISSUE,
  SLICE_ISSUE,
  REPO_ISSUES,
  PAGE_DEFAULT_TITLE_FIELD,
  DISPLAYTITLE_FIELD,
  inlineLinkTitleFixturesSkipReason,
  normalizeInlineLinkTitleField,
  buildRenderLinkPreviewUrl,
  resolveInlineLinkTitle,
  pathItemsWithIds,
  extractTitleFromPreviewBody,
  extractInlineRenderLinkTitle: extractTitleFromPreviewBody,
};
