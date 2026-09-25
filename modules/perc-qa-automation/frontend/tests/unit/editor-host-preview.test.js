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

/**
 * Unit tests for EditorHost preview URL helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  isPageRenderPreviewUrl,
  isAssetViewUrlRequest,
  isEditorHostPreviewUrl,
  parseEditorPreviewUrl,
} = require("../helpers/editor-host-preview");

describe("editor-host-preview helpers (#4568)", () => {
  it("exports stable test ids", () => {
    assert.equal(TEST_IDS.host, "editor-host");
    assert.equal(TEST_IDS.preview, "editor-preview");
    assert.equal(TEST_IDS.previewDone, "editor-preview-done");
    assert.equal(TEST_IDS.previewError, "editor-preview-error");
    assert.equal(TEST_IDS.previewTemplate, "editor-preview-template");
    assert.equal(TEST_IDS.previewFrame, "editor-preview-frame");
  });

  it("classifies page render and asset view URLs", () => {
    assert.equal(
      isPageRenderPreviewUrl(
        "/Rhythmyx/services/pagemanagement/render/page/42",
      ),
      true,
    );
    assert.equal(
      isAssetViewUrlRequest(
        "/Rhythmyx/services/assetmanagement/asset/assetViewUrl/99",
      ),
      true,
    );
    assert.equal(
      isEditorHostPreviewUrl("/cm/app/editor?contentId=42&mode=view"),
      true,
    );
    assert.equal(
      isPageRenderPreviewUrl("/cm/app/editor?contentId=42"),
      false,
    );
  });

  it("parses item id from preview service URLs", () => {
    assert.deepEqual(
      parseEditorPreviewUrl(
        "http://127.0.0.1/Rhythmyx/services/pagemanagement/render/page/42",
      ),
      { kind: "page", itemId: "42" },
    );
    assert.deepEqual(
      parseEditorPreviewUrl(
        "/Rhythmyx/services/assetmanagement/asset/assetViewUrl/99",
      ),
      { kind: "asset", itemId: "99" },
    );
  });
});
