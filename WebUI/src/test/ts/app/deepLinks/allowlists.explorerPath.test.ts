/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import { normalizeExplorerPath } from "../../../../main/ts/app/deepLinks/allowlists";

describe("normalizeExplorerPath", () => {
  it("accepts ordinary site paths", () => {
    expect(normalizeExplorerPath("/Sites/Foo")).toBe("/Sites/Foo");
    expect(normalizeExplorerPath("/")).toBe("/");
  });

  it("allows folder names containing substring .. (not a segment)", () => {
    expect(normalizeExplorerPath("/Sites/foo..bar/baz")).toBe(
      "/Sites/foo..bar/baz",
    );
  });

  it("rejects parent-directory segments", () => {
    expect(normalizeExplorerPath("/Sites/../etc")).toBeUndefined();
    expect(normalizeExplorerPath("/../x")).toBeUndefined();
    expect(normalizeExplorerPath("/Sites/foo/../../x")).toBeUndefined();
  });

  it("rejects schemes and non-ASCII / special chars in deep-link charset", () => {
    expect(normalizeExplorerPath("http://evil")).toBeUndefined();
    expect(normalizeExplorerPath("/Sites/café")).toBeUndefined();
    expect(normalizeExplorerPath("/Sites/has space")).toBeUndefined();
  });

  it("rejects missing leading slash and overlong paths", () => {
    expect(normalizeExplorerPath("Sites/Foo")).toBeUndefined();
    expect(normalizeExplorerPath("/" + "a".repeat(2048))).toBeUndefined();
  });
});
