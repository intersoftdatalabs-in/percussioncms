/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Unit tests for explorer-display-format-switch helpers (#3618) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  displayFormatsCatalogUrl,
  unwrapDisplayFormatCatalog,
  isNumericDisplayFormatId,
  nonEmptySelectOptionValues,
  isPaginatedFolderDisplayFormatRequest,
  explorerSpaUrl,
} = require("../helpers/explorer-display-format-switch");

describe("explorer-display-format-switch helpers (#3618)", () => {
  it("exports stable product test ids", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.displayFormat, "explorer-display-format");
    assert.equal(TEST_IDS.detailList, "detail-list");
    assert.equal(TEST_IDS.colHeaderPrefix, "detail-col-header-");
  });

  it("builds catalog and explorer SPA URLs", () => {
    assert.equal(
      displayFormatsCatalogUrl("http://127.0.0.1:9992/"),
      "http://127.0.0.1:9992/Rhythmyx/services/displayformats",
    );
    assert.equal(
      displayFormatsCatalogUrl("http://127.0.0.1:9992/", {
        validForFolder: true,
      }),
      "http://127.0.0.1:9992/Rhythmyx/services/displayformats?validForFolder=true",
    );
    assert.match(
      explorerSpaUrl("http://127.0.0.1:9992/"),
      /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+/,
    );
  });

  it("unwraps Jackson DisplayFormat list envelopes", () => {
    assert.deepEqual(
      unwrapDisplayFormatCatalog({
        DisplayFormat: [{ name: "FolderList", displayId: 1 }],
      }).map((r) => r.name),
      ["FolderList"],
    );
    assert.equal(
      unwrapDisplayFormatCatalog({
        DisplayFormatList: {
          DisplayFormat: [{ name: "A" }, { name: "B" }],
        },
      }).length,
      2,
    );
    assert.equal(unwrapDisplayFormatCatalog(null).length, 0);
  });

  it("isNumericDisplayFormatId accepts positive integers only", () => {
    assert.equal(isNumericDisplayFormatId("7"), true);
    assert.equal(isNumericDisplayFormatId(" 12 "), true);
    assert.equal(isNumericDisplayFormatId("0"), false);
    assert.equal(isNumericDisplayFormatId("FolderList"), false);
    assert.equal(isNumericDisplayFormatId(""), false);
  });

  it("nonEmptySelectOptionValues drops the default empty option", () => {
    assert.deepEqual(nonEmptySelectOptionValues(["", "3", "8"]), ["3", "8"]);
  });

  it("isPaginatedFolderDisplayFormatRequest matches displayFormatId exactly", () => {
    const url =
      "http://127.0.0.1:9992/Rhythmyx/services/pathmanagement/path/paginatedFolder/Sites/Foo?startIndex=0&maxResults=50&displayFormatId=8";
    assert.equal(isPaginatedFolderDisplayFormatRequest(url, "8"), true);
    assert.equal(isPaginatedFolderDisplayFormatRequest(url, "3"), false);
    assert.equal(
      isPaginatedFolderDisplayFormatRequest(
        url.replace("displayFormatId=8", "displayFormatId=80"),
        "8",
      ),
      false,
    );
    assert.equal(
      isPaginatedFolderDisplayFormatRequest(
        url.replace("displayFormatId=8", "displayFormatId=18"),
        "8",
      ),
      false,
    );
    assert.equal(
      isPaginatedFolderDisplayFormatRequest(
        "http://x/pathmanagement/path/folder/Sites",
        "8",
      ),
      false,
    );
  });
});
