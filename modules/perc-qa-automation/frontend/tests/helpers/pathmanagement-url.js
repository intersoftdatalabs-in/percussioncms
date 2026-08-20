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

/**
 * Encode each {@code /}-separated CMS path segment (peer of WebUI
 * {@code pathApi.encodePath}). Empty segments are dropped so {@code /Sites/}
 * becomes {@code Sites}, never {@code //Sites}.
 *
 * @param {string | null | undefined} path
 * @returns {string}
 */
function encodePathmanagementPath(path) {
  return String(path || "")
    .split("/")
    .filter((seg) => seg.length > 0)
    .map((seg) => encodeURIComponent(seg))
    .join("/");
}

/**
 * pathmanagement paginatedFolder GET used by Explorer dependency specs.
 *
 * @param {string} baseUrl
 * @param {string | null | undefined} folderPath
 * @returns {string}
 */
function paginatedFolderUrl(baseUrl, folderPath) {
  const suffix = encodePathmanagementPath(folderPath);
  const base = String(baseUrl || "").replace(/\/+$/, "");
  const slashSuffix = suffix ? `/${suffix}` : "/";
  return `${base}/Rhythmyx/services/pathmanagement/path/paginatedFolder${slashSuffix}?startIndex=0&maxResults=50`;
}

module.exports = {
  isDoubleSlashPathmanagementUrl,
  isHumanReadableErrorText,
  EXPECTED_ROOT_FOLDER_NAMES,
  encodePathmanagementPath,
  paginatedFolderUrl,
};
