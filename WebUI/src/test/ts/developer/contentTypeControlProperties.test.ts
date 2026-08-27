/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  cloneControlProperties,
  controlPropertiesEqual,
  toControlPropertyPayload,
} from "../../../main/ts/developer/contentTypeControlProperties";

describe("contentTypeControlProperties helpers (CD-07)", () => {
  it("clones properties and normalizes missing values", () => {
    expect(cloneControlProperties([{ name: "height", value: "200" }])).toEqual([
      { name: "height", value: "200" },
    ]);
    expect(cloneControlProperties({ empty: true })).toEqual([]);
    expect(cloneControlProperties(null)).toEqual([]);
  });

  it("compares name/value lists", () => {
    const a = [{ name: "height", value: "200" }];
    expect(controlPropertiesEqual(a, [{ name: "height", value: "200" }])).toBe(true);
    expect(controlPropertiesEqual(a, [{ name: "height", value: "201" }])).toBe(false);
    expect(controlPropertiesEqual(a, [{ name: "width", value: "200" }])).toBe(false);
    expect(controlPropertiesEqual(a, [])).toBe(false);
  });

  it("drops blank names from the PUT payload", () => {
    expect(
      toControlPropertyPayload([
        { name: " height ", value: "200" },
        { name: "  ", value: "x" },
        { name: "width", value: undefined },
      ]),
    ).toEqual([
      { name: "height", value: "200" },
      { name: "width", value: "" },
    ]);
  });
});
