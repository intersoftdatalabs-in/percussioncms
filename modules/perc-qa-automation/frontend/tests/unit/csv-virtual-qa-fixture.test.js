/**
 * Unit tests for CSV Virtual Site QA fixture helper (no live CMS / Docker).
 *
 * Run: npm run test:unit (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const {
  CSV_VIRTUAL_QA_ROOT,
  csvVirtualFixtureHostDir,
  qaCmsContainer,
} = require("../helpers/csv-virtual-qa-fixture");

describe("csv-virtual-qa-fixture", () => {
  it("uses a POSIX in-container root (not an OS path join)", () => {
    assert.equal(CSV_VIRTUAL_QA_ROOT, "/opt/Percussion/tmp/csv-virtual-qa-3697");
    assert.ok(!CSV_VIRTUAL_QA_ROOT.includes("\\"));
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

  it("host fixture tree has required CSV, config, and theme files", () => {
    const dir = csvVirtualFixtureHostDir();
    assert.ok(fs.existsSync(path.join(dir, "8.2", "pages.csv")));
    assert.ok(fs.existsSync(path.join(dir, "_theme", "page.html")));
  });
});
