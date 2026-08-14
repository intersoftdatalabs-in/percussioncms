/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

import { describe, expect, it } from "vitest";
import {
  buildCreateSiteBody,
  DEFAULT_HOME_PAGE_TITLE,
  parseBaseTemplateList,
  parseCreatedSite,
  pickDefaultBaseTemplate,
  PLAIN_BASE_TEMPLATE_NAME,
  siteFolderPath,
} from "../../../main/ts/api/contentExplorer/siteCreateApi";

describe("siteCreateApi helpers (#3002)", () => {
  it("buildCreateSiteBody matches classic Site wrapper contract", () => {
    const body = buildCreateSiteBody({
      name: " QA-Site ",
      description: "demo",
      baseTemplateName: "perc.base.plain",
      templateName: "QA-SiteTemplate",
    });
    expect(body.Site.name).toBe("QA-Site");
    expect(body.Site.label).toBe("QA-Site");
    expect(body.Site.description).toBe("demo");
    expect(body.Site.homePageTitle).toBe(DEFAULT_HOME_PAGE_TITLE);
    expect(body.Site.navigationTitle).toBe(DEFAULT_HOME_PAGE_TITLE);
    expect(body.Site.baseTemplateName).toBe("perc.base.plain");
    expect(body.Site.templateName).toBe("QA-SiteTemplate");
    expect(body.Site.managedNavigation).toBe(true);
  });

  it("buildCreateSiteBody sends managedNavigation false when opted out", () => {
    const body = buildCreateSiteBody({
      name: "Bare",
      baseTemplateName: "perc.base.plain",
      templateName: "BareTemplate",
      managedNavigation: false,
    });
    expect(body.Site.managedNavigation).toBe(false);
  });

  it("parseCreatedSite accepts Site-wrapped and plain payloads", () => {
    expect(parseCreatedSite({ Site: { name: "A", id: "1" } })).toEqual({
      name: "A",
      id: "1",
      label: undefined,
    });
    expect(parseCreatedSite({ name: "B", label: "Bee" })).toEqual({
      name: "B",
      id: undefined,
      label: "Bee",
    });
  });

  it("parseCreatedSite accepts lowercase site wrapper (Jackson variant)", () => {
    expect(parseCreatedSite({ site: { name: "C", id: "3" } })).toEqual({
      name: "C",
      id: "3",
      label: undefined,
    });
  });

  it("parseCreatedSite accepts capitalized Name/Id/Label Jackson properties", () => {
    expect(parseCreatedSite({ Name: "D", Id: "4", Label: "Dee" })).toEqual({
      name: "D",
      id: "4",
      label: "Dee",
    });
  });

  it("parseCreatedSite rejects missing name", () => {
    expect(() => parseCreatedSite({})).toThrow(/missing site name/i);
  });

  it("parseBaseTemplateList unwraps known keys", () => {
    const list = parseBaseTemplateList({
      TemplateSummary: [
        { name: "perc.base.plain", id: "1" },
        { name: "other", label: "Other" },
      ],
    });
    expect(list).toHaveLength(2);
    expect(list[0]?.name).toBe("perc.base.plain");
    expect(list[1]?.label).toBe("Other");
  });

  it("parseBaseTemplateList skips non-string name rows without throwing", () => {
    const list = parseBaseTemplateList({
      TemplateSummary: [
        { name: 42, id: "bad" },
        { name: "ok", id: "good" },
        { Name: null, id: "also-bad" },
      ],
    });
    expect(list).toEqual([{ id: "good", name: "ok", label: undefined, thumbPath: undefined }]);
  });

  it("pickDefaultBaseTemplate prefers plain base", () => {
    expect(
      pickDefaultBaseTemplate([
        { name: "other" },
        { name: PLAIN_BASE_TEMPLATE_NAME },
      ]),
    ).toBe(PLAIN_BASE_TEMPLATE_NAME);
    expect(pickDefaultBaseTemplate([{ name: "only" }])).toBe("only");
    expect(pickDefaultBaseTemplate([])).toBe(PLAIN_BASE_TEMPLATE_NAME);
  });

  it("siteFolderPath builds product /Sites path", () => {
    expect(siteFolderPath("Acme")).toBe("/Sites/Acme");
    expect(siteFolderPath("/Acme/")).toBe("/Sites/Acme");
  });
});
