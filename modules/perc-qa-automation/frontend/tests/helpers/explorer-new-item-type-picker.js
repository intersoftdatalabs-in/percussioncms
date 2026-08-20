/**
 * Pure helpers for Explorer New-item type picker live Playwright
 * (#3628 / parent #3102). No live CMS. Do not stub GET /actions/find
 * or POST item/create on the product route.
 */

"use strict";

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  tree: "explorer-tree",
  actionToolbar: "action-toolbar",
  typePicker: "explorer-type-picker",
  typePickerSelect: "explorer-type-picker-select",
  typePickerOk: "explorer-type-picker-ok",
  typePickerCancel: "explorer-type-picker-cancel",
  templatePicker: "explorer-template-picker",
  templatePickerSelect: "explorer-template-picker-select",
  templatePickerOk: "explorer-template-picker-ok",
});

/** Toolbar test ids for the New Item host (not nested type children). */
const NEW_ITEM_HOST_TEST_IDS = Object.freeze([
  "action-toolbar-item-New",
  "action-toolbar-item-Create_New_Item",
  "action-toolbar-item-New_Item",
  "action-toolbar-item-New Item",
]);

/**
 * Types that Home/Explorer create with sys_title only (no extra required
 * fields). Prefer these so live POST create is not a 500 validation page.
 */
const PREFERRED_CREATE_TYPE_NAMES = Object.freeze([
  "percSimpleTextAsset",
  "percRawHtmlAsset",
  "percRichTextAsset",
  "percFileAsset",
  "percFile",
  "rffFile",
  "rffEvent",
  "percImageAsset",
]);

const CREATE_MENU_TEST_ID = "action-toolbar-item-Create";

const NEW_ITEM_HOST_KEYS = Object.freeze([
  "new",
  "newitem",
  "contenttypes",
  "content",
  "create",
  "create_new_item",
  "createnewitem",
]);

/**
 * @param {string|null|undefined} name
 * @returns {string}
 */
function normalizeActionKey(name) {
  return String(name || "")
    .replace(/[\s_-]/g, "")
    .toLowerCase();
}

/**
 * @param {string|null|undefined} name
 * @returns {boolean}
 */
function isNewItemHostName(name) {
  return NEW_ITEM_HOST_KEYS.includes(normalizeActionKey(name));
}

/**
 * @param {string|null|undefined} name
 * @returns {boolean}
 */
function isExplorerPageType(name) {
  const n = normalizeActionKey(name);
  return n === "percpage" || n === "page";
}

/**
 * Prefer Home-create-safe asset types, then any non-page, then first name.
 * Empty catalog → "".
 *
 * @param {readonly string[]|null|undefined} names
 * @returns {string}
 */
function preferredContentTypeName(names) {
  const list = (names || [])
    .map((n) => String(n || "").trim())
    .filter((n) => n.length > 0);
  const byKey = new Map(list.map((n) => [normalizeActionKey(n), n]));
  for (const preferred of PREFERRED_CREATE_TYPE_NAMES) {
    const hit = byKey.get(normalizeActionKey(preferred));
    if (hit) {
      return hit;
    }
  }
  const nonPage = list.find((n) => !isExplorerPageType(n));
  return nonPage || list[0] || "";
}

/**
 * Live New / create / content-types URLs used for HTTP 5xx gating.
 *
 * @param {string|null|undefined} url
 * @returns {boolean}
 */
function isFeatureUrl(url) {
  const u = String(url || "");
  return (
    /\/actions\/find/i.test(u) ||
    /\/itemmanagement\/item\/create/i.test(u) ||
    /\/services\/contenttypes/i.test(u)
  );
}

/**
 * Leftover Data Flow Content Editor HTML (must not be requested).
 *
 * @param {string|null|undefined} url
 * @returns {boolean}
 */
function isDataFlowCeHtmlUrl(url) {
  const u = String(url || "");
  return (
    /rx_ce/i.test(u) ||
    /contenteditorurls\.html/i.test(u) ||
    /checkoutedit\.xml/i.test(u)
  );
}

/**
 * Create POST success: JSON 200, or documented empty-success 201/204.
 *
 * @param {number} status
 * @returns {boolean}
 */
function isCreateSuccessStatus(status) {
  return status === 200 || status === 201 || status === 204;
}

/**
 * @param {string|null|undefined} url
 * @returns {boolean}
 */
function isItemCreateUrl(url) {
  return /\/itemmanagement\/item\/create/i.test(String(url || ""));
}

/**
 * @param {string|object|null|undefined} body
 * @returns {string}
 */
function parseContentTypeFromCreateBody(body) {
  if (body == null || body === "") {
    return "";
  }
  if (typeof body === "object") {
    const inner =
      body.ItemCreateRequest || body.itemCreateRequest || body;
    return String(inner.contentType || inner.ContentType || "").trim();
  }
  const raw = String(body);
  try {
    const rec = JSON.parse(raw);
    const inner = rec.ItemCreateRequest || rec.itemCreateRequest || rec;
    return String(inner.contentType || inner.ContentType || "").trim();
  } catch {
    const m = raw.match(/"contentType"\s*:\s*"([^"]+)"/i);
    return m ? m[1] : "";
  }
}

/**
 * Names from GET /contenttypes or POST /actions/find/types payloads.
 *
 * @param {unknown} payload
 * @returns {string[]}
 */
function unwrapContentTypeNames(payload) {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload
      .map((row) => {
        if (row == null) return "";
        if (typeof row === "string") return row.trim();
        return String(row.name || row.Name || "").trim();
      })
      .filter((n) => n.length > 0 && !isNewItemHostName(n));
  }
  if (typeof payload !== "object") {
    return [];
  }
  const rec = payload;
  const raw =
    rec.ActionMenuList ??
    rec.ActionMenu ??
    rec.actionMenuList ??
    rec.ContentType ??
    rec.contentType ??
    rec.ContentTypes ??
    rec.items;
  if (raw == null) {
    const name = String(rec.name || rec.Name || "").trim();
    return name && !isNewItemHostName(name) ? [name] : [];
  }
  return unwrapContentTypeNames(raw);
}

function newItemMissingFailMessage() {
  return (
    "New-item host missing on spa.jsp?entry=explorer after selecting a " +
    "Sites/Assets folder (#3628). Do not skip."
  );
}

function pickerEmptyFailMessage() {
  return (
    "New-item type picker opened with no live content types (#3628). Do not skip."
  );
}

module.exports = {
  TEST_IDS,
  NEW_ITEM_HOST_TEST_IDS,
  CREATE_MENU_TEST_ID,
  NEW_ITEM_HOST_KEYS,
  PREFERRED_CREATE_TYPE_NAMES,
  normalizeActionKey,
  isNewItemHostName,
  isExplorerPageType,
  preferredContentTypeName,
  isFeatureUrl,
  isDataFlowCeHtmlUrl,
  isCreateSuccessStatus,
  isItemCreateUrl,
  parseContentTypeFromCreateBody,
  unwrapContentTypeNames,
  newItemMissingFailMessage,
  pickerEmptyFailMessage,
};
