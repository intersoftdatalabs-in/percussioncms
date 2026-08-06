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

  it("unknown entry falls back to home", () => {
    expect(parseEntryQuery("?entry=nope").entry).toBe("home");
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
    expect(parseClientPath("/workflow/users").tab).toBe("users");
    expect(parseClientPath("/admin/tools").clientPath).toBe("/admin/tools");
    expect(
      parseClientPath("/explorer", "?path=/Sites/demo").path,
    ).toBe("/Sites/demo");
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


