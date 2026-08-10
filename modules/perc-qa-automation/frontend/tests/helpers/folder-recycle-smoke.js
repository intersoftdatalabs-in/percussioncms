/**
 * Folder + recycle REST smoke helpers (#2464 / parent #2423 residual).
 *
 * <p>Exercises the pathmanagement + recycleService chain that failed Spring
 * context startup with the folderHelper circular dependency. Pure helpers are
 * unit-tested without a live CMS; live API helpers reuse empty-recycling
 * patterns (addNewFolder / deleteFolder / empty / list).</p>
 *
 * <p>No machine hard-coded install paths. Base URL and credentials come from
 * auth / resolve-cms-env ({@code TEST_CMS_URL} or {@code DEV_PERCUSSION_*}).</p>
 */

"use strict";

const {
  cmsUrl,
  uniqueSeedFolderName,
  listFolderChildren,
  recyclingHasName,
  emptyRecyclingViaApi,
  emptyApiFailureMessage,
  PATH_FOLDER,
  PATH_ADD_NEW_FOLDER,
  PATH_DELETE_FOLDER,
  RECYCLE_EMPTY_PATH,
} = require("./empty-recycling");

/** Pathmanagement restore endpoint (sitemanage PSPathService). */
const PATH_RESTORE_FOLDER =
  "/Rhythmyx/services/pathmanagement/path/restoreFolder";

/**
 * True when an HTTP status means Rhythmyx / pathmanagement is answering.
 * 401/403 still prove the webapp is up (auth required); 5xx / connection
 * failures indicate the pre-#2423 "connector up, context dead" class of bug.
 *
 * @param {number | null | undefined} status
 * @returns {boolean}
 */
function isContextHealthyStatus(status) {
  if (status == null || !Number.isFinite(Number(status))) {
    return false;
  }
  const s = Number(status);
  // 2xx success, 3xx redirect, 401/403 auth — all prove the webapp is live.
  if (s >= 200 && s < 400) {
    return true;
  }
  if (s === 401 || s === 403) {
    return true;
  }
  return false;
}

/**
 * Operator-facing message when Rhythmyx context is down (cycle / startup fail).
 *
 * @param {{ status?: number, url?: string, bodySnippet?: string, cause?: string }} [detail]
 * @returns {string}
 */
function contextDownFailureMessage(detail = {}) {
  const status = detail.status != null ? String(detail.status) : "n/a";
  const url = detail.url || "pathmanagement";
  const body = detail.bodySnippet
    ? String(detail.bodySnippet).replace(/\s+/g, " ").slice(0, 240)
    : "";
  const cause = detail.cause ? ` cause=${detail.cause}` : "";
  return (
    `Rhythmyx / pathmanagement context appears DOWN (HTTP ${status} for ${url}).` +
    ` This is a hard fail for folder+recycle smoke (#2464 / parent #2423):` +
    ` Jetty connector can bind while Spring fails (folderHelper→recycleService cycle).` +
    ` Check container logs for BeanCurrentlyInCreationException / folderHelper.` +
    (body ? ` body=${body}` : "") +
    cause
  );
}

/**
 * Extract a stable path item from common Jackson wrappers.
 * @param {unknown} body
 * @returns {Record<string, unknown>}
 */
function extractPathItem(body) {
  if (!body || typeof body !== "object") {
    return {};
  }
  const o = /** @type {Record<string, unknown>} */ (body);
  if (
    o.PathItem &&
    typeof o.PathItem === "object" &&
    !Array.isArray(o.PathItem)
  ) {
    return /** @type {Record<string, unknown>} */ (o.PathItem);
  }
  if (
    o.pathItem &&
    typeof o.pathItem === "object" &&
    !Array.isArray(o.pathItem)
  ) {
    return /** @type {Record<string, unknown>} */ (o.pathItem);
  }
  return o;
}

/**
 * Prefer id / guid string from a PathItem-like object.
 * @param {unknown} item
 * @returns {string}
 */
function extractPathItemGuid(item) {
  if (!item || typeof item !== "object") {
    return "";
  }
  const o = /** @type {Record<string, unknown>} */ (item);
  const raw = o.id ?? o.guid ?? o.Id ?? o.Guid ?? "";
  return raw != null ? String(raw).trim() : "";
}

/**
 * Find a child PathItem by exact name (or path basename).
 * @param {object[]} items
 * @param {string} name
 * @returns {object | null}
 */
function findNamedPathItem(items, name) {
  const target = String(name || "");
  if (!target) {
    return null;
  }
  for (const it of items || []) {
    if (!it || typeof it !== "object") {
      continue;
    }
    if (it.name != null && String(it.name) === target) {
      return it;
    }
    if (it.path != null) {
      const p = String(it.path);
      if (p === target) {
        return it;
      }
      const base = p.split("/").filter(Boolean).pop() || "";
      if (base === target) {
        return it;
      }
    }
  }
  return null;
}

/**
 * Build restoreFolder absolute URL for a guid.
 * @param {string} baseUrl
 * @param {string} guid
 * @returns {string}
 */
