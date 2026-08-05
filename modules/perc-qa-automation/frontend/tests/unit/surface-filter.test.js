/**
 * Unit tests for Playwright surface-filter arg builder (no live CMS).
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  buildPlaywrightSurfaceArgs,
  formatSurfaceCommand,
  hasSurfaceFilter,
  normalizeTag,
  optionsFromEnv,
  splitPathList,
} = require("../helpers/surface-filter");

describe("normalizeTag", () => {
  it("adds leading @ when missing", () => {
    assert.equal(normalizeTag("smoke"), "@smoke");
  });
  it("preserves leading @", () => {
    assert.equal(normalizeTag("@login"), "@login");
  });
  it("trims whitespace", () => {
    assert.equal(normalizeTag("  explorer  "), "@explorer");
  });
  it("returns empty for blank", () => {
    assert.equal(normalizeTag(""), "");
    assert.equal(normalizeTag("   "), "");
  });
});

describe("splitPathList", () => {
  it("splits comma and semicolon", () => {
    assert.deepEqual(splitPathList("a.spec.js, b.spec.js;c.spec.js"), [
      "a.spec.js",
      "b.spec.js",
      "c.spec.js",
    ]);
  });
  it("accepts arrays", () => {
    assert.deepEqual(splitPathList(["x", " y "]), ["x", "y"]);
  });
  it("handles empty", () => {
    assert.deepEqual(splitPathList(""), []);
    assert.deepEqual(splitPathList(null), []);
  });
});

describe("optionsFromEnv", () => {
  it("reads SURFACE_PATH and SURFACE_GREP", () => {
    const o = optionsFromEnv({
      SURFACE_PATH: "tests/login.spec.js",
      SURFACE_GREP: "Admin",
      SURFACE_TAG: "smoke",
    });
    assert.deepEqual(o.paths, ["tests/login.spec.js"]);
    assert.equal(o.grep, "Admin");
    assert.equal(o.tag, "smoke");
  });
  it("merges SURFACE_PATHS", () => {
    const o = optionsFromEnv({
      SURFACE_PATH: "tests/login.spec.js",
      SURFACE_PATHS: "tests/bugs/bug-1.spec.js,tests/us1-core-explorer.spec.js",
    });
    assert.deepEqual(o.paths, [
      "tests/login.spec.js",
      "tests/bugs/bug-1.spec.js",
      "tests/us1-core-explorer.spec.js",
    ]);
  });
});

describe("buildPlaywrightSurfaceArgs", () => {
  it("emits path only", () => {
    assert.deepEqual(
      buildPlaywrightSurfaceArgs({ path: "tests/login.spec.js" }),
      ["tests/login.spec.js"],
    );
  });
  it("emits multiple paths", () => {
    assert.deepEqual(
      buildPlaywrightSurfaceArgs({
        paths: ["tests/login.spec.js", "tests/logout.spec.js"],
      }),
      ["tests/login.spec.js", "tests/logout.spec.js"],
    );
  });
  it("emits --grep for title filter", () => {
    assert.deepEqual(buildPlaywrightSurfaceArgs({ grep: "Admin login" }), [
      "--grep",
      "Admin login",
    ]);
  });
  it("emits --grep @tag for tag convenience", () => {
    assert.deepEqual(buildPlaywrightSurfaceArgs({ tag: "smoke" }), [
      "--grep",
      "@smoke",
    ]);
  });
  it("combines path + grep + list", () => {
    assert.deepEqual(
      buildPlaywrightSurfaceArgs({
        path: "tests/login.spec.js",
        grep: "Admin",
        list: true,
      }),
      ["tests/login.spec.js", "--grep", "Admin", "--list"],
    );
  });
  it("emits --grep-invert", () => {
    assert.deepEqual(
      buildPlaywrightSurfaceArgs({
        path: "tests/",
        grepInvert: "edge-cases",
      }),
      ["tests/", "--grep-invert", "edge-cases"],
    );
  });
  it("combines grep and tag as alternation", () => {
    const args = buildPlaywrightSurfaceArgs({
      grep: "login",
      tag: "smoke",
    });
    assert.equal(args[0], "--grep");
    assert.match(args[1], /login/);
    assert.match(args[1], /@smoke/);
  });
  it("appends extraArgs", () => {
    assert.deepEqual(
      buildPlaywrightSurfaceArgs({
        path: "tests/login.spec.js",
        extraArgs: ["--project=chromium", "--reporter=list"],
      }),
      ["tests/login.spec.js", "--project=chromium", "--reporter=list"],
    );
  });
  it("returns empty when no filters", () => {
    assert.deepEqual(buildPlaywrightSurfaceArgs({}), []);
  });
});

describe("hasSurfaceFilter", () => {
  it("true for path", () => {
    assert.equal(hasSurfaceFilter({ path: "tests/x.spec.js" }), true);
  });
  it("true for grep", () => {
    assert.equal(hasSurfaceFilter({ grep: "foo" }), true);
  });
  it("true for tag", () => {
    assert.equal(hasSurfaceFilter({ tag: "smoke" }), true);
  });
  it("false for list-only or empty", () => {
    assert.equal(hasSurfaceFilter({ list: true }), false);
    assert.equal(hasSurfaceFilter({}), false);
  });
});

describe("formatSurfaceCommand", () => {
  it("formats printable command", () => {
    const cmd = formatSurfaceCommand({
      path: "tests/login.spec.js",
      list: true,
    });
    assert.equal(cmd, "npx playwright test tests/login.spec.js --list");
  });
  it("quotes args with spaces", () => {
    const cmd = formatSurfaceCommand({ grep: "Admin login" });
    assert.equal(cmd, 'npx playwright test --grep "Admin login"');
  });
});
