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
 * Unit tests for SQL Virtual Site QA fixture helper (no live CMS / Docker).
 *
 * Run: npm run test:unit (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const {
  SQL_VIRTUAL_QA_ROOT,
  SQL_VIRTUAL_PUBLISHED_HTML,
  SQL_VIRTUAL_PUBLISH_MARKER,
  sqlVirtualFixtureHostDir,
  qaCmsContainer,
  normalizeQaPublishDestPath,
  posixJoin,
} = require("../helpers/sql-virtual-qa-fixture");

describe("sql-virtual-qa-fixture", () => {
  it("uses a POSIX in-container root (not an OS path join)", () => {
    assert.equal(SQL_VIRTUAL_QA_ROOT, "/opt/Percussion/tmp/sql-virtual-qa");
    assert.ok(!SQL_VIRTUAL_QA_ROOT.includes("\\"));
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

  it("host fixture has required _config.yaml sql mapping and theme", () => {
    const dir = sqlVirtualFixtureHostDir();
    const config = fs.readFileSync(path.join(dir, "_config.yaml"), "utf8");
    assert.match(config, /^sql:/m);
    assert.match(config, /jdbc:h2:mem:vsql_qa3759/);
    assert.match(config, /SELECT/i);
    assert.doesNotMatch(config, /INIT\s*=/i);
    assert.doesNotMatch(config, /RUNSCRIPT/i);
    assert.doesNotMatch(config, /jdbc:oracle:/i);
    assert.ok(fs.existsSync(path.join(dir, "_theme", "page.html")));
  });

  it("normalizeQaPublishDestPath accepts Linux cell abs paths and rejects traversal", () => {
    assert.equal(
      normalizeQaPublishDestPath(" /opt/Percussion/fastforward/CI_Home "),
      "/opt/Percussion/fastforward/CI_Home",
    );
    assert.equal(SQL_VIRTUAL_PUBLISHED_HTML, "8.2/index.html");
    assert.equal(SQL_VIRTUAL_PUBLISH_MARKER, "Hello from SQL.");
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
      posixJoin("/opt/Percussion/pub", SQL_VIRTUAL_PUBLISHED_HTML),
      "/opt/Percussion/pub/8.2/index.html",
    );
    assert.throws(() => posixJoin("/opt/Percussion/pub", "../etc/passwd"), /unsafe/);
  });
});
