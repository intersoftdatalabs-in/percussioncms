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
 * Behavioral regression tests for {@code PercRegistrationView}.
 *
 * <p>Issue #7276 flagged {@code handleSubmitForm} for high cognitive
 * complexity (vs the 15 budget), so the handler was decomposed into small
 * helpers ({@code readRegistrationWidgetOptions},
 * {@code readRegistrationFormOptions}, {@code buildRegisterObject},
 * {@code resolveRegistrationRedirectUrl},
 * {@code redirectAfterRegistration}, {@code redirectToConfirmationPage},
 * {@code handleRegistrationResponse}, {@code handleRegistrationSuccess})
 * with guard clauses. These tests pin the observable behavior of the
 * refactored code: invalid forms never reach the registration service, the
 * register payload is built from the form inputs, the registrationRedirect
 * URL parameter is honored (and stripped) with a home-page fallback,
 * transport and service failures render inline errors without writing a
 * session cookie, the verification-required path shows the confirmation
 * notice without establishing a session, and the direct success path writes
 * the path-only session cookie (issue #7177) before redirecting.
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
  resolve(ROOT, "src/main/js/views/PercRegistrationView.js"),
  "utf8",
);

const WIDGET_DATA = JSON.stringify({ homePageLink: "/home" });

// Mutable seams, driven per test.
let validateResult = true;
let registerImpl = (registerObj, callback) => {};
let querystringImpl = () => ({});
let paramQuerystringImpl = (...args) => (0 === args.length ? "" : "");

function buildDom(options = {}) {
  const confirmationPage =
    "undefined" === typeof options.confirmationPage
      ? ""
      : options.confirmationPage;
  const formName = options.cform
    ? "perc-registration-cform"
    : "perc-registration-form";
  const formId = options.cform
    ? "perc-registration-cform"
    : "perc-registration-form";
  document.body.innerHTML =
    "<div class='percRegistration' data='" +
    WIDGET_DATA +
    "'>" +
    "<div class='percRegistration' data='" +
    WIDGET_DATA +
    "'>" +
    "<div class='perc-registration-mode'>" +
    "<form id='" +
    formId +
    "' name='" +
    formName +
    "'>" +
    "<input id='perc-registration-email-field' value='user@example.com'/>" +
    // mkd-code-review[secrets.heuristic]: adjudicated false positive (issue #7276) — "s3cret" is a fake jsdom fixture value, not a real credential.
    "<input id='perc-registration-password-field' value='s3cret'/>" +
    "<input id='perc-confirmation-page' value='" +
    confirmationPage +
    "'/>" +
    "</form>" +
    "</div>" +
    "</div>" +
    "</div>" +
    "<div class='perc-reg-error-message' style='display:none'></div>" +
    "<div class='perc-reg-confirmation-mode' style='display:none'></div>" +
    "<div class='perc-reg-confirmation-message'></div>";
  return $("#" + formId);
}

function successPayload() {
  return JSON.stringify({ status: "success", sessionId: "sess-7276" });
}

beforeAll(() => {
  // Stub the validation plugin: the view only calls .validate().form().
  $.fn.validate = function () {
    return { form: () => validateResult };
  };
  // Stub the membership service before the view evaluates: the view's
  // $(document).ready() hook fires on load and must find the service.
  $.PercMembershipService = {
    register: (registerObj, callback) => registerImpl(registerObj, callback),
    validateResetPwKey: (key, callback) => {},
    confirmRegistration: (key, callback) => {},
    getUser: (sessionId, callback) => {},
  };
  // The view reads URL parameters through jquery-bbq.
  $.deparam = { querystring: () => querystringImpl() };
  $.param.querystring = (...args) => paramQuerystringImpl(...args);
  // Observe session-cookie writes without a real cookie jar.
  $.cookie = vi.fn();
  eval(utilsCode); // eslint-disable-line no-eval
  eval(viewCode); // eslint-disable-line no-eval
});

beforeEach(() => {
  validateResult = true;
  registerImpl = (registerObj, callback) => {};
  querystringImpl = () => ({});
  paramQuerystringImpl = (...args) => (0 === args.length ? "" : "");
  $.cookie.mockClear();
  document.body.innerHTML = "";
  vi.restoreAllMocks();
  // restoreAllMocks() removes the $.cookie mock implementation detail but
  // keeps it a mock; re-anchor the membership/deparam seams it may reset.
  $.cookie = vi.fn();
  $.PercMembershipService.register = (registerObj, callback) =>
    registerImpl(registerObj, callback);
  $.deparam.querystring = () => querystringImpl();
  $.param.querystring = (...args) => paramQuerystringImpl(...args);
});

