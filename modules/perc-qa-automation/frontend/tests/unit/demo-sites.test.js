/**
 * Unit tests for demo-sites sample site helpers (no live CMS).
 *
 * Run: npm run test:unit (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  EXPECTED_SAMPLE_SITE_NAMES,
  normalizeSiteName,
  pathItemNames,
  hasAllExpectedSampleSites,
  hasAnyExpectedSampleSite,
  isTruthyEnvFlag,
  shouldEnforceDemoSites,
  demoSitesSkipReason,
} = require("../helpers/demo-sites");

describe("normalizeSiteName", () => {
  it("trims, collapses space, and maps underscores", () => {
    assert.equal(normalizeSiteName("  Corporate_Investments "), "corporate investments");
    assert.equal(normalizeSiteName("Enterprise Investments"), "enterprise investments");
  });

  it("handles empty values", () => {
    assert.equal(normalizeSiteName(null), "");
    assert.equal(normalizeSiteName(""), "");
  });
});

describe("pathItemNames", () => {
  it("reads PathItem wrapper", () => {
    assert.deepEqual(
      pathItemNames({
        PathItem: [
          { name: "Corporate Investments" },
          { name: "Enterprise Investments" },
        ],
      }),
      ["Corporate Investments", "Enterprise Investments"],
    );
  });

  it("reads bare array and skips empties", () => {
    assert.deepEqual(
      pathItemNames([{ name: "A" }, { name: "  " }, {}, { name: "B" }]),
      ["A", "B"],
    );
  });

  it("returns [] for null/empty bodies", () => {
    assert.deepEqual(pathItemNames(null), []);
    assert.deepEqual(pathItemNames({}), []);
    assert.deepEqual(pathItemNames([]), []);
  });
});

describe("hasAllExpectedSampleSites / hasAnyExpectedSampleSite", () => {
  it("matches underscore and spaced names", () => {
    const names = ["Corporate_Investments", "Enterprise Investments"];
    assert.equal(hasAllExpectedSampleSites(names), true);
    assert.equal(hasAnyExpectedSampleSite(names), true);
  });

  it("fails when Sites empty (original #1750 regression shape)", () => {
    assert.equal(hasAllExpectedSampleSites([]), false);
    assert.equal(hasAnyExpectedSampleSite([]), false);
  });

  it("any vs all when only one sample present", () => {
    const names = ["Corporate Investments"];
    assert.equal(hasAnyExpectedSampleSite(names), true);
    assert.equal(hasAllExpectedSampleSites(names), false);
  });
});

describe("shouldEnforceDemoSites", () => {
  it("accepts truthy aliases on both env keys", () => {
    assert.equal(shouldEnforceDemoSites({ EXPECT_DEMO_SITES: "1" }), true);
    assert.equal(shouldEnforceDemoSites({ TEST_EXPECT_DEMO_SITES: "true" }), true);
    assert.equal(shouldEnforceDemoSites({ EXPECT_DEMO_SITES: "yes" }), true);
    assert.equal(shouldEnforceDemoSites({ EXPECT_DEMO_SITES: "0" }), false);
    assert.equal(shouldEnforceDemoSites({}), false);
  });
});

describe("isTruthyEnvFlag", () => {
  it("accepts 1/true/yes/on", () => {
    assert.equal(isTruthyEnvFlag("1"), true);
    assert.equal(isTruthyEnvFlag("TRUE"), true);
    assert.equal(isTruthyEnvFlag("no"), false);
  });
});

describe("demoSitesSkipReason", () => {
  it("embeds durable issue URLs (skip-with-BUG)", () => {
    const reason = demoSitesSkipReason();
    assert.match(reason, /^BUG:/);
    assert.match(reason, /#2192/);
    assert.match(reason, /#1750/);
    assert.match(reason, /#2194/);
    assert.match(
      reason,
      /https:\/\/github\.com\/intersoftdatalabs-in\/percussioncms\/issues\/2192/,
    );
  });
});

describe("EXPECTED_SAMPLE_SITE_NAMES", () => {
  it("lists Corporate and Enterprise Investments", () => {
    assert.ok(EXPECTED_SAMPLE_SITE_NAMES.includes("Corporate Investments"));
    assert.ok(EXPECTED_SAMPLE_SITE_NAMES.includes("Enterprise Investments"));
    assert.equal(EXPECTED_SAMPLE_SITE_NAMES.length, 2);
  });
});
