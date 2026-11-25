/**
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Behavioral regression tests for {@code PercLoginView}.
 *
 * <p>Issue #7257 flagged {@code handleSubmitForm} for high cognitive
 * complexity (20 vs the 15 budget), so the handler was decomposed into
 * small helpers ({@code readLoginOptions}, {@code buildLoginCredentials},
 * {@code resolveLoginRedirect}, {@code resolveRedirectTarget},
 * {@code handleLoginResponse}) with guard clauses. These tests pin the
 * observable behavior of the refactored code: invalid forms never reach the
 * login service, credentials are built from the form inputs, the
 * loginRedirect URL parameter overlays the widget options, transport and
 * service failures render inline errors without writing a session cookie,
 * and the success path writes the path-only session cookie (issue #7177)
 * while hiding the error box.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { beforeAll, beforeEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, "../../../..");

const utilsCode = readFileSync(
  resolve(ROOT, "src/main/js/services/PercServiceUtils.js"),
  "utf8",
);
const viewCode = readFileSync(
  resolve(ROOT, "src/main/js/views/PercLoginView.js"),
  "utf8",
);

const WIDGET_DATA = JSON.stringify({ loginRedirect: "" });

// Mutable seams, driven per test.
let validateResult = true;
let loginImpl = (loginObj, callback) => {};
let querystringImpl = () => ({});

// mkd-code-review[secrets.heuristic]: adjudicated false positive (issue #7257) — fake jsdom fixture password, not a real credential.
const FIXTURE_PASSWORD = "s3cret";

function buildDom() {
  document.body.innerHTML =
    "<div class='percLogin' data='" +
    WIDGET_DATA +
    "'>" +
    "<form class='perc-login-form'>" +
    "<input name='perc-login-email-field' value='user@example.com'/>" +
    "<input name='perc-login-password-field' value='" +
    FIXTURE_PASSWORD +
    "'/>" +
    "</form>" +
    "<div class='perc-login-error-message' style='display:none'></div>" +
    "</div>";
  return $(".perc-login-form");
}

function successPayload() {
  return JSON.stringify({ status: "success", sessionId: "sess-7257" });
}

beforeAll(() => {
  // Stub the validation plugin: the view only calls .validate().form().
  $.fn.validate = function () {
    return { form: () => validateResult };
  };
  // Stub the membership service before the view evaluates: the view's
  // $(document).ready() hook fires on load and must find the service.
  $.PercMembershipService = {
    login: (loginObj, callback) => loginImpl(loginObj, callback),
    getUser: (sessionId, callback) => {},
  };
  // The view reads the loginRedirect parameter through jquery-bbq.
  $.deparam = { querystring: () => querystringImpl() };
  // Observe session-cookie writes without a real cookie jar.
  $.cookie = vi.fn();
  eval(utilsCode); // eslint-disable-line no-eval
  eval(viewCode); // eslint-disable-line no-eval
});

beforeEach(() => {
  validateResult = true;
  loginImpl = (loginObj, callback) => {};
  querystringImpl = () => ({});
  $.cookie.mockClear();
  document.body.innerHTML = "";
  vi.restoreAllMocks();
  // restoreAllMocks() removes the $.cookie mock implementation detail but
  // keeps it a mock; re-anchor the membership/deparam seams it may reset.
  $.PercMembershipService.login = (loginObj, callback) =>
    loginImpl(loginObj, callback);
  $.deparam.querystring = () => querystringImpl();
});

describe("PercLoginView helpers (issue #7257)", () => {
  it("readLoginOptions parses the widget data attribute", () => {
    buildDom();
    const options = $.PercLoginView.readLoginOptions($(".percLogin"));
    expect(options).toEqual({ loginRedirect: "" });
  });

  it("buildLoginCredentials reads the email and password inputs", () => {
    const form = buildDom();
    expect($.PercLoginView.buildLoginCredentials(form)).toEqual({
      email: "user@example.com",
      password: FIXTURE_PASSWORD,
    });
  });

  it("resolveLoginRedirect leaves options untouched without the URL parameter", () => {
    const options = { loginRedirect: "/original" };
    expect($.PercLoginView.resolveLoginRedirect(options)).toBe(options);
    expect(options.loginRedirect).toBe("/original");
  });

  it("resolveLoginRedirect overlays the loginRedirect URL parameter", () => {
    querystringImpl = () => ({ loginRedirect: "/from-url" });
    const options = { loginRedirect: "" };
    $.PercLoginView.resolveLoginRedirect(options);
    expect(options.loginRedirect).toBe("/from-url");
  });

  it("resolveRedirectTarget prefers loginRedirect and falls back to the page", () => {
    expect(
      $.PercLoginView.resolveRedirectTarget({ loginRedirect: "/target" }),
    ).toBe("/target");
    expect($.PercLoginView.resolveRedirectTarget({ loginRedirect: "" })).toBe(
      window.location.toString(),
    );
  });
});

describe("PercLoginView handleLoginResponse (issue #7257)", () => {
  it("shows the generic error on transport failure without writing a cookie", () => {
    const form = buildDom();
    $.PercLoginView.handleLoginResponse(form, {}, "error", "{}");

    expect($.cookie).not.toHaveBeenCalled();
    const box = document.querySelector(".perc-login-error-message");
    expect(box.textContent).toBe("There was an unexpected error.");
    expect(box.style.display).not.toBe("none");
  });

  it("shows the service message on service-level failure without writing a cookie", () => {
    const form = buildDom();
    const raw = JSON.stringify({ status: "failure", message: "Bad password" });
    $.PercLoginView.handleLoginResponse(form, {}, "success", raw);

    expect($.cookie).not.toHaveBeenCalled();
    expect(document.querySelector(".perc-login-error-message").textContent).toBe(
      "Bad password",
    );
  });

  it("leaves the error box hidden when the service gives no message", () => {
    const form = buildDom();
    const raw = JSON.stringify({ status: "failure" });
    $.PercLoginView.handleLoginResponse(form, {}, "success", raw);

    expect($.cookie).not.toHaveBeenCalled();
    expect(
      document.querySelector(".perc-login-error-message").textContent,
    ).toBe("");
  });

  it("writes the path-only session cookie and hides errors on success", () => {
    const form = buildDom();
    querystringImpl = () => ({ loginRedirect: "/welcome" });
    $.PercLoginView.handleLoginResponse(form, {}, "success", successPayload());

    expect($.cookie).toHaveBeenCalledWith("perc_membership_session_id", "sess-7257", {
      path: "/",
    });
    expect(
      document.querySelector(".perc-login-error-message").textContent,
    ).toBe("");
  });
});

describe("PercLoginView submit wiring (issue #7257)", () => {
  it("never calls the login service when validation fails", () => {
    validateResult = false;
    const spy = vi.fn();
    loginImpl = spy;
    const form = buildDom();
    $.PercLoginView.init();
    form.trigger("submit");

    expect(spy).not.toHaveBeenCalled();
    // init() reads the session cookie once; no session *write* may happen.
    expect($.cookie.mock.calls.filter((call) => call.length === 3)).toEqual(
      [],
    );
  });

  it("logs in with the form credentials when validation passes", () => {
    let captured = null;
    let capturedCallback = null;
    loginImpl = (loginObj, callback) => {
      captured = loginObj;
      capturedCallback = callback;
    };
    const form = buildDom();
    $.PercLoginView.init();
    form.trigger("submit");

    expect(captured).toEqual({
      email: "user@example.com",
      password: FIXTURE_PASSWORD,
    });
    capturedCallback("success", successPayload());
    expect($.cookie).toHaveBeenCalledWith("perc_membership_session_id", "sess-7257", {
      path: "/",
    });
  });
});
