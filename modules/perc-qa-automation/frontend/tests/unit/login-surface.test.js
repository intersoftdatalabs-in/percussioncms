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
const fs = require("node:fs");
const path = require("node:path");
const {
  isOffLoginPath,
  classifyLoginSurface,
  loginUiWaitSelector,
  DEFAULT_LOGIN_ATTEMPTS,
  resolveLoginAttempts,
  loginRetryDelayMs,
  isServerAuthFailureText,
  summarizeLoginFailure,
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

describe("login.spec.js timeout vs stacked login waits (#4714)", () => {  it("gives loginAsAdmin more than one 30s waitForLoginSurface budget", () => {
    const spec = fs.readFileSync(
      path.join(__dirname, "../login.spec.js"),
      "utf8",
    );
    const m = spec.match(/setTimeout\(([\d_]+)\)/);
    assert.ok(m, "login.spec.js must set a Playwright test timeout");
    const ms = Number(m[1].replace(/_/g, ""));
    assert.ok(
      ms >= 90_000,
      `expected >= 90000ms (got ${ms}) so login() surface+fill+nav waits do not close the page`,
    );
  });
});

describe("login retry loop (#5937)", () => {
  it("defaults to three attempts", () => {
    assert.equal(DEFAULT_LOGIN_ATTEMPTS, 3);
    assert.equal(resolveLoginAttempts({}), 3);
    assert.equal(resolveLoginAttempts(undefined), 3);
  });

  it("honors PERC_QA_LOGIN_ATTEMPTS and falls back on garbage", () => {
    assert.equal(resolveLoginAttempts({ PERC_QA_LOGIN_ATTEMPTS: "1" }), 1);
    assert.equal(resolveLoginAttempts({ PERC_QA_LOGIN_ATTEMPTS: "5" }), 5);
    assert.equal(resolveLoginAttempts({ PERC_QA_LOGIN_ATTEMPTS: "0" }), 3);
    assert.equal(resolveLoginAttempts({ PERC_QA_LOGIN_ATTEMPTS: "-2" }), 3);
    assert.equal(resolveLoginAttempts({ PERC_QA_LOGIN_ATTEMPTS: "many" }), 3);
    assert.equal(resolveLoginAttempts({ PERC_QA_LOGIN_ATTEMPTS: "" }), 3);
  });

  it("caps attempts so a wedged cell cannot loop forever", () => {
    assert.equal(resolveLoginAttempts({ PERC_QA_LOGIN_ATTEMPTS: "99" }), 10);
  });

  it("backs off exponentially with bounded jitter", () => {
    const orig = Math.random;
    try {
      Math.random = () => 0;
      assert.equal(loginRetryDelayMs(0), 1500);
      assert.equal(loginRetryDelayMs(1), 3000);
      assert.equal(loginRetryDelayMs(2), 6000);
      Math.random = () => 0.999999;
      assert.ok(loginRetryDelayMs(0) <= 2250, "attempt-0 delay stays capped");
      assert.ok(loginRetryDelayMs(1) <= 3750, "attempt-1 delay stays capped");
      assert.ok(
        loginRetryDelayMs(99) <= 8750,
        "deep attempts cap at 8000 + jitter",
      );
      assert.ok(
        loginRetryDelayMs(1) > loginRetryDelayMs(0),
        "backoff grows between attempts",
      );
    } finally {
      Math.random = orig;
    }
  });

  it("classifies server-side auth rejection vs empty banner", () => {
    assert.equal(
      isServerAuthFailureText("BackEndTable/rxmaster could not authenticate"),
      true,
    );
    assert.equal(isServerAuthFailureText("LoginException: bad login"), true);
    assert.equal(isServerAuthFailureText("Invalid username or password"), true);
    assert.equal(isServerAuthFailureText(""), false);
    assert.equal(isServerAuthFailureText(null), false);
    assert.equal(isServerAuthFailureText("Welcome back"), false);
  });

  it("summarizes attempts, POST status, and the server banner", () => {
    const msg = summarizeLoginFailure({
      attempts: 3,
      url: "http://127.0.0.1:12001/Rhythmyx/login",
      errorBanner: "could not authenticate",
      lastPostStatus: 200,
    });
    assert.match(msg, /3 attempt\(s\)/);
    assert.match(msg, /Rhythmyx\/login/);
    assert.match(msg, /200/);
    assert.match(msg, /could not authenticate/);
    assert.match(msg, /server-side auth rejection/);
    assert.match(msg, /PERC_QA_LOGIN_ATTEMPTS=1/);
  });

  it("summarizes a missing POST probe without crashing", () => {
    const msg = summarizeLoginFailure({ url: "http://x/Rhythmyx/login" });
    assert.match(msg, /not observed/);
    assert.match(msg, /No server error banner/);
  });

  it("wires auth.js login() through the retry helpers", () => {
    const src = fs.readFileSync(
      path.join(__dirname, "../helpers/auth.js"),
      "utf8",
    );
    assert.match(src, /resolveLoginAttempts\(process\.env\)/);
    assert.match(src, /loginRetryDelayMs\(attempt - 1\)/);
    assert.match(src, /summarizeLoginFailure\(/);
    assert.match(src, /perc-login-error/);
  });
});
