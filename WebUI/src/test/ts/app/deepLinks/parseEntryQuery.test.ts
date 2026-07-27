/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  parseEntryQuery,
  toSpaEntryUrl,
} from "../../../../main/ts/app/deepLinks/parseEntryQuery";

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
