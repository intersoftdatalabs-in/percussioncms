/**
 * Unit tests for Developer smoke inventory (#2188).
 * Ensures skip-with-BUG entries always carry durable issue URLs.
 *
 * Run: npm run test:unit  (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  DEVELOPER_SMOKE_SET,
  REPO_ISSUES,
  skipReasonFor,
  getSmokeEntry,
  listSkipEntries,
  listGreenEntries,
} = require("../helpers/developer-smoke-set");

describe("DEVELOPER_SMOKE_SET", () => {
  it("lists green + skip entries covering critical catalogs and entry paths", () => {
    assert.ok(DEVELOPER_SMOKE_SET.length >= 10);
    const ids = new Set(DEVELOPER_SMOKE_SET.map((e) => e.id));
    assert.ok(ids.has("rest-slots"));
    assert.ok(ids.has("catalog-content-types"));
    assert.ok(ids.has("catalog-keywords"));
    assert.ok(ids.has("template-source-viewer"));
    assert.ok(ids.has("server-configs-write"));
    assert.ok(ids.has("golden-login-explorer"));
    assert.ok(ids.has("login-admin"));
    assert.ok(ids.has("application-files-write"));
  });

  it("requires unique ids", () => {
    const ids = DEVELOPER_SMOKE_SET.map((e) => e.id);
    assert.equal(ids.length, new Set(ids).size);
  });

  it("skip entries always have durable BUG issue URL (no silent flakes)", () => {
    for (const entry of listSkipEntries()) {
      assert.equal(entry.status, "skip");
      assert.ok(
        Number.isInteger(entry.bugIssue) && entry.bugIssue > 0,
        `${entry.id} missing bugIssue`,
      );
      assert.match(
        entry.bugUrl,
        new RegExp(`^${REPO_ISSUES.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}/\\d+$`),
        `${entry.id} bugUrl must be durable issue URL`,
      );
      assert.ok(
        entry.bugUrl.endsWith(`/${entry.bugIssue}`),
        `${entry.id} bugUrl must match bugIssue`,
      );
    }
  });

  it("green entries do not carry skip-only fields as required", () => {
    for (const entry of listGreenEntries()) {
      assert.equal(entry.status, "green");
      assert.ok(entry.file.endsWith(".spec.js"));
      assert.ok(entry.title.length > 0);
      // Smoke-gate contract: green must not retain stale skip metadata
      assert.equal(
        entry.bugIssue,
        undefined,
        `${entry.id} green entry must not carry bugIssue`,
      );
      assert.equal(
        entry.bugUrl,
        undefined,
        `${entry.id} green entry must not carry bugUrl`,
      );
    }
  });

  it("content-types residual points at selector harden #2186", () => {
    const ct = getSmokeEntry("catalog-content-types");
    assert.equal(ct.status, "skip");
    assert.equal(ct.bugIssue, 2186);
  });

  it("template source viewer residual points at product #2189", () => {
    const tpl = getSmokeEntry("template-source-viewer");
    assert.equal(tpl.status, "skip");
    assert.equal(tpl.bugIssue, 2189);
  });
});

describe("skipReasonFor", () => {
  it("embeds issue URL for Playwright test.skip messages", () => {
    const reason = skipReasonFor(getSmokeEntry("catalog-content-types"));
    assert.match(reason, /^BUG:/);
    assert.match(reason, /#2186/);
    assert.match(reason, /issues\/2186/);
  });

  it("rejects green entries", () => {
    assert.throws(
      () => skipReasonFor(getSmokeEntry("rest-slots")),
      TypeError,
    );
  });
});
