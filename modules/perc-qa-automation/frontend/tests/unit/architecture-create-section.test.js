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
 * Unit tests for architecture-create-section helpers (#3589) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  TEST_IDS,
  SECTION_TITLE_PREFIX,
  architectureSpaUrl,
  siteListUrl,
  sectionTreeUrl,
  shouldRequireNavTree,
  firstSampleDemoSite,
  uniqueSectionTitle,
  uniqueSectionUrlName,
  isKnownArchitectureConsoleNoise,
  missingNavTreeFailMessage,
  SAMPLE_DEMO_SITE_NAMES,
} = require("../helpers/architecture-create-section");

describe("architecture-create-section helpers (#3589)", () => {
  it("exports stable product test ids", () => {
    assert.equal(TEST_IDS.actionCreate, "architecture-action-create");
    assert.equal(TEST_IDS.createDialog, "architecture-create-dialog");
    assert.equal(TEST_IDS.navTree, "architecture-nav-tree");
    assert.equal(SECTION_TITLE_PREFIX, "QA3589");
  });

  it("builds SPA and REST URLs without a trailing-slash double path", () => {
    const spa = architectureSpaUrl("http://127.0.0.1:9992/", {
      site: "Corporate_Investments",
    });
    assert.match(
      spa,
      /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=architecture&_=/,
    );
    assert.match(spa, /site=Corporate_Investments/);
    assert.equal(
      siteListUrl("http://127.0.0.1:9992/"),
      "http://127.0.0.1:9992/Rhythmyx/services/sitemanage/site/",
    );
    assert.equal(
      sectionTreeUrl("http://127.0.0.1:9992", "Corporate_Investments"),
      "http://127.0.0.1:9992/Rhythmyx/services/sitemanage/section/tree/Corporate_Investments",
    );
  });

  it("encodes site names in the tree URL", () => {
    assert.equal(
      sectionTreeUrl("http://127.0.0.1:9992", "A B"),
      "http://127.0.0.1:9992/Rhythmyx/services/sitemanage/section/tree/A%20B",
    );
  });

  it("requires NavTree on H2 demo-sites default and EXPECT_DEMO_SITES", () => {
    assert.equal(
      shouldRequireNavTree({ TEST_DB_TYPE: "h2" }),
      true,
    );
    assert.equal(
      shouldRequireNavTree({ TEST_DB_TYPE: "h2", DEMO_SITES: "true" }),
      true,
    );
    assert.equal(
      shouldRequireNavTree({ EXPECT_DEMO_SITES: "1" }),
      true,
    );
    assert.equal(
      shouldRequireNavTree({ TEST_DB_TYPE: "postgres" }),
      false,
    );
    assert.equal(
      shouldRequireNavTree({ TEST_DB_TYPE: "h2", DEMO_SITES: "0" }),
      false,
    );
  });

  it("picks the first seeded demo site in stock order", () => {
    assert.equal(
      firstSampleDemoSite([
        "BareSite",
        "Enterprise_Investments",
        "Corporate_Investments",
      ]),
      "Corporate_Investments",
    );
    assert.equal(firstSampleDemoSite(["BareSite"]), null);
    assert.equal(firstSampleDemoSite([]), null);
    assert.deepEqual(SAMPLE_DEMO_SITE_NAMES[0], "Corporate_Investments");
  });

  it("builds a unique title and a valid folder URL name", () => {
    const title = uniqueSectionTitle(1710000000000);
    assert.equal(title, "QA3589-1710000000000");
    assert.equal(uniqueSectionUrlName(title), "qa3589-1710000000000");
    assert.equal(uniqueSectionUrlName("QA 3589 Title!"), "qa-3589-title");
  });

  it("filters known console noise and describes a missing-tree fail", () => {
    assert.equal(
      isKnownArchitectureConsoleNoise("Failed to load resource: net::ERR_FAILED"),
      true,
    );
    assert.equal(isKnownArchitectureConsoleNoise("TypeError: boom"), false);
    assert.match(missingNavTreeFailMessage(), /#3352/);
    assert.match(missingNavTreeFailMessage(), /Do not skip/);
  });
});