function restoreFolderUrl(baseUrl, guid) {
  const id = encodeURIComponent(String(guid || "").trim());
  return cmsUrl(baseUrl, `${PATH_RESTORE_FOLDER}/${id}`);
}

/**
 * Probe pathmanagement so a dead Rhythmyx context fails hard (not soft skip).
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers
 * @returns {Promise<{ ok: true, status: number } | { ok: false, status: number, message: string }>}
 */
async function probePathmanagementContext(request, baseUrl, headers) {
  const url = cmsUrl(baseUrl, `${PATH_FOLDER}/`);
  let status = 0;
  let bodySnippet = "";
  try {
    const res = await request.get(url, {
      headers: {
        ...headers,
        Accept: "application/json",
      },
      // Fail fast when Jetty is up but webapp is dead / hanging.
      timeout: 20_000,
    });
    status = res.status();
    bodySnippet = (await res.text().catch(() => "")).slice(0, 300);
  } catch (err) {
    const cause = err && err.message ? String(err.message) : String(err);
    return {
      ok: false,
      status: 0,
      message: contextDownFailureMessage({ status: 0, url, cause }),
    };
  }

  if (!isContextHealthyStatus(status)) {
    return {
      ok: false,
      status,
      message: contextDownFailureMessage({ status, url, bodySnippet }),
    };
  }
  // 5xx bodies that mention the cycle should still hard-fail even if we
  // somehow classified status wrong — belt-and-suspenders for log text.
  if (
    /BeanCurrentlyInCreationException|folderHelper|circular reference/i.test(
      bodySnippet,
    )
  ) {
    return {
      ok: false,
      status,
      message: contextDownFailureMessage({ status, url, bodySnippet }),
    };
  }
  return { ok: true, status };
}

/**
 * Create a uniquely named folder under a parent (default Assets).
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers
 * @param {{ parentPath?: string, name?: string }} [opts]
 * @returns {Promise<{ name: string, path: string, guid: string, raw: object }>}
 */

/**
 * True when an addNewFolder/pathmanagement failure body is a *product* path error
 * (context is live) rather than the pre-#2423 "context dead" class of bug.
 *
 * <p>Stable product markers: Jackson {@code "Errors"}, "Path not found", and Spring
 * {@code Transaction silently rolled back}. The #2488 Hibernate 6 regression is
 * matched only when {@code parentFolders} co-occurs with PersistentSet /
 * PropertyAccessException stack markers — those substrings alone can appear in
 * docs or other payloads, so they are never used as lone matchers.</p>
 *
 * @param {number} status HTTP status
 * @param {string | null | undefined} body response text
 * @returns {boolean}
 */
function isProductPathErrorBody(status, body) {
  if (status !== 404 && status !== 500) {
    return false;
  }
  const text = String(body || "");
  if (/"Errors"|Path not found|Transaction silently rolled back/i.test(text)) {
    return true;
  }
  // #2488: HashSet parentFolders + Hibernate PersistentSet injection failure.
  return (
    /parentFolders/i.test(text) &&
    /PropertyAccessException|PersistentSet|Could not set value of type/i.test(
      text,
    )
  );
}

