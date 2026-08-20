/**
 * Unit tests for Explorer New-item type picker live helpers (#3628).
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
  NEW_ITEM_HOST_TEST_IDS,
  CREATE_MENU_TEST_ID,
  isNewItemHostName,
  isExplorerPageType,
  preferredContentTypeName,
  isFeatureUrl,
  isDataFlowCeHtmlUrl,
  isCreateSuccessStatus,
  isItemCreateUrl,
  parseContentTypeFromCreateBody,
  unwrapContentTypeNames,
  newItemMissingFailMessage,
  pickerEmptyFailMessage,
} = require("../helpers/explorer-new-item-type-picker");

describe("explorer-new-item-type-picker helpers (#3628)", () => {
  it("exports stable picker and toolbar test ids", () => {
    assert.equal(TEST_IDS.typePicker, "explorer-type-picker");
    assert.equal(TEST_IDS.typePickerSelect, "explorer-type-picker-select");
    assert.equal(TEST_IDS.typePickerOk, "explorer-type-picker-ok");
    assert.equal(TEST_IDS.typePickerCancel, "explorer-type-picker-cancel");
    assert.equal(TEST_IDS.actionToolbar, "action-toolbar");
    assert.ok(NEW_ITEM_HOST_TEST_IDS.includes("action-toolbar-item-New"));
    assert.equal(CREATE_MENU_TEST_ID, "action-toolbar-item-Create");
  });

  it("treats New / Create_New_Item as hosts and percPage as a page type", () => {
    assert.equal(isNewItemHostName("New"), true);
    assert.equal(isNewItemHostName("Create_New_Item"), true);
    assert.equal(isNewItemHostName("rffEvent"), false);
    assert.equal(isExplorerPageType("percPage"), true);
    assert.equal(isExplorerPageType("Page"), true);
    assert.equal(isExplorerPageType("percFile"), false);
  });

  it("preferredContentTypeName skips percPage when another type exists", () => {
    assert.equal(
      preferredContentTypeName(["percPage", "percFile", "rffEvent"]),
      "percFile",
    );
    assert.equal(preferredContentTypeName(["percPage"]), "percPage");
    assert.equal(preferredContentTypeName([]), "");
    assert.equal(preferredContentTypeName(null), "");
  });

  it("preferredContentTypeName prefers simple assets over required-field widgets", () => {
    assert.equal(
      preferredContentTypeName([
        "percBlogIndexAsset",
        "percPage",
        "percSimpleTextAsset",
      ]),
      "percSimpleTextAsset",
    );
  });

  it("classifies live find/create URLs and leftover CE HTML", () => {
    assert.equal(
      isFeatureUrl("http://127.0.0.1:1/Rhythmyx/services/actions/find"),
      true,
    );
    assert.equal(
      isItemCreateUrl(
        "http://127.0.0.1:1/Rhythmyx/services/itemmanagement/item/create",
      ),
      true,
    );
    assert.equal(isFeatureUrl("http://127.0.0.1:1/Rhythmyx/login"), false);
    assert.equal(
      isDataFlowCeHtmlUrl("/Rhythmyx/sys_cxSupport/contenteditorurls.html"),
      true,
    );
    assert.equal(isDataFlowCeHtmlUrl("/Rhythmyx/cm/app/spa.jsp"), false);
  });

  it("treats 200/201/204 as create success (documented empty-success)", () => {
    assert.equal(isCreateSuccessStatus(200), true);
    assert.equal(isCreateSuccessStatus(201), true);
    assert.equal(isCreateSuccessStatus(204), true);
    assert.equal(isCreateSuccessStatus(500), false);
    assert.equal(isCreateSuccessStatus(404), false);
  });

  it("parses contentType from ItemCreateRequest envelope and raw JSON", () => {
    assert.equal(
      parseContentTypeFromCreateBody(
        JSON.stringify({
          ItemCreateRequest: { contentType: "rffEvent", folderPath: "/Sites" },
        }),
      ),
      "rffEvent",
    );
    assert.equal(
      parseContentTypeFromCreateBody({ contentType: "percFile" }),
      "percFile",
    );
    assert.equal(parseContentTypeFromCreateBody(""), "");
    assert.equal(parseContentTypeFromCreateBody("{not-json"), "");
  });

  it("unwraps type names from ActionMenuList and ContentType envelopes", () => {
    assert.deepEqual(
      unwrapContentTypeNames({
        ActionMenuList: [
          { name: "New", label: "New Item" },
          { name: "percFile", label: "File" },
        ],
      }),
      ["percFile"],
    );
    assert.deepEqual(
      unwrapContentTypeNames({
        ContentType: [{ name: "rffEvent" }, { Name: "percFile" }],
      }),
      ["rffEvent", "percFile"],
    );
    assert.deepEqual(unwrapContentTypeNames(null), []);
  });

  it("fail messages mention #3628 and forbid skip", () => {
    assert.match(newItemMissingFailMessage(), /#3628/);
    assert.match(newItemMissingFailMessage(), /Do not skip/i);
    assert.match(pickerEmptyFailMessage(), /Do not skip/i);
  });

  it("live spec must not stub actions/find or item create", () => {
    const spec = fs.readFileSync(
      path.join(__dirname, "..", "explorer-new-item-type-picker.spec.js"),
      "utf8",
    );
    assert.equal(/page\.route\s*\(\s*["']\*\*\/actions\/find/i.test(spec), false);
    assert.equal(
      /page\.route\s*\(\s*["']\*\*\/itemmanagement\/item\/create/i.test(spec),
      false,
    );
    assert.equal(/stubNewItemHostLeaf/i.test(spec), false);
    assert.equal(/test\.skip\s*\(/i.test(spec), false);
  });
});
