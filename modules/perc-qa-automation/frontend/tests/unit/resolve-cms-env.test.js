/**
 * Unit tests for CMS URL / credential resolution (#2064).
 * No live CMS, no host install required.
 *
 * Run: npm run test:unit  (from frontend/)
 *   or: node --test tests/unit/resolve-cms-env.test.js
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  resolveCmsBaseUrl,
  resolveRolePassword,
  hasQaModeUrlEnv,
  DEV_FALLBACK_URL,
  QA_PREFERRED_FALLBACK_URL,
  CMS_URL_ENV_KEYS,
} = require("../helpers/resolve-cms-env");

describe("resolveCmsBaseUrl", () => {
  it("prefers TEST_CMS_URL over all other sources", () => {
    const r = resolveCmsBaseUrl({
      TEST_CMS_URL: "http://127.0.0.1:12001",
      CMS_HOST_PORT: "9993",
      DEV_PERCUSSION_URL: "http://localhost:9992",
    });
    assert.equal(r.url, "http://127.0.0.1:12001");
    assert.equal(r.source, "TEST_CMS_URL");
  });

  it("accepts documented aliases CMS_BASE_URL and QA_CMS_URL", () => {
    assert.equal(
      resolveCmsBaseUrl({ CMS_BASE_URL: "http://127.0.0.1:1" }).url,
      "http://127.0.0.1:1",
    );
    assert.equal(
      resolveCmsBaseUrl({ QA_CMS_URL: "http://127.0.0.1:2/" }).url,
      "http://127.0.0.1:2",
    );
  });

  it("strips trailing slashes from env URLs", () => {
    const r = resolveCmsBaseUrl({
      TEST_CMS_URL: "http://127.0.0.1:9993/",
    });
    assert.equal(r.url, "http://127.0.0.1:9993");
  });

  it("constructs URL from QA_CMS_HOST_PORT when TEST_CMS_URL is unset", () => {
    const r = resolveCmsBaseUrl({ QA_CMS_HOST_PORT: "12055" });
    assert.equal(r.url, "http://127.0.0.1:12055");
    assert.equal(r.source, "CMS_HOST_PORT");
  });

  it("constructs URL from CMS_HOST_PORT (matrix freeport) after QA_CMS_HOST_PORT", () => {
    const r = resolveCmsBaseUrl({ CMS_HOST_PORT: "13001" });
    assert.equal(r.url, "http://127.0.0.1:13001");
    assert.equal(r.source, "CMS_HOST_PORT");
  });

  it("prefers QA_CMS_HOST_PORT over CMS_HOST_PORT", () => {
    const r = resolveCmsBaseUrl({
      QA_CMS_HOST_PORT: "1111",
      CMS_HOST_PORT: "2222",
    });
    assert.equal(r.url, "http://127.0.0.1:1111");
  });

  it("uses DEV_PERCUSSION_URL when no QA env is set", () => {
    const r = resolveCmsBaseUrl({
      DEV_PERCUSSION_URL: "http://localhost:9992",
    });
    assert.equal(r.url, "http://localhost:9992");
    assert.equal(r.source, "DEV_PERCUSSION_URL");
  });

  it("uses installUrl when no env URLs are set", () => {
    const r = resolveCmsBaseUrl({}, { installUrl: "http://localhost:8888" });
    assert.equal(r.url, "http://localhost:8888");
    assert.equal(r.source, "install");
  });

  it("falls back to documented dev default without install or QA env", () => {
    const r = resolveCmsBaseUrl({});
    assert.equal(r.url, DEV_FALLBACK_URL);
    assert.equal(r.source, "fallback");
  });

  it("allows explicit fallbackUrl override (e.g. QA preferred pin)", () => {
    const r = resolveCmsBaseUrl({}, { fallbackUrl: QA_PREFERRED_FALLBACK_URL });
    assert.equal(r.url, QA_PREFERRED_FALLBACK_URL);
    assert.equal(r.source, "fallback");
  });

  it("does not treat blank TEST_CMS_URL as set", () => {
    const r = resolveCmsBaseUrl({
      TEST_CMS_URL: "   ",
      DEV_PERCUSSION_URL: "http://localhost:9992",
    });
    assert.equal(r.source, "DEV_PERCUSSION_URL");
  });

  it("rejects non-numeric host port values", () => {
    const r = resolveCmsBaseUrl({
      CMS_HOST_PORT: "not-a-port",
      DEV_PERCUSSION_URL: "http://localhost:9992",
    });
    assert.equal(r.source, "DEV_PERCUSSION_URL");
  });

  it("rejects invalid host port and falls back when DEV_PERCUSSION_URL is absent", () => {
    const r = resolveCmsBaseUrl({ CMS_HOST_PORT: "not-a-port" });
    assert.equal(r.source, "fallback");
    assert.equal(r.url, DEV_FALLBACK_URL);
  });

  it("rejects host ports outside the valid TCP range (1-65535)", () => {
    const tooHigh = resolveCmsBaseUrl({
      CMS_HOST_PORT: "99999",
      DEV_PERCUSSION_URL: "http://localhost:9992",
    });
    assert.equal(tooHigh.source, "DEV_PERCUSSION_URL");
    const zeroPad = resolveCmsBaseUrl({ CMS_HOST_PORT: "00000" });
    // "00000" parses as 0 → out of range → treat as unset → fallback
    assert.equal(zeroPad.source, "fallback");
  });

  it("documents TEST_CMS_URL as the primary CMS_URL_ENV_KEYS entry", () => {
    assert.equal(CMS_URL_ENV_KEYS[0], "TEST_CMS_URL");
  });
});

describe("resolveRolePassword", () => {
  it("prefers ADMIN_PASSWORD env over install map", () => {
    const r = resolveRolePassword(
      "Admin",
      { ADMIN_PASSWORD: "from-env" },
      { Admin: "from-install" },
    );
    assert.equal(r.password, "from-env");
    assert.equal(r.source, "ADMIN_PASSWORD");
  });

  it("uses install map when env is missing", () => {
    const r = resolveRolePassword("Admin", {}, { Admin: "install-pw" });
    assert.equal(r.password, "install-pw");
    assert.equal(r.source, "install");
  });

  it("returns null/missing when neither env nor install provides a password", () => {
    const r = resolveRolePassword("Admin", {}, {});
    assert.equal(r.password, null);
    assert.equal(r.source, "missing");
  });

  it("resolves Editor and Contributor env keys", () => {
    assert.equal(
      resolveRolePassword("Editor", { EDITOR_PASSWORD: "e" }).password,
      "e",
    );
    assert.equal(
      resolveRolePassword("Contributor", {
        CONTRIBUTOR_PASSWORD: "c",
      }).password,
      "c",
    );
  });
});

describe("hasQaModeUrlEnv", () => {
  it("is true when TEST_CMS_URL is set", () => {
    assert.equal(hasQaModeUrlEnv({ TEST_CMS_URL: "http://127.0.0.1:1" }), true);
  });

  it("is true when CMS_HOST_PORT is set", () => {
    assert.equal(hasQaModeUrlEnv({ CMS_HOST_PORT: "9993" }), true);
  });

  it("is false for empty env or dev-only URL", () => {
    assert.equal(hasQaModeUrlEnv({}), false);
    assert.equal(
      hasQaModeUrlEnv({ DEV_PERCUSSION_URL: "http://localhost:9992" }),
      false,
    );
  });
});

describe("QA mode without DEV_PERCUSSION_INSTALL", () => {
  it("resolves a freeport URL with only TEST_CMS_URL + ADMIN_PASSWORD", () => {
    const env = {
      TEST_CMS_URL: "http://127.0.0.1:14002",
      ADMIN_USERNAME: "Admin",
      ADMIN_PASSWORD: "qa-secret-not-committed",
    };
    const url = resolveCmsBaseUrl(env, { installUrl: null });
    const pw = resolveRolePassword("Admin", env, {});
    assert.equal(url.url, "http://127.0.0.1:14002");
    assert.equal(url.source, "TEST_CMS_URL");
    assert.equal(pw.password, "qa-secret-not-committed");
    assert.equal(hasQaModeUrlEnv(env), true);
  });
});
