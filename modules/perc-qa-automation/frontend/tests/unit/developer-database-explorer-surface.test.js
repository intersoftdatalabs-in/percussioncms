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
 * Unit tests for Developer Database Explorer browse surface helpers (#4343).
 * No live CMS.
 *
 * Run: npm run test:unit  (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  developerDatabaseExplorerUrl,
  developerDatabaseExplorerRestUrl,
  unwrapDatabaseExplorerDatasources,
  unexpectedConsoleErrors,
} = require("../helpers/developer-database-explorer-surface");

describe("developerDatabaseExplorerUrl", () => {
  it("builds Developer Database Explorer SPA URL with cache buster", () => {
    const url = developerDatabaseExplorerUrl("http://127.0.0.1:9992/");
    assert.match(
      url,
      /^http:\/\/127\.0\.0\.1:9992\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=developer&section=database-explorer&_=\d+$/,
    );
  });

  it("does not invent a JDBC URL in the SPA query", () => {
    const url = developerDatabaseExplorerUrl("http://127.0.0.1:1");
    assert.equal(url.includes("jdbc:"), false);
    assert.equal(url.includes(".."), false);
  });
});

describe("developerDatabaseExplorerRestUrl", () => {
  it("defaults to Rhythmyx context", () => {
    const prev = process.env.CMS_WEBAPP_CONTEXT;
    delete process.env.CMS_WEBAPP_CONTEXT;
    try {
      assert.equal(
        developerDatabaseExplorerRestUrl("http://127.0.0.1:9992"),
        "http://127.0.0.1:9992/Rhythmyx/services/databaseexplorer",
      );
    } finally {
      if (prev === undefined) {
        delete process.env.CMS_WEBAPP_CONTEXT;
      } else {
        process.env.CMS_WEBAPP_CONTEXT = prev;
      }
    }
  });

  it("honors empty CMS_WEBAPP_CONTEXT as root context", () => {
    const prev = process.env.CMS_WEBAPP_CONTEXT;
    process.env.CMS_WEBAPP_CONTEXT = "";
    try {
      assert.equal(
        developerDatabaseExplorerRestUrl("http://127.0.0.1:9992/"),
        "http://127.0.0.1:9992/services/databaseexplorer",
      );
    } finally {
      if (prev === undefined) {
        delete process.env.CMS_WEBAPP_CONTEXT;
      } else {
        process.env.CMS_WEBAPP_CONTEXT = prev;
      }
    }
  });
});

describe("unwrapDatabaseExplorerDatasources", () => {
  it("accepts a bare array of catalog ids", () => {
    const list = unwrapDatabaseExplorerDatasources([
      { id: "cms", displayName: "cms", repository: true },
    ]);
    assert.equal(list.length, 1);
    assert.equal(list[0].id, "cms");
  });

  it("unwraps Jackson DatabaseExplorerDatasource envelope", () => {
    const list = unwrapDatabaseExplorerDatasources({
      DatabaseExplorerDatasource: { id: "cms", displayName: "cms" },
    });
    assert.equal(list.length, 1);
    assert.equal(list[0].id, "cms");
  });

  it("skips unsafe ids (traversal / empty)", () => {
    assert.deepEqual(unwrapDatabaseExplorerDatasources([{ id: "../x" }]), []);
    assert.deepEqual(unwrapDatabaseExplorerDatasources([{ id: "" }]), []);
    assert.deepEqual(unwrapDatabaseExplorerDatasources(null), []);
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
  it("uses Database Explorer panel test ids (not File Explorer)", () => {
    assert.equal(TEST_IDS.panel, "developer-dbx-panel");
    assert.equal(TEST_IDS.browse, "developer-dbx-browse");
    assert.equal(TEST_IDS.tab, "tab-developer-database-explorer");
  });
});
