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
 * Unit tests for Virtual Site source-kind option contract (no live CMS).
 *
 * Run: npm run test:unit (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  requiredVirtualSourceKindValues,
  missingVirtualSourceKindValues,
  formatMissingVirtualSourceKindMessage,
} = require("../helpers/virtual-source-kind-options");

describe("virtual-source-kind-options", () => {
  it("requires object-storage, rss-atom, and icalendar among the live kind options (#3893 / #3927 / #3983)", () => {
    const required = requiredVirtualSourceKindValues();
    assert.deepEqual(required, [
      "repository",
      "git-filesystem",
      "csv-filesystem",
      "sql-database",
      "http-json",
      "object-storage",
      "rss-atom",
      "icalendar",
    ]);
    assert.equal(required.includes("object-storage"), true);
    assert.equal(required.includes("rss-atom"), true);
    assert.equal(required.includes("icalendar"), true);
    assert.equal(required.includes("sql-api"), false);
  });

  it("reports object-storage missing when the stale SPA omits it", () => {
    const live = [
      "repository",
      "git-filesystem",
      "csv-filesystem",
      "sql-database",
      "http-json",
      "rss-atom",
      "icalendar",
    ];
    const missing = missingVirtualSourceKindValues(live);
    assert.deepEqual(missing, ["object-storage"]);
    const msg = formatMissingVirtualSourceKindMessage(missing, live);
    assert.match(msg, /object-storage/);
    assert.match(msg, /qa-deploy-webui/);
    assert.match(msg, /perc-modern-ui\.js/);
  });

  it("is empty when the live select includes every required kind", () => {
    const live = requiredVirtualSourceKindValues();
    assert.deepEqual(missingVirtualSourceKindValues(live), []);
  });
});
