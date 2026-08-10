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
 * Unit tests for explorer-site-copy helpers (#2767) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  explorerSpaUrl,
  explorerSpaUrlWithPath,
} = require("../helpers/explorer-site-copy");

describe("explorer-site-copy helpers (#2767)", () => {
  it("exports stable product test ids", () => {
    assert.equal(TEST_IDS.shell, "content-explorer-shell");
    assert.equal(TEST_IDS.siteCopyMenu, "explorer-content-site-copy");
    assert.equal(TEST_IDS.wizard, "site-copy-wizard");
    assert.equal(TEST_IDS.siteCopyPanel, "explorer-site-copy-panel");
  });

  it("builds explorer SPA entry URL with cache buster", () => {
    const url = explorerSpaUrl("http://127.0.0.1:9992/");
    assert.match(url, /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+/);
    assert.ok(!url.includes("9992//"));
  });

  it("builds deep-link path query when site path provided", () => {
    const url = explorerSpaUrlWithPath(
      "http://127.0.0.1:9992",
      "/Sites/Demo",
    );
    assert.match(url, /entry=explorer&path=%2FSites%2FDemo&_=\d+/);
  });
});
