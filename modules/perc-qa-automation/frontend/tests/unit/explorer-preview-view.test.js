/**
 * Unit tests for Explorer preview + View residual helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const {
  TEST_IDS,
  explorerEntryUrl,
  pageRenderPreviewPath,
  sitePathPreviewUrl,
  noPreviewableItemSkipMessage,
  noListedPageSkipMessage,
  isPreviewableRow,
  isListedPageRow,
  unwrapPathItems,
  resolveExplorerListPath,
  parentFolderCmsPath,
  isProductPagePreviewUrl,
  listedPageSiteNames,
  foldSiteName,
  detailRowHasExactName,
  detailRowMatchesFoldedSite,
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

  it("isListedPageRow accepts percPage/Page and rejects folders (#3456)", () => {
    assert.equal(
      isListedPageRow({
        type: "percPage",
        path: "/Sites/Corporate_Investments/Pages/About",
        id: "16777215-101-9",
      }),
      true,
    );
    assert.equal(
      isListedPageRow({ type: "Page", path: "/Sites/D/Home", id: "1" }),
      true,
    );
    assert.equal(
      isListedPageRow({
        type: "rffHome",
        path: "/Sites/CorporateInvestments/Pages/Home",
        id: "16777215-101-551",
      }),
      true,
    );
    assert.equal(
      isListedPageRow({ type: "folder", path: "/Sites/D/Pages/" }),
      false,
    );
    assert.equal(
      isListedPageRow({ type: "site", path: "/Sites/D/" }),
      false,
    );
  });

  it("unwrapPathItems reads PathItem and PagedItemList children", () => {
    assert.deepEqual(
      unwrapPathItems({ PathItem: [{ name: "A" }, { name: "B" }] }).map(
        (i) => i.name,
      ),
      ["A", "B"],
    );
    assert.deepEqual(
      unwrapPathItems({
        PagedItemList: {
          childrenInPage: [{ name: "About", type: "percPage" }],
          childrenCount: 1,
        },
      }).map((i) => i.name),
      ["About"],
    );
    assert.deepEqual(unwrapPathItems(null), []);
  });

  it("resolveExplorerListPath prefers folderPath over site-name path", () => {
    assert.equal(
      resolveExplorerListPath({
        path: "/Sites/Corporate_Investments/",
        folderPath: "//Sites/CorporateInvestments",
      }),
      "/Sites/CorporateInvestments",
    );
    assert.equal(
      resolveExplorerListPath({ path: "/Sites/Demo/" }),
      "/Sites/Demo",
    );
  });

  it("parentFolderCmsPath is a logical CMS parent", () => {
    assert.equal(
      parentFolderCmsPath("/Sites/Demo/Pages/About"),
      "/Sites/Demo/Pages",
    );
    assert.equal(parentFolderCmsPath("/Sites/Demo"), "/Sites");
    assert.equal(parentFolderCmsPath("Sites/Demo/Home"), "/Sites/Demo");
  });

  it("isProductPagePreviewUrl matches render and site-path preview", () => {
    assert.equal(
      isProductPagePreviewUrl(
        "/Rhythmyx/services/pagemanagement/render/page/16777215-101-9",
      ),
      true,
    );
    assert.equal(
      isProductPagePreviewUrl("/Sites/Demo/Home?percmobilepreview=false"),
      true,
    );
    assert.equal(
      isProductPagePreviewUrl(
        "/Rhythmyx/psx_cerffHome/rffHome.html?sys_command=preview&sys_contentid=551",
      ),
      true,
    );
    assert.equal(isProductPagePreviewUrl("/Rhythmyx/cm/app/spa.jsp"), false);
    assert.equal(
      isProductPagePreviewUrl("/Rhythmyx/psx_ce/admin/preview-settings"),
      false,
    );
  });

  it("listedPageSiteNames reads finder and repository site folders", () => {
    assert.deepEqual(
      listedPageSiteNames({
        path: "/Sites/Corporate_Investments/Pages/About",
        folderPath: "//Sites/CorporateInvestments",
      }),
      ["CorporateInvestments"],
    );
    assert.deepEqual(listedPageSiteNames({ path: "/Assets/x" }), []);
    assert.equal(foldSiteName("Corporate Investments"), "corporateinvestments");
    assert.equal(foldSiteName("Corporate_Investments"), "corporateinvestments");
  });

  it("noPreviewableItemSkipMessage is stable and cites issue", () => {
    assert.match(noPreviewableItemSkipMessage(), /#2733/);
  });

  it("noListedPageSkipMessage cites listing slice", () => {
    assert.match(noListedPageSkipMessage(), /#3457/);
    assert.match(noListedPageSkipMessage(), /#3456/);
  });

  it("detailRowHasExactName matches a name cell, not whole-row /^Pages$/", () => {
    const rowText = "Pages\nFolder\n/Sites/Corporate_Investments/Pages/";
    assert.equal(detailRowHasExactName(rowText, "Pages"), true);
    assert.equal(detailRowHasExactName(rowText, "Files"), false);
    assert.equal(/^Pages$/.test(rowText), false);
  });

  it("detailRowMatchesFoldedSite matches finder underscore site rows", () => {
    const rowText =
      "Corporate_Investments\nsite\n/Sites/Corporate_Investments/";
    assert.equal(
      detailRowMatchesFoldedSite(rowText, ["corporateinvestments"]),
      true,
    );
    assert.equal(
      detailRowMatchesFoldedSite(rowText, ["enterpriseinvestments"]),
      false,
    );
  });

  it("preview spec imports adminBasicAuthHeaders for REST listing (#3463)", () => {
    const specPath = path.join(__dirname, "..", "explorer-preview-view.spec.js");
    const src = fs.readFileSync(specPath, "utf8");
    assert.match(
      src,
      /const\s*\{[^}]*adminBasicAuthHeaders[^}]*\}\s*=\s*require\(["']\.\/helpers\/auth["']\)/,
    );
    assert.match(src, /adminBasicAuthHeaders\s*\(\s*\)/);
  });
});
