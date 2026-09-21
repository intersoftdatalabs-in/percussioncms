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
  TEST_IDS,
  editorSpaUrl,
  isItemCreateUrl,
} = require("../helpers/editor-host-create-item");

describe("editor-host-create-item helpers (#4646)", () => {
  it("exposes create panel test ids", () => {
    assert.equal(TEST_IDS.panel, "editor-create-panel");
    assert.equal(TEST_IDS.submit, "editor-create-submit");
  });

  it("builds spa editor URL with slash path segments", () => {
    const url = editorSpaUrl("http://cms:9993");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?/);
    assert.match(url, /entry=editor/);
  });

  it("matches item create POST URLs", () => {
    assert.equal(
      isItemCreateUrl(
        "http://cms/Rhythmyx/services/itemmanagement/item/create",
      ),
      true,
    );
    assert.equal(isItemCreateUrl("/services/itemmanagement/item/fields/1"), false);
  });
});
