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
 * Unit tests for the shared DTS cell helpers (#6719). No live CMS, no
 * DTS cell, and no seeded fixtures required:
 * {@code node --test tests/unit/dts-cell.test.js} (wired into
 * {@code npm run test:unit}).
 */

"use strict";

const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");
const { describe, it } = require("node:test");

const {
  SKIP_NO_DTS_BASE,
  SKIP_NO_DTS_HTTPS,
  dtsBaseUrl,
  dtsHttpsBaseUrl,
  openDtsHttpsContext,
} = require("../helpers/dts-cell");

const FRONTEND_DIR = path.join(__dirname, "..");
const HELPERS_DIR = path.join(FRONTEND_DIR, "helpers");

const BASE_ENVS = ["DTS_BASE_URL", "DTS_URL"];
const HTTPS_ENVS = ["DTS_HTTPS_URL", "DTS_HTTPS_HOST_PORT", "DTS_HTTPS_PORT"];

function savedEnv(names) {
  const saved = {};
  for (const name of names) {
    saved[name] = process.env[name];
    delete process.env[name];
  }
  return saved;
}

function restoreEnv(saved) {
  for (const [name, value] of Object.entries(saved)) {
    if (value === undefined) {
      delete process.env[name];
    } else {
      process.env[name] = value;
    }
  }
}

/** Sentinel thrown by the fake {@code test.skip} when the skip engages. */
class SkipSignal extends Error {}

function fakeTest(calls) {
  return {
    skip(condition, reason) {
      calls.push({ condition: !!condition, reason: String(reason) });
      if (condition) {
        throw new SkipSignal(String(reason));
      }
    },
  };
}

function fakeBaseRequest(calls, { answer = true } = {}) {
  const context = {
    get: async () => {
      calls.push("get");
      if (!answer) {
        throw new Error("connect ECONNREFUSED 127.0.0.1:18443");
      }
      return { ok: () => true };
    },
    dispose: async () => {
      calls.push("dispose");
    },
  };
  return {
    newContext: async (options) => {
      calls.push(["newContext", options]);
      return context;
    },
  };
}

describe("dts-cell shared helpers (#6719)", () => {
  it("resolves the DTS origin from DTS_BASE_URL and accepts the DTS_URL banner alias", () => {
    const saved = savedEnv(BASE_ENVS);
    try {
      assert.equal(dtsBaseUrl(), null, "no cell origin without env");
      process.env.DTS_URL = "http://127.0.0.1:9983/";
      assert.equal(
        dtsBaseUrl().origin,
        "http://127.0.0.1:9983",
        "banner DTS_URL resolves the cell (the #6719 all-skip trap)",
      );
      process.env.DTS_BASE_URL = "http://127.0.0.1:9993/";
      assert.equal(
        dtsBaseUrl().origin,
        "http://127.0.0.1:9993",
        "DTS_BASE_URL wins over the alias when both are set",
      );
      delete process.env.DTS_BASE_URL;
      assert.equal(
        dtsBaseUrl().origin,
        "http://127.0.0.1:9983",
        "alias still resolves once the primary is unset",
      );
      assert.ok(SKIP_NO_DTS_BASE.includes("DTS_BASE_URL"), "skip names the primary env");
      assert.ok(SKIP_NO_DTS_BASE.includes("DTS_URL"), "skip names the banner alias");
    } finally {
      restoreEnv(saved);
    }
  });

  it("resolves the DTS HTTPS writer origin only from the HTTPS env", () => {
    const saved = savedEnv(HTTPS_ENVS);
    try {
      assert.equal(dtsHttpsBaseUrl(), null, "no HTTPS origin without env");
      process.env.DTS_HTTPS_PORT = "8443";
      assert.equal(
        dtsHttpsBaseUrl().origin,
        "https://127.0.0.1:8443",
        "port env builds the loopback HTTPS origin",
      );
      process.env.DTS_HTTPS_HOST_PORT = "9443";
      assert.equal(
        dtsHttpsBaseUrl().origin,
        "https://127.0.0.1:9443",
        "host port env wins over the bare port",
      );
      process.env.DTS_HTTPS_URL = "https://dts.example:9443";
      assert.equal(
        dtsHttpsBaseUrl().origin,
        "https://dts.example:9443",
        "explicit URL wins over the port env",
      );
      assert.ok(SKIP_NO_DTS_HTTPS.includes("DTS_HTTPS_URL"), "https skip names the missing env");
      assert.ok(
        SKIP_NO_DTS_HTTPS.includes("DTS_HTTPS_HOST_PORT"),
        "https skip names the host-port fallback",
      );
    } finally {
      restoreEnv(saved);
    }
  });

  it("openDtsHttpsContext skips by name without creating a context when no HTTPS origin is published", async () => {
    const saved = savedEnv(HTTPS_ENVS);
    try {
      const skipCalls = [];
      const requestCalls = [];
      let signal = null;
      try {
        await openDtsHttpsContext(fakeTest(skipCalls), fakeBaseRequest(requestCalls), {
          label: "probe (Probe)",
        });
      } catch (error) {
        signal = error;
      }
      assert.ok(signal instanceof SkipSignal, "skips when no HTTPS origin is published");
      assert.deepEqual(requestCalls, [], "no request context is created on the no-origin skip");
      assert.equal(skipCalls.length, 1);
      assert.ok(skipCalls[0].reason.includes("DTS_HTTPS_URL"), "skip names the missing env");
      assert.ok(skipCalls[0].reason.startsWith("probe (Probe):"), "skip carries the suite label");
    } finally {
      restoreEnv(saved);
    }
  });

  it("openDtsHttpsContext disposes before skipping on an unanswered origin (the writer-context leak)", async () => {
    const saved = savedEnv(HTTPS_ENVS);
    try {
      process.env.DTS_HTTPS_URL = "https://127.0.0.1:18443";
      const skipCalls = [];
      const requestCalls = [];
      let signal = null;
      try {
        await openDtsHttpsContext(fakeTest(skipCalls), fakeBaseRequest(requestCalls, { answer: false }), {
          label: "probe (Probe)",
        });
      } catch (error) {
        signal = error;
      }
      assert.ok(signal instanceof SkipSignal, "skips when the origin never answers");
      assert.deepEqual(
        requestCalls.map((call) => (Array.isArray(call) ? call[0] : call)),
        ["newContext", "get", "dispose"],
        "the opener probes the origin, then disposes before the skip throws (leak fix)",
      );
      assert.deepEqual(
        requestCalls[0][1],
        { ignoreHTTPSErrors: true },
        "the writer context allows the shipped/self-signed cell chain",
      );
      const lastSkip = skipCalls[skipCalls.length - 1];
      assert.ok(lastSkip.reason.includes("no answer"), "skip names the unanswered origin");
    } finally {
      restoreEnv(saved);
    }
  });

  it("openDtsHttpsContext returns the origin and context when the origin answers", async () => {
    const saved = savedEnv(HTTPS_ENVS);
    try {
      process.env.DTS_HTTPS_URL = "https://127.0.0.1:18443";
      const skipCalls = [];
      const requestCalls = [];
      const opened = await openDtsHttpsContext(
        fakeTest(skipCalls),
        fakeBaseRequest(requestCalls, { answer: true }),
        { label: "probe (Probe)" },
      );
      assert.equal(opened.httpsBase.origin, "https://127.0.0.1:18443");
      assert.ok(opened.httpsRequest, "a request context is returned for the caller to dispose");
      assert.ok(requestCalls.includes("get"), "the origin is probed");
      assert.ok(!requestCalls.includes("dispose"), "an answering origin is not disposed by the opener");
      assert.ok(
        skipCalls.every((call) => call.condition === false),
        "no skip engages when the origin answers",
      );
    } finally {
      restoreEnv(saved);
    }
  });

  it("is the single choke point: the four matrix helpers delegate instead of duplicating", () => {
    for (const name of [
      "forms-confirmation-matrix.js",
      "feeds-entries-matrix.js",
      "metadata-index-matrix.js",
      "comments-moderation-matrix.js",
      "published-widget-surfaces.js",
    ]) {
      const src = fs.readFileSync(path.join(HELPERS_DIR, name), "utf8");
      assert.ok(src.includes('require("./dts-cell")'), `${name} requires the shared cell helper`);
      assert.ok(
        !src.includes("process.env.DTS_HTTPS_"),
        `${name} reads no DTS HTTPS env directly (hoisted, #6719)`,
      );
      assert.ok(
        !src.includes("process.env.DTS_BASE_URL") && !src.includes("process.env.DTS_URL"),
        `${name} reads no DTS origin env directly (aliased in dts-cell, #6719)`,
      );
    }
  });

  it("is used by the matrix specs: no per-spec httpsWriterContext copies remain", () => {
    for (const name of [
      "feeds-entries-matrix.spec.js",
      "metadata-index-matrix.spec.js",
      "comments-moderation-matrix.spec.js",
      "forms-confirmation-matrix.spec.js",
    ]) {
      const spec = fs.readFileSync(path.join(FRONTEND_DIR, name), "utf8");
      assert.ok(spec.includes("openDtsHttpsContext"), `${name} opens the shared HTTPS writer context`);
      assert.ok(
        !spec.includes("baseRequest.newContext"),
        `${name} keeps no per-spec request-context copy (leak surface, #6719)`,
      );
    }
  });
});
