/**
 * Unit tests for folder + recycle smoke pure helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  isContextHealthyStatus,
  isProductPathErrorBody,
  contextDownFailureMessage,
  extractPathItem,
  extractPathItemGuid,
  findNamedPathItem,
  findInRecycling,
  restoreFolderUrl,
  PATH_RESTORE_FOLDER,
} = require("../helpers/folder-recycle-smoke");

describe("folder-recycle-smoke helpers", () => {
  it("isContextHealthyStatus treats 2xx/3xx/401/403 as live context", () => {
    assert.equal(isContextHealthyStatus(200), true);
    assert.equal(isContextHealthyStatus(204), true);
    assert.equal(isContextHealthyStatus(302), true);
    assert.equal(isContextHealthyStatus(401), true);
    assert.equal(isContextHealthyStatus(403), true);
    assert.equal(isContextHealthyStatus(404), false);
    assert.equal(isContextHealthyStatus(500), false);
    assert.equal(isContextHealthyStatus(503), false);
    assert.equal(isContextHealthyStatus(0), false);
    assert.equal(isContextHealthyStatus(null), false);
    assert.equal(isContextHealthyStatus(undefined), false);
  });

  it("isProductPathErrorBody classifies product vs context-down bodies (#2488)", () => {
    assert.equal(
      isProductPathErrorBody(404, '{"Errors":{"global":["Path not found"]}}'),
      true,
    );
    assert.equal(
      isProductPathErrorBody(404, "Path not found for Assets"),
      true,
    );
    assert.equal(
      isProductPathErrorBody(500, "Transaction silently rolled back"),
      true,
    );
    // Lone parentFolders / PropertyAccessException must not match (docs/noise).
    assert.equal(
      isProductPathErrorBody(500, "docs mention parentFolders field"),
      false,
    );
    assert.equal(
      isProductPathErrorBody(500, "PropertyAccessException alone"),
      false,
    );
    // #2488 Hibernate stack: both markers required.
    assert.equal(
      isProductPathErrorBody(
        500,
        "PropertyAccessException: Could not set value of type [PersistentSet] on parentFolders",
      ),
      true,
    );
    assert.equal(isProductPathErrorBody(503, "Path not found"), false);
    assert.equal(isProductPathErrorBody(200, "Path not found"), false);
  });

  it("contextDownFailureMessage cites #2464/#2423 and folderHelper cycle", () => {
    const msg = contextDownFailureMessage({
      status: 503,
      url: "http://127.0.0.1:9993/Rhythmyx/services/pathmanagement/path/folder/",
      bodySnippet: "BeanCurrentlyInCreationException folderHelper",
    });
    assert.match(msg, /#2464/);
    assert.match(msg, /#2423/);
    assert.match(msg, /folderHelper/);
    assert.match(msg, /hard fail/i);
    assert.match(msg, /503/);
  });

  it("extractPathItem unwraps PathItem / pathItem wrappers", () => {
    assert.deepEqual(extractPathItem(null), {});
    assert.equal(extractPathItem({ PathItem: { name: "a" } }).name, "a");
    assert.equal(extractPathItem({ pathItem: { name: "b" } }).name, "b");
    assert.equal(extractPathItem({ name: "c" }).name, "c");
  });

  it("extractPathItemGuid reads id/guid variants", () => {
    assert.equal(extractPathItemGuid(null), "");
    assert.equal(extractPathItemGuid({ id: "1-2-3" }), "1-2-3");
    assert.equal(extractPathItemGuid({ guid: "g-1" }), "g-1");
    assert.equal(extractPathItemGuid({ Id: "X" }), "X");
    assert.equal(extractPathItemGuid({ name: "only" }), "");
  });

  it("findNamedPathItem matches name and path basename exactly", () => {
    const items = [
      { name: "qa-folder-1", path: "/Assets/qa-folder-1" },
      { path: "/Recycling/Assets/other" },
    ];
    assert.equal(findNamedPathItem(items, "qa-folder-1").name, "qa-folder-1");
    assert.equal(
      findNamedPathItem(items, "other").path,
      "/Recycling/Assets/other",
    );
    assert.equal(findNamedPathItem(items, "missing"), null);
    // Substring must not count.
    assert.equal(
      findNamedPathItem([{ name: "qa-folder-10" }], "qa-folder-1"),
      null,
    );
  });

  it("restoreFolderUrl builds pathmanagement restore URL", () => {
    assert.match(PATH_RESTORE_FOLDER, /\/pathmanagement\/path\/restoreFolder$/);
    assert.equal(
      restoreFolderUrl("http://127.0.0.1:9993/", "jcr:guid-1"),
      "http://127.0.0.1:9993/Rhythmyx/services/pathmanagement/path/restoreFolder/jcr%3Aguid-1",
    );
  });

  it("findInRecycling rethrows non-404 listFolderChildren failures", async () => {
    const request = {
      get: async () => ({
        ok: () => false,
        status: () => 503,
        text: async () => "BeanCurrentlyInCreationException folderHelper",
      }),
    };
    await assert.rejects(
      () => findInRecycling(request, "http://127.0.0.1:9993", {}, "seed"),
      /failed status=503/,
    );
  });

  it("findInRecycling treats 404 roots as empty and returns not found", async () => {
    const request = {
      get: async () => ({
        ok: () => false,
        status: () => 404,
        text: async () => "missing",
      }),
    };
    const result = await findInRecycling(
      request,
      "http://127.0.0.1:9993",
      {},
      "seed",
    );
    assert.equal(result.found, false);
    assert.equal(result.item, null);
  });

  it("findInRecycling returns hit when list contains name", async () => {
    const request = {
      get: async (url) => {
        const path = String(url || "");
        if (
          path.includes("/folder/Recycling") &&
          !path.includes("Recycling/")
        ) {
          return {
            ok: () => true,
            status: () => 200,
            json: async () => [
              { name: "seed-a", path: "/Recycling/Assets/seed-a" },
            ],
            text: async () => "[]",
          };
        }
        return {
          ok: () => false,
          status: () => 404,
          text: async () => "missing",
        };
      },
    };
    const result = await findInRecycling(
      request,
      "http://127.0.0.1:9993",
      {},
      "seed-a",
    );
    assert.equal(result.found, true);
    assert.equal(result.location, "Recycling");
    assert.equal(result.item.name, "seed-a");
  });
});
