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
  sqlVirtualFixtureHostDir,
  qaCmsContainer,
} = require("../helpers/sql-virtual-qa-fixture");

describe("sql-virtual-qa-fixture", () => {
  it("uses a POSIX in-container root (not an OS path join)", () => {
    assert.equal(SQL_VIRTUAL_QA_ROOT, "/opt/Percussion/tmp/sql-virtual-qa-3759");
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
});
