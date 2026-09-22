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
  editorSpaUrl,
  htmlLooksUnsafe,
  isItemFieldsPutUrl,
} = require("../helpers/editor-host-html-fields");

describe("editor-host-html-fields helpers", () => {
  it("builds editor SPA URL with entry=editor", () => {
    const url = editorSpaUrl("http://localhost:9993", "contentId=42&mode=edit");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?/);
    assert.match(url, /entry=editor/);
    assert.match(url, /contentId=42/);
  });

  it("recognizes item fields PUT URLs", () => {
    assert.equal(
      isItemFieldsPutUrl(
        "http://localhost:9993/Rhythmyx/services/itemmanagement/item/fields/42",
      ),
      true,
    );
    assert.equal(isItemFieldsPutUrl("/cm/app/editor"), false);
  });

  it("flags unsafe HTML", () => {
    assert.equal(htmlLooksUnsafe("<p>ok</p>"), false);
    assert.equal(htmlLooksUnsafe("<script>x</script>"), true);
  });
});
