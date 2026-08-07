/**
 * Pure helpers + REST seed/assert utilities for Empty Recycling coverage
 * (issue #2207 / parent #944 slice 3).
 *
 * <p>No machine hard-coded install paths. Base URL and credentials come from
 * auth / resolve-cms-env (TEST_CMS_URL or DEV_PERCUSSION_*).</p>
 *
 * <p>Client Vitest for PercRecycleService.emptyRecycling lives with product
 * slice #2206 ({@code WebUI/src/test/js/percEmptyRecycling.test.js}).</p>
 */

"use strict";

const RECYCLE_EMPTY_PATH =
  "/Rhythmyx/services/pathmanagement/recycle/empty";
const PATH_FOLDER = "/Rhythmyx/services/pathmanagement/path/folder";
const PATH_ADD_NEW_FOLDER =
  "/Rhythmyx/services/pathmanagement/path/addNewFolder";
const PATH_RENAME_FOLDER =
  "/Rhythmyx/services/pathmanagement/path/renameFolder";
const PATH_DELETE_FOLDER =
  "/Rhythmyx/services/pathmanagement/path/deleteFolder";

/** Stable selectors from #2206 Empty Recycling Actions menu entry. */
const SELECTORS = {
  emptyAction: '[data-testid="perc-finder-empty-recycling"]',
  emptyActionId: "#perc-finder-empty-recycling",
  actionsButton: "#perc-finder-actions-button",
  actionsMenu: "#perc-finder-actions",
  confirmDialog: "#perc-finder-empty-recycling-confirm",
  confirmOk: "#perc-confirm-generic-ok",
  confirmCancel: "#perc-confirm-generic-cancel",
  confirmWarn: "#perc-empty-recycling-warn-msg",
  finderOuter: ".perc-finder-outer",
  pathSummary: "#mcol-path-summary",
  pathGo: "#perc-finder-go-action",
  finderExpander: "#perc-finder-expander",
};

/**
 * Normalize JAX-RS / Jackson list or wrapper bodies into a PathItem array.
 * @param {unknown} body
 * @returns {object[]}
 */
function normalizePathItems(body) {
  if (body == null) {
    return [];
  }
  if (Array.isArray(body)) {
    return body.filter((x) => x && typeof x === "object");
  }
  if (typeof body !== "object") {
    return [];
  }
  const o = /** @type {Record<string, unknown>} */ (body);
  if (Array.isArray(o.PathItem)) {
    return o.PathItem.filter((x) => x && typeof x === "object");
  }
  if (Array.isArray(o.pathItem)) {
    return o.pathItem.filter((x) => x && typeof x === "object");
  }
  // Single item wrappers
  if (o.PathItem && typeof o.PathItem === "object" && !Array.isArray(o.PathItem)) {
    return [o.PathItem];
  }
  return [];
}

/**
 * True when EmptyRecycleResult (or JSON wrapper) indicates an already-empty bin.
 * @param {unknown} body
 * @returns {boolean}
 */
function isAlreadyEmptyResult(body) {
  if (!body || typeof body !== "object") {
    return false;
  }
  const o = /** @type {Record<string, unknown>} */ (body);
  const nested =
    o.EmptyRecycleResult && typeof o.EmptyRecycleResult === "object"
      ? /** @type {Record<string, unknown>} */ (o.EmptyRecycleResult)
      : o;
  return nested.alreadyEmpty === true || nested.alreadyEmpty === "true";
}

/**
 * Sum purged folder + item counts from EmptyRecycleResult.
 * @param {unknown} body
 * @returns {number}
 */
function purgedTotal(body) {
  if (!body || typeof body !== "object") {
    return 0;
  }
  const o = /** @type {Record<string, unknown>} */ (body);
  const nested =
    o.EmptyRecycleResult && typeof o.EmptyRecycleResult === "object"
      ? /** @type {Record<string, unknown>} */ (o.EmptyRecycleResult)
      : o;
  const folders = Number(nested.purgedFolderCount || 0);
  const items = Number(nested.purgedItemCount || 0);
  return (Number.isFinite(folders) ? folders : 0) + (Number.isFinite(items) ? items : 0);
}

