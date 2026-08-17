/**
 * Unit tests for login surface classification (#3492).
 * No live CMS, no host install required.
 *
 * Run: npm run test:unit  (from frontend/)
 *   or: node --test tests/unit/login-surface.test.js
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  isOffLoginPath,
  classifyLoginSurface,
  loginUiWaitSelector,
} = require("../helpers/login-surface");

describe("isOffLoginPath", () => {
  it("treats /Rhythmyx/login and /login as still on the login page", () => {
    assert.equal(isOffLoginPath("/Rhythmyx/login"), false);
    assert.equal(isOffLoginPath("/Rhythmyx/login/"), false);
    assert.equal(isOffLoginPath("/login"), false);
    assert.equal(isOffLoginPath("http://127.0.0.1:12001/Rhythmyx/login"), false);
    assert.equal(
      isOffLoginPath("http://127.0.0.1:12001/Rhythmyx/login?return=/cm/app/"),
      false,
    );
  });

  it("treats spa, assembly, and index as off the login path", () => {
    assert.equal(
      isOffLoginPath("/Rhythmyx/cm/app/spa.jsp?entry=assembly"),
      true,
    );
    assert.equal(isOffLoginPath("/Rhythmyx/index.jsp"), true);
    assert.equal(isOffLoginPath("http://127.0.0.1:1/Rhythmyx/cm/app/"), true);
  });

  it("does not treat empty as off-login (still waiting)", () => {
    assert.equal(isOffLoginPath(""), false);
    assert.equal(isOffLoginPath(null), false);
  });
});

describe("classifyLoginSurface", () => {
  it("does not treat a hidden perc-login-root as the login UI", () => {
    const d = classifyLoginSurface({
      pathname: "/Rhythmyx/login",
      formVisible: false,
      legacyVisible: false,
      spaVisible: false,
      assemblyVisible: false,
      rootPresent: true,
      rootVisible: false,
    });
    assert.equal(d.kind, "pending");
    assert.equal(d.reason, "hidden-perc-login-root");
    assert.notEqual(d.kind, "modern_form");
  });

  it("prefers a visible modern form even when the root is hidden", () => {
    const d = classifyLoginSurface({
      pathname: "/Rhythmyx/login",
      formVisible: true,
      rootPresent: true,
      rootVisible: false,
    });
    assert.equal(d.kind, "modern_form");
    assert.equal(d.reason, "perc-login-form-visible");
  });

  it("uses the visible legacy j_username field", () => {
    const d = classifyLoginSurface({
      pathname: "/Rhythmyx/login",
      formVisible: false,
      legacyVisible: true,
      rootPresent: true,
      rootVisible: false,
    });
    assert.equal(d.kind, "legacy_form");
  });

  it("treats a visible perc-spa-app as already authenticated", () => {
    const d = classifyLoginSurface({
      pathname: "/Rhythmyx/cm/app/spa.jsp",
      formVisible: false,
      spaVisible: true,
      rootPresent: true,
      rootVisible: false,
    });
    assert.equal(d.kind, "already_authenticated");
    assert.equal(d.reason, "perc-spa-app-visible");
  });

  it("treats a visible assembly-host as already authenticated", () => {
    const d = classifyLoginSurface({
      pathname: "/Rhythmyx/cm/app/spa.jsp",
      formVisible: false,
      assemblyVisible: true,
      rootPresent: true,
      rootVisible: false,
    });
    assert.equal(d.kind, "already_authenticated");
    assert.equal(d.reason, "assembly-host-visible");
  });

  it("treats leaving /login with a hidden root as already authenticated", () => {
    const d = classifyLoginSurface({
      url: "http://127.0.0.1:12001/Rhythmyx/index.jsp",
      pathname: "/Rhythmyx/index.jsp",
      formVisible: false,
      legacyVisible: false,
      spaVisible: false,
      assemblyVisible: false,
      rootPresent: true,
      rootVisible: false,
    });
    assert.equal(d.kind, "already_authenticated");
    assert.equal(d.reason, "left-login-path");
  });

  it("stays pending on /login when no form and no authenticated chrome", () => {
    const d = classifyLoginSurface({
      pathname: "/Rhythmyx/login",
      formVisible: false,
      legacyVisible: false,
      spaVisible: false,
      assemblyVisible: false,
      rootPresent: false,
      rootVisible: false,
    });
    assert.equal(d.kind, "pending");
    assert.equal(d.reason, "waiting-for-login-ui");
  });
});

describe("loginUiWaitSelector", () => {
  it("omits perc-login-root so a hidden mount cannot satisfy the wait", () => {
    const sel = loginUiWaitSelector();
    assert.match(sel, /perc-login-form/);
    assert.match(sel, /j_username/);
    assert.match(sel, /perc-spa-app/);
    assert.match(sel, /assembly-host/);
    assert.doesNotMatch(sel, /perc-login-root/);
  });
});
