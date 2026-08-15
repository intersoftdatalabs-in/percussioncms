/**
 * Pure helpers for Explorer Copy Folder wire envelope (#3362).
 *
 * CXF JAXB rejects a bare {@code sourcePath} root on moveItem. Copy must
 * use {@code CopyFolderItemRequest} ({@code itemPath} + {@code targetFolderPath}).
 *
 * @see tests/bugs/bug-3362-copy-folder-envelope.spec.js
 */

"use strict";

/**
 * True when a JSON body is a bare sourcePath object (the HTTP 400 shape).
 *
 * @param {unknown} body
 * @returns {boolean}
 */
function isBareSourcePathRoot(body) {
  if (body == null || typeof body !== "object" || Array.isArray(body)) {
    return false;
  }
  const rec = /** @type {Record<string, unknown>} */ (body);
  if (rec.MoveFolderItem != null || rec.CopyFolderItemRequest != null) {
    return false;
  }
  return typeof rec.sourcePath === "string";
}

/**
 * True when the body is a wrapped MoveFolderItem with server field names.
 *
 * @param {unknown} body
 * @returns {boolean}
 */
function isMoveFolderItemEnvelope(body) {
  if (body == null || typeof body !== "object" || Array.isArray(body)) {
    return false;
  }
  const inner = /** @type {Record<string, unknown>} */ (body).MoveFolderItem;
  if (inner == null || typeof inner !== "object" || Array.isArray(inner)) {
    return false;
  }
  const rec = /** @type {Record<string, unknown>} */ (inner);
  return (
    typeof rec.itemPath === "string" &&
    typeof rec.targetFolderPath === "string" &&
    rec.sourcePath === undefined &&
    rec.copy === undefined
  );
}

/**
 * True when the body is a wrapped CopyFolderItemRequest.
 *
 * @param {unknown} body
 * @returns {boolean}
 */
function isCopyFolderItemRequestEnvelope(body) {
  if (body == null || typeof body !== "object" || Array.isArray(body)) {
    return false;
  }
  const inner = /** @type {Record<string, unknown>} */ (body)
    .CopyFolderItemRequest;
  if (inner == null || typeof inner !== "object" || Array.isArray(inner)) {
    return false;
  }
  const rec = /** @type {Record<string, unknown>} */ (inner);
  return (
    typeof rec.itemPath === "string" &&
    typeof rec.targetFolderPath === "string" &&
    rec.sourcePath === undefined
  );
}

module.exports = {
  isBareSourcePathRoot,
  isMoveFolderItemEnvelope,
  isCopyFolderItemRequestEnvelope,
};
