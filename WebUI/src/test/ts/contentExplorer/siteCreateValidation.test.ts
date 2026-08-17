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
  canSubmitCreateSite,
  clampSiteDescription,
  defaultTemplateNameForSite,
  filterSiteNameInput,
  filterTemplateNameInput,
  hidesManagedNavigation,
  isSiteCreateKindEnabled,
  managedNavigationForcedOn,
  requiresPageTemplate,
  SITE_DESCRIPTION_MAX_LENGTH,
  SITE_NAME_MAX_LENGTH,
  validateSiteName,
  validateTemplateName,
  validateVirtualRootPath,
  wizardStepsForKind,
} from "../../../main/ts/contentExplorer/wizards/siteCreateValidation";

describe("siteCreateValidation (#3002 / #3521)", () => {
  it("filterSiteNameInput keeps hostname-safe characters only", () => {
    expect(filterSiteNameInput("My Site!")).toBe("MySite");
    expect(filterSiteNameInput("a-b.c_d")).toBe("a-b.cd");
    expect(filterSiteNameInput("")).toBe("");
  });

  it("filterTemplateNameInput strips spaces and path separators", () => {
    expect(filterTemplateNameInput("My Template")).toBe("MyTemplate");
    expect(filterTemplateNameInput("a/b\\c")).toBe("abc");
  });

  it("validateSiteName requires non-empty hostname-safe name", () => {
    expect(validateSiteName("")).toEqual({ ok: false, reason: "empty" });
    expect(validateSiteName("  ")).toEqual({ ok: false, reason: "empty" });
    expect(validateSiteName("bad name")).toEqual({
      ok: false,
      reason: "invalidChars",
    });
    expect(validateSiteName("Good-Site.1")).toEqual({
      ok: true,
      name: "Good-Site.1",
    });
    const long = "a".repeat(SITE_NAME_MAX_LENGTH + 1);
    expect(validateSiteName(long)).toEqual({ ok: false, reason: "tooLong" });
  });

  it("validateTemplateName requires non-empty trimmed name", () => {
    expect(validateTemplateName("")).toEqual({ ok: false, reason: "empty" });
    expect(validateTemplateName("T1")).toEqual({ ok: true, name: "T1" });
  });

  it("defaultTemplateNameForSite seeds {name}Template", () => {
    expect(defaultTemplateNameForSite("Acme")).toBe("AcmeTemplate");
    expect(defaultTemplateNameForSite("")).toBe("SiteTemplate");
  });

  it("clampSiteDescription enforces product max length", () => {
    const long = "x".repeat(SITE_DESCRIPTION_MAX_LENGTH + 10);
    expect(clampSiteDescription(long).length).toBe(SITE_DESCRIPTION_MAX_LENGTH);
  });

  it("canSubmitCreateSite Traditional requires only a site name", () => {
    expect(
      canSubmitCreateSite({
        siteName: "A",
        siteType: "traditional",
      }),
    ).toBe(true);
    expect(
      canSubmitCreateSite({
        siteName: "",
        siteType: "traditional",
      }),
    ).toBe(false);
  });

  it("canSubmitCreateSite Page requires template name and base template", () => {
    expect(
      canSubmitCreateSite({
        siteName: "A",
        siteType: "page",
        templateName: "ATemplate",
        baseTemplateName: "perc.base.plain",
      }),
    ).toBe(true);
    expect(
      canSubmitCreateSite({
        siteName: "A",
        siteType: "page",
      }),
    ).toBe(false);
    expect(
      canSubmitCreateSite({
        siteName: "A",
        siteType: "page",
        templateName: "ATemplate",
        baseTemplateName: "  ",
      }),
    ).toBe(false);
  });

  it("canSubmitCreateSite Virtual requires name only; root is optional", () => {
    expect(
      canSubmitCreateSite({
        siteName: "Docs",
        siteType: "virtual",
      }),
    ).toBe(true);
    expect(
      canSubmitCreateSite({
        siteName: "Docs",
        siteType: "virtual",
        virtualRootPath: "/opt/Percussion",
      }),
    ).toBe(true);
    expect(
      canSubmitCreateSite({
        siteName: "Docs",
        siteType: "virtual",
        virtualRootPath: "../escape",
      }),
    ).toBe(false);
    expect(
      canSubmitCreateSite({
        siteName: "",
        siteType: "virtual",
      }),
    ).toBe(false);
  });

  it("validateVirtualRootPath allows blank and rejects traversal", () => {
    expect(validateVirtualRootPath("")).toEqual({ ok: true, path: null });
    expect(validateVirtualRootPath("  ")).toEqual({ ok: true, path: null });
    expect(validateVirtualRootPath("/opt/docs")).toEqual({
      ok: true,
      path: "/opt/docs",
    });
    expect(validateVirtualRootPath("C:/docs/..")).toEqual({
      ok: false,
      reason: "unsafe",
    });
  });

  it("kind helpers enable Traditional, Page, and Virtual", () => {
    expect(isSiteCreateKindEnabled("traditional")).toBe(true);
    expect(isSiteCreateKindEnabled("page")).toBe(true);
    expect(isSiteCreateKindEnabled("virtual")).toBe(true);
    expect(requiresPageTemplate("page")).toBe(true);
    expect(requiresPageTemplate("traditional")).toBe(false);
    expect(requiresPageTemplate("virtual")).toBe(false);
    expect(managedNavigationForcedOn("page")).toBe(true);
    expect(managedNavigationForcedOn("traditional")).toBe(false);
    expect(managedNavigationForcedOn("virtual")).toBe(false);
    expect(hidesManagedNavigation("virtual")).toBe(true);
    expect(hidesManagedNavigation("traditional")).toBe(false);
    expect(wizardStepsForKind("page")).toEqual([
      "type",
      "details",
      "template",
      "confirm",
      "progress",
    ]);
    expect(wizardStepsForKind("traditional")).toEqual([
      "type",
      "details",
      "confirm",
      "progress",
    ]);
    expect(wizardStepsForKind("virtual")).toEqual([
      "type",
      "details",
      "confirm",
      "progress",
    ]);
  });
});
