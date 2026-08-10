/**
 * Unit tests for Explorer preview + View residual helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  explorerEntryUrl,
  pageRenderPreviewPath,
  sitePathPreviewUrl,
  noPreviewableItemSkipMessage,
  isPreviewableRow,
} = require("../helpers/explorer-preview-view");

describe("explorer-preview-view helpers (#2733)", () => {
  it("exports stable test ids used by shell chrome", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.preview, "action-preview");
    assert.equal(TEST_IDS.refresh, "explorer-refresh-list");
    assert.equal(TEST_IDS.viewTools, "explorer-view-tools");
  });

  it("explorerEntryUrl builds spa.jsp explorer entry with cache-buster", () => {
    assert.equal(
      explorerEntryUrl("http://127.0.0.1:9993", { cacheBuster: "42" }),
      "http://127.0.0.1:9993/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=42",
    );
    assert.equal(
      explorerEntryUrl("http://localhost:9992/", { cacheBuster: "a b" }),
      "http://localhost:9992/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=a%20b",
    );
  });

  it("pageRenderPreviewPath encodes content id", () => {
    assert.equal(
      pageRenderPreviewPath("/Rhythmyx/services", "16777215-101-1"),
      "/Rhythmyx/services/pagemanagement/render/page/16777215-101-1",
    );
    assert.equal(pageRenderPreviewPath("/services", ""), "");
  });

  it("sitePathPreviewUrl only for Sites paths", () => {
    assert.equal(
      sitePathPreviewUrl("/Sites/Demo/Home"),
      "/Sites/Demo/Home?percmobilepreview=false",
    );
    assert.equal(sitePathPreviewUrl("/Assets/x"), "");
  });

  it("isPreviewableRow accepts pages/assets with ids", () => {
    assert.equal(
      isPreviewableRow({ type: "page", path: "/Sites/D/H", id: "1" }),
      true,
    );
    assert.equal(
      isPreviewableRow({ type: "folder", path: "/Sites/D" }),
      false,
    );
    assert.equal(
      isPreviewableRow({ type: "asset", path: "/Assets/a", id: "9" }),
      true,
    );
    assert.equal(
      isPreviewableRow({ type: "asset", path: "/Assets/a" }),
      false,
    );
  });

  it("noPreviewableItemSkipMessage is stable and cites issue", () => {
    assert.match(noPreviewableItemSkipMessage(), /#2733/);
  });
});
