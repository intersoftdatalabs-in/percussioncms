/*
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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Unit tests for the auth helper login-form fill router (#5946).
 *
 * Covers the fillLoginForm / fillModernLoginForm / fillLegacyLoginForm
 * split (modern vs legacy branch selection, locale defaulting) plus the
 * best-effort listbox close in listModernLoginLocales — with a fake
 * Playwright page. No live CMS, no host install required.
 *
 * Run: npm run test:unit  (from frontend/)
 *   or: node --test tests/unit/auth-fill-login-form.test.js
 */

"use strict";

// Pin a side-effect-free load: no install discovery, no QA docker probe,
// no .env writes when helpers/auth.js runs its module top-level code.
delete process.env.DEV_PERCUSSION_INSTALL;
delete process.env.DEV_PERCUSSION_DTS_INSTALL;
delete process.env.DEV_PERCUSSION_URL;
delete process.env.TEST_CMS_URL;
delete process.env.CMS_HOST_PORT;
process.env.PERC_QA_SKIP_ENV_WRITE = "1";

const { describe, it, before } = require("node:test");
const assert = require("node:assert/strict");

let fillLoginForm;
let fillModernLoginForm;
let fillLegacyLoginForm;
let ensureLoginFormReady;
let selectModernLoginLocale;
let listModernLoginLocales;

// Dummy credential for the fake-page fill router tests below: never a real
// secret, never sent anywhere (the page double only records calls).
// mkd-code-review[secrets.heuristic]: adjudicated false positive (issue #5946) — test-only placeholder, no live CMS.
const DUMMY_PASSWORD = "dummy";

before(() => {
  ({
    fillLoginForm,
    fillModernLoginForm,
    fillLegacyLoginForm,
    ensureLoginFormReady,
    selectModernLoginLocale,
    listModernLoginLocales,
  } = require("../helpers/auth"));
});

function stripFirst(selector) {
  return String(selector).replace(/::first$/, "");
}

class FakeLocator {
  constructor(page, selector) {
    this._page = page;
    this._selector = selector;
  }

  async count() {
    this._page.calls.push(["count", this._selector]);
    return this._page._counts[stripFirst(this._selector)] ?? 0;
  }

  first() {
    return new FakeLocator(this._page, `${this._selector}::first`);
  }

  or() {
    return new FakeLocator(this._page, "__or-combined__");
  }

  filter() {
    return new FakeLocator(this._page, `${this._selector}::filtered`);
  }

  async click() {
    this._page.calls.push(["click", this._selector]);
  }

  async fill(value) {
    this._page.calls.push(["fill", this._selector, value]);
  }

  async inputValue() {
    this._page.calls.push(["inputValue", this._selector]);
    return this._page._inputValues[stripFirst(this._selector)] ?? "";
  }

  async waitFor() {
    this._page.calls.push(["waitFor", this._selector]);
    if (this._page._waitForThrows.has(stripFirst(this._selector))) {
      throw new Error(`waiting for ${this._selector} failed`);
    }
  }

  async selectOption(value) {
    this._page.calls.push(["selectOption", this._selector, value]);
  }

  async isVisible() {
    return this._page._visible[stripFirst(this._selector)] ?? false;
  }

  async evaluateAll() {
    this._page.calls.push(["evaluateAll", this._selector]);
    return this._page._evalTags;
  }

  async innerText() {
    return "";
  }
}

/**
 * Minimal Playwright page double. Visibility/counts/input values are
 * driven by selector maps so snapshotLoginSurface classifies as needed.
 */
function makePage({
  url = "http://127.0.0.1:12001/Rhythmyx/login",
  visible = {},
  counts = {},
  inputValues = {},
  waitForThrows = [],
  evalTags = [],
  escapeRejects = false,
} = {}) {
  const page = {
    calls: [],
    _visible: visible,
    _counts: counts,
    _inputValues: inputValues,
    _waitForThrows: new Set(waitForThrows),
    _evalTags: evalTags,
    url: () => url,
    locator: (selector) => new FakeLocator(page, selector),
    fill: async (selector, value) => {
      page.calls.push(["page.fill", selector, value]);
    },
    keyboard: {
      press: async (key) => {
        page.calls.push(["keyboard.press", key]);
        if (escapeRejects) {
          throw new Error("listbox already closed");
        }
      },
    },
    waitForTimeout: async () => {},
    waitForFunction: async () => {},
    waitForLoadState: async () => {},
    waitForResponse: async () => null,
    goto: async () => {},
  };
  return page;
}