/**
 * Unique folder name for seed recycle fixtures (ASCII-safe).
 * @param {string} [prefix]
 * @returns {string}
 */
function uniqueSeedFolderName(prefix) {
  const p = String(prefix || "qa-empty-recycl").replace(/[^a-zA-Z0-9_-]/g, "");
  return `${p}-${Date.now().toString(36)}-${Math.floor(Math.random() * 1e4)}`;
}

/**
 * Build Absolute URLs under a CMS base (no trailing slash required).
 * @param {string} baseUrl
 * @param {string} path starts with /
 * @returns {string}
 */
function cmsUrl(baseUrl, path) {
  const base = String(baseUrl || "").replace(/\/+$/, "");
  const p = path.startsWith("/") ? path : `/${path}`;
  return `${base}${p}`;
}

/**
 * List children under a finder path (e.g. Recycling or Assets).
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers admin basic-auth headers
 * @param {string} finderPath e.g. "Recycling" or "Recycling/" (no leading slash on wire)
 * @returns {Promise<object[]>}
 */
async function listFolderChildren(request, baseUrl, headers, finderPath) {
  const wire = String(finderPath || "")
    .replace(/^\/+/, "")
    .replace(/\/+$/, "");
  const url = cmsUrl(baseUrl, `${PATH_FOLDER}/${wire}`);
  const res = await request.get(url, { headers });
  if (!res.ok()) {
    const text = await res.text().catch(() => "");
    throw new Error(
      `GET ${url} failed status=${res.status()} body=${text.slice(0, 300)}`,
    );
  }
  return normalizePathItems(await res.json());
}

/**
 * Create a uniquely named folder under Assets and recycle it (shouldPurge=false).
 * Leaves at least one child under /Recycling/ for empty-bin tests.
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers
 * @param {{ parentPath?: string, name?: string }} [opts]
 * @returns {Promise<{ name: string, livePath: string, recycledPath: string }>}
 */
async function seedRecycledFolder(request, baseUrl, headers, opts = {}) {
  const parent = String(opts.parentPath || "Assets").replace(/^\/+|\/+$/g, "");
  const name = opts.name || uniqueSeedFolderName();
  const addUrl = cmsUrl(
    baseUrl,
    `${PATH_ADD_NEW_FOLDER}/${parent}?name=${encodeURIComponent(name)}`,
  );
  // addNewFolder may ignore query and return "New Folder"; rename afterward.
  let addRes = await request.get(addUrl, { headers });
  if (!addRes.ok()) {
    // Fallback without query (server may use default "New Folder")
    addRes = await request.get(
      cmsUrl(baseUrl, `${PATH_ADD_NEW_FOLDER}/${parent}`),
      { headers },
    );
  }
  if (!addRes.ok()) {
    const text = await addRes.text().catch(() => "");
    throw new Error(
      `addNewFolder under ${parent} failed status=${addRes.status()} body=${text.slice(0, 300)}`,
    );
  }
  const created = await addRes.json().catch(() => ({}));
  const createdItem =
    (created && created.PathItem) ||
    (created && created.pathItem) ||
    created ||
    {};
  const createdName = createdItem.name || "New Folder";
  const createdPath =
    createdItem.path || `/${parent}/${createdName}`;

  // Rename to unique name when server used default label.
  let livePath = createdPath;
  let finalName = createdName;
  if (createdName !== name) {
    const renameBody = {
      path: createdPath.endsWith("/") ? createdPath : `${createdPath}/`,
      name,
    };
    // Some serializers expect RenameFolderItem root.
    const renameRes = await request.post(cmsUrl(baseUrl, PATH_RENAME_FOLDER), {
      headers: {
        ...headers,
        "Content-Type": "application/json",
      },
      data: renameBody,
    });
    if (renameRes.ok()) {
      const renamed = await renameRes.json().catch(() => ({}));
      const item = (renamed && renamed.PathItem) || renamed || {};
      finalName = item.name || name;
      livePath = item.path || `/${parent}/${finalName}`;
    } else {
      // Keep whatever was created; still recycle it.
      finalName = createdName;
      livePath = createdPath;
    }
  }

  const deletePath = livePath.startsWith("/") ? livePath : `/${livePath}`;
  const pathForDelete = deletePath.endsWith("/") ? deletePath : `${deletePath}/`;
  // JAX-RS expects the Jackson root name DeleteFolderCriteria (see PercPathService).
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
        guid: createdItem.id || createdItem.guid || "",
      },
    },
  });
  if (!delRes.ok()) {
    const text = await delRes.text().catch(() => "");
    throw new Error(
      `deleteFolder (recycle) ${pathForDelete} failed status=${delRes.status()} body=${text.slice(0, 400)}`,
    );
  }

  return {
    name: finalName,
    livePath: deletePath,
    recycledPath: `/Recycling/${finalName}`,
  };
}

