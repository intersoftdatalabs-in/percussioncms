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
  isH2QaEnv,
  shouldSkipListedPagePreview,
  encodeCmsRelPath,
  isPreviewableRow,
  isListedPageRow,
  unwrapPathItems,
  resolveExplorerListPath,
  parentFolderCmsPath,
  isProductPagePreviewUrl,
  listedPageSiteNames,
  foldSiteName,
  isExplorerSiteRootTestId,
  detailRowHasExactName,
  detailRowMatchesFoldedSite,
  treeNodeMatchesFoldedSite,
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
    assert.equal(
      isListedPageRow({
        type: "NewsArticle",
        category: "ASSET",
        path: "/Sites/Demo/Pages/Q3 Brief",
        id: "16777215-101-88",
      }),
      true,
    );
    assert.equal(
      isListedPageRow({
        type: "rffImage",
        path: "/Sites/Demo/Images/logo.png",
        id: "16777216-101-9",
      }),
      false,
    );
    assert.equal(
      isListedPageRow({
        type: "rffFile",
        path: "/Sites/Demo/Files/spec.pdf",
        id: "16777216-101-10",
      }),
      false,
    );
  });

  it("unwrapPathItems reads PathItem, PagedItemList, and PSPathItemList (#3627)", () => {
    assert.deepEqual(
      unwrapPathItems({ PathItem: [{ name: "A" }, { name: "B" }] }).map(
        (i) => i.name,
      ),
      ["A", "B"],
    );
    assert.deepEqual(
      unwrapPathItems({ PathItem: { name: "Solo", type: "percPage" } }).map(
        (i) => i.name,
      ),
      ["Solo"],
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
    assert.deepEqual(
      unwrapPathItems({
        PSPathItemList: {
          PathItem: [{ name: "Corporate Investments" }],
        },
      }).map((i) => i.name),
      ["Corporate Investments"],
    );
    assert.deepEqual(unwrapPathItems(null), []);
  });

  it("shouldSkipListedPagePreview never skips when H2 or a previewable row exists (#3627)", () => {
    assert.equal(
      shouldSkipListedPagePreview(
        { listedPage: { name: "Home" }, previewableRowCount: 0 },
        {},
      ),
      false,
    );
    assert.equal(
      shouldSkipListedPagePreview(
        { listedPage: null, previewableRowCount: 1 },
        {},
      ),
      false,
    );
    assert.equal(
      shouldSkipListedPagePreview(
        { listedPage: null, previewableRowCount: 0, h2: true },
        {},
      ),
      false,
    );
    assert.equal(
      shouldSkipListedPagePreview(
        { listedPage: null, previewableRowCount: 0 },
        { TEST_DB_TYPE: "h2" },
      ),
      false,
    );
    assert.equal(
      shouldSkipListedPagePreview(
        { listedPage: null, previewableRowCount: 0 },
        { TEST_DB_TYPE: "mssql" },
      ),
      true,
    );
    assert.equal(isH2QaEnv({ TEST_DB_TYPE: "h2" }), true);
    assert.equal(isH2QaEnv({ TEST_DB_TYPE: "mysql" }), false);
  });

  it("encodeCmsRelPath encodes site-name spaces", () => {
    assert.equal(
      encodeCmsRelPath("/Sites/Corporate Investments/Pages"),
      "Sites/Corporate%20Investments/Pages",
    );
    assert.equal(encodeCmsRelPath("Sites/Demo"), "Sites/Demo");
    assert.equal(encodeCmsRelPath(""), "");
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
    assert.equal(
      isProductPagePreviewUrl(
        "/Rhythmyx/cm/app/spa.jsp?entry=editor&contentId=551&mode=view",
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

  it("listedPageSiteNames adds a site hint from Corporate Investments Home (#3684)", () => {
    assert.deepEqual(
      listedPageSiteNames({
        name: "Corporate Investments Home",
        path: "/Sites/16777215-101-703/Home",
      }),
      ["16777215-101-703", "Corporate Investments"],
    );
    assert.deepEqual(
      listedPageSiteNames({
        name: "Home",
        path: "/Assets/x",
      }),
      [],
    );
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

  it("isExplorerSiteRootTestId rejects nested Pages nodes", () => {
    assert.equal(
      isExplorerSiteRootTestId("tree-node-/Sites/Corporate_Investments/"),
      true,
    );
    assert.equal(
      isExplorerSiteRootTestId("tree-node-/Sites/16777215-101-703/"),
      true,
    );
    assert.equal(isExplorerSiteRootTestId("tree-node-/Sites/"), false);
    assert.equal(
      isExplorerSiteRootTestId("tree-node-/Sites/Corporate_Investments/Pages/"),
      false,
    );
  });

  it("treeNodeMatchesFoldedSite matches finder path, GUID+name, and folderPath (#3684)", () => {
    assert.equal(
      treeNodeMatchesFoldedSite(
        "tree-node-/Sites/Corporate_Investments/",
        "Corporate_Investments",
        "Corporate_Investments",
        ["corporateinvestments"],
      ),
      true,
    );
    assert.equal(
      treeNodeMatchesFoldedSite(
        "tree-node-/Sites/16777215-101-703/",
        "Corporate_Investments",
        "Corporate_Investments",
        ["corporateinvestments"],
      ),
      true,
    );
    assert.equal(
      treeNodeMatchesFoldedSite(
        "tree-node-/Sites/16777215-101-703/",
        "16777215-101-703",
        "16777215-101-703",
        ["corporateinvestments"],
        "//Sites/CorporateInvestments",
      ),
      true,
    );
    assert.equal(
      treeNodeMatchesFoldedSite(
        "tree-node-/Sites/16777215-101-703/",
        "Corporate Investments",
        "Corporate Investments",
        ["corporateinvestments"],
      ),
      true,
    );
    assert.equal(
      treeNodeMatchesFoldedSite(
        "tree-node-/Sites/16777215-101-703/",
        "",
        "",
        ["corporateinvestments"],
      ),
      false,
    );
    assert.equal(
      treeNodeMatchesFoldedSite(
        "tree-node-/Sites/Enterprise_Investments/",
        "Enterprise_Investments",
        "Enterprise_Investments",
        ["corporateinvestments"],
      ),
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
    assert.match(src, /shouldSkipListedPagePreview/);
    assert.doesNotMatch(
      src,
      /if\s*\(\s*!listed\s*\)\s*\{\s*test\.skip/,
    );
  });
});
