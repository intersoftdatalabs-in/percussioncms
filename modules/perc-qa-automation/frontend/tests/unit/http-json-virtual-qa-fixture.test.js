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
 * Unit tests for HTTP JSON Virtual Site QA fixture helper (no live CMS / Docker).
 *
 * Run: npm run test:unit (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const {
  HTTP_JSON_VIRTUAL_QA_ROOT,
  httpJsonVirtualFixtureHostDir,
  qaCmsContainer,
} = require("../helpers/http-json-virtual-qa-fixture");

describe("http-json-virtual-qa-fixture", () => {
  it("uses a POSIX in-container root (not an OS path join)", () => {
    assert.equal(HTTP_JSON_VIRTUAL_QA_ROOT, "/opt/Percussion/tmp/http-json-virtual-qa");
    assert.ok(!HTTP_JSON_VIRTUAL_QA_ROOT.includes("\\"));
  });

  it("defaults the QA container name and honors QA_CMS_CONTAINER", () => {
    const prevQa = process.env.QA_CMS_CONTAINER;
    const prevPerc = process.env.PERC_QA_CMS_CONTAINER;
    try {
      delete process.env.QA_CMS_CONTAINER;
      delete process.env.PERC_QA_CMS_CONTAINER;
      assert.equal(qaCmsContainer(), "perc-matrix-cms-h2");
      process.env.QA_CMS_CONTAINER = "  perc-matrix-cms-h2-custom  ";
      assert.equal(qaCmsContainer(), "perc-matrix-cms-h2-custom");
    } finally {
      if (prevQa === undefined) {
        delete process.env.QA_CMS_CONTAINER;
      } else {
        process.env.QA_CMS_CONTAINER = prevQa;
      }
      if (prevPerc === undefined) {
        delete process.env.PERC_QA_CMS_CONTAINER;
      } else {
        process.env.PERC_QA_CMS_CONTAINER = prevPerc;
      }
    }
  });

  it("host fixture has required _config.yaml http.file, pages.json, and theme", () => {
    const dir = httpJsonVirtualFixtureHostDir();
    const config = fs.readFileSync(path.join(dir, "_config.yaml"), "utf8");
    assert.match(config, /^http:/m);
    assert.match(config, /file:\s*pages\.json/);
    assert.doesNotMatch(config, /authorization/i);
    assert.doesNotMatch(config, /api[_-]?key/i);
    const catalog = fs.readFileSync(path.join(dir, "pages.json"), "utf8");
    const parsed = JSON.parse(catalog);
    assert.ok(Array.isArray(parsed.pages));
    assert.equal(parsed.pages[0].id, "home");
    assert.match(parsed.pages[0].body, /Hello from JSON/);
    assert.ok(fs.existsSync(path.join(dir, "_theme", "page.html")));
  });
});
