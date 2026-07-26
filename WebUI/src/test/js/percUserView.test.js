/**
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Regression tests for WebUI/src/main/webapp/cm/views/PercUserView.js
 *
 * Closes GitHub CodeQL alerts (js/xss-through-dom) flagged on:
 *   - `narrowSearchLabel.html(I18N.message(..., [maxNumberOfUsers]))`
 *     in `updateImportUsersDialog` (the i18n-substitution sink)
 *   - `$("<option/>").val(role).html(role)` in `updateAssignedRoles` and
 *     `updateAvailableRoles` (the role-name sink)
 *
 * Pre-fix code parses user-supplied role names as HTML, so a role whose
 * name contains `<script>...</script>` or `<img onerror=...>` produces live
 * DOM elements inside `<select>` controls. Post-fix code uses `.text(...)`
 * which sets a text node; HTML markup in the role name is rendered literally.
 *
 * Test strategy (Constitution III fail-then-pass):
 *   - Drive `$.PercUserView()` factory and call the API methods it exposes
 *     (`updateAssignedRoles`, `updateAvailableRoles`,
 *     `updateImportUsersDialog`).
 *   - Feed attacker-controlled role names / values through the documented
 *     method signatures.
 *   - Assert that no live `<script>` / `<img>` elements are produced and
 *     that the role string is present as inert text.
 *
 * The factory expects a populated DOM (it binds to `#perc-users-*` ids), so
 * each test seeds the body with the minimum markup the methods touch.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../main/webapp/cm/views/PercUserView.js"
);

let $;
let api;
let capturedView;

// ---------------------------------------------------------------------------
// Bootstrap: ensure the test DOM has every element the factory binds to.
// `$.PercUserView()` reads `$("#perc-users-...")` etc. at construction time
// and `.html(...)`/`.text(...)` calls fail if those nodes are missing.
// ---------------------------------------------------------------------------
function seedDom() {
  document.body.innerHTML = `
    <div id="perc-users-assigned-roles"><select></select></div>
    <div id="perc-users-available-roles"><select></select></div>
    <div id="perc-users-import-users-dialog-fixed"></div>
    <div id="perc-users-directory-users-table"></div>
    <input id="perc-users-search-input" />
    <button id="perc-users-search-button"></button>
    <input id="perc-users-directory-users-selectall-checkbox" type="checkbox" />
    <label id="perc-users-directory-users-selectall-label"></label>
    <button id="perc-users-directory-users-cancel-button"></button>
    <button id="perc-users-import-users-button"></button>
    <button id="perc-users-directory-users-import-button"></button>
    <label id="perc-users-select-at-least-one-user-label"></label>
    <div id="perc-users-narrow-search"></div>
    <div id="perc-users-iframe-wrapper"></div>
    <div id="perc-users-content-frame-wrapper"></div>
    <iframe id="perc-users-iframe"></iframe>
    <div id="perc-users-tabs"></div>
    <div id="perc-users-tab-adduser"></div>
    <div id="perc-users-tab-listusers"></div>
    <input id="perc-users-username-field" />
    <input id="perc-users-password-field" />
    <input id="perc-users-password-confirm-field" />
    <input id="perc-users-email-field" />
    <button id="perc-users-add-button"></button>
    <button id="perc-users-cancel-add-button"></button>
    <div id="perc-users-username-label"></div>
    <div id="perc-users-adduser-error"></div>
    <div id="perc-users-message-bar"></div>
    <button id="perc-users-add-save-button"></button>
    <table id="perc-users-table"><tbody></tbody></table>
    <div id="perc-users-adduser-row"></div>
    <div id="perc-users-listuser-row"></div>
    <div id="perc-users-message-bar-text"></div>
  `;
}

function loadFactory() {
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    // jquery(window) sometimes returns an empty jQuery collection; fall back
    // to the function form, matching the pattern from compat.test.js.
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn) jq.fn = jq.prototype;
  }

  // The IIFE source ends with `})(jQuery)`, so the global symbol `jQuery`
  // must resolve to our bound instance.
  globalThis.jQuery = jq;
  globalThis.$ = jq;

  // The IIFE references a few sibling namespaces that don't need to be
  // functional for the sinks under test — they just need to be present.
  jq.PercDirtyController = jq.PercDirtyController || { markClean: () => {} };
  jq.PercUserController = jq.PercUserController || {
    // Capture the viewApi the factory passes to the controller; the factory
    // does not return viewApi itself, but it is handed off to the controller
    // via init(viewApi) at line 85 of PercUserView.js.
    init: (vew) => {
      capturedView = vew;
    },
    getUsers: () => {},
    save: () => {},
    delete: () => {},
    addUser: () => {},
    loadUser: () => {},
    importDirectoryUsers: () => {},
    getDirectoryUserNames: () => {},
    addUserToDb: () => {},
    removeUserFromDb: () => {},
    getCurrentUserRoles: () => {},
    addNewUserEmailLogin: () => {},
  };
  jq.PercPathService = jq.PercPathService || {};

  // Default I18N shim — tests may overwrite globalThis.I18N BEFORE
  // calling loadFactory() to exercise the i18n sink with attacker-controlled
  // template values.
  if (!globalThis.I18N) {
    globalThis.I18N = {
      message: (key, subs) =>
        `[${key}` + (subs && subs.length ? ":" + subs.join(",") : "") + "]",
    };
  }

  jq.fn.dialog =
    jq.fn.dialog ||
    function () {
      return this;
    };
  jq.fn.perc_dialog =
    jq.fn.perc_dialog ||
    function () {
      return this;
    };
  jq.fn.setDirty =
    jq.fn.setDirty ||
    function () {
      return this;
    };
  jq.fn.fancytree =
    jq.fn.fancytree ||
    function () {
      return this;
    };

  // Run the source in the current global scope so its `})(jQuery)` tail
  // can see globalThis.jQuery without a Function wrapper.
  try {
    (0, eval)(readFileSync(SRC_PATH, "utf8"));
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error("DEBUG eval error:", e.message);
    throw e;
  }
  // The factory does not return viewApi; it hands it to the controller via
  // controller.init(viewApi). Mock captured it.
  capturedView = null;
  jq.PercUserView();
  $ = jq;
  api = capturedView;
}

beforeEach(() => {
  seedDom();
  loadFactory();
});

afterEach(() => {
  vi.restoreAllMocks();
  document.body.innerHTML = "";
  delete globalThis.I18N;
  delete window.__pwned;
});

// ---------------------------------------------------------------------------
// Source-pattern tests — these pin the security-relevant pattern in the
// source so that any future regression that reintroduces `.html(...)` on
// user-controlled inputs fails immediately. Erlang rules warn against pure
// grep tests for non-trivial logic; here the "logic" is the very absence of
// the unsafe pattern, so a presence/absence check is the right tool.
// ---------------------------------------------------------------------------
describe("source-pattern (anti-regression for js/xss-through-dom)", () => {
  const src = readFileSync(SRC_PATH, "utf8");

  it("does not call .html() with a user-controlled role name", () => {
    expect(src).not.toMatch(/\.html\(\s*userRole\s*\)/);
    expect(src).not.toMatch(/\.html\(\s*rolesArrayCache\[i\]\s*\)/);
  });

  it("does not call .html() with the i18n message for the narrow-search label", () => {
    expect(src).not.toMatch(/narrowSearchLabel\.html\(/);
  });

  it("uses .text() for the role and narrow-search sinks", () => {
    expect(src).toMatch(/\.val\([^)]+\)\s*\.text\([^)]*userRole[^)]*\)/);
    expect(src).toMatch(
      /\.val\([^)]+\)\s*\.text\([^)]*rolesArrayCache\[i\][^)]*\)/
    );
    expect(src).toMatch(/narrowSearchLabel\.text\(/);
  });
});

// ---------------------------------------------------------------------------
// Behavioral tests — exercise the live factory and assert no live DOM
// elements are produced from attacker-controlled role strings.
// ---------------------------------------------------------------------------
describe("updateAssignedRoles (role-name sink)", () => {
  it("does not produce a <script> element from a malicious role name", () => {
    const malicious = "<script>window.__pwned=true</script>";
    api.updateAssignedRoles([malicious]);

    const select = document.querySelector("#perc-users-assigned-roles select");
    expect(select).toBeTruthy();
    expect(
      select.querySelectorAll("script").length,
      "no <script> from role"
    ).toBe(0);
    expect(window.__pwned).toBeUndefined();
  });

  it("does not produce an event-handler <img> from a malicious role name", () => {
    const malicious = '<img src="x" onerror="window.__pwned=1">';
    api.updateAssignedRoles([malicious]);

    const select = document.querySelector("#perc-users-assigned-roles select");
    expect(select.querySelectorAll("img").length, "no <img> from role").toBe(0);
    expect(window.__pwned).toBeUndefined();
  });

  it("renders a benign role name as the option's text content", () => {
    api.updateAssignedRoles(["Editor"]);
    const select = document.querySelector("#perc-users-assigned-roles select");
    const option = select.querySelector("option");
    expect(option).toBeTruthy();
    expect(option.value).toBe("Editor");
    expect(option.textContent).toBe("Editor");
    expect(option.children.length, "option has no element children").toBe(0);
  });
});

describe("updateAvailableRoles (role-name sink)", () => {
  it("does not produce a <script> element from a malicious role name", () => {
    const malicious = "<script>window.__pwned=true</script>";
    api.updateAvailableRoles([malicious], []);

    const select = document.querySelector("#perc-users-available-roles select");
    expect(select).toBeTruthy();
    expect(select.querySelectorAll("script").length).toBe(0);
    expect(window.__pwned).toBeUndefined();
  });

  it("does not inject event-handler elements into available roles", () => {
    api.updateAvailableRoles(['<img src="x" onerror="window.__pwned=1">'], []);
    const select = document.querySelector("#perc-users-available-roles select");
    expect(select.querySelectorAll("img").length).toBe(0);
    expect(window.__pwned).toBeUndefined();
  });

  it("skips roles that are already assigned", () => {
    api.updateAvailableRoles(["A", "B", "C"], ["B"]);
    const select = document.querySelector("#perc-users-available-roles select");
    const opts = Array.from(select.querySelectorAll("option")).map(
      (o) => o.textContent
    );
    expect(opts).toEqual(["A", "C"]);
  });
});

describe("updateImportUsersDialog (i18n narrow-search sink)", () => {
  it("does not parse HTML markup from the i18n template via .html()", () => {
    // Override I18N.message with one that returns attacker-controlled markup
    // so we can prove the sink would have rendered it as HTML pre-fix.
    globalThis.I18N = {
      message: (key, subs) =>
        key === "perc.ui.users.import.dialogs@NarrowSearch"
          ? '<img src=x onerror="window.__pwned=1">'
          : "[other]",
    };
    // Re-seed and reload so the factory captures the new I18N.
    seedDom();
    loadFactory();
    api.init();

    const label = document.querySelector("#perc-users-narrow-search");
    expect(label).toBeTruthy();
    expect(label.querySelectorAll("img").length, "no <img> from i18n").toBe(0);
    expect(window.__pwned).toBeUndefined();
  });

  it("renders the i18n template as inert text", () => {
    globalThis.I18N = {
      message: (key, subs) =>
        key === "perc.ui.users.import.dialogs@NarrowSearch"
          ? "Showing 1-200 of N (narrow your search)"
          : "[other]",
    };
    seedDom();
    loadFactory();
    api.init();

    const label = document.querySelector("#perc-users-narrow-search");
    expect(label.textContent).toContain("Showing 1-200");
    expect(label.children.length, "label has no element children").toBe(0);
  });
});

// ---------------------------------------------------------------------------
// Public API surface — pin the factory's exported method names so callers
// (UserController, ImportUsersDialog opener, etc.) keep working.
// ---------------------------------------------------------------------------
describe("public API", () => {
  it("$.PercUserView() returns the documented viewApi methods", () => {
    const expected = [
      "init",
      "updateListOfUsers",
      "updateUserNameField",
      "updateAssignedRoles",
      "updateAvailableRoles",
      "updateEmail",
      "resetUserDetails",
      "selectUser",
      "showSelectedUserEditor",
      "showNewUserEditor",
      "updateImportUsersDialog",
      "showImportWarning",
      "showImportError",
      "disableUserImport",
      "alertDialog",
    ];
    for (const name of expected) {
      expect(typeof api[name], `api.${name} should be a function`).toBe(
        "function"
      );
    }
  });
});
// ---------------------------------------------------------------------------
// Lockstep residual sinks (legacy mirror + role-move helpers)
// CodeQL #1587/#1588: .html(selected*RoleValue) on role transfer buttons
// ---------------------------------------------------------------------------
describe("lockstep residual .html sinks (source pattern)", () => {
  const legacySrc = readFileSync(
    resolve(
      __dirname,
      "../../main/webapp/cm/app/js/legacy/views/PercUserView.js"
    ),
    "utf8"
  );
  const mainSrc = readFileSync(
    resolve(__dirname, "../../main/webapp/cm/views/PercUserView.js"),
    "utf8"
  );

  it("legacy PercUserView does not use .html() for selected role transfer", () => {
    expect(legacySrc).not.toMatch(/\.html\(selectedAssignedRoleValue\)/);
    expect(legacySrc).not.toMatch(/\.html\(selectedAvailableRoleValue\)/);
    expect(legacySrc).toMatch(/\.text\(selectedAssignedRoleValue\)/);
    expect(legacySrc).toMatch(/\.text\(selectedAvailableRoleValue\)/);
  });

  it("main PercUserView remains free of selected-role .html sinks", () => {
    expect(mainSrc).not.toMatch(/\.html\(selectedAssignedRoleValue\)/);
    expect(mainSrc).not.toMatch(/\.html\(selectedAvailableRoleValue\)/);
  });
});