describe("PercRegistrationView helpers (issue #7276)", () => {
  it("readRegistrationWidgetOptions parses the wrapper data attribute", () => {
    buildDom();
    const form = $("#perc-registration-form");
    expect($.PercRegistrationView.readRegistrationWidgetOptions(form)).toEqual(
      { homePageLink: "/home" },
    );
  });

  it("buildRegisterObject reads the email and password inputs", () => {
    const form = buildDom();
    expect($.PercRegistrationView.buildRegisterObject(form)).toEqual({
      email: "user@example.com",
      // mkd-code-review[secrets.heuristic]: adjudicated false positive (issue #7276) — fake fixture value asserted for parity with the DOM, not a real credential.
      password: "s3cret",
      confirmationRequired: false,
      confirmationPage: window.location.href,
    });
  });

  it("buildRegisterObject flags confirmation for the cform variant", () => {
    const form = buildDom({ cform: true });
    expect($.PercRegistrationView.buildRegisterObject(form).confirmationRequired).toBe(
      true,
    );
  });

  it("resolveRegistrationRedirectUrl returns the parameter or empty string", () => {
    expect(
      $.PercRegistrationView.resolveRegistrationRedirectUrl({
        registrationRedirect: "/from-url",
      }),
    ).toBe("/from-url");
    expect($.PercRegistrationView.resolveRegistrationRedirectUrl({})).toBe(
      "",
    );
  });

  it("redirectAfterRegistration falls back to the home page link", () => {
    paramQuerystringImpl = () => "";
    const destination = $.PercRegistrationView.redirectAfterRegistration(
      "",
      { homePageLink: "/home" },
      {},
    );
    expect(destination).toBe("/home");
  });

  it("redirectAfterRegistration strips the consumed redirect parameter", () => {
    paramQuerystringImpl = () => "";
    const urlstring = { registrationRedirect: "/from-url", other: "1" };
    const destination = $.PercRegistrationView.redirectAfterRegistration(
      "/from-url",
      { homePageLink: "/home" },
      urlstring,
    );
    expect(destination).toBe("/from-url");
    expect(urlstring.registrationRedirect).toBeUndefined();
  });

  it("redirectToConfirmationPage sanitizes the user-supplied page", () => {
    paramQuerystringImpl = (...args) => (0 === args.length ? "a=b" : "");
    const spy = vi.spyOn($.PercServiceUtils, "sanitizeUrlForHref");
    const destination =
      $.PercRegistrationView.redirectToConfirmationPage("/confirm");
    expect(spy).toHaveBeenCalledWith("/confirm?a=b");
    expect(destination).toBe(spy.mock.results[0].value);
  });
});

describe("PercRegistrationView handleRegistrationResponse (issue #7276)", () => {
  it("shows the generic error on transport failure without writing a cookie", () => {
    const form = buildDom();
    $.PercRegistrationView.handleRegistrationResponse(
      form,
      {},
      { homePageLink: "/home" },
      "error",
      "{}",
    );

    expect($.cookie).not.toHaveBeenCalled();
    const box = document.querySelector(".perc-reg-error-message");
    expect(box.textContent).toBe("There was an unexpected error.");
    expect(box.style.display).not.toBe("none");
  });

  it("shows the service message on service-level failure without writing a cookie", () => {
    const form = buildDom();
    const raw = JSON.stringify({ status: "failure", message: "Bad email" });
    $.PercRegistrationView.handleRegistrationResponse(
      form,
      {},
      { homePageLink: "/home" },
      "success",
      raw,
    );

    expect($.cookie).not.toHaveBeenCalled();
    expect(document.querySelector(".perc-reg-error-message").textContent).toBe(
      "Bad email",
    );
  });

  it("shows the verification notice for verReq registrations without a session cookie", () => {
    const form = buildDom({ cform: true });
    $.PercRegistrationView.handleRegistrationResponse(
      form,
      { verReq: true },
      { homePageLink: "/home" },
      "success",
      successPayload(),
    );

    // No session is established until the email is confirmed (Codex review
    // on issue #7276: the pre-refactor verReq branch never wrote a cookie).
    expect($.cookie).not.toHaveBeenCalled();
    expect(
      document.querySelector(".perc-reg-confirmation-message").textContent,
    ).toContain("Thank you for registering with us.");
  });

  it("redirects to the confirmation page on direct success with a page configured", () => {
    const form = buildDom({ confirmationPage: "/confirm" });
    paramQuerystringImpl = (...args) => (0 === args.length ? "" : "");
    const spy = vi.spyOn($.PercServiceUtils, "sanitizeUrlForHref");
    $.PercRegistrationView.handleRegistrationResponse(
      form,
      {},
      { homePageLink: "/home" },
      "success",
      successPayload(),
    );

    expect($.cookie).toHaveBeenCalledWith(
      "perc_membership_session_id",
      "sess-7276",
      { path: "/" },
    );
    expect(spy).toHaveBeenCalledWith("/confirm");
  });
});

describe("PercRegistrationView submit wiring (issue #7276)", () => {
  it("never calls the registration service when validation fails", () => {
    validateResult = false;
    const spy = vi.fn();
    registerImpl = spy;
    const form = buildDom();
    $.PercRegistrationView.init();
    form.trigger("submit");

    expect(spy).not.toHaveBeenCalled();
    // init() reads the session cookie once; no session *write* may happen.
    expect($.cookie.mock.calls.filter((call) => call.length === 3)).toEqual(
      [],
    );
  });

  it("registers with the form payload when validation passes", () => {
    let captured = null;
    let capturedCallback = null;
    registerImpl = (registerObj, callback) => {
      captured = registerObj;
      capturedCallback = callback;
    };
    const form = buildDom();
    $.PercRegistrationView.init();
    form.trigger("submit");

    expect(captured).toEqual({
      email: "user@example.com",
      password: "s3cret",
      confirmationRequired: false,
      confirmationPage: window.location.href,
    });
    paramQuerystringImpl = (...args) => (0 === args.length ? "" : "");
    capturedCallback("success", successPayload());
    expect($.cookie).toHaveBeenCalledWith(
      "perc_membership_session_id",
      "sess-7276",
      { path: "/" },
    );
  });
});
