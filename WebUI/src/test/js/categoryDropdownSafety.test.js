/**
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Regression for categoryDropdown createSubCategorySelect
 * (js/html-constructed-from-input #1408/#1409).
 *
 * Pre-fix: $('<select id="' + id + '" name="' + name + '" />')
 * Post-fix: $("<select>").attr("id", id).attr("name", name)
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect, beforeEach, afterEach } from "vitest";
import jquery from "jquery";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC = resolve(
  __dirname,
  "../../../../modules/perc-packages/src/main/resources/Packages/perc.CategoryDropDownControl/SupportFile-rx_resources/widgets/categoryDropDown/js/categoryDropdown.js"
);

describe("categoryDropdown createSubCategorySelect", () => {
  let $;

  beforeEach(() => {
    let jq = jquery(globalThis.window);
    if (typeof jq !== "function") {
      jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    }
    $ = jq;
    globalThis.jQuery = $;
    globalThis.$ = $;
    document.body.innerHTML =
      '<div id="maindiv-testParam"></div><div id="datadisplay-testParam"></div>';
  });

  afterEach(() => {
    document.body.innerHTML = "";
    delete globalThis.jQuery;
    delete globalThis.$;
  });

  it("source no longer concatenates id/name into an HTML select string", () => {
    const src = readFileSync(SRC, "utf8");
    expect(src).not.toMatch(/\$\(\s*['"]<select id=['"]\s*\+/);
    expect(src).toMatch(/\$\(["']<select>["']\)/);
    expect(src).toMatch(/\.attr\(\s*["']id["']/);
    expect(src).toMatch(/\.attr\(\s*["']name["']/);
  });

  it("jQuery .attr API places hostile id/name into attributes, not markup injection", () => {
    const id = 'x"><img src=x onerror=alert(1)>';
    const name = 'n"><script>alert(1)</script>';
    const sel = $("<select>").attr("id", id).attr("name", name);
    $("#maindiv-testParam").append(sel);
    expect(document.querySelectorAll("img").length).toBe(0);
    expect(document.querySelectorAll("script").length).toBe(0);
    const el = $("#maindiv-testParam select")[0];
    expect(el.getAttribute("id")).toBe(id);
    expect(el.getAttribute("name")).toBe(name);
  });
});
