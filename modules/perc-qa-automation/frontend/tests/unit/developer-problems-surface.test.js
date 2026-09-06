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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Unit tests for Developer Problems panel surface helpers (#4345).
 * No live CMS.
 *
 * Run: npm run test:unit  (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  INVALID_SESSION_FIXTURE,
  developerProblemsUrl,
  developerProblemsRestUrl,
  unwrapDesignProblems,
  unexpectedConsoleErrors,
} = require("../helpers/developer-problems-surface");

describe("developerProblemsUrl", () => {
  it("builds Developer Problems SPA URL with cache buster", () => {
    const url = developerProblemsUrl("http://127.0.0.1:9992/");
    assert.match(
      url,
      /^http:\/\/127\.0\.0\.1:9992\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=developer&section=problems&_=\d+$/,
    );
  });

  it("does not invent a filesystem path in the SPA query", () => {
    const url = developerProblemsUrl("http://127.0.0.1:1");
    assert.equal(url.includes(".."), false);
    assert.equal(url.includes("C:"), false);
  });
});

describe("developerProblemsRestUrl", () => {
  it("defaults to Rhythmyx context", () => {
    const prev = process.env.CMS_WEBAPP_CONTEXT;
    delete process.env.CMS_WEBAPP_CONTEXT;
    try {
      assert.equal(
        developerProblemsRestUrl("http://127.0.0.1:9992"),
        "http://127.0.0.1:9992/Rhythmyx/services/problems",
      );
    } finally {
      if (prev === undefined) {
        delete process.env.CMS_WEBAPP_CONTEXT;
      } else {
        process.env.CMS_WEBAPP_CONTEXT = prev;
      }
    }
  });

  it("honors empty CMS_WEBAPP_CONTEXT as root context", () => {
    const prev = process.env.CMS_WEBAPP_CONTEXT;
    process.env.CMS_WEBAPP_CONTEXT = "";
    try {
      assert.equal(
        developerProblemsRestUrl("http://127.0.0.1:9992/"),
        "http://127.0.0.1:9992/services/problems",
      );
    } finally {
      if (prev === undefined) {
        delete process.env.CMS_WEBAPP_CONTEXT;
      } else {
        process.env.CMS_WEBAPP_CONTEXT = prev;
      }
    }
  });
});

describe("unwrapDesignProblems", () => {
  it("accepts a bare array of catalog ids", () => {
    const list = unwrapDesignProblems([
      { id: "invalid-session", message: "Open editor is missing a required name." },
    ]);
    assert.equal(list.length, 1);
    assert.equal(list[0].id, INVALID_SESSION_FIXTURE);
  });

  it("unwraps Jackson DesignProblem envelope", () => {
    const list = unwrapDesignProblems({
      DesignProblem: { id: "invalid-session", message: "missing" },
    });
    assert.equal(list.length, 1);
    assert.equal(list[0].id, "invalid-session");
  });

  it("skips unsafe ids (traversal / empty)", () => {
    assert.deepEqual(unwrapDesignProblems([{ id: "../x" }]), []);
    assert.deepEqual(unwrapDesignProblems([{ id: "" }]), []);
    assert.deepEqual(unwrapDesignProblems(null), []);
  });
});

describe("unexpectedConsoleErrors", () => {
  it("ignores favicon and failed-resource noise", () => {
    assert.deepEqual(
      unexpectedConsoleErrors([
        "Failed to load resource: the server responded with a status of 404",
        "favicon.ico",
        "TypeError: boom",
      ]),
      ["TypeError: boom"],
    );
  });
});

describe("TEST_IDS", () => {
  it("uses Problems panel test ids (not pipeline Problems)", () => {
    assert.equal(TEST_IDS.panel, "developer-prob-panel");
    assert.equal(TEST_IDS.table, "developer-prob-table");
    assert.equal(TEST_IDS.tab, "tab-developer-problems");
  });
});