function filledValues(page) {
  return page.calls
    .filter(([op]) => op === "fill" || op === "page.fill")
    .map(([, selector, value]) => `${selector}=${value}`);
}

describe("fillLoginForm router (#5946)", () => {
  it("fills the modern form and returns the modern submit locator", async () => {
    const page = makePage({
      visible: { '[data-testid="perc-login-form"]': true },
    });
    const result = await fillLoginForm(page, "Admin", DUMMY_PASSWORD);
    assert.equal(result.alreadyAuthenticated, false);
    assert.equal(result.submit._selector, '[data-testid="perc-login-submit"]');
    assert.deepEqual(filledValues(page), [
      '[data-testid="perc-login-username"]=Admin',
      '[data-testid="perc-login-password"]=dummy', // mkd-code-review[secrets.heuristic]: adjudicated false positive (issue #5946) — expected fill recording of a dummy test value, no real secret.
    ]);
  });

  it("fills the legacy form when only j_username is visible", async () => {
    const page = makePage({
      visible: { 'input[name="j_username"]': true },
    });
    const result = await fillLoginForm(page, "Editor", DUMMY_PASSWORD);
    assert.equal(result.alreadyAuthenticated, false);
    assert.equal(result.submit._selector, 'button[type="submit"]');
    assert.deepEqual(filledValues(page), [
      'input[name="j_username"]=Editor',
      'input[name="j_password"]=dummy', // mkd-code-review[secrets.heuristic]: adjudicated false positive (issue #5946) — expected fill recording of a dummy test value, no real secret.
    ]);
  });

  it("short-circuits when the session is already authenticated", async () => {
    const page = makePage({
      visible: { '[data-testid="perc-spa-app"]': true },
    });
    const result = await fillLoginForm(page, "Admin", DUMMY_PASSWORD);
    assert.deepEqual(result, { submit: null, alreadyAuthenticated: true });
    assert.deepEqual(filledValues(page), []);
  });

  it("selects the requested locale on the modern form", async () => {
    const page = makePage({
      visible: { '[data-testid="perc-login-form"]': true },
      counts: {
        '[data-testid="perc-login-locale-option-de-de"]': 1,
      },
    });
    await fillLoginForm(page, "Admin", DUMMY_PASSWORD, { locale: "de-de" });
    const clicks = page.calls
      .filter(([op]) => op === "click")
      .map(([, selector]) => stripFirst(selector));
    assert.ok(
      clicks.includes('[data-testid="perc-login-locale"]'),
      `expected locale trigger click, saw ${JSON.stringify(clicks)}`,
    );
    assert.ok(
      clicks.includes('[data-testid="perc-login-locale-option-de-de"]'),
      `expected de-de option click, saw ${JSON.stringify(clicks)}`,
    );
  });

  it("forces en-us when the hidden locale is non-English", async () => {
    const hidden =
      'input[type="hidden"][name="j_locale"], input[name="j_locale"]';
    const page = makePage({
      visible: { '[data-testid="perc-login-form"]': true },
      counts: {
        [hidden]: 1,
        '[data-testid="perc-login-locale-option-en-us"]': 1,
      },
      inputValues: { [hidden]: "de-de" },
    });
    await fillLoginForm(page, "Admin", DUMMY_PASSWORD);
    const clicks = page.calls
      .filter(([op]) => op === "click")
      .map(([, selector]) => stripFirst(selector));
    assert.ok(
      clicks.includes('[data-testid="perc-login-locale-option-en-us"]'),
      `expected en-us option click, saw ${JSON.stringify(clicks)}`,
    );
  });

  it("selects en-us on the native legacy locale select by default", async () => {
    const page = makePage({
      visible: { 'input[name="j_username"]': true },
      counts: { 'select[name="j_locale"]': 1 },
    });
    await fillLoginForm(page, "Editor", DUMMY_PASSWORD);
    const selects = page.calls.filter(([op]) => op === "selectOption");
    assert.deepEqual(selects, [
      ["selectOption", 'select[name="j_locale"]', "en-us"],
    ]);
  });
});

