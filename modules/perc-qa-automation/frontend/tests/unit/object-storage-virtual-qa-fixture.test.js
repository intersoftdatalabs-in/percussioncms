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
 * Unit tests for object-storage Virtual Site QA fixture helper (no live CMS / Docker).
 *
 * Run: npm run test:unit (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const {
  OBJECT_STORAGE_VIRTUAL_QA_ROOT,
  OBJECT_STORAGE_VIRTUAL_PUBLISHED_HTML,
  OBJECT_STORAGE_VIRTUAL_PUBLISH_MARKER,
  objectStorageVirtualFixtureHostDir,
  qaCmsContainer,
  normalizeQaPublishDestPath,
  posixJoin,
} = require("../helpers/object-storage-virtual-qa-fixture");

describe("object-storage-virtual-qa-fixture", () => {
  it("uses a POSIX in-container root (not an OS path join)", () => {
    assert.equal(OBJECT_STORAGE_VIRTUAL_QA_ROOT, "/opt/Percussion/tmp/object-storage-virtual-qa");
    assert.ok(!OBJECT_STORAGE_VIRTUAL_QA_ROOT.includes("\\"));
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

  it("host fixture has required _config.yaml, object-key Markdown, and theme (no secrets)", () => {
    const dir = objectStorageVirtualFixtureHostDir();
    const config = fs.readFileSync(path.join(dir, "_config.yaml"), "utf8");
    assert.match(config, /^site:/m);
    assert.doesNotMatch(config, /authorization/i);
    assert.doesNotMatch(config, /access[_-]?key/i);
    assert.doesNotMatch(config, /s3:\/\//i);
    const index = fs.readFileSync(path.join(dir, "8.2", "index.md"), "utf8");
    assert.match(index, /Object Home/);
    assert.match(index, /Hello from objects/);
    assert.ok(fs.existsSync(path.join(dir, "_theme", "page.html")));
  });

  it("normalizeQaPublishDestPath accepts Linux cell abs paths and rejects traversal", () => {
    assert.equal(
      normalizeQaPublishDestPath(" /opt/Percussion/fastforward/CI_Home "),
      "/opt/Percussion/fastforward/CI_Home",
    );
    assert.equal(OBJECT_STORAGE_VIRTUAL_PUBLISHED_HTML, "8.2/index.html");
    assert.equal(OBJECT_STORAGE_VIRTUAL_PUBLISH_MARKER, "Hello from objects.");
    assert.throws(() => normalizeQaPublishDestPath(""), /blank/);
    assert.throws(() => normalizeQaPublishDestPath("tmp/out"), /not absolute/);
    assert.throws(() => normalizeQaPublishDestPath("C:/inetpub/wwwroot"), /Linux QA cell/);
    assert.throws(() => normalizeQaPublishDestPath("/opt/../etc"), /unsafe/);
    assert.throws(() => normalizeQaPublishDestPath("/opt/Percussion/tmp/foo/.."), /unsafe/);
  });

  it("posixJoin appends published HTML with forward slashes only", () => {
    assert.equal(
      posixJoin("/opt/Percussion/pub", "8.2", "index.html"),
      "/opt/Percussion/pub/8.2/index.html",
    );
    assert.equal(
      posixJoin("/opt/Percussion/pub", OBJECT_STORAGE_VIRTUAL_PUBLISHED_HTML),
      "/opt/Percussion/pub/8.2/index.html",
    );
    assert.throws(() => posixJoin("/opt/Percussion/pub", "../etc/passwd"), /unsafe/);
  });
});
