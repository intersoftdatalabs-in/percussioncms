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

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  siteNamesFromPayload,
  isEmptyTreePayload,
} = require("../helpers/nav-tree-live");

describe("nav-tree-live helpers (#3218)", () => {
  it("reads SiteSummary name list", () => {
    assert.deepEqual(
      siteNamesFromPayload({
        SiteSummary: [{ name: "Corporate_Investments" }, { Name: "EI" }],
      }),
      ["Corporate_Investments", "EI"],
    );
  });

  it("returns empty list for missing payload", () => {
    assert.deepEqual(siteNamesFromPayload(null), []);
    assert.deepEqual(siteNamesFromPayload({}), []);
  });

  it("treats H2 empty SectionNode as empty tree", () => {
    const body = JSON.stringify({
      SectionNode: {
        childNodes: [],
        requiresLogin: false,
        sectionType: "section",
        title: "Corporate_Investments",
      },
    });
    assert.equal(isEmptyTreePayload(body), true);
  });

  it("treats empty children array without id as empty tree", () => {
    const body = JSON.stringify({
      SectionNode: { title: "BareSite", childNodes: [] },
    });
    assert.equal(isEmptyTreePayload(body), true);
  });

  it("does not treat a real navon as empty", () => {
    const body = JSON.stringify({
      SectionNode: {
        id: "0-101-1",
        title: "Home",
        childNodes: [{ id: "0-101-2", title: "About" }],
      },
    });
    assert.equal(isEmptyTreePayload(body), false);
  });

  it("rejects non-JSON as not empty-tree (caller treats as error body)", () => {
    assert.equal(isEmptyTreePayload("<html>error</html>"), false);
  });

  it("rejects unrecognized 200 JSON without SectionNode wrapper", () => {
    assert.equal(isEmptyTreePayload(JSON.stringify({})), false);
    assert.equal(isEmptyTreePayload(JSON.stringify({ title: "x" })), false);
    assert.equal(isEmptyTreePayload(JSON.stringify([])), false);
    assert.equal(isEmptyTreePayload("null"), false);
  });

  it("accepts lowercase sectionNode wrapper", () => {
    const body = JSON.stringify({
      sectionNode: { title: "BareSite", childNodes: [] },
    });
    assert.equal(isEmptyTreePayload(body), true);
  });
});