/**
 * DELETE /pathmanagement/recycle/empty (Admin-only bulk purge).
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} baseUrl
 * @param {Record<string, string>} headers
 * @returns {Promise<{ status: number, body: unknown }>}
 */
async function emptyRecyclingViaApi(request, baseUrl, headers) {
  const url = cmsUrl(baseUrl, RECYCLE_EMPTY_PATH);
  const res = await request.delete(url, {
    headers: {
      ...headers,
      Accept: "application/json",
    },
  });
  let body = null;
  const text = await res.text();
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = text;
  }
  return { status: res.status(), body, text };
}

/**
 * Human-readable failure when empty API is missing or not Admin-deployed.
 * @param {{ status: number, body: unknown, text?: string }} result
 * @returns {string}
 */
function emptyApiFailureMessage(result) {
  const snippet =
    typeof result.body === "string"
      ? result.body.slice(0, 240)
      : result.text
        ? String(result.text).slice(0, 240)
        : JSON.stringify(result.body).slice(0, 240);
  if (result.status === 404 || /Not Found/i.test(snippet)) {
    return (
      `Empty Recycling API missing (HTTP ${result.status}). Deploy backend #2205 / PR #2215 ` +
      `(DELETE ${RECYCLE_EMPTY_PATH}). body=${snippet}`
    );
  }
  if (result.status === 403) {
    return `Empty Recycling forbidden (HTTP 403) — Admin required. body=${snippet}`;
  }
  return `Empty Recycling failed HTTP ${result.status}: ${snippet}`;
}

/**
 * True when Recycling children list is empty (or only empty structural roots
 * that the empty service treats as no-op — we assert zero PathItems).
 * @param {object[]} items
 * @returns {boolean}
 */
function isRecyclingListEmpty(items) {
  return !items || items.length === 0;
}

/**
 * Whether a name appears among recycling children (exact match).
 * Matches item.name equality, full path equality, or path basename —
 * not substring includes (avoids partial-prefix false positives).
 * @param {object[]} items
 * @param {string} name
 * @returns {boolean}
 */
function recyclingHasName(items, name) {
  const target = String(name || "");
  if (!target) {
    return false;
  }
  return (items || []).some((it) => {
    if (!it || typeof it !== "object") {
      return false;
    }
    if (it.name != null && String(it.name) === target) {
      return true;
    }
    if (it.path != null) {
      const p = String(it.path);
      if (p === target) {
        return true;
      }
      const base = p.split("/").filter(Boolean).pop() || "";
      return base === target;
    }
    return false;
  });
}

module.exports = {
  RECYCLE_EMPTY_PATH,
  PATH_FOLDER,
  PATH_ADD_NEW_FOLDER,
  PATH_DELETE_FOLDER,
  SELECTORS,
  cmsUrl,
  normalizePathItems,
  isAlreadyEmptyResult,
  purgedTotal,
  uniqueSeedFolderName,
  listFolderChildren,
  seedRecycledFolder,
  emptyRecyclingViaApi,
  emptyApiFailureMessage,
  isRecyclingListEmpty,
  recyclingHasName,
};
