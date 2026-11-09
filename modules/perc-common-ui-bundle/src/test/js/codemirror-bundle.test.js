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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Tests for the CodeMirror 5 npm bundle contract (issue #5784).
 *
 * The classic template editor (`editTemplate_js.jsp`) and the `sys_TextArea`
 * code-editor control (`sys_Templates.xsl`) load
 * `/sys_resources/codemirror/codemirror/codemirror.bundle.js` built by
 * `build-scripts/bundle-codemirror.mjs` from the `codemirror` npm package.
 * These tests pin the two halves of that contract that a plain build would
 * only catch at runtime on a live CMS:
 *
 * 1. The bundled dependency stays on the CodeMirror 5 line until issue #5701
 *    lands the CodeMirror 6 replacement (a CM6 install changes the entry
 *    imports and the global-constructor contract).
 * 2. `unwrapCodeMirror` recovers the constructor through every level of
 *    esbuild CJS/ESM interop nesting the entry can observe, so
 *    `window.CodeMirror` keeps working for the legacy consumers.
 */

import { createRequire } from "module";
import { describe, it, expect } from "vitest";
import {
  unwrapCodeMirror,
  CODEMIRROR_BUNDLE_MARKERS,
  CODEMIRROR_PINNED_MAJOR,
} from "../../../build-scripts/codemirror-helpers.mjs";

const require = createRequire(import.meta.url);

describe("codemirror npm dependency line", () => {
  it("stays on the pinned CodeMirror major until #5701 lands CM6", () => {
    const pkg = require("codemirror/package.json");
    const major = Number(String(pkg.version).split(".")[0]);
    expect(major).toBe(CODEMIRROR_PINNED_MAJOR);
  });

  it("publishes the bundle markers the build sanity-check requires", () => {
    expect(CODEMIRROR_BUNDLE_MARKERS).toEqual([
      "htmlmixed",
      "searchcursor",
      "matchBrackets",
    ]);
  });
});

describe("unwrapCodeMirror", () => {
  it("returns a bare constructor unchanged", () => {
    function CodeMirror() {}
    expect(unwrapCodeMirror(CodeMirror)).toBe(CodeMirror);
  });

  it("unwraps one level of esbuild default interop", () => {
    function CodeMirror() {}
    expect(unwrapCodeMirror({ default: CodeMirror })).toBe(CodeMirror);
  });

  it("unwraps nested default objects down to the constructor", () => {
    function CodeMirror() {}
    const nested = { default: { default: CodeMirror } };
    expect(unwrapCodeMirror(nested)).toBe(CodeMirror);
  });

  it("prefers the default export over the namespace record", () => {
    function CodeMirror() {}
    const record = { default: CodeMirror, extra: 1 };
    expect(unwrapCodeMirror(record)).toBe(CodeMirror);
  });

  it("passes through nullish records without throwing", () => {
    expect(unwrapCodeMirror(null)).toBeNull();
    expect(unwrapCodeMirror(undefined)).toBeUndefined();
  });
});
