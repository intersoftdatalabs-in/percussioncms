/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it } from "vitest";
import { applyEntryQueryToPath } from "../../../../main/ts/app/App";
import {
  parseClientPath,
  parseEntryQuery,
  resolveSpaReturnFromLocation,
  toSpaEntryUrl,
} from "../../../../main/ts/app/deepLinks/parseEntryQuery";
import {
  detectSpaBasename,
  pathAfterBasename,
} from "../../../../main/ts/app/deepLinks/spaBasename";

describe("parseEntryQuery", () => {
  it("defaults to home", () => {
    expect(parseEntryQuery("")).toEqual({
      entry: "home",
      clientPath: "/home",
    });
  });

  it("maps home section and aliases", () => {
    expect(parseEntryQuery("?entry=home&section=library").clientPath).toBe(
      "/home/library",
    );
    expect(parseEntryQuery("?entry=home&initialScreen=list").section).toBe(
      "recent",
    );
  });

  it("maps publish with ids", () => {
    const p = parseEntryQuery(
      "?entry=publish&section=logs&siteId=site1&serverId=srv2",
    );
    expect(p.entry).toBe("publish");
    expect(p.clientPath).toContain("/publish/logs");
    expect(p.siteId).toBe("site1");
    expect(p.serverId).toBe("srv2");
  });

  it("rejects bad ids", () => {
    const p = parseEntryQuery("?entry=publish&siteId=bad/id");
    expect(p.siteId).toBeUndefined();
  });

  it("maps widgetbuilder alias", () => {
    expect(parseEntryQuery("?entry=widgetbuilder").entry).toBe("widget-builder");
    expect(parseEntryQuery("?entry=widgetbuilder").clientPath).toBe(
      "/widget-builder",
    );
  });

  it("maps developer entry and section aliases", () => {
    expect(parseEntryQuery("?entry=developer").clientPath).toBe("/developer");
    expect(
      parseEntryQuery("?entry=developer&section=content-types").clientPath,
    ).toBe("/developer/content-types");
    expect(parseEntryQuery("?entry=developer&section=contenttypes").section).toBe(
      "content-types",
    );
    expect(parseClientPath("/developer/pipelines").entry).toBe("developer");
    expect(parseClientPath("/developer/pipelines").section).toBe("pipelines");
  });

  it("maps design entry and section aliases (#2808)", () => {
    expect(parseEntryQuery("?entry=design").entry).toBe("design");
    expect(parseEntryQuery("?entry=design").clientPath).toBe("/design");
    expect(
      parseEntryQuery("?entry=design&section=templates").clientPath,
    ).toBe("/design/templates");
    expect(parseEntryQuery("?entry=design&section=library").section).toBe(
      "templates",
    );
    expect(parseClientPath("/design/templates").entry).toBe("design");
    expect(parseClientPath("/design/templates").section).toBe("templates");
  });

  it("maps architecture, arch, and navigation aliases plus site (#3094/#3219)", () => {
    expect(parseEntryQuery("?entry=architecture").entry).toBe("architecture");
    expect(parseEntryQuery("?entry=architecture").clientPath).toBe(
      "/architecture",
    );
    expect(parseEntryQuery("?entry=arch").entry).toBe("architecture");
    expect(parseEntryQuery("?entry=navigation").entry).toBe("architecture");
    expect(parseEntryQuery("?entry=navigation").clientPath).toBe(
      "/architecture",
    );
    expect(parseClientPath("/navigation").entry).toBe("architecture");
    expect(parseEntryQuery("?entry=architecture&site=Demo").site).toBe("Demo");
    expect(parseEntryQuery("?entry=architecture&site=Demo").clientPath).toBe(
      "/architecture/Demo",
    );
    expect(parseClientPath("/architecture").entry).toBe("architecture");
    expect(parseClientPath("/architecture/Demo").site).toBe("Demo");
    expect(parseClientPath("/architecture/Demo").clientPath).toBe(
      "/architecture/Demo",
    );
    expect(parseClientPath("/navigation/Demo").entry).toBe("architecture");
    expect(parseClientPath("/navigation/Demo").site).toBe("Demo");
    expect(parseClientPath("/navigation/Demo").clientPath).toBe(
      "/architecture/Demo",
    );
  });

  it("unknown entry falls back to home", () => {
    expect(parseEntryQuery("?entry=nope").entry).toBe("home");
  });

  it("maps profile entry to /profile client path", () => {
    expect(parseEntryQuery("?entry=profile").entry).toBe("profile");
    expect(parseEntryQuery("?entry=profile").clientPath).toBe("/profile");
    expect(parseClientPath("/profile").entry).toBe("profile");
    expect(parseClientPath("/profile").clientPath).toBe("/profile");
  });

  it("maps assembly entry to chrome-less /assembly with ids", () => {
    const p = parseEntryQuery(
      "?entry=assembly&contentId=42&templateId=7",
    );
    expect(p.entry).toBe("assembly");
    expect(p.contentId).toBe("42");
    expect(p.templateId).toBe("7");
    expect(p.clientPath).toBe("/assembly?contentId=42&templateId=7");
    expect(parseClientPath("/assembly", "?contentId=42&templateId=7").entry).toBe(
      "assembly",
    );
    expect(
      toSpaEntryUrl({
        entry: "assembly",
        contentId: "42",
        templateId: "7",
        clientPath: "/assembly?contentId=42&templateId=7",
      }),
    ).toBe(
      "/cm/app/spa.jsp?entry=assembly&contentId=42&templateId=7",
    );
  });

  it("toSpaEntryUrl rebuilds query contract", () => {
    const url = toSpaEntryUrl({
      entry: "home",
      section: "library",
      clientPath: "/home/library",
    });
    expect(url).toBe("/cm/app/spa.jsp?entry=home&section=library");
  });
});

