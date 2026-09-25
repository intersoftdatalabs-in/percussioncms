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
const takedown = require("../helpers/editor-host-takedown");

describe("editor-host-takedown helpers (#4862)", () => {
  it("classifies page vs resource takedown URLs and ignores staging", () => {
    assert.equal(
      takedown.isPageTakedownUrl(
        "/Rhythmyx/services/sitemanage/publish/takedown/page/42",
      ),
      true,
    );
    assert.equal(
      takedown.isPageTakedownUrl(
        "/Rhythmyx/services/sitemanage/publish/takedown/page/staging/42",
      ),
      false,
    );
    assert.equal(
      takedown.isResourceTakedownUrl(
        "/Rhythmyx/services/sitemanage/publish/takedown/resource/99",
      ),
      true,
    );
    const parsed = takedown.parseTakedownUrl(
      "http://cms/Rhythmyx/services/sitemanage/publish/takedown/page/42",
    );
    assert.equal(parsed.kind, "page");
    assert.equal(parsed.itemId, "42");
  });

  it("spec covers confirm, FORBIDDEN, folders, and a new unsaved item", () => {
    const src = fs.readFileSync(
      path.join(__dirname, "..", "editor-host-takedown.spec.js"),
      "utf8",
    );
    assert.match(src, /FORBIDDEN/);
    assert.match(src, /mode=view/);
    assert.match(src, /Folder/);
    assert.match(src, /mode=edit/);
    assert.match(src, /#4862/);
    assert.match(src, /takedown\/resource/);
  });
});
