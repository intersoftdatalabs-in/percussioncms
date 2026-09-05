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
 * Unit tests for Developer File Explorer browse surface helpers (#4327).
 * No live CMS.
 *
 * Run: npm run test:unit  (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  developerFileExplorerUrl,
  developerFileExplorerRestUrl,
  unwrapFileExplorerRoots,
  unexpectedConsoleErrors,
} = require("../helpers/developer-file-explorer-surface");

describe("developerFileExplorerUrl", () => {
  it("builds Developer File Explorer SPA URL with cache buster", () => {
    const url = developerFileExplorerUrl("http://127.0.0.1:9992/");
    assert.match(
      url,
      /^http:\/\/127\.0\.0\.1:9992\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=developer&section=file-explorer&_=\d+$/,
    );
  });

  it("does not invent an OS filesystem path in the SPA query", () => {
    const url = developerFileExplorerUrl("http://127.0.0.1:1");
    assert.equal(url.includes("\\"), false);
    assert.equal(url.includes(".."), false);
  });
});

describe("developerFileExplorerRestUrl", () => {
  it("defaults to Rhythmyx context", () => {
    const prev = process.env.CMS_WEBAPP_CONTEXT;
    delete process.env.CMS_WEBAPP_CONTEXT;
    try {
      assert.equal(
        developerFileExplorerRestUrl("http://127.0.0.1:9992"),
        "http://127.0.0.1:9992/Rhythmyx/services/fileexplorer",
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
        developerFileExplorerRestUrl("http://127.0.0.1:9992/"),
        "http://127.0.0.1:9992/services/fileexplorer",
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

describe("unwrapFileExplorerRoots", () => {
  it("accepts a bare array of catalog ids", () => {
    const roots = unwrapFileExplorerRoots([
      { id: "rx_resources", displayName: "rx_resources", exists: true },
    ]);
    assert.equal(roots.length, 1);
    assert.equal(roots[0].id, "rx_resources");
  });

  it("unwraps Jackson FileExplorerRoot envelope", () => {
    const roots = unwrapFileExplorerRoots({
      FileExplorerRoot: { id: "drop", displayName: "drop" },
    });
    assert.equal(roots.length, 1);
    assert.equal(roots[0].id, "drop");
  });

  it("skips unsafe ids (traversal / empty)", () => {
    assert.deepEqual(unwrapFileExplorerRoots([{ id: "../x" }]), []);
    assert.deepEqual(unwrapFileExplorerRoots([{ id: "" }]), []);
    assert.deepEqual(unwrapFileExplorerRoots(null), []);
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
  it("uses File Explorer panel test ids (not SY-05 app files)", () => {
    assert.equal(TEST_IDS.panel, "developer-fe-panel");
    assert.equal(TEST_IDS.browse, "developer-fe-browse");
    assert.equal(TEST_IDS.tab, "tab-developer-file-explorer");
  });
});
