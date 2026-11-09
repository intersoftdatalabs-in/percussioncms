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
 * codemirror-helpers.mjs
 *
 * Shared helpers for the npm-managed CodeMirror 5 bundle (issue #5784).
 *
 * The classic template editor (`editTemplate_js.jsp`) and the `sys_TextArea`
 * code-editor control (`sys_Templates.xsl`) consume the bundle through the
 * browser global `CodeMirror`. The UMD wrapper inside
 * `codemirror/lib/codemirror.js` assigns to its bundler module record instead
 * of the browser global when bundled, so the entry exposes the shared
 * instance explicitly. The import is unwrapped defensively: depending on the
 * esbuild CJS/ESM interop path the constructor may sit one namespace level
 * deep.
 *
 * Kept in a separate module (rather than inline in the entry) so the
 * unwrapping contract is unit-tested by
 * `src/test/js/codemirror-bundle.test.js`.
 */

/**
 * Unwraps the CodeMirror 5 constructor out of an esbuild module record.
 *
 * @param {*} moduleRecord the `import * as …` namespace (or any level of
 *   `{ default: … }` nesting around the constructor).
 * @returns {Function|*} the constructor function when found, otherwise the
 *   deepest unwrapped candidate.
 */
export function unwrapCodeMirror(moduleRecord) {
  let candidate =
    moduleRecord && moduleRecord.default !== undefined
      ? moduleRecord.default
      : moduleRecord;
  while (
    candidate &&
    typeof candidate === "object" &&
    typeof candidate.default === "function"
  ) {
    candidate = candidate.default;
  }
  return candidate;
}

/**
 * Markers the built `codemirror.bundle.js` must contain. They prove the
 * htmlmixed mode plus the search/matchbrackets addons the classic consumers
 * rely on survived tree-shaking/minification.
 */
export const CODEMIRROR_BUNDLE_MARKERS = [
  "htmlmixed",
  "searchcursor",
  "matchBrackets",
];

/**
 * The CodeMirror major line this bundle tracks. Pinned to 5 until issue
 * #5701 lands the CodeMirror 6 replacement; the vitest suite fails the build
 * if the npm dependency drifts off this line.
 */
export const CODEMIRROR_PINNED_MAJOR = 5;