describe("fillModernLoginForm / fillLegacyLoginForm (#5946)", () => {
  it("modern helper returns the modern submit without locale work when hidden locale is absent", async () => {
    const page = makePage({});
    const result = await fillModernLoginForm(page, "Admin", DUMMY_PASSWORD, "");
    assert.equal(result.alreadyAuthenticated, false);
    assert.equal(result.submit._selector, '[data-testid="perc-login-submit"]');
    assert.ok(
      !page.calls.some(
        ([op, selector]) =>
          op === "click" && selector === '[data-testid="perc-login-locale"]',
      ),
      "locale combobox must stay closed when no locale is needed",
    );
  });

  it("legacy helper honors an explicit locale", async () => {
    const page = makePage({
      counts: { 'select[name="j_locale"]': 1 },
    });
    const result = await fillLegacyLoginForm(page, "Admin", DUMMY_PASSWORD, "hi-in");
    assert.equal(result.alreadyAuthenticated, false);
    assert.equal(result.submit._selector, 'button[type="submit"]');
    const selects = page.calls.filter(([op]) => op === "selectOption");
    assert.deepEqual(selects, [
      ["selectOption", 'select[name="j_locale"]', "hi-in"],
    ]);
  });
});

describe("ensureLoginFormReady (#5946)", () => {
  it("returns the first decision without waiting when a form is visible", async () => {
    const page = makePage({
      visible: { '[data-testid="perc-login-form"]': true },
    });
    const decision = await ensureLoginFormReady(
      page,
      page.locator('[data-testid="perc-login-form"]'),
      page.locator('input[name="j_username"]'),
    );
    assert.equal(decision.kind, "modern_form");
    assert.ok(
      !page.calls.some(([op]) => op === "waitFor"),
      "no wait expected when the form is already visible",
    );
  });

  it("throws the login-form timeout when the wait never resolves", async () => {
    const page = makePage({ waitForThrows: ["__or-combined__"] });
    await assert.rejects(
      () =>
        ensureLoginFormReady(
          page,
          page.locator('[data-testid="perc-login-form"]'),
          page.locator('input[name="j_username"]'),
        ),
      /Login form did not become visible within 30000ms/,
    );
  });
});

describe("selectModernLoginLocale / listModernLoginLocales (#5946)", () => {
  it("skips the combobox when the hidden locale already matches", async () => {
    const hidden =
      'input[type="hidden"][name="j_locale"], input[name="j_locale"]';
    const page = makePage({
      counts: { [hidden]: 1 },
      inputValues: { [hidden]: "de-de" },
    });
    await selectModernLoginLocale(page, "de-de");
    assert.ok(
      !page.calls.some(([op]) => op === "click"),
      `no clicks expected, saw ${JSON.stringify(page.calls)}`,
    );
  });

  it("lists locale tags and closes the listbox best-effort", async () => {
    const page = makePage({
      counts: { '[data-testid="perc-login-locale"]': 1 },
      evalTags: ["en-us", "de-de"],
    });
    const tags = await listModernLoginLocales(page);
    assert.deepEqual(tags, ["en-us", "de-de"]);
    assert.ok(
      page.calls.some(
        ([op, key]) => op === "keyboard.press" && key === "Escape",
      ),
      "Escape dismisses the listbox so it cannot obscure submit",
    );
  });

  it("still returns tags when Escape is rejected (already closed)", async () => {
    const page = makePage({
      counts: { '[data-testid="perc-login-locale"]': 1 },
      evalTags: ["en-us"],
      escapeRejects: true,
    });
    const tags = await listModernLoginLocales(page);
    assert.deepEqual(tags, ["en-us"]);
  });

  it("returns [] without touching the keyboard when no locale trigger exists", async () => {
    const page = makePage({});
    const tags = await listModernLoginLocales(page);
    assert.deepEqual(tags, []);
    assert.ok(
      !page.calls.some(([op]) => op === "keyboard.press"),
      "no Escape press without a listbox trigger",
    );
  });
});
