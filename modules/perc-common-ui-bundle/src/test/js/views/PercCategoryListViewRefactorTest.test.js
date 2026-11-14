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
 * Behavioral regression tests for {@code PercCategoryListView}.
 *
 * <p>Issue #6195 flagged {@code updateCategoryList} for high cognitive
 * complexity (29 vs the 15 budget), so the function was decomposed into
 * small helpers ({@code readListQuery}, {@code resolveBaseURL},
 * {@code updateOneCategoryList}, {@code handleCategoryResponse},
 * {@code renderCategoryEntries}, {@code renderCollapsibleTree},
 * {@code renderExpandedTree}). These tests pin the observable behavior of
 * the refactored code: empty queries are skipped, expanded and collapsible
 * trees render, and service errors leave the DOM untouched while logging.
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
  resolve(ROOT, "src/main/js/views/PercCategoryListView.js"),
  "utf8",
);

const ENTRIES = [
  {
    category: "News",
    count: { first: 3 },
    children: [{ category: "Local", count: { first: 1 } }],
  },
  { category: "Sports", count: { first: 0 } },
];

function queryFor(display) {
  return JSON.stringify({
    category_page_result: "/cat/result",
    display_option: display,
    max_categories: 0,
    criteria: [],
  });
}

// Mutable service behavior, driven per test.
let serviceImpl = (query, callback) => callback(true, JSON.stringify(ENTRIES));

beforeAll(() => {
  // FancyTree is a browser plugin not present in jsdom; stub it so the
  // collapsible render path can run without the real widget.
  $.fn.fancytree = function () {
    return this;
  };
  // Stub the category service before the view evaluates: the view's
  // $(document).ready() hook fires on load and must find the service.
  $.PercCategoryListService = {
    getCategories: (query, callback) => serviceImpl(query, callback),
  };
  eval(utilsCode); // eslint-disable-line no-eval
  eval(viewCode); // eslint-disable-line no-eval
});

beforeEach(() => {
  serviceImpl = (query, callback) => callback(true, JSON.stringify(ENTRIES));
  document.body.innerHTML = "";
  vi.restoreAllMocks();
});

describe("PercCategoryListView - updateCategoryList behavior (issue #6195)", () => {
  it("skips elements with an empty data-query without calling the service", () => {
    document.body.innerHTML =
      "<div class='perc-category-list' data-query=''>" +
      "<div class='perc-category-list-expanded'><ul><li>original</li></ul></div>" +
      "</div>";
    const spy = vi.fn((query, callback) => callback(true, JSON.stringify(ENTRIES)));
    serviceImpl = spy;

    $.PercCategoryListView.updateCategoryList();

    expect(spy).not.toHaveBeenCalled();
    expect(
      document.querySelector(".perc-category-list-expanded li").textContent,
    ).toBe("original");
  });

  it("renders the expanded tree with names, counts, and result links", () => {
    document.body.innerHTML =
      "<div class='perc-category-list' data-query='" +
      queryFor("expanded") +
      "'><div class='perc-category-list-expanded'></div></div>";

    $.PercCategoryListView.updateCategoryList();

    const anchors = Array.from(
      document.querySelectorAll(
        ".perc-category-list-expanded ul.perc-category-elements a.perc-node",
      ),
    );
    expect(anchors.map((a) => a.textContent)).toEqual([
      "News (3)",
      "Local (1)",
      "Sports",
    ]);
    const newsHref = anchors[0].getAttribute("href");
    expect(newsHref).toContain("/cat/result?filter=News");
    expect(newsHref).not.toMatch(/^javascript:/i);
  });

  it("renders the collapsible tree into the collapsible container", () => {
    document.body.innerHTML =
      "<div class='perc-category-list' data-query='" +
      queryFor("collapsible") +
      "'><div class='perc-category-list-collapsible'></div></div>";

    $.PercCategoryListView.updateCategoryList();

    const anchors = Array.from(
      document.querySelectorAll(
        ".perc-category-list-collapsible ul.perc-category-elements a.perc-node",
      ),
    );
    expect(anchors.map((a) => a.textContent)).toEqual([
      "News (3)",
      "Local (1)",
      "Sports",
    ]);
  });

  it("leaves the DOM untouched and logs on service error", () => {
    document.body.innerHTML =
      "<div class='perc-category-list' data-query='" +
      queryFor("expanded") +
      "'><div class='perc-category-list-expanded'><ul><li>original</li></ul></div></div>";
    serviceImpl = (query, callback) => callback(false, null);
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    $.PercCategoryListView.updateCategoryList();

    expect(errorSpy).toHaveBeenCalled();
    expect(
      document.querySelector(".perc-category-list-expanded li").textContent,
    ).toBe("original");
  });

  it("renders nothing new when the service returns zero entries", () => {
    document.body.innerHTML =
      "<div class='perc-category-list' data-query='" +
      queryFor("expanded") +
      "'><div class='perc-category-list-expanded'></div></div>";
    serviceImpl = (query, callback) => callback(true, '[]');

    $.PercCategoryListView.updateCategoryList();

    expect(
      document.querySelector(".perc-category-list-expanded").innerHTML,
    ).toBe("");
  });
});
