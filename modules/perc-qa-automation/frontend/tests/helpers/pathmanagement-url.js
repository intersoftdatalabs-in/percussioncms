/**
 * Pure helpers for pathmanagement URL / explorer error assertions.
 *
 * Used by Playwright bug-1622 (encodePath / folder// regression) and covered
 * by Node unit tests under tests/unit/ (no live CMS).
 *
 * @see tests/bugs/bug-1622-explorer-root-folders.spec.js
 * @see WebUI pathApi encodePath / joinPathUrl (#1680)
 */

"use strict";

/**
 * True when a pathmanagement URL uses the double-slash form that causes HTTP
 * 400 (encodePath regression): folder//, paginatedFolder//, item//, etc.
 *
 * @param {string | null | undefined} url
 * @returns {boolean}
 */
function isDoubleSlashPathmanagementUrl(url) {
  if (!url || typeof url !== "string") {
    return false;
  }
  // Match …/pathmanagement/path/<resource>//… (resource may have query after).
  return /\/pathmanagement\/path\/[^/?#]+\/\//.test(url);
}

/**
 * True when explorer tree error chrome text is human-readable (not empty and
 * not bare object coercion from formatApiError regression).
 *
 * @param {string | null | undefined} text
 * @returns {boolean}
 */
function isHumanReadableErrorText(text) {
  const t = String(text || "").trim();
  if (!t) {
    return false;
  }
  // formatApiError (#1691) must never leave bare object coercion.
  if (/\[object\s+Object\]/i.test(t)) {
    return false;
  }
  return t.length >= 3;
}

/**
 * Well-known CMS root folders returned by path/folder/ on a stock install.
 * Includes classic Rhythmyx Folders (//Folders) next to Sites/Assets/Design (#3044).
 * Recycling / Search may also appear depending on roles; not required here.
 */
const EXPECTED_ROOT_FOLDER_NAMES = Object.freeze([
  "Sites",
  "Folders",
  "Assets",
  "Design",
]);

module.exports = {
  isDoubleSlashPathmanagementUrl,
  isHumanReadableErrorText,
  EXPECTED_ROOT_FOLDER_NAMES,
};
