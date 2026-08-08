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
  contextDownFailureMessage,
  extractPathItem,
  extractPathItemGuid,
  findNamedPathItem,
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
    assert.equal(findNamedPathItem(items, "other").path, "/Recycling/Assets/other");
    assert.equal(findNamedPathItem(items, "missing"), null);
    // Substring must not count.
    assert.equal(findNamedPathItem([{ name: "qa-folder-10" }], "qa-folder-1"), null);
  });

  it("restoreFolderUrl builds pathmanagement restore URL", () => {
    assert.match(PATH_RESTORE_FOLDER, /\/pathmanagement\/path\/restoreFolder$/);
    assert.equal(
      restoreFolderUrl("http://127.0.0.1:9993/", "jcr:guid-1"),
      "http://127.0.0.1:9993/Rhythmyx/services/pathmanagement/path/restoreFolder/jcr%3Aguid-1",
    );
  });
});
