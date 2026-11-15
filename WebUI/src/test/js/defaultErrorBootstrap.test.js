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
 * Branded error page Bootstrap source pin (issue #6320).
 *
 * The orphan rx_resources Bootstrap 4.0.0 CSS copy
 * ({@code system/.../rx_resources/ApplicationFiles/css/bootstrap/4.0.0/})
 * is retired: the Jetty error page ({@code ui/default-error.html}, served
 * from the Rhythmyx webapp) and its classpath fallback
 * ({@code modules/utils/.../default-error.html}, served by
 * {@code PSRestApiAuthFilter}) now load the npm-tracked Bootstrap 5 CSS at
 * the same stable versionless jslib URL the #5661 overlay generates
 * (bootstrap ^5.3.8, Dependabot-tracked in
 * {@code WebUI/src/main/frontend/package.json}).
 *
 * Covers:
 *   - no rx_resources Bootstrap stylesheet reference remains.
 *   - the stylesheet link is the npm jslib bootstrap.min.css URL.
 *   - Bootstrap 4-only classes the page still uses (btn-block, text-left)
 *     keep local compat rules so the layout is pixel-stable on Bootstrap 5.
 *   - webapp and utils copies stay byte-identical.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const WEBAPP_ROOT = resolve(__dirname, "../../main/webapp");
const REPO_ROOT = resolve(__dirname, "../../../..");
const WEBAPP_PAGE = resolve(WEBAPP_ROOT, "ui/default-error.html");
const UTILS_PAGE = resolve(
  REPO_ROOT,
  "modules/utils/src/main/resources/default-error.html",
);
const NPM_CSS_URL =
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/bootstrap/css/bootstrap.min.css";

describe("default-error.html Bootstrap source (issue #6320)", () => {
  it("stays byte-identical across the webapp and utils copies", () => {
    const webapp = readFileSync(WEBAPP_PAGE, "utf8");
    const utils = readFileSync(UTILS_PAGE, "utf8");
    expect(utils, "utils fallback drifted from the webapp error page").toBe(
      webapp,
    );
  });

  it("references no rx_resources Bootstrap copy", () => {
    for (const [label, file] of [
      ["webapp", WEBAPP_PAGE],
      ["utils", UTILS_PAGE],
    ]) {
      const html = readFileSync(file, "utf8");
      expect(
        html,
        `${label} copy still references the retired 4.0.0 copy`,
      ).not.toMatch(/bootstrap\/4\.0\.0/);
      expect(
        html,
        `${label} copy still references an rx_resources Bootstrap copy`,
      ).not.toMatch(/rx_resources\/.*bootstrap/i);
    }
  });

  it("loads the npm-served Bootstrap 5 stylesheet", () => {
    for (const [label, file] of [
      ["webapp", WEBAPP_PAGE],
      ["utils", UTILS_PAGE],
    ]) {
      const html = readFileSync(file, "utf8");
      expect(
        html.includes(NPM_CSS_URL),
        `${label} copy missing the npm jslib Bootstrap CSS link`,
      ).toBe(true);
    }
  });

  it("keeps Bootstrap 4-only class compat rules for the BS5 stylesheet", () => {
    const html = readFileSync(WEBAPP_PAGE, "utf8");
    // btn-block and text-left were removed in Bootstrap 5; the page still
    // uses both, so local rules preserve the branded layout exactly.
    expect(html, "btn-block compat rule missing").toMatch(
      /\.btn-block\s*\{[^}]*width:\s*100%/,
    );
    expect(html, "text-left compat rule missing").toMatch(
      /\.text-left\s*\{[^}]*text-align:\s*left/,
    );
    expect(html.includes("btn-block"), "btn-block usage removed").toBe(true);
    expect(html.includes("text-left"), "text-left usage removed").toBe(true);
  });
});