describe("path-based SPA URLs (PR-9)", () => {
  it("detectSpaBasename handles context prefix and dual-tree", () => {
    expect(detectSpaBasename("/cm/app/home")).toBe("/cm/app");
    expect(detectSpaBasename("/Rhythmyx/cm/app/spa.jsp")).toBe(
      "/Rhythmyx/cm/app",
    );
    expect(detectSpaBasename("/Rhythmyx/cm/pages/app/publish")).toBe(
      "/Rhythmyx/cm/pages/app",
    );
  });

  it("pathAfterBasename strips spa.jsp and keeps client path", () => {
    expect(pathAfterBasename("/cm/app/spa.jsp", "/cm/app")).toBe("");
    expect(pathAfterBasename("/cm/app/home/library", "/cm/app")).toBe(
      "/home/library",
    );
    expect(
      pathAfterBasename("/Rhythmyx/cm/app/explorer", "/Rhythmyx/cm/app"),
    ).toBe("/explorer");
  });

  it("parseClientPath maps path segments to entry", () => {
    expect(parseClientPath("/home/gadgets").entry).toBe("home");
    expect(parseClientPath("/home/gadgets").section).toBe("gadgets");
    expect(parseClientPath("/publish/logs", "?siteId=s1").siteId).toBe("s1");
    // Legacy /workflow/* still parses, client path folds into Admin (#3088)
    expect(parseClientPath("/workflow/users").tab).toBe("users");
    expect(parseClientPath("/workflow/users").clientPath).toBe("/admin/users");
    expect(parseClientPath("/admin/tools").clientPath).toBe("/admin/tools");
    expect(parseClientPath("/admin/roles").tab).toBe("roles");
    expect(
      parseClientPath("/explorer", "?path=/Sites/demo").path,
    ).toBe("/Sites/demo");
  });

  it("workflow entry maps to unified Admin client paths (#3088)", () => {
    expect(parseEntryQuery("?entry=workflow").clientPath).toBe(
      "/admin/workflow",
    );
    expect(parseEntryQuery("?entry=workflow&tab=roles").clientPath).toBe(
      "/admin/roles",
    );
    expect(parseEntryQuery("?entry=workflow&tab=roles").tab).toBe("roles");
  });

  it("resolveSpaReturnFromLocation prefers query then path", () => {
    expect(
      resolveSpaReturnFromLocation(
        "/cm/app/spa.jsp",
        "?entry=publish&section=logs",
      ),
    ).toBe("/cm/app/spa.jsp?entry=publish&section=logs");
    expect(
      resolveSpaReturnFromLocation("/cm/app/home/library", ""),
    ).toBe("/cm/app/spa.jsp?entry=home&section=library");
    expect(
      resolveSpaReturnFromLocation("/Rhythmyx/cm/app/workflow/roles", ""),
    ).toBe("/cm/app/spa.jsp?entry=workflow&tab=roles");
  });

  it("applyEntryQueryToPath rewrites spa.jsp?entry= to client path", () => {
    window.history.replaceState({}, "", "/cm/app/spa.jsp?entry=home&section=library");
    const next = applyEntryQueryToPath(
      "/cm/app/spa.jsp",
      "?entry=home&section=library",
      "/cm/app",
    );
    expect(next).toBe("/cm/app/home/library");
    expect(window.location.pathname).toBe("/cm/app/home/library");
  });

  afterEach(() => {
    window.history.replaceState({}, "", "/");
  });
});


