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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  createdFolderPath,
  seededSiteName,
  siteNames,
} = require("../helpers/site-default-workflow-fixture");

describe("site default workflow fixture (#5075)", () => {
  it("reads site names without requiring folderRoot", () => {
    const payload = {
      SiteList: [
        { name: "Corporate_Investments", baseUrl: "http://example" },
        { name: "  Enterprise_Investments  " },
        { description: "no name" },
      ],
    };
    assert.deepEqual(siteNames(payload), [
      "Corporate_Investments",
      "Enterprise_Investments",
    ]);
    assert.equal(seededSiteName(payload), "Corporate_Investments");
    assert.equal(
      seededSiteName({ SiteList: [{ name: "WfSite1", folderRoot: "" }] }),
      "",
    );
    assert.equal(seededSiteName(null), "");
  });

  it("reads a content-explorer or finder folder path", () => {
    assert.equal(
      createdFolderPath({ RxFolder: { path: "  //Sites/WfSite1  " } }),
      "//Sites/WfSite1",
    );
    assert.equal(
      createdFolderPath({ Folder: { path: "/Sites/WfSite1" } }),
      "/Sites/WfSite1",
    );
    assert.equal(createdFolderPath({ RxFolder: { name: "WfSite1" } }), "");
    assert.equal(createdFolderPath(null), "");
  });
});
