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
 * Unit tests for Design SPA surface helpers (#3307) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  CLASSIC_ASSIGNED_TEMPLATES_ID,
  SKIP,
  designTemplatesUrl,
  designLegacyAdminUrl,
  designLegacyViewUrl,
  skipReasonForChrome,
  filterConsoleNoise,
} = require("../helpers/design-spa-surface");

describe("design-spa-surface helpers (#3307)", () => {
  it("exports stable Design SPA test ids", () => {
    assert.equal(TEST_IDS.shell, "perc-design-shell");
    assert.equal(TEST_IDS.nav, "nav-design");
    assert.equal(TEST_IDS.tabTemplates, "tab-design-templates");
    assert.equal(TEST_IDS.panel, "design-tpl-panel");
    assert.equal(TEST_IDS.create, "design-tpl-create");
    assert.equal(TEST_IDS.editor, "design-tpl-editor");
    assert.equal(CLASSIC_ASSIGNED_TEMPLATES_ID, "perc-assigned-templates");
  });

  it("builds Design templates SPA URL with cache buster", () => {
    const url = designTemplatesUrl("http://127.0.0.1:9992/");
    assert.match(
      url,
      /^http:\/\/127\.0\.0\.1:9992\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=design&section=templates&_=\d+$/,
    );
  });

  it("builds classic Design list URLs with cache buster", () => {
    assert.match(
      designLegacyAdminUrl("http://127.0.0.1:2050"),
      /^http:\/\/127\.0\.0\.1:2050\/Rhythmyx\/cm\/app\/admin\.jsp\?_=\d+$/,
    );
    assert.match(
      designLegacyViewUrl("http://127.0.0.1:2050/"),
      /^http:\/\/127\.0\.0\.1:2050\/Rhythmyx\/cm\/app\/\?view=design&_=\d+$/,
    );
  });

  it("skips when Design shell chrome is absent", () => {
    assert.equal(
      skipReasonForChrome({ shellPresent: false }),
      SKIP.SHELL,
    );
  });

  it("skips create when sibling #3305 chrome is absent", () => {
    assert.equal(
      skipReasonForChrome({
        shellPresent: true,
        wantCreate: true,
        createPresent: false,
      }),
      SKIP.CREATE,
    );
  });

  it("skips editor when catalog is empty", () => {
    assert.equal(
      skipReasonForChrome({
        shellPresent: true,
        wantEdit: true,
        catalogEmpty: true,
      }),
      SKIP.EDIT_EMPTY,
    );
  });

  it("skips redirect when sibling #3306 cutover is absent", () => {
    assert.equal(
      skipReasonForChrome({
        shellPresent: true,
        wantRedirect: true,
        redirectToSpa: false,
      }),
      SKIP.REDIRECT,
    );
  });

  it("runs when requested chrome is present", () => {
    assert.equal(
      skipReasonForChrome({
        shellPresent: true,
        wantCreate: true,
        createPresent: true,
      }),
      null,
    );
    assert.equal(
      skipReasonForChrome({
        shellPresent: true,
        wantEdit: true,
        catalogEmpty: false,
      }),
      null,
    );
    assert.equal(
      skipReasonForChrome({
        shellPresent: true,
        wantRedirect: true,
        redirectToSpa: true,
      }),
      null,
    );
  });

  it("filters known console noise", () => {
    assert.deepEqual(
      filterConsoleNoise([
        "Download the React DevTools for a better development experience",
        "Uncaught TypeError: boom",
        "Failed to load resource: net::ERR_FAILED",
      ]),
      ["Uncaught TypeError: boom"],
    );
  });
});
