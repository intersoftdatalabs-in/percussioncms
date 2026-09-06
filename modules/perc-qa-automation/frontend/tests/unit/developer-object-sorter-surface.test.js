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
 * Unit tests for Developer Object Sorter surface helpers (#4344).
 * No live CMS.
 *
 * Run: npm run test:unit  (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  developerObjectSorterUrl,
  unexpectedConsoleErrors,
} = require("../helpers/developer-object-sorter-surface");

describe("developerObjectSorterUrl", () => {
  it("builds Developer Object Sorter SPA URL with cache buster", () => {
    const url = developerObjectSorterUrl("http://127.0.0.1:9992/");
    assert.match(
      url,
      /^http:\/\/127\.0\.0\.1:9992\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=developer&section=object-sorter&_=\d+$/,
    );
  });

  it("does not invent a filesystem path in the SPA query", () => {
    const url = developerObjectSorterUrl("http://127.0.0.1:1");
    assert.equal(url.includes(".."), false);
    assert.equal(url.includes("C:"), false);
  });
});

describe("unexpectedConsoleErrors", () => {
  it("ignores favicon and failed-resource noise", () => {
    assert.deepEqual(
      unexpectedConsoleErrors([
        "Failed to load resource: the server responded with a status of 404",
        "favicon.ico",
        "TypeError: boom",
      ]),
      ["TypeError: boom"],
    );
  });
});

describe("TEST_IDS", () => {
  it("uses Object Sorter panel test ids", () => {
    assert.equal(TEST_IDS.panel, "developer-os-panel");
    assert.equal(TEST_IDS.table, "developer-os-table");
    assert.equal(TEST_IDS.tab, "tab-developer-object-sorter");
  });
});
