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
 * Unit tests for architecture-landing-drop helpers (#3660) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  FINDER_FOLDER_MIME,
  FINDER_PAGE_MIME,
  serializeFinderItemDrag,
} = require("../helpers/architecture-landing-drop");

describe("architecture-landing-drop helpers (#3660)", () => {
  it("serializes Finder PAGE JSON for drop mapping", () => {
    const raw = serializeFinderItemDrag({
      id: "page-1",
      name: "About",
      path: "//Sites/Demo/About",
      type: "page",
      category: "PAGE",
    });
    assert.equal(FINDER_PAGE_MIME, "application/x-percussion-finder-page");
    assert.equal(FINDER_FOLDER_MIME, "application/x-percussion-finder-folder");
    assert.deepEqual(JSON.parse(raw), {
      id: "page-1",
      name: "About",
      path: "//Sites/Demo/About",
      type: "page",
      category: "PAGE",
    });
  });
});
