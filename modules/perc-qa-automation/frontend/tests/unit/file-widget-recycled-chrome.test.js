/**
 * Unit tests for File widget recycled chrome pure helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  SELECTORS,
  PARENT_ISSUE,
  PRODUCT_FIX_ISSUE,
  RESIDUAL_ISSUE,
  WIDGET_TEST_FILE_PAGE_PATH_CANDIDATES,
  hasRecycledAssetChrome,
  isCleanWidgetChrome,
  classifyWidgetChrome,
  decorationCssDefinesRecycledChrome,
  detectFileAssetTypes,
  siteSummaryNames,
  pathNamesSuggestWidgetTestFile,
  cmsUrl,
  widgetTestFilePageUrls,
  shouldEnforceFileWidgetFixtures,
  fileWidgetFixturesSkipReason,
  gateFileWidgetFixtures,
} = require("../helpers/file-widget-recycled-chrome");

describe("file-widget-recycled-chrome helpers", () => {
  it("exposes stable recycled chrome selector and issue ids", () => {
    assert.equal(SELECTORS.recycledWidget, ".perc-widget.perc-recycled-asset");
    assert.equal(SELECTORS.recycledClass, "perc-recycled-asset");
    assert.equal(SELECTORS.widget, ".perc-widget");
    assert.equal(PARENT_ISSUE, 777);
    assert.equal(PRODUCT_FIX_ISSUE, 2238);
    assert.equal(RESIDUAL_ISSUE, 2239);
    assert.ok(WIDGET_TEST_FILE_PAGE_PATH_CANDIDATES.length >= 1);
  });

  it("hasRecycledAssetChrome / isCleanWidgetChrome classify class tokens", () => {
    assert.equal(
      hasRecycledAssetChrome("perc-widget perc-recycled-asset"),
      true,
    );
    assert.equal(hasRecycledAssetChrome("perc-widget"), false);
    assert.equal(hasRecycledAssetChrome(""), false);
    assert.equal(hasRecycledAssetChrome(null), false);
    assert.equal(hasRecycledAssetChrome("perc-recycled-asset-extra"), false);
    assert.equal(isCleanWidgetChrome("perc-widget"), true);
    assert.equal(isCleanWidgetChrome("perc-widget perc-recycled-asset"), false);
  });

  it("classifyWidgetChrome maps assembly attributes", () => {
    const recycled = classifyWidgetChrome({
      className: "perc-widget perc-recycled-asset",
      title: SELECTORS.recycledTitle,
      assetId: "123-456",
    });
    assert.equal(recycled.recycled, true);
    assert.equal(recycled.clean, false);
    assert.equal(recycled.assetId, "123-456");
    assert.equal(recycled.title, "Asset is in Recycle Bin");

    const clean = classifyWidgetChrome({
      className: "perc-widget",
      title: "",
      assetId: "789",
    });
    assert.equal(clean.recycled, false);
    assert.equal(clean.clean, true);
  });

  it("decorationCssDefinesRecycledChrome requires rule + outline", () => {
    assert.equal(
      decorationCssDefinesRecycledChrome(
        ".perc-recycled-asset { outline-style: dotted; outline-color: red; }",
      ),
      true,
    );
    assert.equal(
      decorationCssDefinesRecycledChrome(".other { color: red; }"),
      false,
    );
    assert.equal(decorationCssDefinesRecycledChrome(""), false);
    assert.equal(
      decorationCssDefinesRecycledChrome(
        ".perc-recycled-asset { color: blue; }",
      ),
      false,
    );
  });

  it("detectFileAssetTypes finds percFileAsset tokens", () => {
    assert.deepEqual(detectFileAssetTypes(null), {
      hasFileAssetType: false,
      matchedTokens: [],
    });
    const hit = detectFileAssetTypes({
      ContentType: [{ name: "percFileAsset" }, { name: "percImage" }],
    });
    assert.equal(hit.hasFileAssetType, true);
    assert.ok(hit.matchedTokens.includes("percFileAsset"));
    assert.equal(
      detectFileAssetTypes("no file types here").hasFileAssetType,
      false,
    );
  });

  it("siteSummaryNames normalizes wrappers and arrays", () => {
    assert.deepEqual(siteSummaryNames(null), []);
    assert.deepEqual(siteSummaryNames([]), []);
    assert.deepEqual(siteSummaryNames([{ name: "Corporate Investments" }]), [
      "Corporate Investments",
    ]);
    assert.deepEqual(
      siteSummaryNames({ SiteSummary: [{ name: "A" }, { siteName: "B" }] }),
      ["A", "B"],
    );
  });

  it("pathNamesSuggestWidgetTestFile detects widget-test / file folders", () => {
    assert.equal(pathNamesSuggestWidgetTestFile(["Sites", "Assets"]), false);
    assert.equal(
      pathNamesSuggestWidgetTestFile(["widget-test-page", "file"]),
      true,
    );
    assert.equal(pathNamesSuggestWidgetTestFile(["My widget-test page"]), true);
  });

  it("cmsUrl and widgetTestFilePageUrls join without double slash", () => {
    assert.equal(
      cmsUrl("http://127.0.0.1:9993/", "/Rhythmyx/x"),
      "http://127.0.0.1:9993/Rhythmyx/x",
    );
    const urls = widgetTestFilePageUrls("http://localhost:9992/");
    assert.ok(urls.length >= 2);
    assert.ok(urls.every((u) => u.startsWith("http://localhost:9992/")));
    assert.ok(urls.some((u) => u.includes("widget-test-page/file")));
  });

  it("shouldEnforceFileWidgetFixtures reads env flags", () => {
    assert.equal(shouldEnforceFileWidgetFixtures({}), false);
    assert.equal(
      shouldEnforceFileWidgetFixtures({ EXPECT_FILE_WIDGET_FIXTURES: "1" }),
      true,
    );
    assert.equal(
      shouldEnforceFileWidgetFixtures({
        TEST_EXPECT_FILE_WIDGET_FIXTURES: "true",
      }),
      true,
    );
  });

  it("fileWidgetFixturesSkipReason embeds durable issue URLs", () => {
    const msg = fileWidgetFixturesSkipReason();
    assert.match(msg, /BUG:/);
    assert.match(msg, new RegExp(`#${PARENT_ISSUE}`));
    assert.match(msg, new RegExp(`#${PRODUCT_FIX_ISSUE}`));
    assert.match(msg, new RegExp(`#${RESIDUAL_ISSUE}`));
    assert.match(msg, /EXPECT_FILE_WIDGET_FIXTURES/);
    assert.match(
      msg,
      /https:\/\/github\.com\/intersoftdatalabs-in\/percussioncms\/issues\/2238/,
    );
  });

  it("gateFileWidgetFixtures skips when incomplete unless enforce", () => {
    const soft = gateFileWidgetFixtures(
      { hasFileAssetType: false, hasSites: false },
      { enforce: false },
    );
    assert.equal(soft.skip, true);
    assert.match(soft.reason, /BUG:/);

    const hard = gateFileWidgetFixtures(
      { hasFileAssetType: false, hasSites: true },
      { enforce: true },
    );
    assert.equal(hard.skip, false);
    assert.match(hard.reason, /no percFileAsset/);

    const ok = gateFileWidgetFixtures(
      {
        hasFileAssetType: true,
        hasSites: true,
        hasWidgetTestPath: true,
      },
      { enforce: true },
    );
    assert.equal(ok.skip, false);
    assert.equal(ok.reason, "");
  });
});
