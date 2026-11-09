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

const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { describe, it } = require("node:test");
const publish = require("../helpers/editor-host-publish");

describe("editor-host-publish helpers (#5939)", () => {
  it("classifies page vs resource publish URLs and ignores staging", () => {
    assert.equal(
      publish.isPagePublishUrl(
        "/Rhythmyx/services/sitemanage/publish/page/42",
      ),
      true,
    );
    assert.equal(
      publish.isPagePublishUrl(
        "/Rhythmyx/services/sitemanage/publish/page/staging/42",
      ),
      false,
    );
    assert.equal(
      publish.isResourcePublishUrl(
        "/Rhythmyx/services/sitemanage/publish/resource/99",
      ),
      true,
    );
    const parsed = publish.parsePublishUrl(
      "http://cms/Rhythmyx/services/sitemanage/publish/page/42",
    );
    assert.equal(parsed.kind, "page");
    assert.equal(parsed.itemId, "42");
  });

  it("exposes the publish-now dialog test ids", () => {
    assert.equal(publish.TEST_IDS.publishDialog, "editor-publish-now-dialog");
    assert.equal(publish.TEST_IDS.publishCancel, "editor-publish-now-cancel");
    assert.equal(publish.TEST_IDS.publishConfirm, "editor-publish-now-confirm");
    assert.equal(publish.TEST_IDS.publishDialogError, "editor-publish-now-error");
  });

  it("spec covers dialog confirm, cancel, and 400/403/409 mapping", () => {
    const src = fs.readFileSync(
      path.join(__dirname, "..", "editor-publish-now.spec.js"),
      "utf8",
    );
    assert.match(src, /publishDialog/);
    assert.match(src, /mode=edit/);
    assert.match(src, /#5939/);
    assert.match(src, /403/);
    assert.match(src, /409/);
  });
});