async function createNamedFolder(request, baseUrl, headers, opts = {}) {
  const parent = String(opts.parentPath || "Assets").replace(/^\/+|\/+$/g, "");
  const name = opts.name || uniqueSeedFolderName("qa-folder-recycle");
  const addUrl = cmsUrl(
    baseUrl,
    `${PATH_ADD_NEW_FOLDER}/${parent}?name=${encodeURIComponent(name)}`,
  );
  const fallbackUrl = cmsUrl(baseUrl, `${PATH_ADD_NEW_FOLDER}/${parent}`);
  // Track the URL of the request that actually failed (named first, then fallback).
  let attemptedUrl = addUrl;
  let addRes = await request.get(addUrl, { headers });
  if (!addRes.ok()) {
    attemptedUrl = fallbackUrl;
    addRes = await request.get(fallbackUrl, { headers });
  }
  if (!addRes.ok()) {
    const text = await addRes.text().catch(() => "");
    // Product 404 JSON (Path not found / Errors) means pathmanagement is live —
    // do not mis-label as the pre-#2423 "context dead" class of bug (#2488).
    const productPathError = isProductPathErrorBody(addRes.status(), text);
    // Surface context-down class failures with the hard-fail message.
    if (!isContextHealthyStatus(addRes.status()) && !productPathError) {
      throw new Error(
        contextDownFailureMessage({
          status: addRes.status(),
          url: attemptedUrl,
          bodySnippet: text,
        }),
      );
    }
    throw new Error(
      `addNewFolder under ${parent} failed status=${addRes.status()} url=${attemptedUrl} body=${text.slice(0, 300)}`,
    );
  }
  const created = await addRes.json().catch(() => ({}));
  const createdItem = extractPathItem(created);
  let finalName = String(createdItem.name || "New Folder");
  let livePath = String(createdItem.path || `/${parent}/${finalName}`);
  let guid = extractPathItemGuid(createdItem);

  // Rename when server ignored ?name= and used default "New Folder".
  if (finalName !== name) {
    const renameBody = {
      path: livePath.endsWith("/") ? livePath : `${livePath}/`,
      name,
    };
    const renameRes = await request.post(
      cmsUrl(baseUrl, "/Rhythmyx/services/pathmanagement/path/renameFolder"),
      {
        headers: {
          ...headers,
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        data: renameBody,
      },
    );
    if (renameRes.ok()) {
      const renamed = await renameRes.json().catch(() => ({}));
      const item = extractPathItem(renamed);
      finalName = String(item.name || name);
      livePath = String(item.path || `/${parent}/${finalName}`);
      guid = extractPathItemGuid(item) || guid;
    }
  }

  return {
    name: finalName,
    path: livePath.startsWith("/") ? livePath : `/${livePath}`,
    guid,
    raw: createdItem,
  };
}

/**
 * Soft-delete (recycle) a folder via deleteFolder shouldPurge=false.
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers
 * @param {{ path: string, guid?: string }} folder
 * @returns {Promise<void>}
 */
async function recycleFolder(request, baseUrl, headers, folder) {
  const deletePath = String(folder.path || "").startsWith("/")
    ? String(folder.path)
    : `/${folder.path}`;
  const pathForDelete = deletePath.endsWith("/")
    ? deletePath
    : `${deletePath}/`;
  const delRes = await request.post(cmsUrl(baseUrl, PATH_DELETE_FOLDER), {
    headers: {
      ...headers,
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    data: {
      DeleteFolderCriteria: {
        path: pathForDelete,
        shouldPurge: false,
        skipItems: "YES",
        guid: folder.guid || "",
      },
    },
  });
  if (!delRes.ok()) {
    const text = await delRes.text().catch(() => "");
    if (!isContextHealthyStatus(delRes.status())) {
      throw new Error(
        contextDownFailureMessage({
          status: delRes.status(),
          url: PATH_DELETE_FOLDER,
          bodySnippet: text,
        }),
      );
    }
    throw new Error(
      `deleteFolder (recycle) ${pathForDelete} failed status=${delRes.status()} body=${text.slice(0, 400)}`,
    );
  }
}

/**
 * Restore a recycled folder by guid (PUT restoreFolder/{id}).
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers
 * @param {string} guid
 * @returns {Promise<{ status: number, body: unknown, text: string }>}
 */
async function restoreFolderByGuid(request, baseUrl, headers, guid) {
  if (!guid) {
    throw new Error("restoreFolderByGuid requires a non-empty guid");
  }
  const url = restoreFolderUrl(baseUrl, guid);
  const res = await request.fetch(url, {
    method: "PUT",
    headers: {
      ...headers,
      Accept: "application/json",
    },
  });
  const text = await res.text().catch(() => "");
  let body = null;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = text;
  }
  return { status: res.status(), body, text };
}

/**
 * Search Recycling (and structural Assets/Sites children) for a folder name.
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers
 * @param {string} name
 * @returns {Promise<{ found: boolean, item: object | null, location: string }>}
 */
async function findInRecycling(request, baseUrl, headers, name) {
  const roots = ["Recycling", "Recycling/Assets", "Recycling/Sites"];
  for (const root of roots) {
    let items;
    try {
      items = await listFolderChildren(request, baseUrl, headers, root);
    } catch (err) {
      const msg = err && err.message ? String(err.message) : String(err);
      // Optional structural children (e.g. Recycling/Sites) may 404 when empty
      // or absent. Network / 5xx / auth / context-down must not become
      // "not in Recycling" — rethrow so the smoke fails honestly.
      if (/\bfailed status=404\b/i.test(msg)) {
        continue;
      }
      throw err;
    }
    const hit = findNamedPathItem(items, name);
    if (hit) {
      return { found: true, item: hit, location: root };
    }
    // Also accept recyclingHasName for basename-only list shapes.
    if (recyclingHasName(items, name)) {
      return {
        found: true,
        item: findNamedPathItem(items, name),
        location: root,
      };
    }
  }
  return { found: false, item: null, location: "" };
}

module.exports = {
  PATH_RESTORE_FOLDER,
  PATH_FOLDER,
  PATH_ADD_NEW_FOLDER,
  PATH_DELETE_FOLDER,
  RECYCLE_EMPTY_PATH,
  isContextHealthyStatus,
  isProductPathErrorBody,
  contextDownFailureMessage,
  extractPathItem,
  extractPathItemGuid,
  findNamedPathItem,
  restoreFolderUrl,
  probePathmanagementContext,
  createNamedFolder,
  recycleFolder,
  restoreFolderByGuid,
  findInRecycling,
  listFolderChildren,
  emptyRecyclingViaApi,
  emptyApiFailureMessage,
  uniqueSeedFolderName,
  cmsUrl,
  recyclingHasName,
};
